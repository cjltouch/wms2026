package com.example.wms.business.purchase.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseItemSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 行号 */
    private Integer lineNo;

    /** SKU ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** SKU 编码 */
    private String skuCode;

    /** 内部编码 */
    private String innerCode;

    /** SKU 名称 */
    private String skuName;

    /** 规格描述 */
    private String specText;

    /** 单位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    /** 单位名称 */
    private String unitName;

    /** 采购数量 */
    private Integer quantity;

    /** 采购单价 */
    private BigDecimal purchasePrice;

    /** 税率 */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 金额小计 */
    private BigDecimal subtotal;

    /** 预计到货日期 */
    private LocalDate expectDate;

    /** 建议批次 */
    private String suggestBatch;

    /** 备注 */
    private String remark;
}
