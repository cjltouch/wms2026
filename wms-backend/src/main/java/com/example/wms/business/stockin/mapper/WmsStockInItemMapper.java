package com.example.wms.business.stockin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.stockin.entity.WmsStockInItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsStockInItemMapper extends BaseMapper<WmsStockInItem> {

    List<WmsStockInItem> selectByStockInId(@Param("stockInId") Long stockInId);

    void deleteByStockInId(@Param("stockInId") Long stockInId);
}
