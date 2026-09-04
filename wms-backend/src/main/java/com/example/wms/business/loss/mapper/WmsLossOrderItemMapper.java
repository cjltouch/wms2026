package com.example.wms.business.loss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.loss.entity.WmsLossOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsLossOrderItemMapper extends BaseMapper<WmsLossOrderItem> {

    List<WmsLossOrderItem> selectByLossId(@Param("lossId") Long lossId);

    void deleteByLossId(@Param("lossId") Long lossId);
}
