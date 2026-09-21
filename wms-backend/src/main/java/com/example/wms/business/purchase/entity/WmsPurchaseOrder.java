package com.example.wms.business.purchase.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wms_purchase_order")
public class WmsPurchaseOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 采购单主键ID */
    @TableId(value = "purchase_id", type = IdType.ASSIGN_ID)
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
    @TableField("purchaser_id")
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
    @TableField("other_fee")
    private BigDecimal otherAmount;

    /** 总数量 */
    @TableField("total_quantity")
    private Integer totalQty;

    /** 商品金额小计 */
    @TableField("goods_amount")
    private BigDecimal subtotal;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 最终金额（合计） */
    @TableField("final_amount")
    private BigDecimal totalAmount;

    /** 单据状态 */
    private Integer status;

    /** 审核人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditBy;

    /** 审核人姓名（非持久化，查询时回填） */
    @TableField(exist = false)
    private String auditName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 已到货数量 */
    private Integer deliveredQty;

    /** 未到货数量 */
    private Integer unreceivedQty;

    /** 退货数量 */
    private Integer returnedQty;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    /** 创建人姓名（非持久化，查询时回填） */
    @TableField(exist = false)
    private String createName;

    /** 采购员姓名（非持久化，查询时回填） */
    @TableField(exist = false)
    private String purchaserName;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 逻辑删除标识 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer deleted;
}
