package cn.m2c.scm.port.adapter.restful.web.goods;

import cn.m2c.common.MCode;
import cn.m2c.common.MPager;
import cn.m2c.common.MResult;
import cn.m2c.scm.application.classify.data.bean.GoodsClassifyBean;
import cn.m2c.scm.application.classify.query.GoodsClassifyQueryApplication;
import cn.m2c.scm.application.dealer.data.bean.DealerBean;
import cn.m2c.scm.application.dealer.query.DealerQuery;
import cn.m2c.scm.application.goods.query.GoodsQueryApplication;
import cn.m2c.scm.application.goods.query.data.bean.GoodsBean;
import cn.m2c.scm.application.goods.query.data.representation.GoodsChoiceRepresentation;
import cn.m2c.scm.application.goods.query.data.representation.GoodsDetailMultipleRepresentation;
import cn.m2c.scm.application.goods.query.data.representation.GoodsInformationRepresentation;
import cn.m2c.scm.application.goods.query.data.representation.GoodsRandomRepresentation;
import cn.m2c.scm.application.goods.query.data.representation.GoodsSimpleDetailRepresentation;
import cn.m2c.scm.application.goods.query.data.representation.GoodsSkuInfoRepresentation;
import cn.m2c.scm.domain.util.GetDisconfDataGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品查询(提供出去的)
 */
