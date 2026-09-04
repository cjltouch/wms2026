package com.example.wms.business.check.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.check.entity.WmsCheckOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsCheckOrderItemMapper extends BaseMapper<WmsCheckOrderItem> {

    List<WmsCheckOrderItem> selectByCheckId(@Param("checkId") Long checkId);

    void deleteByCheckId(@Param("checkId") Long checkId);
}
