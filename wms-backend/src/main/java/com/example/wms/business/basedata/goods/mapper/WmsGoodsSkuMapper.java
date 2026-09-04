package com.example.wms.business.basedata.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WmsGoodsSkuMapper extends BaseMapper<WmsGoodsSku> {

    @Delete("DELETE FROM wms_goods_sku WHERE spu_id = #{spuId}")
    int physicalDeleteBySpuId(@Param("spuId") Long spuId);
}
