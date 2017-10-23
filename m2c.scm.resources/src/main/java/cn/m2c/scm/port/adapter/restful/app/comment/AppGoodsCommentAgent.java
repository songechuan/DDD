package cn.m2c.scm.port.adapter.restful.app.comment;

import cn.m2c.common.MCode;
import cn.m2c.common.MResult;
import cn.m2c.scm.application.comment.GoodsCommentApplication;
import cn.m2c.scm.application.comment.command.AddGoodsCommentCommand;
import cn.m2c.scm.application.goods.query.GoodsQueryApplication;
import cn.m2c.scm.application.goods.query.data.representation.GoodsSkuInfoRepresentation;
import cn.m2c.scm.domain.IDGenerator;
import cn.m2c.scm.domain.NegativeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品评论
 */
@RestController
@RequestMapping("/goods/comment/app")
public class AppGoodsCommentAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppGoodsCommentAgent.class);


    @Autowired
    GoodsQueryApplication goodsQueryApplication;
    @Autowired
    GoodsCommentApplication goodsCommentApplication;

    /**
     * 发布评价
     *
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResponseEntity<MResult> addGoodsComment(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "skuId", required = false) String skuId,
            @RequestParam(value = "buyerId", required = false) String buyerId,
            @RequestParam(value = "buyerName", required = false) String buyerName,
            @RequestParam(value = "buyerPhoneNumber", required = false) String buyerPhoneNumber,
            @RequestParam(value = "buyerIcon", required = false) String buyerIcon,
            @RequestParam(value = "starLevel", required = false) Integer starLevel,
            @RequestParam(value = "commentContent", required = false) String commentContent,
            @RequestParam(value = "commentImages", required = false) String commentImages
    ) {
        MResult result = new MResult(MCode.V_1);

        // 查询商品信息
        GoodsSkuInfoRepresentation info = goodsQueryApplication.queryGoodsBySkuId(skuId);
        if (null == info) {
            result = new MResult(MCode.V_300, "商品信息不存在");
            return new ResponseEntity<MResult>(result, HttpStatus.OK);
        }
        String id = IDGenerator.get(IDGenerator.SCM_GOODS_PREFIX_TITLE);
        AddGoodsCommentCommand command = new AddGoodsCommentCommand(id, orderId, skuId, info.getSkuName(), buyerId, buyerName,
                buyerPhoneNumber, buyerIcon, commentContent, commentImages,
                info.getGoodsId(), info.getGoodsName(), info.getDealerId(), info.getDealerName(), starLevel);
        try {
            goodsCommentApplication.addGoodsComment(command);
            result.setStatus(MCode.V_200);
        } catch (NegativeException ne) {
            LOGGER.error("addGoodsComment NegativeException e:", ne);
            result = new MResult(ne.getStatus(), ne.getMessage());
        } catch (Exception e) {
            LOGGER.error("addGoodsComment Exception e:", e);
            result = new MResult(MCode.V_400, "添加评论失败");
        }
        return new ResponseEntity<MResult>(result, HttpStatus.OK);
    }
}