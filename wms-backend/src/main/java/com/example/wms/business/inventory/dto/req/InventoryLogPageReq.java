package com.example.wms.business.inventory.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryLogPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联单据号 */
    private String billNo;

    /** 单据类型（兼容两种：varchar 字符串如 STOCK_IN，或数字代码如 2=入库） */
    private Object billType;

    /** 变动方向（入/出） */
    private Integer direction;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** SKUID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** SKU编码（用于前端搜索） */
    private String skuCode;

    /** 批次号 */
    private String batchNo;

    /** 查询开始日期 */
    private String startDate;

    /** 查询结束日期 */
    private String endDate;
}
