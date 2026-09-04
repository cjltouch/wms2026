package com.example.wms.business.inventory.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** SKUID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** 库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    /** 批次号 */
    private String batchNo;

    /** 是否只查可用库存（1是 0否） */
    private Integer availableOnly;

    /** 商品关键字（SKU编码/SKU名称模糊搜索） */
    private String keyword;

    /** 查询开始日期（用于库存流水时间过滤） */
    private String startDate;

    /** 查询结束日期（用于库存流水时间过滤） */
    private String endDate;
}
