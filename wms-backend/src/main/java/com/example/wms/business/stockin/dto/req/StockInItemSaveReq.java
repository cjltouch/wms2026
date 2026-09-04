package com.example.wms.business.stockin.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockInItemSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细ID（新增时为空，更新时必填） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 来源单明细ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceItemId;

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

    /** 应收数量 */
    private Integer expectedQty;

    /** 实收数量 */
    private Integer actualQty;

    /** 差异数量 */
    private Integer diffQty;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 金额小计 */
    private BigDecimal subtotal;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 过期日期 */
    private LocalDate expireDate;

    /** 供应商批次 */
    private String supplierBatch;

    /** 库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** SN序列号列表 */
    private String snList;

    /** 备注 */
    private String remark;
}
