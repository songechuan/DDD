package cn.m2c.scm.port.adapter.restful.mini.goods;

import cn.m2c.common.JsonUtils;
import cn.m2c.common.MCode;
import cn.m2c.common.MResult;
import cn.m2c.scm.application.config.query.ConfigQueryApplication;
import cn.m2c.scm.application.goods.GoodsApplication;
import cn.m2c.scm.application.goods.query.GoodsGuaranteeQueryApplication;
import cn.m2c.scm.application.goods.query.GoodsQueryApplication;
import cn.m2c.scm.application.goods.query.data.bean.GoodsBean;
import cn.m2c.scm.application.goods.query.data.bean.GoodsGuaranteeBean;
import cn.m2c.scm.application.goods.query.data.representation.mini.MiniGoodsDetailRepresentation;
import cn.m2c.scm.application.shop.query.ShopQuery;
import cn.m2c.scm.application.special.data.bean.GoodsSpecialBean;
import cn.m2c.scm.application.special.query.GoodsSpecialQueryApplication;
import cn.m2c.scm.application.unit.query.UnitQuery;
import cn.m2c.scm.domain.service.goods.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商品
 */
@RestController
@RequestMapping("/goods/mini")
public class MiniGoodsAgent {
    private final static Logger LOGGER = LoggerFactory.getLogger(MiniGoodsAgent.class);

    @Autowired
    GoodsQueryApplication goodsQueryApplication;

    @Autowired
    GoodsGuaranteeQueryApplication goodsGuaranteeQueryApplication;

    @Autowired
    UnitQuery unitQuery;

    @Autowired
    GoodsApplication goodsApplication;

    @Autowired
    ShopQuery shopQuery;

    @Autowired
    GoodsSpecialQueryApplication goodsSpecialQueryApplication;

    @Autowired
    ConfigQueryApplication configQueryApplication;

    @Resource(name = "goodsDubboService")
    GoodsService goodsDubboService;

    /**
     * 微信小程序拍照获取商品
     *
     * @param recognizedInfo
     * @param barNo
     * @return
     */
    @RequestMapping(value = "/recognized", method = RequestMethod.GET)
    public ResponseEntity<MResult> miniRecognizedPic(
            @RequestParam(value = "recognizedInfo", required = false) String recognizedInfo,
            @RequestParam(value = "barNo", required = false) String barNo,
            @RequestParam(value = "searchSupplier", required = false, defaultValue = "") String searchSupplier
    ) {
        MResult result = new MResult(MCode.V_1);
        Map mediaMap = goodsDubboService.getMediaResourceInfo(barNo);
        String mediaId = null == mediaMap ? "" : (String) mediaMap.get("mediaId");
        String mediaName = null == mediaMap ? "" : (String) mediaMap.get("mediaName");
        String mresId = null == mediaMap ? "" : (String) mediaMap.get("mresId");
        String mresName = null == mediaMap ? "" : (String) mediaMap.get("mresName");

        try {
            List<GoodsBean> goodsBeans = goodsQueryApplication.recognizedGoods(recognizedInfo, null, searchSupplier);
            if (null != goodsBeans && goodsBeans.size() > 0) {
                List<MiniGoodsDetailRepresentation> representations = new ArrayList<>();
                for (GoodsBean goodsBean : goodsBeans) {
                    List<GoodsGuaranteeBean> goodsGuarantee = goodsGuaranteeQueryApplication.queryGoodsGuaranteeByIds(JsonUtils.toList(goodsBean.getGoodsGuarantee(), String.class));
                    String goodsUnitName = unitQuery.getUnitNameByUnitId(goodsBean.getGoodsUnitId());

                    //特惠价/优惠券
                    GoodsSpecialBean goodsSpecialBean = null;
                    //小程序拍照识别商品，查询特惠价
                    goodsSpecialBean = goodsSpecialQueryApplication.queryGoodsSpecialByGoodsId(goodsBean.getGoodsId());

                    MiniGoodsDetailRepresentation representation = new MiniGoodsDetailRepresentation(goodsBean,
                            goodsGuarantee, goodsUnitName, mresId, goodsSpecialBean);

                    representations.add(representation);
                }
                result.setContent(representations);
            }
            result.setStatus(MCode.V_200);
        } catch (Exception e) {
            LOGGER.error("miniRecognizedPic Exception e:", e);
            result = new MResult(MCode.V_400, "小程序拍照获取商品失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }

    /**
     * 小程序查询商品图文详情
     *
     * @param writer
     * @param goodsId
     */
    @RequestMapping(value = "/desc", method = RequestMethod.GET)
    public void miniGoodsDesc(Writer writer,
                              @RequestParam(value = "goodsId", required = true) String goodsId) {
        try {
            GoodsBean goodsBean = goodsQueryApplication.appGoodsDetailByGoodsId(goodsId);
            StringBuffer sb = new StringBuffer();
            sb.append("<!doctype html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no\" /><style>img{max-width:100%;border:0; margin : 0;padding :0 ;vertical-align:top;}</style>");
            sb.append(goodsBean.getGoodsDesc());
            writer.write(sb.toString());
            writer.close();
        } catch (IllegalArgumentException e) {
            LOGGER.error("miniGoodsDesc Exception e:", e);
        } catch (Exception e) {
            LOGGER.error("miniGoodsDesc Exception e:", e);
        }
    }
}
