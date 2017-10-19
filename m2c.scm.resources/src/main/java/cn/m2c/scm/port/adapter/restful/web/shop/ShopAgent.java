package cn.m2c.scm.port.adapter.restful.web.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.m2c.common.MCode;
import cn.m2c.common.MPager;
import cn.m2c.common.MResult;
import cn.m2c.scm.application.shop.ShopApplication;
import cn.m2c.scm.application.shop.command.ShopInfoUpdateCommand;
import cn.m2c.scm.application.shop.data.bean.ShopBean;
import cn.m2c.scm.application.shop.query.ShopQuery;
import cn.m2c.scm.domain.IDGenerator;

@RestController
@RequestMapping("/shop/sys")
public class ShopAgent {
	private static final Logger log = LoggerFactory.getLogger(ShopAgent.class);
	@Autowired
	ShopApplication shopApplication;

	@Autowired
	ShopQuery query;
	/**
	 * 查询店铺列表
	 * @param dealerName
	 * @param dealerClassify
	 * @param dealerId
	 * @param pageNum
	 * @param rows
	 * @return
	 */
	 @RequestMapping(value = "/shop", method = RequestMethod.GET)
	    public ResponseEntity<MPager> dealerShopList(
	            @RequestParam(value = "dealerName", required = false) String dealerName,
	            @RequestParam(value = "dealerClassify", required = false) String dealerClassify,
	            @RequestParam(value = "dealerId", required = false) String dealerId,
	            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
	            @RequestParam(value = "rows", required = false, defaultValue = "10") Integer rows) {
	        MPager result = new MPager(MCode.V_1);
	        try {
	            List<Map<String, Object>> dealerList = new ArrayList<Map<String,Object>>();
	            Map<String, Object> map = new HashMap<String, Object>();
	            map.put("dealerId", "JXS4485CE8E3EE849F4BAAD4A1A290CD95A");
	            map.put("shopName", "青青草原旗舰店");
	            map.put("dealerName", "轻轻的我来了供应商");
	            map.put("shopIcon", "http://dl.m2c2017.com/3pics/20170822/W8bq135021.jpg");
	            map.put("onSaleGoods", 100);
	            Map<String, Object> map1 = new HashMap<String, Object>();
	            map1.put("dealerId", "JXS2B8734CCD4B8477983CDA97C68D6AF8C");
	            map1.put("shopName", "北大青鸟旗舰店");
	            map1.put("shopIcon", "http://dl.m2c2017.com/3pics/20170822/W8bq135021.jpg");
	            map1.put("onSaleGoods", 100);

	            dealerList.add(map);
	            dealerList.add(map1);
	            result.setContent(dealerList);
	            result.setPager(2, pageNum, rows);
	            result.setStatus(MCode.V_200);
	        } catch (Exception e) {
	        	log.error("店铺列表查询出错", e);
	            result = new MPager(MCode.V_400, "服务器开小差了，请稍后再试");
	        }
	        return new ResponseEntity<MPager>(result, HttpStatus.OK);
	    }
	 
	 
	 /**
		 * 查询店铺详情
		 * @param dealerName
		 * @param dealerClassify
		 * @param dealerId
		 * @param pageNum
		 * @param rows
		 * @return
		 */
		 @RequestMapping(value = "/detail", method = RequestMethod.GET)
		    public ResponseEntity<MResult> dealerShopDetail(
		            @RequestParam(value = "dealerId", required = false) String dealerId) {
			 MResult result = new MResult(MCode.V_1);
		        try {
		            Map<String, Object> map = new HashMap<String, Object>();
		            map.put("dealerId", "JXS4485CE8E3EE849F4BAAD4A1A290CD95A");
		            map.put("shopName", "青青草原旗舰店");
		            map.put("dealerName", "轻轻的我来了供应商");
		            map.put("shopIcon", "http://dl.m2c2017.com/3pics/20170822/W8bq135021.jpg");
		            map.put("onSaleGoods", 100);
		            result.setContent(map);
		            result.setStatus(MCode.V_200);
		        } catch (Exception e) {
		        	log.error("店铺详情出错", e);
		            result = new MPager(MCode.V_400, "服务器开小差了，请稍后再试");
		        }
		        return new ResponseEntity<MResult>(result, HttpStatus.OK);
		    }
		 /**
		  * 修改店铺信息
		  * @param dealerId
		  * @return
		  */
		 @RequestMapping(value = "/shopInfo", method = RequestMethod.PUT)
		    public ResponseEntity<MResult> updateShopInfo(
		            @RequestParam(value = "dealerId", required = true) String dealerId,
		            @RequestParam(value = "shopId", required = true) String shopId,
		            @RequestParam(value = "shopName", required = true) String shopName,
		            @RequestParam(value = "shopIcon", required = true) String shopIcon,
		            @RequestParam(value = "shopIntroduce", required = false) String shopIntroduce,
		            @RequestParam(value = "customerServiceTel", required = true) String customerServiceTel
		            ) {
			 MResult result = new MResult(MCode.V_1);
		        try {
		        	ShopInfoUpdateCommand command = new ShopInfoUpdateCommand(dealerId,shopId, shopName, shopIcon, shopIntroduce, customerServiceTel);
		        	shopApplication.updateShopInfo(command);
		            result.setStatus(MCode.V_200);
		        } catch (Exception e) {
		        	log.error("修改店铺信息出错", e);
		            result = new MPager(MCode.V_400, "服务器开小差了，请稍后再试");
		        }
		        return new ResponseEntity<MResult>(result, HttpStatus.OK);
		    }
		 
		 
		 /**
		  * 新增店铺信息
		  * @param dealerId
		  * @return
		  */
		 @RequestMapping(value = "/shopInfo", method = RequestMethod.POST)
		    public ResponseEntity<MResult> addShopInfo(
		            @RequestParam(value = "dealerId", required = true) String dealerId,
		            @RequestParam(value = "shopName", required = true) String shopName,
		            @RequestParam(value = "shopIcon", required = true) String shopIcon,
		            @RequestParam(value = "shopIntroduce", required = false) String shopIntroduce,
		            @RequestParam(value = "customerServiceTel", required = true) String customerServiceTel
		            ) {
			 MResult result = new MResult(MCode.V_1);
		        try {
		        	String shopId = IDGenerator.get(IDGenerator.SHOP_PREFIX_TITLE);
		        	ShopInfoUpdateCommand command = new ShopInfoUpdateCommand(dealerId,shopId, shopName, shopIcon, shopIntroduce, customerServiceTel);
		        	shopApplication.addShopInfo(command);
		            result.setStatus(MCode.V_200);
		        } catch (Exception e) {
		        	log.error("修改店铺信息出错", e);
		            result = new MPager(MCode.V_400, "服务器开小差了，请稍后再试");
		        }
		        return new ResponseEntity<MResult>(result, HttpStatus.OK);
		    }
		 /**
		  * 查询店铺信息
		  * @param dealerId
		  * @return
		  */
		 @RequestMapping(value = "/shopInfo", method = RequestMethod.GET)
		    public ResponseEntity<MResult> queryShopInfo(
		            @RequestParam(value = "dealerId", required = true) String dealerId
		            ) {
			 MResult result = new MResult(MCode.V_1);
		        try {
		        	ShopBean resultData = query.getShopInfoByDealerId(dealerId);
		        	result.setContent(resultData);
		            result.setStatus(MCode.V_200);
		        } catch (Exception e) {
		        	log.error("根据id查询店铺详情出错", e);
		            result = new MPager(MCode.V_400, "服务器开小差了，请稍后再试");
		        }
		        return new ResponseEntity<MResult>(result, HttpStatus.OK);
		    }
	
}