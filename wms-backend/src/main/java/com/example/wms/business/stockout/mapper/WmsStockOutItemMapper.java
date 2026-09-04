package com.example.wms.business.stockout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.stockout.entity.WmsStockOutItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsStockOutItemMapper extends BaseMapper<WmsStockOutItem> {

    List<WmsStockOutItem> selectByStockOutId(@Param("stockOutId") Long stockOutId);

    void deleteByStockOutId(@Param("stockOutId") Long stockOutId);
}
