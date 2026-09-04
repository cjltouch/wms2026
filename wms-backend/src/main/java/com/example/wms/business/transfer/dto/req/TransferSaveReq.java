package com.example.wms.business.transfer.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TransferSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 调拨单ID(新增时为空,更新时必填) */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long transferId;

    /** 调拨单号 */
    private String transferNo;

    /** 调出仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outWarehouseId;

    /** 调出仓库名称 */
    private String outWarehouseName;

    /** 调入仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inWarehouseId;

    /** 调入仓库名称 */
    private String inWarehouseName;

    /** 调拨类型 */
    private Integer transferType;

    /** 总数量 */
    private Integer totalQty;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 预计调拨日期 */
    private LocalDate expectDate;

    /** 备注 */
    private String remark;

    /** 调拨明细列表 */
    private List<TransferItemSaveReq> items;
}
