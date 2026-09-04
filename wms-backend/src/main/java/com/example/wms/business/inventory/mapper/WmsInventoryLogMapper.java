package com.example.wms.business.inventory.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.inventory.entity.WmsInventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WmsInventoryLogMapper extends BaseMapper<com.example.wms.business.inventory.entity.WmsInventoryLog> {
}
