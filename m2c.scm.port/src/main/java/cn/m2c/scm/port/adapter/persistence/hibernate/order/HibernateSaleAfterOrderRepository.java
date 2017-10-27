package cn.m2c.scm.port.adapter.persistence.hibernate.order;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import cn.m2c.ddd.common.port.adapter.persistence.hibernate.HibernateSupperRepository;
import cn.m2c.scm.domain.model.order.DealerOrder;
import cn.m2c.scm.domain.model.order.DealerOrderDtl;
import cn.m2c.scm.domain.model.order.SaleAfterOrder;
import cn.m2c.scm.domain.model.order.SaleAfterOrderRepository;
/**
 * 售后订单仓储
 * @author fanjc
 * <br>created date 2017年10月14日
 * <br>copyrighted@m2c
 */
@Repository
public class HibernateSaleAfterOrderRepository extends HibernateSupperRepository implements SaleAfterOrderRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(HibernateSaleAfterOrderRepository.class);
	@Override
	public void save(SaleAfterOrder order) {
		// TODO Auto-generated method stub
		this.session().save(order);
	}

	@Override
	public SaleAfterOrder getSaleAfterOrderByNo(String saleAfterNo) {
		// TODO Auto-generated method stub
		StringBuilder sql = new StringBuilder("select * from t_scm_order_after_sell where after_sell_order_id =:saleAfterNo");
		Query query = this.session().createSQLQuery(sql.toString()).addEntity(SaleAfterOrder.class);
		query.setParameter("saleAfterNo", saleAfterNo);
		return (SaleAfterOrder)query.uniqueResult();
	}
	
	@Override
	public void updateSaleAfterOrder(SaleAfterOrder order) {
		Session s = this.session();
		s.save(order);
	}

	@Override
	public DealerOrder getDealerOrderByNo(String dealerOrderId) {
		// TODO Auto-generated method stub
		StringBuilder sql = new StringBuilder("select * from t_scm_order_dealer where dealer_order_id =:dealerOrderId");
		Query query = this.session().createSQLQuery(sql.toString()).addEntity(DealerOrder.class);
		query.setParameter("dealerOrderId", dealerOrderId);
		return (DealerOrder)query.uniqueResult();
	}

	@Override
	public DealerOrderDtl getDealerOrderDtlBySku(String dealerOrderId, String skuId) {
		// TODO Auto-generated method stub
		StringBuilder sql = new StringBuilder("select * from t_scm_order_detail where dealer_order_id =:dealerOrderId and sku_id=:skuId");
		Query query = this.session().createSQLQuery(sql.toString()).addEntity(DealerOrderDtl.class);
		query.setParameter("dealerOrderId", dealerOrderId);
		query.setParameter("skuId", skuId);
		return (DealerOrderDtl)query.uniqueResult();
	}
	@Override
	public SaleAfterOrder getSaleAfterOrderByNo(String saleAfterNo, String dealerId) {
		StringBuilder sql = new StringBuilder("select * from t_scm_order_after_sell where after_sell_order_id =:saleAfterNo");
		sql.append(" and dealer_id=:dealerId");
		Query query = this.session().createSQLQuery(sql.toString()).addEntity(SaleAfterOrder.class);
		query.setParameter("saleAfterNo", saleAfterNo);
		query.setParameter("dealerId", dealerId);
		return (SaleAfterOrder)query.uniqueResult();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List<SaleAfterOrder> getSaleAfterOrderStatusAgree() {
		return this.session().createSQLQuery("FROM SaleAfterOrder WHERE status = 4").list();
		
	}
}
