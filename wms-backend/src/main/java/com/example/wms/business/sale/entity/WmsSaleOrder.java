package com.example.wms.business.sale.entity;

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
@TableName("wms_sale_order")
public class WmsSaleOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 销售单主键ID */
    @TableId(value = "sale_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long saleId;

    /** 销售单号 */
    private String saleNo;

    /** 客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 销售类型 */
    private Integer saleType;

    /** 总数量 */
    private Integer totalQty;

    /** 商品金额 */
    private BigDecimal goodsAmount;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 销售金额 */
    private BigDecimal saleAmount;

    /** 已收金额 */
    private BigDecimal receivedAmount;

    /** 付款状态 */
    private Integer payStatus;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货人地址 */
    private String receiverAddress;

    /** 快递公司 */
    private String expressCompany;

    /** 快递单号 */
    private String expressNo;

    /** 快递费用 */
    private BigDecimal expressFee;

    /** 销售日期 */
    private LocalDate saleDate;

    /** 单据状态 */
    private Integer status;

    /** 审核人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 出库单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stockOutId;

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

    /** 逻辑删除标志 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer deleted;
}
