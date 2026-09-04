package com.example.wms.business.purchase.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 采购单ID（新增时为空，更新时必填） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchaseId;

    /** 采购单号 */
    private String purchaseNo;

    /** 供应商ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 采购员ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchaseBy;

    /** 预计到货日期 */
    private LocalDate expectDate;

    /** 税率 */
    private BigDecimal taxRate;

    /** 运费 */
    private BigDecimal freight;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 其他费用 */
    private BigDecimal otherAmount;

    /** 总数量 */
    private Integer totalQty;

    /** 商品金额小计 */
    private BigDecimal subtotal;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 最终金额（合计） */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    /** 采购单明细列表 */
    private List<PurchaseItemSaveReq> items;
}
