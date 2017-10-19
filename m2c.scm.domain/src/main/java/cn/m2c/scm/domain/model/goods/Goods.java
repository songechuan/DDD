package cn.m2c.scm.domain.model.goods;

import cn.m2c.ddd.common.domain.model.ConcurrencySafeEntity;
import cn.m2c.ddd.common.domain.model.DomainEventPublisher;
import cn.m2c.ddd.common.serializer.ObjectSerializer;
import cn.m2c.scm.domain.model.goods.event.GoodsApproveAddEvent;
import cn.m2c.scm.domain.model.goods.event.GoodsDeleteEvent;
import cn.m2c.scm.domain.util.GetMapValueUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 商品
 */
public class Goods extends ConcurrencySafeEntity {
    /**
     * 商品id
     */
    private String goodsId;

    /**
     * 商家ID
     */
    private String dealerId;

    /**
     * 商家名称
     */
    private String dealerName;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品副标题
     */
    private String goodsSubTitle;

    /**
     * 商品分类id
     */
    private String goodsClassifyId;

    /**
     * 商品品牌id
     */
    private String goodsBrandId;

    /**
     * 商品品牌名称
     */
    private String goodsBrandName;

    /**
     * 商品计量单位id
     */
    private String goodsUnitId;

    /**
     * 最小起订量
     */
    private Integer goodsMinQuantity;

    /**
     * 运费模板id
     */
    private String goodsPostageId;

    /**
     * 商品条形码
     */
    private String goodsBarCode;

    /**
     * 关键词
     */
    private String goodsKeyWord;

    /**
     * 商品保障
     */
    private String goodsGuarantee;

    /**
     * 识别图片id
     */
    private String recognizedId;

    /**
     * 识别图片url
     */
    private String recognizedUrl;

    /**
     * 商品主图  存储类型是[“url1”,"url2"]
     */
    private String goodsMainImages;

    /**
     * 商品主图视频
     */
    private String goodsMainVideo;

    /**
     * 商品描述
     */
    private String goodsDesc;

    /**
     * 1:手动上架,2:审核通过立即上架
     */
    private Integer goodsShelves;

    /**
     * 商品状态，1：仓库中，2：出售中，3：已售罄
     */
    private Integer goodsStatus;

    /**
     * 商品规格,格式：[{"itemName":"尺寸","itemValue":["L","M"]},{"itemName":"颜色","itemValue":["蓝色","白色"]}]
     */
    private String goodsSpecifications;

    /**
     * 商品规格
     */
    private List<GoodsSku> goodsSKUs;

    /**
     * 是否删除，1:正常，2：已删除
     */
    private Integer delStatus;

    /**
     * 创建时间
     */
    private Date createdDate;


    public Goods() {
        super();
    }

    public Goods(String goodsId, String dealerId, String dealerName, String goodsName, String goodsSubTitle,
                 String goodsClassifyId, String goodsBrandId, String goodsBrandName, String goodsUnitId, Integer goodsMinQuantity,
                 String goodsPostageId, String goodsBarCode, String goodsKeyWord, String goodsGuarantee,
                 String goodsMainImages, String goodsDesc, Integer goodsShelves, String goodsSpecifications, String goodsSKUs) {
        this.goodsId = goodsId;
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.goodsName = goodsName;
        this.goodsSubTitle = goodsSubTitle;
        this.goodsClassifyId = goodsClassifyId;
        this.goodsBrandId = goodsBrandId;
        this.goodsBrandName = goodsBrandName;
        this.goodsUnitId = goodsUnitId;
        this.goodsMinQuantity = goodsMinQuantity;
        this.goodsPostageId = goodsPostageId;
        this.goodsBarCode = goodsBarCode;
        this.goodsKeyWord = goodsKeyWord;
        this.goodsGuarantee = goodsGuarantee;
        this.goodsMainImages = goodsMainImages;
        this.goodsDesc = goodsDesc;
        this.goodsShelves = goodsShelves;
        if (this.goodsShelves == 1) {//1:手动上架,2:审核通过立即上架
            this.goodsStatus = 1;
        } else {
            this.goodsStatus = 2;
        }
        this.goodsSpecifications = goodsSpecifications;
        this.createdDate = new Date();
        if (null == this.goodsSKUs) {
            this.goodsSKUs = new ArrayList<>();
        } else {
            this.goodsSKUs.clear();
        }
        List<Map> skuList = ObjectSerializer.instance().deserialize(goodsSKUs, List.class);
        if (null != skuList && skuList.size() > 0) {
            for (Map map : skuList) {
                this.goodsSKUs.add(createGoodsSku(map));
            }
        }
    }

