package com.example.wms.business.stockout.entity;

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
import java.time.LocalDateTime;

@Data
@TableName("wms_stock_out")
public class WmsStockOut implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 出库单主键ID */
    @TableId(value = "stock_out_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stockOutId;

    /** 出库单号 */
    private String stockOutNo;

    /** 出库类型 */
    @TableField("out_type")
    private Integer type;

    /** 来源单号 */
    private String sourceBillNo;

    /** 来源单据明细ID */
    @TableField("source_bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceItemId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称（冗余） */
    private String warehouseName;

    /** 客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    /** 客户名称（冗余，支持手工录入） */
    private String customerName;

    /** 出库操作人ID */
    @TableField(exist = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outBy;

    /** 分配规则 */
    @TableField(exist = false)
    private Integer allocationRule;

    /** 总数量 */
    @TableField("total_quantity")
    private Integer totalQty;

    /** 货品成本总额 */
    @TableField("goods_amount")
    private BigDecimal totalCost;

    /** 销售总额 */
    @TableField("sale_amount")
    private BigDecimal totalSale;

    /** 单据状态 */
    private Integer status;

    /** 审核人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditBy;

    /** 出库人姓名（查询时按创建人ID解析，不落库） */
    @TableField(exist = false)
    private String outByName;

    /** 审核人姓名（查询时按审核人ID解析，不落库） */
    @TableField(exist = false)
    private String auditName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

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