@RestController
@RequestMapping("/goods")
public class GoodsQueryAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoodsQueryAgent.class);

    @Autowired
    GoodsQueryApplication goodsQueryApplication;
    @Autowired
    DealerQuery dealerQuery;
    @Autowired
    GoodsClassifyQueryApplication goodsClassifyQueryApplication;

    /**
     * 商品筛选根据商品类别，名称、标题、编号筛选
     *
     * @param goodsClassifyId 商品类别
     * @param condition       名称、标题、编号
     * @param pageNum         第几页
     * @param rows            每页多少行
     * @return
     */
    @RequestMapping(value = "/choice", method = RequestMethod.GET)
    public ResponseEntity<MPager> goodsChoice(
            @RequestParam(value = "dealerId", required = false) String dealerId,
            @RequestParam(value = "goodsClassifyId", required = false) String goodsClassifyId,
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "rows", required = false, defaultValue = "10") Integer rows) {
        MPager result = new MPager(MCode.V_1);
        try {
            Integer total = goodsQueryApplication.goodsChoiceTotal(dealerId, goodsClassifyId, condition);
            if(total<=(pageNum-1)*rows){//页面切换有问题
        		pageNum = 1;
        	}
            if (total > 0) {
                List<GoodsBean> goodsBeans = goodsQueryApplication.goodsChoice(dealerId, goodsClassifyId,
                        condition, pageNum, rows);
                if (null != goodsBeans && goodsBeans.size() > 0) {
                    List<GoodsChoiceRepresentation> representations = new ArrayList<GoodsChoiceRepresentation>();
                    for (GoodsBean bean : goodsBeans) {
                        String shopName = "";
                        List<DealerBean> list = dealerQuery.getDealers(bean.getDealerId());
                        if (null != list && list.size() > 0) {
                            shopName = list.get(0).getShopName();
                        }
                        representations.add(new GoodsChoiceRepresentation(bean, shopName));
                    }
                    result.setContent(representations);
                }
            }
            result.setPager(total, pageNum, rows);
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("goods choice Exception e:", e);
            result = new MPager(MCode.V_400, "筛选商品失败");
        }
        return new ResponseEntity<MPager>(result, HttpStatus.OK);
    }

    /**
     * 商品详情
     *
     * @param goodsId 商品ID
     * @return
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public ResponseEntity<MResult> goodsDetail(
            @RequestParam(value = "goodsId", required = false) String goodsId) {
        MResult result = new MResult(MCode.V_1);
        try {
            GoodsBean goodsBean = goodsQueryApplication.queryGoodsByGoodsId(goodsId);
            if (null != goodsBean) {
                GoodsClassifyBean goodsClassifyBean = goodsClassifyQueryApplication.queryGoodsClassifiesById(goodsBean.getGoodsClassifyId());
                String classifyName = null != goodsClassifyBean ? goodsClassifyBean.getClassifyName() : "";
                GoodsSimpleDetailRepresentation representation = new GoodsSimpleDetailRepresentation(goodsBean, classifyName);
                result.setContent(representation);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("goods Detail Exception e:", e);
            result = new MResult(MCode.V_400, "查询商品详情失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    /**
     * 多个商品详情
     *
     * @param goodsIds 多个商品ID逗号分隔
     * @return
     */
    @RequestMapping(value = "/detail/multiple", method = RequestMethod.GET)
    public ResponseEntity<MResult> goodsDetails(
            @RequestParam(value = "goodsIds", required = false) List goodsIds) {
        MResult result = new MResult(MCode.V_1);
        try {
            List<GoodsBean> goodsBeanList = goodsQueryApplication.queryGoodsByGoodsIds(goodsIds);
            if (null != goodsBeanList && goodsBeanList.size() > 0) {
                List<GoodsDetailMultipleRepresentation> resultList = new ArrayList<>();
                for (GoodsBean goodsBean : goodsBeanList) {
                    resultList.add(new GoodsDetailMultipleRepresentation(goodsBean));
                }
                result.setContent(resultList);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("goodsDetails Exception e:", e);
            result = new MResult(MCode.V_400, "查询商品详情失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    /**
     * 随机取商品
     *
     * @param number 随机取商品的数量
     * @return
     */
    @RequestMapping(value = "/random", method = RequestMethod.GET)
    public ResponseEntity<MResult> goodsRandom(
            @RequestParam(value = "number", required = false, defaultValue = "10") Integer number) {
        MResult result = new MResult(MCode.V_1);
        try {
            List<GoodsBean> goodsBeanList = goodsQueryApplication.queryGoodsRandom(number);
            if (null != goodsBeanList && goodsBeanList.size() > 0) {
                List<GoodsRandomRepresentation> resultList = new ArrayList<>();
                for (GoodsBean goodsBean : goodsBeanList) {
                    resultList.add(new GoodsRandomRepresentation(goodsBean));
                }
                result.setContent(resultList);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("goodsRandom Exception e:", e);
            result = new MResult(MCode.V_400, "随机查询商品失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    /**
     * 随机取商品关键字
     *
     * @param number 随机取商品关键字的数量
     * @return
     */
    @RequestMapping(value = "/keyword/random", method = RequestMethod.GET)
    public ResponseEntity<MResult> goodsKeyWordRandom(
            @RequestParam(value = "number", required = false, defaultValue = "10") Integer number) {
        MResult result = new MResult(MCode.V_1);
        try {
            List<String> list = goodsQueryApplication.queryGoodsKeyWordRandom(number);
            result.setContent(list);
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("goodsKeyWordRandom Exception e:", e);
            result = new MResult(MCode.V_400, "随机查询商品关键字失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/sku/ids", method = RequestMethod.GET)
    public ResponseEntity<MResult> queryGoodsBySkuIds(
            @RequestParam(value = "skuIds", required = false) List<String> skuIds) {
        MResult result = new MResult(MCode.V_1);
        try {
            List<GoodsSkuInfoRepresentation> list = goodsQueryApplication.queryGoodsBySkuIds(skuIds);
            if (null != list && list.size() > 0) {
                result.setContent(list);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("queryGoodsBySkuIds Exception e:", e);
            result = new MResult(MCode.V_400, "根据skuIds查询商品失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/max/price", method = RequestMethod.GET)
    public ResponseEntity<MResult> queryMaxPriceGoodsByGoodsIds(
            @RequestParam(value = "goodsIds", required = false) List<String> goodsIds) {
        MResult result = new MResult(MCode.V_1);
        try {
            GoodsSkuInfoRepresentation representation = goodsQueryApplication.queryMaxPriceGoodsByGoodsIds(goodsIds);
            if (null != representation) {
                result.setContent(representation);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("queryMaxPriceGoodsByGoodsIds Exception e:", e);
            result = new MResult(MCode.V_400, "查询拍获价最大的商品失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<MResult> queryGoodsStatusByGoodsIds(
            @RequestParam(value = "goodsIds", required = false) List<String> goodsIds) {
        MResult result = new MResult(MCode.V_1);
        try {
            List<GoodsBean> goodsBeans = goodsQueryApplication.queryAllGoodsByGoodsIds(goodsIds);
            if (null != goodsBeans && goodsBeans.size() > 0) {
                Map map = new HashMap<>();
                for (GoodsBean goodsBean : goodsBeans) {
                    Integer status = goodsBean.getGoodsStatus(); //商品状态，1：仓库中，2：出售中，3：已售罄
                    Integer delStatus = goodsBean.getDelStatus(); //是否删除，1:正常，2：已删除
                    if (delStatus == 2) {
                        status = 4;
                    }
                    map.put(goodsBean.getGoodsId(), status);
                }
                result.setContent(map);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("queryGoodsStatusByGoodsIds Exception e:", e);
            result = new MResult(MCode.V_400, "查询商品的状态失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    /**
     * 根据商品Id查询识别图id和url(支持多个)
     *
     * @param goodsIds 多个商品ID逗号分隔
     * @return
     */
    @RequestMapping(value = "/recognizeds", method = RequestMethod.GET)
    public ResponseEntity<MPager> queryRecognizedsByGoodsIds(
            @RequestParam(value = "goodsIds", required = false) List<String> goodsIds) {
        MPager result = new MPager(MCode.V_1);
        try {
            List<GoodsBean> goodsBeans = goodsQueryApplication.queryAllGoodsByGoodsIds(goodsIds);
            if (null != goodsBeans && goodsBeans.size() > 0) {
                List<GoodsInformationRepresentation> resultList = new ArrayList<>();
                for (GoodsBean goodsBean : goodsBeans) {
                    resultList.add(new GoodsInformationRepresentation(goodsBean));
                }
                result.setContent(resultList);
            }
            result.setStatus(MCode.V_200);
            // 商品识别图限制
            Integer recognizedMax = Integer.parseInt(GetDisconfDataGetter.getDisconfProperty("goods.recognized.upload.max.limit"));
            result.setTotalCount(recognizedMax);
        } catch (Exception e) {
            LOGGER.error("queryRecognizedsByGoodsIds Exception e:", e);
            result = new MPager(MCode.V_400, "查询商品识别图失败");
        }
        return new ResponseEntity<MPager>(result, HttpStatus.OK);
    }

    /**
     * 根据商品名/ID,商家名/ID,商品是否投放,查询商品详情
     *
     * @param goodsMessage      商品信息(商品id或商品名)
     * @param dealerMessage     商家信息(商家id或商家名)
     * @param goodsLaunchStatus 商品投放状态(0:未投放, 1:投放)
     * @param pageOrNot         是否分页(0:不分页, 1:分页)
     * @param pageNum           第几页
     * @param rows              每页多少行
     * @return
     */
    @RequestMapping(value = "/information", method = RequestMethod.GET)
    public ResponseEntity<MPager> queryGoodsDetailByGoodsAndDealer(
            @RequestParam(value = "goodsMessage", required = false) String goodsMessage,
            @RequestParam(value = "dealerMessage", required = false) String dealerMessage,
            @RequestParam(value = "goodsLaunchStatus", required = false) Integer goodsLaunchStatus,
            @RequestParam(value = "pageOrNot", required = false, defaultValue = "0") Integer pageOrNot,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "rows", required = false, defaultValue = "5") Integer rows) {
        MPager result = new MPager(MCode.V_1);
        try {
            Integer total = goodsQueryApplication.queryGoodsByGoodOrDealerTotal(goodsMessage, dealerMessage, goodsLaunchStatus);
            if (total > 0) {
                List<GoodsBean> goodsBeans = goodsQueryApplication.queryGoodsByGoodOrDealer(goodsMessage, dealerMessage, goodsLaunchStatus, pageOrNot, pageNum, rows);
                if (null != goodsBeans && goodsBeans.size() > 0) {
                    List<GoodsInformationRepresentation> resultList = new ArrayList<GoodsInformationRepresentation>();
                    for (GoodsBean goodsBean : goodsBeans) {
                        resultList.add(new GoodsInformationRepresentation(goodsBean));
                    }
                    result.setContent(resultList);
                }
            }
            result.setPager(total, pageNum, rows);
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("queryGoodsDetailByGoodsAndDealer Exception e:", e);
            result = new MPager(MCode.V_400, "查询商品信息失败");
        }
        return new ResponseEntity<MPager>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/recognized/id", method = RequestMethod.GET)
    public ResponseEntity<MResult> getRecognizedGoods() {
        MResult result = new MResult(MCode.V_1);
        try {
            List<String> recognizedIds = goodsQueryApplication.getRecognizedGoods();
            if (null != recognizedIds && recognizedIds.size() > 0) {
                result.setContent(recognizedIds);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("getRecognizedGoods Exception e:", e);
            result = new MResult(MCode.V_400, "查询商品识别图id失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    /**
     * 根据商品名称/商家名称筛选有识别图的且商品状态为出售中、仓库中商品
     *
     * @param condition 商品名称/商家名称
     * @param pageNum   第几页
     * @param rows      每页多少行
     * @return
     */
    @RequestMapping(value = "/choice/recognized", method = RequestMethod.GET)
    public ResponseEntity<MPager> goodsChoiceRecognized(
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "rows", required = false, defaultValue = "10") Integer rows) {
        MPager result = new MPager(MCode.V_1);
        try {
            Integer total = goodsQueryApplication.goodsChoiceRecognizedTotal(condition);
            if (total > 0) {
                List<GoodsBean> goodsBeans = goodsQueryApplication.goodsChoiceRecognized(condition, pageNum, rows);
                if (null != goodsBeans && goodsBeans.size() > 0) {
                    List<GoodsChoiceRepresentation> representations = new ArrayList<GoodsChoiceRepresentation>();
                    for (GoodsBean bean : goodsBeans) {
                        String shopName = "";
                        List<DealerBean> list = dealerQuery.getDealers(bean.getDealerId());
                        if (null != list && list.size() > 0) {
                            shopName = list.get(0).getShopName();
                        }
                        representations.add(new GoodsChoiceRepresentation(bean, shopName));
                    }
                    result.setContent(representations);
                }
            }
            result.setPager(total, pageNum, rows);
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("goodsChoiceRecognized Exception e:", e);
            result = new MPager(MCode.V_400, "筛选商品失败");
        }
        return new ResponseEntity<MPager>(result, HttpStatus.OK);
    }
}