    private GoodsSku createGoodsSku(Map map) {
        String skuId = GetMapValueUtils.getStringFromMapKey(map, "skuId");
        String skuName = GetMapValueUtils.getStringFromMapKey(map, "skuName");
        Integer availableNum = GetMapValueUtils.getIntFromMapKey(map, "availableNum");
        Float weight = GetMapValueUtils.getFloatFromMapKey(map, "weight");
        Long photographPrice = GetMapValueUtils.getLongFromMapKey(map, "photographPrice");
        Long marketPrice = GetMapValueUtils.getLongFromMapKey(map, "marketPrice");
        Long supplyPrice = GetMapValueUtils.getLongFromMapKey(map, "supplyPrice");
        String goodsCode = GetMapValueUtils.getStringFromMapKey(map, "goodsCode");
        Integer showStatus = GetMapValueUtils.getIntFromMapKey(map, "showStatus");
        GoodsSearchInfo goodsSearchInfo = new GoodsSearchInfo(this.dealerId,this.dealerName, this.goodsName, this.goodsSubTitle, this.goodsClassifyId,
                this.goodsBrandId, this.goodsBrandName, this.goodsBarCode, this.goodsDesc, this.goodsKeyWord,
                this.goodsStatus, this.createdDate);
        GoodsSku goodsSku = new GoodsSku(this, skuId, skuName, availableNum, availableNum, weight,
                photographPrice, marketPrice, supplyPrice, goodsCode, showStatus, goodsSearchInfo);
        return goodsSku;
    }

    /**
     * 修改商品需要审核的供货价和拍获价或增加sku
     *
     * @param goodsSKUs
     */
    public void modifyApproveGoodsSku(String goodsSKUs) {
        List<Map> skuList = ObjectSerializer.instance().deserialize(goodsSKUs, List.class);
        if (null != skuList && skuList.size() > 0) {
            if (null == this.goodsSKUs) {
                this.goodsSKUs = new ArrayList<>();
            }
            for (Map map : skuList) {
                String skuId = GetMapValueUtils.getStringFromMapKey(map, "skuId");
                // 判断商品规格sku是否存在,存在就修改供货价和拍获价，不存在就增加商品sku
                GoodsSku goodsSku = getGoodsSKU(skuId);
                if (null == goodsSku) {// 增加规格
                    this.goodsSKUs.add(createGoodsSku(map));
                } else { // 修改供货价和拍获价
                    Long photographPrice = GetMapValueUtils.getLongFromMapKey(map, "photographPrice");
                    Long supplyPrice = GetMapValueUtils.getLongFromMapKey(map, "supplyPrice");
                    goodsSku.modifyApprovePrice(photographPrice, supplyPrice);
                }
            }
        }
    }

    /**
     * 根据skuId获取商品规格
     *
     * @param skuId
     * @return
     */
    private GoodsSku getGoodsSKU(String skuId) {
        GoodsSku goodsSku = null;
        List<GoodsSku> goodsSKUs = this.goodsSKUs;
        for (GoodsSku sku : goodsSKUs) {
            goodsSku = sku.getGoodsSKU(skuId);
            if (null != goodsSku) {
                return goodsSku;
            }
        }
        return goodsSku;
    }

