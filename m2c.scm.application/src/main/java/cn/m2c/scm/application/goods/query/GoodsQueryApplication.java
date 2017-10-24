package cn.m2c.scm.application.goods.query;

import cn.m2c.common.JsonUtils;
import cn.m2c.common.RedisUtil;
import cn.m2c.ddd.common.port.adapter.persistence.springJdbc.SupportJdbcTemplate;
import cn.m2c.scm.application.classify.query.GoodsClassifyQueryApplication;
import cn.m2c.scm.application.goods.query.data.bean.GoodsBean;
import cn.m2c.scm.application.goods.query.data.bean.GoodsSkuBean;
import cn.m2c.scm.application.goods.query.data.representation.GoodsSkuInfoRepresentation;
import cn.m2c.scm.application.order.query.dto.GoodsDto;
import cn.m2c.scm.application.utils.Utils;
import cn.m2c.scm.domain.NegativeException;
import cn.m2c.scm.domain.service.goods.GoodsService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品查询
 */
@Service
public class GoodsQueryApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoodsQueryApplication.class);

    @Resource
    private SupportJdbcTemplate supportJdbcTemplate;

    public SupportJdbcTemplate getSupportJdbcTemplate() {
        return supportJdbcTemplate;
    }

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    GoodsClassifyQueryApplication goodsClassifyQueryApplication;
    @Resource(name = "goodsDubboService")
    GoodsService goodsDubboService;

    /**
     * 搜索
     * 商家平台搜索框：可按照商品名称、商品编码（商家自己的内部编码）、条形码、品牌查找，都是模糊搜索
     * 商家管理平台搜索框：可按照商品名称、平台sku（平台自己生成的内部唯一编码）、商家编码（商家自己的内部编码）、条形码、商家名称、品牌查找，都是模糊搜索
     *
     * @param dealerId
     * @param goodsClassifyId
     * @param goodsStatus
     * @param condition
     * @param startTime
     * @param endTime
     * @param pageNum
     * @param rows
     * @return
     */
    public List<GoodsBean> searchGoodsByCondition(String dealerId, String goodsClassifyId, Integer goodsStatus,
                                                  String condition, String startTime, String endTime,
                                                  Integer pageNum, Integer rows) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" g.* ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id ");
        if (StringUtils.isNotEmpty(dealerId)) {
            sql.append(" AND g.dealer_id = ? ");
            params.add(dealerId);
        }
        // 根据商品分类id,找到所有下级分类ID
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND g.goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (null != goodsStatus) {
            sql.append(" AND g.goods_status = ? ");
            params.add(goodsStatus);
        }
        if (StringUtils.isNotEmpty(condition)) {
            if (StringUtils.isNotEmpty(dealerId)) {//商家平台
                sql.append(" AND (g.goods_name LIKE ? OR g.goods_bar_code LIKE ? OR s.goods_code LIKE ? OR g.goods_brand_name LIKE ?)");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
            } else {//商家管理平台
                sql.append(" AND (s.sku_id LIKE ? OR g.goods_name LIKE ? OR g.goods_bar_code LIKE ? OR s.goods_code LIKE ? OR g.dealer_name LIKE ? OR g.goods_brand_name LIKE ? )");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
            }
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            sql.append(" AND g.created_date BETWEEN ? AND ?");
            params.add(startTime);
            params.add(endTime);
        }
        sql.append(" AND g.del_status= 1 group by g.goods_id ORDER BY g.created_date DESC ");
        sql.append(" LIMIT ?,?");
        params.add(rows * (pageNum - 1));
        params.add(rows);

        List<GoodsBean> goodsBeanList = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, params.toArray());
        if (null != goodsBeanList && goodsBeanList.size() > 0) {
            for (GoodsBean goodsBean : goodsBeanList) {
                goodsBean.setGoodsSkuBeans(queryGoodsSKUsByGoodsId(goodsBean.getId()));
            }
        }
        return goodsBeanList;
    }

    public Integer searchGoodsByConditionTotal(String dealerId, String goodsClassifyId, Integer goodsStatus,
                                               String condition, String startTime, String endTime) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" count(distinct g.goods_id) ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id ");
        if (StringUtils.isNotEmpty(dealerId)) {
            sql.append(" AND g.dealer_id = ? ");
            params.add(dealerId);
        }
        // 根据商品分类id,找到所有下级分类ID
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND g.goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (null != goodsStatus) {
            sql.append(" AND g.goods_status = ? ");
            params.add(goodsStatus);
        }
        if (StringUtils.isNotEmpty(condition)) {
            if (StringUtils.isNotEmpty(dealerId)) {//商家平台
                sql.append(" AND (g.goods_name LIKE ? OR g.goods_bar_code LIKE ? OR s.goods_code LIKE ? OR g.goods_brand_name LIKE ?)");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
            } else {//商家管理平台
                sql.append(" AND (s.sku_id LIKE ? OR g.goods_name LIKE ? OR g.goods_bar_code LIKE ? OR s.goods_code LIKE ? OR g.dealer_name LIKE ? OR g.goods_brand_name LIKE ? )");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
            }
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            sql.append(" AND g.created_date BETWEEN ? AND ?");
            params.add(startTime);
            params.add(endTime);
        }
        sql.append(" AND g.del_status= 1");

        return supportJdbcTemplate.jdbcTemplate().queryForObject(sql.toString(), params.toArray(), Integer.class);
    }

    public List<GoodsSkuBean> queryGoodsSKUsByGoodsId(Integer goodsId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods_sku WHERE 1 = 1 AND goods_id = ?");
        return this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsSkuBean.class, goodsId);
    }

    public GoodsBean queryGoodsById(Integer id) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods WHERE 1 = 1 AND id = ?");
        GoodsBean goodsBean = this.getSupportJdbcTemplate().queryForBean(sql.toString(), GoodsBean.class, id);
        if (null != goodsBean) {
            goodsBean.setGoodsSkuBeans(queryGoodsSKUsByGoodsId(id));
        }
        return goodsBean;
    }

    public List<GoodsBean> goodsChoice(String goodsClassifyId, String condition,
                                       Integer pageNum, Integer rows) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" g.* ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id");
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND g.goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (StringUtils.isNotEmpty(condition)) {
            sql.append(" AND g.goods_name like ? ");
            params.add("%" + condition + "%");
        }
        sql.append(" AND g.del_status= 1 group by g.goods_id ORDER BY g.created_date desc,s.photograph_price desc ");
        sql.append(" LIMIT ?,?");
        params.add(rows * (pageNum - 1));
        params.add(rows);

        List<GoodsBean> goodsBeanList = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, params.toArray());
        if (null != goodsBeanList && goodsBeanList.size() > 0) {
            for (GoodsBean goodsBean : goodsBeanList) {
                goodsBean.setGoodsSkuBeans(queryGoodsSKUsByGoodsId(goodsBean.getId()));
            }
        }
        return goodsBeanList;
    }

    public Integer goodsChoiceTotal(String goodsClassifyId, String condition) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" count(distinct g.goods_id) ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id");
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND g.goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (StringUtils.isNotEmpty(condition)) {
            sql.append(" AND g.goods_name like ? ");
            params.add("%" + condition + "%");
        }
        sql.append(" AND g.del_status= 1");
        return supportJdbcTemplate.jdbcTemplate().queryForObject(sql.toString(), params.toArray(), Integer.class);
    }

    public GoodsBean queryGoodsByGoodsId(String goodsId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods WHERE 1 = 1 AND goods_id = ?");
        GoodsBean goodsBean = this.getSupportJdbcTemplate().queryForBean(sql.toString(), GoodsBean.class, goodsId);
        if (null != goodsBean) {
            goodsBean.setGoodsSkuBeans(queryGoodsSKUsByGoodsId(goodsBean.getId()));
        }
        return goodsBean;
    }

    public List<GoodsBean> queryGoodsByGoodsIds(List<String> goodsIds) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods where goods_id in (" + Utils.listParseString(goodsIds) + ") AND del_status = 1");
        return this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class);
    }

    public List<GoodsBean> queryGoodsRandom(Integer number) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods where del_status= 1 order by rand() limit 0,?");
        List<GoodsBean> goodsBeans = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, number);
        return goodsBeans;
    }

    public List<String> queryGoodsKeyWordRandom(Integer number) {
        List<String> keyWords = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods where del_status= 1 and goods_key_word is not null order by rand() limit 0,?");
        List<GoodsBean> goodsBeans = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, number);
        List<String> tempList = new ArrayList<>();
        if (null != goodsBeans && goodsBeans.size() > 0) {
            for (GoodsBean goodsBean : goodsBeans) {
                String keyWord = goodsBean.getGoodsKeyWord();
                List<String> tempKeyWord = JsonUtils.toList(keyWord, String.class);
                tempList.addAll(tempKeyWord);
            }
            if (tempList.size() > number) {
                keyWords = tempList.subList(0, number);
            } else {
                keyWords = tempList;
            }
        }
        return keyWords;
    }

    public List<GoodsBean> queryGoodsGuessCache(Integer positionType) {
        //猜你喜欢位置：1:首页(共32条，分页，每页8条，分4页) 2:购物车页面（共12条，不需分页）3:商品详情页（共16条，不需分页）4:搜索结果页(共32条，分页，每页8条，分4页)
        Integer number = 32;
        if (positionType == 2) {
            number = 12;
        }
        if (positionType == 3) {
            number = 16;
        }

        List<GoodsBean> goodsBeans = new ArrayList<>();
        String key = "scm.goods.guess." + positionType;
        String guess = redisUtil.getString(key); //从缓存中取数据
        if (StringUtils.isNotEmpty(guess)) { // 缓存不为空
            List<GoodsBean> guessInfoList = JsonUtils.toList(guess, GoodsBean.class);
            if (guessInfoList.size() < number) { //随机取剩余部分
                List<String> goodsIds = new ArrayList<>();
                for (GoodsBean goodsBean : guessInfoList) {
                    goodsIds.add(goodsBean.getGoodsId());
                }
                guessInfoList.addAll(queryGoodsGuess((number - guessInfoList.size()), positionType, goodsIds));
                redisUtil.setString(key, 24 * 3600, JsonUtils.toStr(guessInfoList));
            }
            goodsBeans = guessInfoList;
        } else {
            goodsBeans = queryGoodsGuess(number, positionType, null);
        }
        return goodsBeans;
    }

    private List<GoodsBean> queryGoodsGuess(Integer number, Integer positionType, List<String> goodsIds) {
        String key = "scm.goods.guess." + positionType;
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods where del_status= 1 ");
        if (null != goodsIds && goodsIds.size() > 0) {
            sql.append(" and goods_id not in (" + Utils.listParseString(goodsIds) + ")");
        }
        sql.append(" order by rand() limit 0,?");
        List<GoodsBean> goodsBeans = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, number);
        if (null == goodsIds) {
            if (null != goodsBeans && goodsBeans.size() > 0) {
                redisUtil.setString(key, 24 * 3600, JsonUtils.toStr(goodsBeans));
            }
        }
        return goodsBeans;
    }

    /**
     * list 分页
     *
     * @param pageNum
     * @param rows
     * @param goodsBeans
     * @return
     */
    public List<GoodsBean> getPagedList(Integer pageNum, Integer rows, List<GoodsBean> goodsBeans) {
        List<GoodsBean> goodsBeanPage = new ArrayList<GoodsBean>();
        int currIdx = (pageNum > 1 ? (pageNum - 1) * rows : 0);
        for (int i = 0; i < rows && i < goodsBeans.size() - currIdx; i++) {
            GoodsBean memberArticleBean = goodsBeans.get(currIdx + i);
            goodsBeanPage.add(memberArticleBean);
        }
        return goodsBeanPage;
    }

    /**
     * 根据skuIds查询商品
     *
     * @param skuIds
     * @return
     */
    public List<GoodsSkuInfoRepresentation> queryGoodsBySkuIds(List<String> skuIds) {
        List<GoodsSkuInfoRepresentation> resultList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods_sku where sku_id in (" + Utils.listParseString(skuIds) + ")");
        List<GoodsSkuBean> goodsSkuBeans = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsSkuBean.class);
        if (null != goodsSkuBeans && goodsSkuBeans.size() > 0) {
            for (GoodsSkuBean goodsSkuBean : goodsSkuBeans) {
                GoodsBean goodsBean = queryGoodsById(goodsSkuBean.getGoodsId());
                if (null != goodsBean) {
                    resultList.add(new GoodsSkuInfoRepresentation(goodsBean, goodsSkuBean));
                }
            }
        }
        return resultList;
    }

    /**
     * 获取拍获价最高的商品
     *
     * @param goodsIds
     * @return
     */
    public GoodsSkuInfoRepresentation queryMaxPriceGoodsByGoodsIds(List<String> goodsIds) {
        List<GoodsBean> goodsBeans = queryGoodsByGoodsIds(goodsIds);
        if (null != goodsBeans && goodsBeans.size() > 0) {
            List<GoodsSkuInfoRepresentation> resultList = new ArrayList<>();
            for (GoodsBean goodsBean : goodsBeans) {
                List<GoodsSkuBean> goodsSkuBeans = queryGoodsSKUsByGoodsId(goodsBean.getId());
                if (null != goodsSkuBeans && goodsSkuBeans.size() > 0) {
                    //排序
                    Collections.sort(goodsSkuBeans, new Comparator<GoodsSkuBean>() {
                        public int compare(GoodsSkuBean bean1, GoodsSkuBean bean2) {
                            Long price1 = bean1.getPhotographPrice();
                            Long price2 = bean2.getPhotographPrice();
                            if (price1 > price2) {
                                return 1;
                            } else if (price1 == price2) {
                                return 0;
                            } else {
                                return -1;
                            }
                        }
                    });
                    resultList.add(new GoodsSkuInfoRepresentation(goodsBean, goodsSkuBeans.get(goodsSkuBeans.size() - 1)));
                }
            }
            if (null != resultList && resultList.size() > 0) {
                //排序
                Collections.sort(resultList, new Comparator<GoodsSkuInfoRepresentation>() {
                    public int compare(GoodsSkuInfoRepresentation bean1, GoodsSkuInfoRepresentation bean2) {
                        Long price1 = bean1.getPhotographPrice();
                        Long price2 = bean2.getPhotographPrice();
                        if (price1 > price2) {
                            return 1;
                        } else if (price1 == price2) {
                            return 0;
                        } else {
                            return -1;
                        }
                    }
                });
                return resultList.get(resultList.size() - 1);
            }
        }
        return null;
    }

    /***
     * 获取商品列表
     *
     * @param skuIds
     * @return
     */
    public List<GoodsDto> getGoodsDtl(Set<String> skuIds) throws NegativeException {
        if (skuIds == null || skuIds.size() < 1)
            return null;
        try {
            StringBuilder sql = new StringBuilder(512);
            sql.append("select a.goods_id as goodsId")
                    .append(", a.goods_name as goodsName")
                    .append(", a.goods_sub_title as goodsTitle")
                    .append(", a.goods_classify_id as goodsTypeId")
                    .append(", c.unit_name as goodsUnit")
                    .append(", b.sku_id as skuId")
                    .append(", b.sku_name as skuName")
                    .append(", a.goods_main_images as goodsIcon")
                    .append(", d.service_rate as rate")
                    .append(", d.classify_name as goodsType")
                    .append(", b.weight, b.dealer_id as dealerId")
                    .append(", b.supply_price as supplyPrice")
                    .append(", b.market_price as price")
                    .append(", b.photograph_price as discountPrice")
                    .append(" from t_scm_goods_sku b, t_scm_goods a")
                    .append(" left outer join t_scm_unit c on a.goods_unit_id=c.unit_id")
                    .append(" left outer join t_scm_goods_classify d on a.goods_classify_id=d.classify_id")
                    .append(" where a.id=b.goods_id ")
                    .append(" and b.sku_id in(");
            int sz = skuIds.size();
            for (int i = 0; i < sz; i++) {
                if (i > 0)
                    sql.append(",?");
                else
                    sql.append("?");
            }
            sql.append(")");
            Object[] args = new Object[skuIds.size()];
            return supportJdbcTemplate.queryForBeanList(sql.toString(), GoodsDto.class, skuIds.toArray(args));
        } catch (Exception e) {
            LOGGER.error("===fanjc==订单获取商品详情出错", e);
            throw new NegativeException(500, "获取商品详情列表出错");
        }
    }

    public GoodsBean appGoodsDetailByGoodsId(String goodsId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods WHERE 1 = 1 AND goods_id = ?");
        GoodsBean goodsBean = this.getSupportJdbcTemplate().queryForBean(sql.toString(), GoodsBean.class, goodsId);
        if (null != goodsBean) {
            goodsBean.setGoodsSkuBeans(queryShowGoodsSKUsByGoodsId(goodsBean.getId()));
        }
        return goodsBean;
    }

    public List<GoodsSkuBean> queryShowGoodsSKUsByGoodsId(Integer goodsId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods_sku WHERE 1 = 1 AND goods_id = ? AND show_status = 2");
        return this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsSkuBean.class, goodsId);
    }

    public List<GoodsBean> appSearchGoods(String goodsClassifyId, String condition, Integer sortType,
                                          Integer sort, Integer pageNum, Integer rows) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" g.* ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id");
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            // 查询所有一级分类的下级分类
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (StringUtils.isNotEmpty(condition)) {
            //商品标题、商品副标题、SKU、品牌、所属分类、商品关键词、商品图文详情文本内容
            sql.append(" AND (g.goods_name LIKE ? OR g.goods_sub_title LIKE ? OR s.sku_id LIKE ? OR g.goods_brand_name LIKE ? OR g.goods_key_word LIKE ? OR g.goods_desc LIKE ?");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.getGoodsSubClassifyIdByName(condition);
            if (null != goodsClassifyIds && goodsClassifyIds.size() > 0) {
                sql.append("  OR g.goods_classify_id in ?");
                params.add("(" + Utils.listParseString(goodsClassifyIds) + ")");
            }
            sql.append(")");
        }
        sql.append(" AND g.del_status= 1 group by g.goods_id");

        //sortType 排序类型：1：综合，2：价格
        //sort 1：降序，2：升序
        if (null != sortType && sortType == 1) {//综合-以商品标题为第一优先级进行搜索结果展示；非该优先级的商品按创建时间降序排列，创建时间相同情况下按价格降序排序（默认综合）
            sql.append(" ORDER BY g.created_date desc,s.photograph_price desc ");
        } else if (null != sortType && sortType == 2) {//价格-首次点击价格降序排列，价格相同创建时间早的优先；再次点击价格升序排列
            if (null != sort && sort == 1) {
                sql.append(" ORDER BY s.photograph_price desc,g.created_date desc");
            } else if (null != sort && sort == 2) {
                sql.append(" ORDER BY s.photograph_price asc,g.created_date asc");
            }
        }
        sql.append(" LIMIT ?,?");
        params.add(rows * (pageNum - 1));
        params.add(rows);
        return this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, params.toArray());
    }

    public Integer appSearchGoodsTotal(String goodsClassifyId, String condition) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" count(distinct g.goods_id) ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id");
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            // 查询所有一级分类的下级分类
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (StringUtils.isNotEmpty(condition)) {
            //商品标题、商品副标题、SKU、品牌、所属分类、商品关键词、商品图文详情文本内容
            sql.append(" AND (g.goods_name LIKE ? OR g.goods_sub_title LIKE ? OR s.sku_id LIKE ? OR g.goods_brand_name LIKE ? OR g.goods_key_word LIKE ? OR g.goods_desc LIKE ?");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            params.add("%" + condition + "%");
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.getGoodsSubClassifyIdByName(condition);
            if (null != goodsClassifyIds && goodsClassifyIds.size() > 0) {
                sql.append("  OR g.goods_classify_id in ?");
                params.add("(" + Utils.listParseString(goodsClassifyIds) + ")");
            }
            sql.append(")");
        }
        sql.append(" AND g.del_status= 1");
        return supportJdbcTemplate.jdbcTemplate().queryForObject(sql.toString(), params.toArray(), Integer.class);
    }


    public List<GoodsBean> searchGoodsExport(String dealerId, String goodsClassifyId, Integer goodsStatus,
                                             String condition, String startTime, String endTime) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" g.* ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods g,t_scm_goods_sku s WHERE g.id=s.goods_id");
        if (StringUtils.isNotEmpty(dealerId)) {
            sql.append(" AND g.dealer_id = ? ");
            params.add(dealerId);
        }
        // 根据商品分类id,找到所有下级分类ID
        if (StringUtils.isNotEmpty(goodsClassifyId)) {
            List<String> goodsClassifyIds = goodsClassifyQueryApplication.recursionQueryGoodsSubClassifyId(goodsClassifyId, new ArrayList<String>());
            goodsClassifyIds.add(goodsClassifyId);
            sql.append(" AND g.goods_classify_id in (" + Utils.listParseString(goodsClassifyIds) + ") ");
        }
        if (null != goodsStatus) {
            sql.append(" AND g.goods_status = ? ");
            params.add(goodsStatus);
        }
        if (StringUtils.isNotEmpty(condition)) {
            if (StringUtils.isNotEmpty(dealerId)) {//商家平台
                sql.append(" AND (g.goods_name LIKE ? OR g.goods_bar_code LIKE ? OR s.goods_code LIKE ? OR g.goods_brand_name LIKE ?)");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
            } else {//商家管理平台
                sql.append(" AND (s.sku_id LIKE ? OR g.goods_name LIKE ? OR g.goods_bar_code LIKE ? OR s.goods_code LIKE ? OR g.dealer_name LIKE ? OR g.goods_brand_name LIKE ? )");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
                params.add("%" + condition + "%");
            }
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            sql.append(" AND g.created_date BETWEEN ? AND ?");
            params.add(startTime);
            params.add(endTime);
        }
        sql.append(" AND g.del_status= 1 group by goods_id");
        List<GoodsBean> goodsBeanList = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class, params.toArray());
        if (null != goodsBeanList && goodsBeanList.size() > 0) {
            for (GoodsBean goodsBean : goodsBeanList) {
                goodsBean.setGoodsSkuBeans(queryGoodsSKUsByGoodsId(goodsBean.getId()));
            }
        }
        return goodsBeanList;
    }

    public List<GoodsBean> recognizedGoods(String recognizedInfo, String location) {
        //解析json
        List<Map<String, Object>> recognizedList = new Gson().fromJson(recognizedInfo, new TypeToken<List<Map<String, Object>>>() {
        }.getType());
        List<Double> scoreList = new ArrayList<Double>();
        List<String> recognizedIds = new ArrayList<String>();
        //识别图片列表逻辑
        if (null != recognizedList && recognizedList.size() > 0) {
            for (Map<String, Object> map : recognizedList) {
                Double score = (Double) map.get("score");
                scoreList.add(score);
            }
            //判断识别率高低
            Collections.sort(scoreList);//升序排序
            Collections.reverse(scoreList);//倒序
            if (scoreList.get(0) > 0.75) {
                recognizedIds.add((String) recognizedList.get(0).get("recognizedId"));
                return queryGoodsByRecognizedIds(recognizedIds);
            } else if (scoreList.get(0) > 0.01) {
                //取前10条
                for (int j = 0; j < scoreList.size() && j < 10; j++) {
                    if (scoreList.get(j) > 0.01)
                        recognizedIds.add((String) recognizedList.get(j).get("recognizedId"));
                }
                return queryGoodsByRecognizedIds(recognizedIds);
            }
        }

        Map<String, Object> locationInfo = new Gson().fromJson(location, new TypeToken<Map<String, Object>>() {
        }.getType());
        if (null != locationInfo) {
            Double longitude = Double.parseDouble((String) locationInfo.get("longitude"));
            Double latitude = Double.parseDouble((String) locationInfo.get("latitude"));
            //根据经纬度坐标查询商品ID
            List<String> goodsIdList = goodsDubboService.getGoodsIdByCoordinate(longitude, latitude);
            if (null != goodsIdList && goodsIdList.size() > 0) {
                return queryGoodsByGoodsIds(goodsIdList);
            }
        }
        return null;
    }

    public List<GoodsBean> queryGoodsByRecognizedIds(List<String> recognizedIds) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods WHERE 1 = 1 AND goods_id IN (" + Utils.listParseString(recognizedIds) + ")");
        List<GoodsBean> goodsBeanList = this.getSupportJdbcTemplate().queryForBeanList(sql.toString(), GoodsBean.class);
        if (null != goodsBeanList && goodsBeanList.size() > 0) {
            for (GoodsBean goodsBean : goodsBeanList) {
                goodsBean.setGoodsSkuBeans(queryGoodsSKUsByGoodsId(goodsBean.getId()));
            }
        }
        return goodsBeanList;
    }

    /**
     * 根据skuId查询商品
     *
     * @param skuId
     * @return
     */
    public GoodsSkuInfoRepresentation queryGoodsBySkuId(String skuId) {
        List<GoodsSkuInfoRepresentation> resultList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ");
        sql.append(" * ");
        sql.append(" FROM ");
        sql.append(" t_scm_goods_sku where sku_id = ?");
        GoodsSkuBean goodsSkuBean = this.getSupportJdbcTemplate().queryForBean(sql.toString(), GoodsSkuBean.class, skuId);
        if (null != goodsSkuBean) {
            GoodsBean goodsBean = queryGoodsById(goodsSkuBean.getGoodsId());
            if (null != goodsBean) {
                return new GoodsSkuInfoRepresentation(goodsBean, goodsSkuBean);
            }
        }
        return null;
    }
}

