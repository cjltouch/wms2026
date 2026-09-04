package com.example.wms.business.purchase.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wms_purchase_order_item")
public class WmsPurchaseOrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 采购单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchaseId;

    /** 采购单号 */
    private String purchaseNo;

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

    /** 已到货数量 */
    private Integer deliveredQty;

    /** 未到货数量 */
    private Integer unreceivedQty;

    /** 退货数量 */
    private Integer returnedQty;

    /** 预计到货日期 */
    private LocalDate expectDate;

    /** 建议批次 */
    private String suggestBatch;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
