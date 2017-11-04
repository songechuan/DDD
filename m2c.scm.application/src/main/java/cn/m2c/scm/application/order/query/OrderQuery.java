package cn.m2c.scm.application.order.query;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import cn.m2c.ddd.common.port.adapter.persistence.springJdbc.SupportJdbcTemplate;
import cn.m2c.scm.application.order.data.bean.OrderDealerBean;
import cn.m2c.scm.application.order.data.bean.AllOrderBean;
import cn.m2c.scm.application.order.data.bean.DealerOrderDetailBean;
import cn.m2c.scm.application.order.data.bean.GoodsInfoBean;
import cn.m2c.scm.application.order.data.bean.MainOrderBean;

@Repository
public class OrderQuery {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderQuery.class);

	@Resource
	private SupportJdbcTemplate supportJdbcTemplate;

	/**
	 * 查询mainOrder订单
	 * 
	 * @param orderStatus  订单状态
	 * @param afterSaleStatus 售后状态
	 * @param startTime 开始时间
	 * @param endTime 结束时间
	 * @param condition 搜索条件(goodsName,orderId,payNo,revPhone,dealerName,dealerId)
	 * @param payWay 支付方式
	 * @param mediaInfo 媒体信息
	 * @param dealerClassify 商家分类
	 * @param pageNum 第几页
	 * @param rows 每页多少行
	 * @return
	 */
	public List<MainOrderBean> mainOrderListQuery(Integer orderStatus, Integer afterSaleStatus, String startTime,String orderId,
			String endTime, String condition, Integer payWay,Integer mediaInfo,String dealerClassify, Integer pageNum, Integer rows) {
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT d.dealer_order_id,m.order_id,m.pay_no,m.pay_way,m.created_date,m.goods_amount,m.order_freight,m.plateform_discount,m.dealer_discount, ");
		sql.append(" d.goods_amount AS dealerAmount,d.order_freight AS orderFreight,d._status,dealer.dealer_name ");
		sql.append("  FROM t_scm_order_dealer d ");
		sql.append(" LEFT OUTER JOIN t_scm_order_main m ON d.order_id = m.order_id ");
		sql.append(" LEFT OUTER JOIN t_scm_dealer dealer ON d.dealer_id = dealer.dealer_id ");
		sql.append(" WHERE 1=1 ");
		if (orderStatus != null) {
			sql.append(" AND d._status =  ? ");
			params.add(orderStatus);
		}
		if (afterSaleStatus != null) {
			sql.append(" AND d.dealer_order_id IN (SELECT af.dealer_order_id FROM t_scm_order_after_sell af WHERE af._status = ?) ");
			params.add(afterSaleStatus);
		}
		if (StringUtils.isNotEmpty(endTime) && StringUtils.isNotEmpty(endTime)) {
			sql.append(" AND (d.created_date BETWEEN ? AND ?)");
			params.add(startTime);
			params.add(endTime);
		}
		if (StringUtils.isNotEmpty(condition) && condition != null && !"".equals(condition)) {
			sql.append(" AND (d.dealer_order_id LIKE ?	OR m.pay_no LIKE ? OR dealer.dealer_name LIKE ?) ");
			params.add("%" + condition + "%");
			params.add("%" + condition + "%");
			params.add("%" + condition + "%");
		}
		if (payWay != null) {
			sql.append(" AND m.pay_way = ? ");
			params.add(payWay);
		}
		if (mediaInfo != null) {
			if (mediaInfo == 0) {
				sql.append("AND d.dealer_order_id IN (SELECT dtl.dealer_order_id FROM t_scm_order_detail dtl WHERE dtl.comment_status = 0 AND dtl.media_res_id = '')");
			}
			if (mediaInfo ==1) {
				sql.append("AND d.dealer_order_id IN (SELECT dtl.dealer_order_id FROM t_scm_order_detail dtl WHERE dtl.comment_status = 0 AND dtl.media_res_id != '')");
			}
		}
		if (!StringUtils.isEmpty(dealerClassify)) {
			sql.append(" AND dealer.dealer_classify = ? ");
			params.add(dealerClassify);
		}
		sql.append(" ORDER BY m.order_id DESC, m.created_date DESC ");
		sql.append(" LIMIT ?,?");
		params.add(rows * (pageNum - 1));
		params.add(rows);

		
		/**
		 * 将商家订单塞进对应的平台订单
		 */
		List<AllOrderBean> allOrderList = this.supportJdbcTemplate.queryForBeanList(sql.toString(),
				AllOrderBean.class, params.toArray());
		List<MainOrderBean> mainOrderList = new ArrayList<MainOrderBean>();
		
		MainOrderBean mainOrder = null;
		String tmpOrderId = null;
		for (AllOrderBean allOrder : allOrderList) {
			OrderDealerBean dealerBean = null;
			String id = allOrder.getOrderId();
			if (!id.equals(tmpOrderId)) {
				tmpOrderId = id;
				mainOrder = new MainOrderBean();
				mainOrderList.add(mainOrder);
				mainOrder.setOrderId(allOrder.getOrderId());
				mainOrder.setPayNo(allOrder.getPayNo());
				mainOrder.setCreateDate(allOrder.getCreatedDate());
				mainOrder.setGoodAmount(allOrder.getMainGoodsAmount());
				mainOrder.setOderFreight(allOrder.getMainOrderFreight());
				mainOrder.setDealerDiscount(allOrder.getDealerDiscount());
				mainOrder.setPlateFormDiscount(allOrder.getPlateformDiscount());
				List<OrderDealerBean> dealerOrderList = new ArrayList<OrderDealerBean>();
				mainOrder.setDealerOrderBeans(dealerOrderList);
			}
			dealerBean = new OrderDealerBean();
			dealerBean.setOrderId(allOrder.getOrderId());
			dealerBean.setDealerName(allOrder.getDealerName());
			dealerBean.setDealerOrderId(allOrder.getDealerOrderId());
			dealerBean.setGoodAmount(allOrder.getDealerGoodsAmount());
			dealerBean.setOderFreight(allOrder.getDealerOrderFreight());
			dealerBean.setPlateFormDiscount(allOrder.getPlateformDiscount());
			dealerBean.setDealerDiscount(allOrder.getDealerDiscount());
			dealerBean.setStatus(allOrder.getStatus());
			mainOrder.getDealerOrderBeans().add(dealerBean);
		}
		return mainOrderList;
	}

	/**
	 * 查询商家订单
	 * 
	 * @param orderStatus
	 * @param afterSaleStatus
	 * @param startTime
	 * @param endTime
	 * @param condition
	 * @param payWay
	 * @param pageNum
	 * @param rows
	 * @return
	 */
	public List<OrderDealerBean> dealerOrderListQuery(String orderId) {
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT odealer.order_id  orderId , odealer.dealer_id  dealerId  ,odealer.dealer_order_id  dealerOrderId  ");
		sql.append("  ,odealer.dealer_discount  dealerDiscount  ,dealer.dealer_name  dealerName  ,odealer._status  status");
		sql.append("  ,odealer.goods_amount  goodsAmount  ,odealer.order_freight  orderFreight  ,odealer.plateform_discount  plateformDiscount ");
		sql.append("  ,odealer.dealer_discount  dealerDiscount");
		sql.append(" FROM t_scm_order_dealer odealer");
		sql.append(" LEFT JOIN t_scm_order_after_sell oafter ");
		sql.append(" ON odealer.dealer_order_id = oafter.dealer_order_id ");
		sql.append(" INNER JOIN t_scm_dealer dealer ");
		sql.append(" WHERE 1=1 AND odealer.order_id = ? ");
		/*if (orderStatus != null) {
			sql.append(" AND odealer._status = ? ");
			params.add(orderStatus);
		}
		if (afterSaleStatus != null) {
			sql.append(" AND oafter._status = ? ");
			params.add(afterSaleStatus);
		}
		if (StringUtils.isNotEmpty(endTime) && StringUtils.isNotEmpty(endTime)) {
			sql.append(" AND omain.created_date BETWEEN ? AND ? ");
			params.add(startTime);
			params.add(endTime);
		}
		if (condition != null && !"".equals(condition)) {
			sql.append(
					" AND omain.order_id = ? OR omain.pay_no = ? OR odetail.rev_phone = ? OR odealer.dealer_id = ? OR odetail.goods_name LIKE ? ");
			params.add(condition);
			params.add("%" + condition + "%");
		}
		if (payWay != null) {
			sql.append(" AND omain.pay_way = ? ");
			params.add(payWay);
		}*/
		sql.append(" AND odealer.dealer_id = dealer.dealer_id ");
		
		sql.append(" ORDER BY odealer.created_date DESC ");
		return this.supportJdbcTemplate.queryForBeanList(sql.toString(), OrderDealerBean.class, orderId);
	}

	/**
	 * 查询订单总数
	 * 
	 * @param orderStatus
	 * @param afterSaleStatus
	 * @param startTime
	 * @param endTime
	 * @param condition
	 * @param mediaInfo 
	 * @param dealerClassify
	 * @param payWay
	 * @return
	 */
	public Integer mainOrderQueryTotal(Integer orderStatus, Integer afterSaleStatus, String startTime, String endTime,
			String condition, Integer payWay,Integer mediaInfo,String dealerClassify) {
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT COUNT(1) ");
		sql.append("  FROM t_scm_order_dealer d");
		sql.append(" LEFT OUTER JOIN t_scm_order_main m ON d.order_id = m.order_id ");
		sql.append(" LEFT OUTER JOIN t_scm_dealer dealer ON d.dealer_id = dealer.dealer_id ");
		sql.append(" WHERE 1=1 ");
		if (orderStatus != null) {
			sql.append(" AND d._status =  ? ");
			params.add(orderStatus);
		}
		if (afterSaleStatus != null) {
			sql.append(" AND d.dealer_order_id IN (SELECT af.dealer_order_id FROM t_scm_order_after_sell af WHERE af._status = ?) ");
			params.add(afterSaleStatus);
		}
		if (StringUtils.isNotEmpty(endTime) && StringUtils.isNotEmpty(endTime)) {
			sql.append(" AND (d.created_date BETWEEN ? AND ?)");
			params.add(startTime);
			params.add(endTime);
		}
		if (StringUtils.isNotEmpty(condition) && condition != null && !"".equals(condition)) {
			sql.append(" AND (d.dealer_order_id LIKE ?	OR m.pay_no LIKE ? OR dealer.dealer_name LIKE ?) ");
			params.add("%" + condition + "%");
			params.add("%" + condition + "%");
			params.add("%" + condition + "%");
		}
		if (payWay != null) {
			sql.append(" AND m.pay_way = ? ");
			params.add(payWay);
		}
		if (mediaInfo != null) {
			if (mediaInfo == 0) {
				sql.append("AND d.dealer_order_id IN (SELECT dtl.dealer_order_id FROM t_scm_order_detail dtl WHERE dtl.comment_status = 0 AND dtl.media_res_id = '')");
			}
			if (mediaInfo ==1) {
				sql.append("AND d.dealer_order_id IN (SELECT dtl.dealer_order_id FROM t_scm_order_detail dtl WHERE dtl.comment_status = 0 AND dtl.media_res_id != '')");
			}
		}
		if (!StringUtils.isEmpty(dealerClassify)) {
			sql.append(" AND dealer.dealer_classify = ? ");
			params.add(dealerClassify);
		}
		Integer total = this.supportJdbcTemplate.jdbcTemplate().queryForObject(sql.toString(), params.toArray(), Integer.class);
		return total;
	}

	/**
	 * 查询商家订单详情
	 * 
	 * @param dealerId
	 * @return
	 */
	public DealerOrderDetailBean dealerOrderDetailQuery(String dealerOrderId) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT t1._status orderStatus,t1.order_id orderId,t1.created_date createdDate, t3.pay_way payWay,t3.pay_time payTime,t3.pay_no payNo, ");
		sql.append(" t1.rev_person revPerson,t1.rev_phone revPhone,t1.province province,t1.city city,t1.area_county areaCounty,t1.street_addr streetAddr, ");
		sql.append(" t4.dealer_name dealerName,t4.dealer_classify dealerClassify,t1.dealer_id dealerId,  t6.seller_name sellerName,t6.seller_phone sellerPhone, ");
		sql.append(" t2.plateform_discount plateformDiscount,t2.dealer_discount dealerDiscount ");
		sql.append(" FROM t_scm_order_dealer t1 ");
		sql.append(" INNER JOIN t_scm_order_detail t2 ");
		sql.append(" INNER JOIN t_scm_order_main t3 ");
		sql.append(" INNER JOIN t_scm_dealer t4");
		sql.append(" INNER JOIN t_scm_goods t5 ");
		sql.append(" INNER JOIN t_scm_dealer_seller t6");
		sql.append(" WHERE 1 = 1 AND t1.dealer_order_id = ? ");
		sql.append(" AND t1.dealer_order_id = t2.dealer_order_id ");
		sql.append(" AND t1.order_id = t3.order_id AND t1.dealer_id = t4.dealer_id ");
		sql.append(" AND t2.goods_id = t5.goods_id ");
		sql.append(" AND t2.saler_user_id  = t6.seller_id ");
		DealerOrderDetailBean dealerOrderDetailBean = this.supportJdbcTemplate.queryForBean(sql.toString(), DealerOrderDetailBean.class, dealerOrderId);
		long totalPrice = 0; // 商品总价格
		long totalFrieght = 0; // 运费总价格
		List<GoodsInfoBean> goodsInfoList = getGoodsInfoList(dealerOrderId);
		dealerOrderDetailBean.setGoodsInfoBeans(goodsInfoList);
		for (GoodsInfoBean goodsInfo : dealerOrderDetailBean.getGoodsInfoBeans()) {
			totalPrice += goodsInfo.getTotalPrice();
			totalFrieght += goodsInfo.getFreight();
		}

		dealerOrderDetailBean.setTotalOrderPrice(totalPrice);
		dealerOrderDetailBean.setTotalFreight(totalFrieght);

		/**
		 * 获取商家订单总价格 (商品总额-运费总额-平台优惠信息-商家优惠信息)
		 */
		return dealerOrderDetailBean;
	}
	
	/**
	 * 查询商家订单详情
	 * fanjc
	 * @param dealerId
	 * @return
	 */
	public DealerOrderDetailBean dealerOrderDetailQuery1(String dealerOrderId) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT t1._status orderStatus,t1.order_id orderId,t1.created_date createdDate, t3.pay_way payWay,t3.pay_time payTime,t3.pay_no payNo, \r\n") 
		.append(" t1.rev_person revPerson,t1.rev_phone revPhone,t1.province province,t1.city city,t1.area_county areaCounty,t1.street_addr streetAddr, \r\n")
		.append(" t1.dealer_id dealerId, t1.noted, t1.invoice_header, t1.invoice_name, t1.invoice_code, t1.invoice_type, \r\n")
		.append(" t1.plateform_discount plateformDiscount,t1.dealer_discount dealerDiscount, t1.goods_amount, t1.order_freight, \r\n")
		.append(" t1.area_code ,t1.city_code, t1.province_code \r\n")
		.append(" FROM t_scm_order_dealer t1 \r\n")
		.append(" LEFT OUTER JOIN t_scm_order_main t3 ON t1.order_id = t3.order_id\r\n") 
		.append(" WHERE t1.dealer_order_id = ?"); 
		DealerOrderDetailBean dealerOrderDetailBean = this.supportJdbcTemplate.queryForBean(sql.toString(), DealerOrderDetailBean.class, dealerOrderId);
		if (dealerOrderDetailBean != null) {
			dealerOrderDetailBean.setDealerOrderId(dealerOrderId);
		}
		List<GoodsInfoBean> goodsInfoList = getGoodsInfoList(dealerOrderId);
		dealerOrderDetailBean.setGoodsInfoBeans(goodsInfoList);
		/*for (GoodsInfoBean goodsInfo : dealerOrderDetailBean.getGoodsInfoBeans()) {
			totalPrice += goodsInfo.getTotalPrice();
			totalFrieght += goodsInfo.getFreight();
		}

		dealerOrderDetailBean.setTotalOrderPrice(totalPrice);
		dealerOrderDetailBean.setTotalFreight(totalFrieght);*/

		/**
		 * 获取商家订单总价格 (商品总额-运费总额-平台优惠信息-商家优惠信息)
		 */
		/*dealerOrderDetailBean
				.setOrderPrice(dealerOrderDetailBean.getTotalOrderPrice() - dealerOrderDetailBean.getTotalFreight()
						- dealerOrderDetailBean.getPlateformDiscount() - dealerOrderDetailBean.getDealerDiscount());*/
		
		return dealerOrderDetailBean;
	}

	/**
	 * 查询商品信息列表 
	 * @param dealerOrderId
	 * @return
	 */
	public List<GoodsInfoBean> getGoodsInfoList(String dealerOrderId) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT  dtl.goods_icon, dtl.goods_name,dtl.sku_name, dtl.sku_id, \r\n")
		.append(" dtl.media_res_id,dtl.sell_num,dtl.goods_unit, dtl.discount_price,dtl.freight \r\n") 
		.append(" FROM  t_scm_order_dealer dealer \r\n")
		.append(" LEFT OUTER JOIN t_scm_order_detail dtl ON dealer.dealer_order_id = dtl.dealer_order_id\r\n")
		.append(" WHERE dealer.dealer_order_id = ? ");
		
		List<GoodsInfoBean> goodsInfoList = this.supportJdbcTemplate.queryForBeanList(sql.toString(),
				GoodsInfoBean.class, dealerOrderId);
		for (GoodsInfoBean goodsInfo : goodsInfoList) {
			goodsInfo.setTotalPrice(goodsInfo.getPrice() * goodsInfo.getSellNum());
		}
		return goodsInfoList;
	}

}
