package cn.m2c.scm.application.goods.query.data.representation;

import cn.m2c.common.JsonUtils;
import cn.m2c.scm.application.goods.query.data.bean.GoodsApproveBean;
import cn.m2c.scm.application.goods.query.data.bean.GoodsSkuApproveBean;
import cn.m2c.scm.application.utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 搜索结果展示
 */
public class GoodsApproveSearchRepresentation {
    private String goodsId;
    private String goodsImageUrl;
    private String goodsName;
    private String goodsClassify;
    private String brandName;
    private String goodsPrice;
    private Integer stockNum;
    private Integer approveStatus;//审核状态，1：审核中，2：审核不通过
    private String dealerName;
    private String dealerType;
    private String dealerId;
    private String rejectReason;
    private Integer settlementMode;//结算模式 1：按供货价 2：按服务费率
    private Float newServiceRate;
    private Float oldServiceRate;
    private String oldClassifyName;


    public GoodsApproveSearchRepresentation(GoodsApproveBean bean, Map goodsClassifyMap, String dealerType, Integer settlementMode,
                                            Float newServiceRate, Float oldServiceRate, String oldClassifyName) {
        List<String> mainImages = JsonUtils.toList(bean.getGoodsMainImages(), String.class);
        if (null != mainImages && mainImages.size() > 0) {
            this.goodsImageUrl = mainImages.get(0);
        }
        this.goodsId = bean.getGoodsId();
        this.goodsName = bean.getGoodsName();
        if (null != goodsClassifyMap) {
            this.goodsClassify = null == goodsClassifyMap.get("name") ? "" : (String) goodsClassifyMap.get("name");
        }
        this.brandName = bean.getGoodsBrandName();
        List<GoodsSkuApproveBean> goodsSkuBeans = bean.getGoodsSkuApproves();
        if (null != goodsSkuBeans && goodsSkuBeans.size() > 0) {
            //排序
            Collections.sort(goodsSkuBeans, new Comparator<GoodsSkuApproveBean>() {
                public int compare(GoodsSkuApproveBean bean1, GoodsSkuApproveBean bean2) {
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
            Integer stockNum = 0;
            Integer sellNum = 0;
            for (GoodsSkuApproveBean skuBean : goodsSkuBeans) {
                stockNum = stockNum + skuBean.getAvailableNum();
            }
            this.goodsPrice = Utils.moneyFormatCN(goodsSkuBeans.get(0).getPhotographPrice());
            this.stockNum = stockNum;
        }

        this.approveStatus = bean.getApproveStatus();
        this.dealerName = bean.getDealerName();
        this.dealerType = dealerType;
        this.dealerId = bean.getDealerId();
        this.rejectReason = bean.getRejectReason();

        this.settlementMode = settlementMode;
        this.newServiceRate = newServiceRate;
        this.oldClassifyName = oldClassifyName;
        this.oldServiceRate = oldServiceRate;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsImageUrl() {
        return goodsImageUrl;
    }

    public void setGoodsImageUrl(String goodsImageUrl) {
        this.goodsImageUrl = goodsImageUrl;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsClassify() {
        return goodsClassify;
    }

    public void setGoodsClassify(String goodsClassify) {
        this.goodsClassify = goodsClassify;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(String goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public Integer getStockNum() {
        return stockNum;
    }

    public void setStockNum(Integer stockNum) {
        this.stockNum = stockNum;
    }

    public Integer getApproveStatus() {
        return approveStatus;
    }

    public void setApproveStatus(Integer approveStatus) {
        this.approveStatus = approveStatus;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getDealerType() {
        return dealerType;
    }

    public void setDealerType(String dealerType) {
        this.dealerType = dealerType;
    }

    public String getDealerId() {
        return dealerId;
    }

    public void setDealerId(String dealerId) {
        this.dealerId = dealerId;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getSettlementMode() {
        return settlementMode;
    }

    public void setSettlementMode(Integer settlementMode) {
        this.settlementMode = settlementMode;
    }

    public Float getNewServiceRate() {
        return newServiceRate;
    }

    public void setNewServiceRate(Float newServiceRate) {
        this.newServiceRate = newServiceRate;
    }

    public Float getOldServiceRate() {
        return oldServiceRate;
    }

    public void setOldServiceRate(Float oldServiceRate) {
        this.oldServiceRate = oldServiceRate;
    }

    public String getOldClassifyName() {
        return oldClassifyName;
    }

    public void setOldClassifyName(String oldClassifyName) {
        this.oldClassifyName = oldClassifyName;
    }
}
