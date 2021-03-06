package cn.m2c.scm.application.goods.query.data.bean;

import cn.m2c.ddd.common.domain.model.IdentifiedValueObject;
import cn.m2c.ddd.common.persistence.orm.ColumnAlias;

/**
 * 商品规格审核
 */
public class GoodsSkuApproveBean extends IdentifiedValueObject {

    /**
     * 商品主键id
     */
    @ColumnAlias(value = "goods_id")
    private Integer goodsId;

    /**
     * 规格id
     */
    @ColumnAlias(value = "sku_id")
    private String skuId;

    /**
     * 规格名称
     */
    @ColumnAlias(value = "sku_name")
    private String skuName;

    /**
     * 可用库存
     */
    @ColumnAlias(value = "available_num")
    private Integer availableNum;

    /**
     * 重量
     */
    @ColumnAlias(value = "weight")
    private Float weight;

    /**
     * 拍获价
     */
    @ColumnAlias(value = "photograph_price")
    private Long photographPrice;

    /**
     * 市场价
     */
    @ColumnAlias(value = "market_price")
    private Long marketPrice;

    /**
     * 供货价
     */
    @ColumnAlias(value = "supply_price")
    private Long supplyPrice;

    /**
     * 商品编码
     */
    @ColumnAlias(value = "goods_code")
    private String goodsCode;

    /**
     * 是否对外展示，1：不展示，2：展示
     */
    @ColumnAlias(value = "show_status")
    private Integer showStatus;

    private boolean show;

    public void setShow(boolean show) {
        this.show = show;
    }

    public boolean isShow() {
        if (this.showStatus == 2) {
            this.show = true;
        }else{
            this.show = false;
        }
        return this.show;
    }

    public Integer getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public Integer getAvailableNum() {
        return availableNum;
    }

    public void setAvailableNum(Integer availableNum) {
        this.availableNum = availableNum;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Long getPhotographPrice() {
        return photographPrice;
    }

    public void setPhotographPrice(Long photographPrice) {
        this.photographPrice = photographPrice;
    }

    public Long getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(Long marketPrice) {
        this.marketPrice = marketPrice;
    }

    public Long getSupplyPrice() {
        return supplyPrice;
    }

    public void setSupplyPrice(Long supplyPrice) {
        this.supplyPrice = supplyPrice;
    }

    public String getGoodsCode() {
        return goodsCode;
    }

    public void setGoodsCode(String goodsCode) {
        this.goodsCode = goodsCode;
    }

    public Integer getShowStatus() {
        return showStatus;
    }

    public void setShowStatus(Integer showStatus) {
        this.showStatus = showStatus;
    }
}