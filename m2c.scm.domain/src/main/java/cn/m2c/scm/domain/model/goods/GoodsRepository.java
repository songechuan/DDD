package cn.m2c.scm.domain.model.goods;

/**
 * 商品
 */
public interface GoodsRepository {
    Goods queryGoodsById(String goodsId);

    void save(Goods goods);
}
