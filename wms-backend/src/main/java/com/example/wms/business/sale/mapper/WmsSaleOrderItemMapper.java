package com.example.wms.business.sale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.sale.entity.WmsSaleOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsSaleOrderItemMapper extends BaseMapper<WmsSaleOrderItem> {

    List<WmsSaleOrderItem> selectBySaleId(@Param("saleId") Long saleId);

    void deleteBySaleId(@Param("saleId") Long saleId);
}
