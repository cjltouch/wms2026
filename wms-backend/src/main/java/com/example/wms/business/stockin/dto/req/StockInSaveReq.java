package com.example.wms.business.stockin.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StockInSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 入库单ID（新增时为空，更新时必填） */
    @JsonAlias("stockInId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stockId;

    /** 入库单号 */
    private String stockInNo;

    /** 入库类型 */
    private Integer type;

    /** 来源单号 */
    private String sourceBillNo;

    /** 来源单明细ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceItemId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 供应商ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 入库人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inBy;

    /** 总数量 */
    private Integer totalQty;

    /** 入库金额合计 */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    /** 入库单明细列表 */
    private List<StockInItemSaveReq> items;
}
