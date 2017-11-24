package cn.m2c.scm.port.adapter.restful.web.order;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;

import cn.m2c.common.JsonUtils;
import cn.m2c.common.MCode;
import cn.m2c.common.MResult;
import cn.m2c.scm.application.order.OrderApplication;
import cn.m2c.scm.application.order.SaleAfterOrderApp;
import cn.m2c.scm.application.order.command.OrderPayedCmd;
import cn.m2c.scm.application.order.data.bean.MainOrderBean;
import cn.m2c.scm.application.order.data.bean.RefundEvtBean;
import cn.m2c.scm.application.order.data.representation.OrderMoney;
import cn.m2c.scm.application.order.data.representation.OrderNums;
import cn.m2c.scm.application.order.query.OrderQuery;
import cn.m2c.scm.domain.NegativeException;


@RestController
@RequestMapping("/order-out")
public class OrderOutAgent {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(OrderOutAgent.class);
	
	@Autowired
	OrderApplication orderApp;
	
	@Autowired
	OrderQuery orderQuery;
    
    /**
     * 获取订单需要支付的金额
     */
    @RequestMapping(value="/amountOfMoney/{orderNo}", method = RequestMethod.GET)
    public ResponseEntity<MResult> getOrderNoMoney(@PathVariable("orderNo") String orderNo
    		,@RequestParam(value="userId", required=false) String userId){
    	MResult result = new MResult(MCode.V_1);
    	try {
    		OrderMoney orderMoney = orderApp.getOrderMoney(orderNo, userId);
    		result.setContent(orderMoney);
    		result.setStatus(MCode.V_200);
		} 
    	catch (NegativeException e) {
    		result.setStatus(e.getStatus());
			result.setContent(e.getMessage());
    	}
    	catch (Exception e) {
			LOGGER.info("获取订单金额失败,e:" + e.getMessage());
			result.setStatus(MCode.V_400);
			result.setContent("获取订单金额失败");
		}
    	return new ResponseEntity<MResult>(result,HttpStatus.OK);
    }
    
    @Autowired
	private SaleAfterOrderApp saleAfterApp;
    @RequestMapping(value="/test", method = RequestMethod.GET)
    public ResponseEntity<MResult> testForMedia(@RequestParam(value="userId", required=false) String userId
    		,@RequestParam(value="orderId", required=false) String orderId
    		){
    	MResult result = new MResult(MCode.V_1);
    	try {
    		//OrderPayedCmd cmd = new OrderPayedCmd(orderId, userId, "89555555555555551", 1, new Date());
    		//orderApp.orderPayed(cmd);
    		//orderApp.cancelAllNotPayed();
    		String a = "{\"orderRefundId\":\"16DR20171124140404228000359543\",\"orderPayId\":\"20171124105931528000543160\",\"afterSellOrderId\":\"20171124140316KD\",\"dealerId\":\"JXS1382A365F7D94350991D84AF23233FD7\",\"userId\":\"HY8155311C70F849DAA126997124D8CB98\",\"payWay\":1,\"refundAmount\":1,\"tradeNo\":\"2017112421001104200248018865\",\"refundTime\":1511503444228}";
    		RefundEvtBean bean = JsonUtils.toBean(a, RefundEvtBean.class);
    		saleAfterApp.refundSuccess(bean);
    		result.setContent("ok");
    		result.setStatus(MCode.V_200);
		} 
    	catch (Exception e) {
			LOGGER.info("获取test失败,e:" + e.getMessage());
			result.setStatus(MCode.V_400);
			result.setContent("获取test失败");
		}
    	return new ResponseEntity<MResult>(result,HttpStatus.OK);
    }
    
    /**
     * 获取订单数据根据订单号
     */
    @RequestMapping(value="/get/{orderNo}", method = RequestMethod.GET)
    public ResponseEntity<MResult> getOrderByNo(@PathVariable("orderNo") String orderNo
    		,@RequestParam(value="userId", required=false) String userId){
    	MResult result = new MResult(MCode.V_1);
    	try {
    		MainOrderBean mainOrder = orderQuery.getOrderByNo(orderNo, userId);
    		result.setContent(mainOrder);
    		result.setStatus(MCode.V_200);
		} 
    	catch (NegativeException e) {
    		result.setStatus(e.getStatus());
			result.setContent(e.getMessage());
    	}
    	catch (Exception e) {
			LOGGER.info("获取订单数据失败,e:" + e.getMessage());
			result.setStatus(MCode.V_400);
			result.setContent("获取订单数据失败");
		}
    	return new ResponseEntity<MResult>(result,HttpStatus.OK);
    }
    
    /**
     * 获取订单数据根据订单号
     */
    @RequestMapping(value="/payed/order", method = RequestMethod.GET)
    public ResponseEntity<MResult> getOrderByNo(@RequestParam(value="userId", required=false) String userId
    		,@RequestParam(value="startTime", required=false) String startTime
    		,@RequestParam(value="endTime", required=false) String endTime){
    	MResult result = new MResult(MCode.V_1);
    	try {
    		Integer i = orderQuery.getPayedOrders(startTime, endTime);
    		result.setContent(new OrderNums(i));
    		result.setStatus(MCode.V_200);
		} 
    	catch (NegativeException e) {
    		result.setStatus(e.getStatus());
			result.setContent(e.getMessage());
    	}
    	catch (Exception e) {
			LOGGER.info("获取订单数失败,e:" + e.getMessage());
			result.setStatus(MCode.V_400);
			result.setContent("获取订单数失败");
		}
    	return new ResponseEntity<MResult>(result,HttpStatus.OK);
    }
    
    /**
     * 根据子订单号查询订单是否可以结算
     */
    @RequestMapping(value="/suborder/status", method = RequestMethod.GET)
    public ResponseEntity<MResult> getCanCheckOutOrders(@RequestParam(value="userId", required=false) String userId
    		,@RequestParam(value="subOrderIds", required=false) String subOrderIds
    		){
    	MResult result = new MResult(MCode.V_1);
    	try {
    		
    		if (StringUtils.isEmpty(subOrderIds)) {
    			result.setContent("subOrderIds参数为空！");
    		}
    		
    		List<String> ls = JsonUtils.toList(subOrderIds, String.class);
    		
    		Map<String, Integer> rs = orderQuery.getCanCheckOrder(ls);
    		result.setContent(rs);
    		result.setStatus(MCode.V_200);
		} 
    	catch (NegativeException e) {
    		result.setStatus(e.getStatus());
			result.setContent(e.getMessage());
    	}
    	catch (Exception e) {
			LOGGER.info("获取可结算订单失败,e:" + e.getMessage());
			result.setStatus(MCode.V_400);
			result.setContent("获取可结算订单失败");
		}
    	return new ResponseEntity<MResult>(result,HttpStatus.OK);
    }
    
    public static void main(String args[]) {
    	BigDecimal g = new BigDecimal(10);
    	
    	BigDecimal g1 = new BigDecimal(6);
    	System.out.println(g.divide(g1, BigDecimal.ROUND_CEILING, BigDecimal.ROUND_HALF_UP));
    }
}
