package com.example.wms.business.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.transfer.entity.WmsTransferOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsTransferOrderItemMapper extends BaseMapper<WmsTransferOrderItem> {

    List<WmsTransferOrderItem> selectByTransferId(@Param("transferId") Long transferId);

    void deleteByTransferId(@Param("transferId") Long transferId);
}
