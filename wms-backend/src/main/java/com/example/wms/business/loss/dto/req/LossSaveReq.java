package com.example.wms.business.loss.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LossSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报损单ID（新增时为空，更新时必填） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long lossId;

    /** 报损单号 */
    private String lossNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 报损类型 */
    private Integer lossType;

    /** 总数量 */
    private Integer totalQty;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 报损日期 */
    private LocalDate lossDate;

    /** 备注 */
    private String remark;

    /** 报损明细列表 */
    private List<LossItemSaveReq> items;
}
