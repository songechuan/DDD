package cn.m2c.scm.port.adapter.messaging.rabbitmq.stantard;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateTransactionManager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.m2c.ddd.common.application.configuration.RabbitmqConfiguration;
import cn.m2c.ddd.common.event.ConsumedEventStore;
import cn.m2c.ddd.common.port.adapter.messaging.rabbitmq.ExchangeListener;
import cn.m2c.scm.domain.model.stantard.Stantard;
import cn.m2c.scm.domain.model.stantard.StantardRepository;

public class StantardAddListener extends ExchangeListener{


	@Autowired
	StantardRepository stantardRepository;

	public StantardAddListener(RabbitmqConfiguration rabbitmqConfiguration,
			HibernateTransactionManager hibernateTransactionManager, ConsumedEventStore consumedEventStore) {
		super(rabbitmqConfiguration, hibernateTransactionManager, consumedEventStore);
	}

	@Override
	protected void filteredDispatch(String aType, String aTextMessage) throws Exception {
		JSONObject jsonObjject = JSONObject.parseObject(aTextMessage);
		System.out.println("全部数据==============================>"+aTextMessage);
		JSONObject object = jsonObjject.getJSONObject("event");
		System.out.println("事件数据-------------------------------"+object);
		JSONArray array = object.getJSONArray("standardId");
		System.out.println("取出Id----------------------------------------->"+array);
		List<String> list = array.toJavaList(String.class);
		System.out.println("取出List======================================="+list);
        if (list != null && list.size() > 0) {
        	for (String stantardId : list) {
            	if (null != stantardId) {
            		Stantard stantard = stantardRepository.getStantardByStantardId(stantardId);
            		stantard.used();
            		stantardRepository.saveStantard(stantard);
            	}
    		}
		}
	}

	@Override
	protected String[] listensTo() {
		return new String[]{"cn.m2c.scm.domain.model.goods.event.GoodsAddEvent"};
	}

	@Override
	protected String packageName() {
		return this.getClass().getPackage().getName();
	}
}