package com.example.wms.business.basedata.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WmsGoodsSkuMapper extends BaseMapper<WmsGoodsSku> {

    @Delete("DELETE FROM wms_goods_sku WHERE spu_id = #{spuId}")
    int physicalDeleteBySpuId(@Param("spuId") Long spuId);

    /**
     * 统计 SKU 在库存及各业务单据明细中的引用数量（仅统计未逻辑删除的数据）
     *
     * @param skuId SKU ID
     * @return 引用总行数，>0 表示该 SKU 不能删除
     */
    @Select("""
            SELECT
              (SELECT COUNT(1) FROM wms_inventory           WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_stock_in_item       WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_stock_out_item      WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_purchase_order_item WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_transfer_order_item WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_check_order_item    WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_loss_order_item     WHERE sku_id = #{skuId} AND deleted = 0) +
              (SELECT COUNT(1) FROM wms_sale_order_item     WHERE sku_id = #{skuId} AND deleted = 0)
            """)
    long countReferences(@Param("skuId") Long skuId);
}