    /**
     * 修改商品
     */
    public void modifyGoods(String goodsName, String goodsSubTitle,
                            String goodsClassifyId, String goodsBrandId, String goodsBrandName, String goodsUnitId, Integer goodsMinQuantity,
                            String goodsPostageId, String goodsBarCode, String goodsKeyWord, String goodsGuarantee,
                            String goodsMainImages, String goodsDesc, String goodsSpecifications, String goodsSKUs) {
        this.goodsName = goodsName;
        this.goodsSubTitle = goodsSubTitle;
        this.goodsClassifyId = goodsClassifyId;
        this.goodsBrandId = goodsBrandId;
        this.goodsBrandName = goodsBrandName;
        this.goodsUnitId = goodsUnitId;
        this.goodsMinQuantity = goodsMinQuantity;
        this.goodsPostageId = goodsPostageId;
        this.goodsBarCode = goodsBarCode;
        this.goodsKeyWord = goodsKeyWord;
        this.goodsGuarantee = goodsGuarantee;
        this.goodsMainImages = goodsMainImages;
        this.goodsDesc = goodsDesc;

        List<Map> skuList = ObjectSerializer.instance().deserialize(goodsSKUs, List.class);
        if (null != skuList && skuList.size() > 0) {
            //修改供货价、拍获价、规格需要审批
            boolean isNeedApprove = false;
            for (Map map : skuList) {
                String skuId = GetMapValueUtils.getStringFromMapKey(map, "skuId");
                // 判断商品规格sku是否存在,存在就修改供货价和拍获价，不存在就增加商品sku
                GoodsSku goodsSku = getGoodsSKU(skuId);
                if (null == goodsSku) {// 增加了规格
                    isNeedApprove = true;
                } else {
                    Integer availableNum = GetMapValueUtils.getIntFromMapKey(map, "availableNum");
                    Float weight = GetMapValueUtils.getFloatFromMapKey(map, "weight");
                    Long marketPrice = GetMapValueUtils.getLongFromMapKey(map, "marketPrice");
                    String goodsCode = GetMapValueUtils.getStringFromMapKey(map, "goodsCode");
                    Integer showStatus = GetMapValueUtils.getIntFromMapKey(map, "showStatus");
                    // 修改商品规格不需要审批的信息
                    GoodsSearchInfo goodsSearchInfo = new GoodsSearchInfo(this.dealerId,this.dealerName, this.goodsName, this.goodsSubTitle, this.goodsClassifyId,
                            this.goodsBrandId, this.goodsBrandName, this.goodsBarCode, this.goodsDesc, this.goodsKeyWord,
                            this.goodsStatus, this.createdDate);
                    goodsSku.modifyNotApproveGoodsSku(availableNum, weight, marketPrice, goodsCode, showStatus, goodsSearchInfo);

                    // 判断供货价和拍获价是否修改
                    Long photographPrice = GetMapValueUtils.getLongFromMapKey(map, "photographPrice");
                    Long supplyPrice = GetMapValueUtils.getLongFromMapKey(map, "supplyPrice");
                    if (goodsSku.isModifyNeedApprovePrice(photographPrice, supplyPrice)) { //修改了供货价和拍获价
                        isNeedApprove = true;
                    }
                }
            }
            if (isNeedApprove) {//发布事件，增加一条待审核商品记录
                DomainEventPublisher
                        .instance()
                        .publish(new GoodsApproveAddEvent(this.goodsId, this.dealerId, this.dealerName, this.goodsName,
                                this.goodsSubTitle, this.goodsClassifyId, this.goodsBrandId, this.goodsUnitId,
                                this.goodsMinQuantity, this.goodsPostageId, this.goodsBarCode,
                                this.goodsKeyWord, this.goodsGuarantee, this.goodsMainImages, this.goodsDesc, goodsSpecifications,
                                goodsSKUs));
            }
        }
    }

    /**
     * 删除商品
     */
    public void remove() {
        this.delStatus = 2;
        for (GoodsSku goodsSku : this.goodsSKUs) {
            goodsSku.remove();
        }
        DomainEventPublisher
                .instance()
                .publish(new GoodsDeleteEvent(this.goodsId));
    }

    /**
     * 上架,商品状态，1：仓库中，2：出售中，3：已售罄
     */
    public void upShelf() {
        this.goodsStatus = 2;
    }

    /**
     * 下架,商品状态，1：仓库中，2：出售中，3：已售罄
     */
    public void offShelf() {
        this.goodsStatus = 1;
    }
}