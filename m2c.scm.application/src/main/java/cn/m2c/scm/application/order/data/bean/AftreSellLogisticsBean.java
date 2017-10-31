package cn.m2c.scm.application.order.data.bean;

import cn.m2c.ddd.common.persistence.orm.ColumnAlias;

/**
 * 退换货物流信息
 * 
 * @author lqwen
 *
 */
public class AftreSellLogisticsBean {

	/**
	 * 售后状态
	 */
	@ColumnAlias(value = "_status")
	private Integer status;
	/**
	 * 售后单号
	 */
	@ColumnAlias(value = "after_sell_order_id")
	private String afterSellOrderId;
	/**
	 * 物流公司
	 */
	@ColumnAlias(value = "back_express_name")
	private String expressName;
	/**
	 * 物流单号
	 */
	@ColumnAlias(value = "back_express_no")
	private String expressNo;

	/**
	 * 商品信息
	 */
	private GoodsInfoBean goodsInfo;

	public Integer getStatus() {
		return status;
	}

	public String getAfterSellOrderId() {
		return afterSellOrderId;
	}

	public String getExpressName() {
		return expressName;
	}

	public String getExpressNo() {
		return expressNo;
	}

	public void setStatus(Integer status) {
		this.status = status;

	}

	public void setAfterSellOrderId(String afterSellOrderId) {
		this.afterSellOrderId = afterSellOrderId;
	}

	public void setExpressName(String expressName) {
		this.expressName = expressName;
	}

	public void setExpressNo(String expressNo) {
		this.expressNo = expressNo;
	}

	public GoodsInfoBean getGoodsInfo() {
		return goodsInfo;
	}

	public void setGoodsInfo(GoodsInfoBean goodsInfo) {
		this.goodsInfo = goodsInfo;
	}

}
