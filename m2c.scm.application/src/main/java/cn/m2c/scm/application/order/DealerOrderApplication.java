package cn.m2c.scm.application.order;

import cn.m2c.common.JsonUtils;
import cn.m2c.common.MCode;
import cn.m2c.ddd.common.event.annotation.EventListener;
import cn.m2c.ddd.common.logger.OperationLogManager;
import cn.m2c.scm.application.order.command.SendOrderCommand;
import cn.m2c.scm.application.order.command.UpdateAddrCommand;
import cn.m2c.scm.application.order.command.UpdateAddrFreightCmd;
import cn.m2c.scm.application.order.command.UpdateOrderFreightCmd;
import cn.m2c.scm.application.order.data.bean.SkuNumBean;
import cn.m2c.scm.application.order.query.AfterSellOrderQuery;
import cn.m2c.scm.application.shop.data.bean.ShopBean;
import cn.m2c.scm.application.shop.query.ShopQuery;
import cn.m2c.scm.application.utils.Utils;
import cn.m2c.scm.domain.NegativeCode;
import cn.m2c.scm.domain.NegativeException;
import cn.m2c.scm.domain.model.order.DealerOrder;
import cn.m2c.scm.domain.model.order.DealerOrderDtl;
import cn.m2c.scm.domain.model.order.DealerOrderDtlRepository;
import cn.m2c.scm.domain.model.order.DealerOrderRepository;
import cn.m2c.scm.domain.model.order.MainOrder;
import cn.m2c.scm.domain.model.order.OrderRepository;
import cn.m2c.scm.domain.model.order.ReceiveAddr;
import cn.m2c.scm.domain.service.order.OrderService;
import cn.m2c.scm.domain.util.GetDisconfDataGetter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DealerOrderApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(DealerOrderApplication.class);

	
	@Autowired
	DealerOrderRepository dealerOrderRepository;
	
	@Autowired
	DealerOrderDtlRepository orderDtlRepository;

	@Autowired
    OrderRepository orderRepository;
	
	@Autowired
	AfterSellOrderQuery afterQuery;
	
	@Resource
    private OperationLogManager operationLogManager;

	@Autowired
	OrderService orderService;
	@Autowired
	ShopQuery shopQuery;

	/**
	 * 更新物流信息
	 * 
	 * @param command
	 * @throws NegativeException
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class },propagation = Propagation.REQUIRES_NEW)
	@EventListener
	public void updateExpress(SendOrderCommand command, String attach) throws NegativeException {
		LOGGER.info("---command： "+command.toString()+"-----attach:"+attach);
		LOGGER.info("更新物流信息");
		DealerOrder dealerOrder = dealerOrderRepository.getDealerOrderById(command.getDealerOrderId());
		if (dealerOrder == null)
			throw new NegativeException(NegativeCode.DEALER_ORDER_IS_NOT_EXIST, "此商家订单不存在.");
		// 获取订单中已经申请的售后商品
		List<SkuNumBean> skuBeans = afterQuery.getSkuIdsByDealerOrderId(command.getDealerOrderId());
		List<String> skuIds = null;
		List<Integer> sortNos = null;
		if (skuBeans != null) {
			skuIds = new ArrayList<String>();
			sortNos = new ArrayList<Integer>();
			for(SkuNumBean s : skuBeans) {
				skuIds.add(s.getSkuId());
				sortNos.add(s.getSortNo());
			}
		}
		if (StringUtils.isNotEmpty(attach))
			operationLogManager.operationLog("更新快递发货", attach, dealerOrder);
		if (dealerOrder.canDoSth()>1) {
			if (!dealerOrder.modifyExpress(command.getExpressName(), command.getExpressNo(), command.getExpressNote(),
				command.getExpressPerson(), command.getExpressPhone(), command.getExpressWay(),
				command.getExpressCode(), command.getUserId(), skuIds, sortNos, command.getShopName(),null))
				throw new NegativeException(MCode.V_300, "订单处于不可重发货状态");
		}
		else if (dealerOrder.canDoSth()> 0) {
			if (!dealerOrder.updateExpress(command.getExpressName(), command.getExpressNo(), command.getExpressNote(),
					command.getExpressPerson(), command.getExpressPhone(), command.getExpressWay(),
					command.getExpressCode(), command.getUserId(), skuIds, sortNos, command.getShopName(),null)) {
				throw new NegativeException(MCode.V_300, "订单处于不可发货状态");
			}
		}
		dealerOrderRepository.save(dealerOrder);

		// 发货消息推送
		MainOrder mOrder = orderRepository.getOrderById(dealerOrder.getOrderNo());
		Map extraMap = new HashMap<>();
		extraMap.put("dealerOrderId", dealerOrder.getId());
		extraMap.put("orderId", dealerOrder.getOrderNo());
		extraMap.put("optType", 1);
		orderService.msgPush(1, mOrder.userId(), JsonUtils.toStr(extraMap), dealerOrder.dealerId());
		dealerOrderRepository.save(dealerOrder);
	}

	/**
	 * 更新收货地址
	 * 
	 * @param command
	 * @throws NegativeException
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	@EventListener
	public void updateAddress(UpdateAddrCommand command, String _attach) throws NegativeException {
		DealerOrder dealerOrder = dealerOrderRepository.getDealerOrderById(command.getDealerOrderId());
		if (dealerOrder == null)
			throw new NegativeException(NegativeCode.DEALER_ORDER_IS_NOT_EXIST, "此商家订单不存在.");
		
		if (StringUtils.isNotEmpty(_attach))
			operationLogManager.operationLog("修改收货地址", _attach, dealerOrder);
		
		ReceiveAddr addr = dealerOrder.getAddr();
		addr.updateAddr(command.getProvince(), command.getProvCode(), command.getCity(), command.getCityCode(),
				command.getArea(), command.getAreaCode(), command.getStreet(), command.getRevPerson(),
				command.getPhone(), command.getPostCode());
		dealerOrder.updateAddr(addr, command.getUserId());
		dealerOrderRepository.save(dealerOrder);
	}

	/**
	 * 更新订单运费
	 * 
	 * @param command
	 * @throws NegativeException
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	@EventListener
	public void updateOrderFreight(UpdateOrderFreightCmd command, String attach) throws NegativeException {
		DealerOrder dealerOrder = dealerOrderRepository.getDealerOrderById(command.getDealerOrderId());
		if (dealerOrder == null)
			throw new NegativeException(NegativeCode.DEALER_ORDER_IS_NOT_EXIST, "此商家订单不存在.");
		if (StringUtils.isNotEmpty(attach))
			operationLogManager.operationLog("修改运费", attach, dealerOrder);
		
		dealerOrder.updateOrderFreight(command.getOrderFreight(), command.getUserId());
		dealerOrderRepository.save(dealerOrder);
	}

	/**
	 * 更新收货地址及运费
	 * 
	 * @param command
	 * @throws NegativeException
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	@EventListener
	public void updateAddrFreight(UpdateAddrFreightCmd cmd, String attach) throws NegativeException {

		DealerOrder dealerOrder = dealerOrderRepository.getDealerOrderById(cmd.getDealerOrderId());
		if (dealerOrder == null)
			throw new NegativeException(NegativeCode.DEALER_ORDER_IS_NOT_EXIST, "此商家订单不存在.");

		if (!dealerOrder.canUpdateFreight())
			throw new NegativeException(MCode.V_1, "此商家订单处于不能修改状态.");

		ReceiveAddr addr = dealerOrder.getAddr();

		// 是否修改运费
		boolean isModifyFreight = dealerOrder.isModifyFreight(cmd.getFreights());
		// 是否修改收货地址
		boolean isModifyAddress = addr.isModifyAddress(cmd.getProvince(), cmd.getProvCode(), cmd.getCity(), cmd.getCityCode(),
				cmd.getArea(), cmd.getAreaCode(), cmd.getStreet(), cmd.getRevPerson(), cmd.getPhone());

		boolean updatedAddr = addr.updateAddr(cmd.getProvince(), cmd.getProvCode(), cmd.getCity(), cmd.getCityCode(),
				cmd.getArea(), cmd.getAreaCode(), cmd.getStreet(), cmd.getRevPerson(), cmd.getPhone()
				, null);
		
		MainOrder mOrder = orderRepository.getOrderById(dealerOrder.getOrderNo());
		if (updatedAddr && isModifyAddress) {
			dealerOrder.updateAddr(addr, cmd.getUserId());
		}
		boolean updatedFreight = false;
		if (isModifyFreight)
			updatedFreight = dealerOrder.updateOrderFreight(cmd.getFreights(), cmd.getUserId());
		if (mOrder.updateAddr(addr) || updatedFreight) {
			mOrder.updateFreight(dealerOrder);
			orderRepository.updateMainOrder(mOrder);
		}
		if (StringUtils.isNotEmpty(attach))
			operationLogManager.operationLog("修改运费及收货地址", attach, mOrder);
		if (updatedFreight || updatedAddr) {
			dealerOrderRepository.updateFreight(dealerOrder);
		}

		// 修改收货地址或运费推送消息
		if (isModifyFreight || isModifyAddress) {
			Map extraMap = new HashMap<>();
			extraMap.put("dealerOrderId", dealerOrder.getId());
			extraMap.put("orderId", dealerOrder.getOrderNo());
			ShopBean shopBean = shopQuery.getShop(dealerOrder.dealerId());
			String shopName = null != shopBean ? shopBean.getShopName() : null;
			extraMap.put("shopName", shopName);
			if (isModifyAddress) {
				extraMap.put("optType", 2);
				orderService.msgPush(1, mOrder.userId(), JsonUtils.toStr(extraMap), dealerOrder.dealerId());
			}
			if (isModifyFreight) {
				extraMap.put("optType", 3);
				orderService.msgPush(1, mOrder.userId(), JsonUtils.toStr(extraMap), dealerOrder.dealerId());
			}
		}
	}
	
	/**
	 * 修改收货地址及运费 新
	 * @param command
	 * @throws NegativeException
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	@EventListener
	public void modifyAddrFreight(UpdateAddrFreightCmd cmd, String attach) throws NegativeException {

		DealerOrder dealerOrder = dealerOrderRepository.getDealerOrderById(cmd.getDealerOrderId());
		if (dealerOrder == null)
			throw new NegativeException(NegativeCode.DEALER_ORDER_IS_NOT_EXIST, "此商家订单不存在.");

		if (!dealerOrder.canUpdateFreight())
			throw new NegativeException(MCode.V_1, "此商家订单处于不能修改状态.");

		ReceiveAddr addr = dealerOrder.getAddr();

		// 是否修改运费
		long longOrderFreight = Utils.convertNeedMoney(cmd.getDealerOrderFreight());
		boolean isModifyFreight = dealerOrder.isModifyOrderFreight(longOrderFreight);
		// 是否修改收货地址
		boolean isModifyAddress = addr.isModifyAddress(cmd.getProvince(), cmd.getProvCode(), cmd.getCity(), cmd.getCityCode(),
				cmd.getArea(), cmd.getAreaCode(), cmd.getStreet(), cmd.getRevPerson(), cmd.getPhone());

		boolean updatedAddr = addr.updateAddr(cmd.getProvince(), cmd.getProvCode(), cmd.getCity(), cmd.getCityCode(),
				cmd.getArea(), cmd.getAreaCode(), cmd.getStreet(), cmd.getRevPerson(), cmd.getPhone()
				, null);
		
		MainOrder mOrder = orderRepository.getOrderById(dealerOrder.getOrderNo());
		if (updatedAddr && isModifyAddress) {
			dealerOrder.updateAddr(addr, cmd.getUserId());
		}
		boolean updatedFreight = false;
		if (isModifyFreight)
			updatedFreight = dealerOrder.updateOrderFreight(longOrderFreight, cmd.getUserId(), cmd.getDealerOrderFreight());
		if (mOrder.updateAddr(addr) || updatedFreight) {
			mOrder.updateFreight(dealerOrder);
			orderRepository.updateMainOrder(mOrder);
		}
		if (StringUtils.isNotEmpty(attach))
			operationLogManager.operationLog("修改运费及收货地址", attach, mOrder);
		if (updatedFreight || updatedAddr) {
			dealerOrderRepository.updateFreight(dealerOrder);
		}

		// 修改收货地址或运费推送消息
		if (isModifyFreight || isModifyAddress) {
			Map extraMap = new HashMap<>();
			extraMap.put("dealerOrderId", dealerOrder.getId());
			extraMap.put("orderId", dealerOrder.getOrderNo());
			ShopBean shopBean = shopQuery.getShop(dealerOrder.dealerId());
			String shopName = null != shopBean ? shopBean.getShopName() : null;
			extraMap.put("shopName", shopName);
			if (isModifyAddress) {
				extraMap.put("optType", 2);
				orderService.msgPush(1, mOrder.userId(), JsonUtils.toStr(extraMap), dealerOrder.dealerId());
			}
			if (isModifyFreight) {
				extraMap.put("optType", 3);
				orderService.msgPush(1, mOrder.userId(), JsonUtils.toStr(extraMap), dealerOrder.dealerId());
			}
		}
	}

	/**
	 * 更新订单（订单详情项）状态<将待收货改为已完成>
	 * 
	 * @param beanList
	 * @throws NegativeException
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	@EventListener(isListening = true)
	public void orderDtlToFinished() throws NegativeException {
		int hour = 168;
		try {
			hour = Integer.parseInt(GetDisconfDataGetter.getDisconfProperty("order.cinfirm"));
			if (hour < 1)
				hour = 1;
		} catch (Exception e) {
			
		}
		List<DealerOrderDtl> dealerOrders = orderDtlRepository.getOrderDtlStatusQeury(hour, 2);

		for (DealerOrderDtl orderDtl : dealerOrders) {
			jobFinishiedOrder(orderDtl);
		}
	}
	
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class,
			NegativeException.class }, propagation = Propagation.REQUIRES_NEW)
	private void jobFinishiedOrder(DealerOrderDtl orderDtl) {
		orderDtl.finished();
		orderDtlRepository.save(orderDtl);
	}
	
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	@EventListener(isListening = true)
	public void orderDtlToDealFinished() throws NegativeException {
		int hour = 168;
		try {
			hour = Integer.parseInt(GetDisconfDataGetter.getDisconfProperty("order.cinfirm"));
			if (hour < 1)
				hour = 1;
		} catch (Exception e) {
			
		}
		List<DealerOrderDtl> dealerOrders = orderDtlRepository.getOrderDtlStatusQeury(hour, 3);

		for (DealerOrderDtl orderDtl : dealerOrders) {
			jobOrderDealFinishied(orderDtl);
		}
	}
	
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class,NegativeException.class }, propagation = Propagation.REQUIRES_NEW)
	private void jobOrderDealFinishied(DealerOrderDtl orderDtl) {
		orderDtl.dealFinished();
		orderDtlRepository.save(orderDtl);
	}
	

	/**
	 * 公用方法
	 * @param dealerOrders
	 * @return
	 * @throws NegativeException
	 */
	public List<DealerOrder> commondMethod(List<DealerOrder> dealerOrders, long hours) throws NegativeException {
		List<DealerOrder> list = new ArrayList<DealerOrder>();
		if (dealerOrders.size() == 0)
			throw new NegativeException(NegativeCode.DEALER_ORDER_IS_NOT_EXIST, "没有满足条件的商家订单.");
		/** 
		 * 计算出超过7天的订单
		 */
		for (DealerOrder bean : dealerOrders) {
			if (((System.currentTimeMillis() - bean.dateToLong()) / (1000 * 60 * 60)) > hours)
				list.add(bean);
		}
		return list;
	}

	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	public void commentSku(String orderId, String skuId, int flag, int sortNo, String dealerOrderId) {
		dealerOrderRepository.updateComment(orderId, skuId, flag, sortNo, dealerOrderId);
	}
	
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	public void commentSku(String orderId, String skuId, int flag, int sortNo) {
		dealerOrderRepository.updateComment(orderId, skuId, flag, sortNo);
	}
	/***
	 * 检测商家订单下的子单完成
	 * @param userId
	 */
	@Transactional(rollbackFor = { Exception.class, RuntimeException.class, NegativeException.class })
	public void dtlCompleteUpdated(String userId) {
		List<String> ids = dealerOrderRepository.getSpecifiedDtlStatus(1);
    	if (ids == null || ids.size() < 1) {
    		return;
    	}
    	//orderIds.add("20171123141428US");
    	
    	for (String a : ids) {
    		jobCompleteDealerOrder(a, userId);
    	}   
	}
	
	@Transactional(rollbackFor = {Exception.class, RuntimeException.class, NegativeException.class},propagation= Propagation.REQUIRES_NEW)
    private void jobCompleteDealerOrder(String id, String userId) {
    	boolean f = dealerOrderRepository.judgeHasAfterSale(id);    
    	DealerOrder m = dealerOrderRepository.getDealerOrderById(id);
    	m.dealComplete(f);
    	dealerOrderRepository.save(m);
    }
}
