package com.example.wms.business.transfer.entity;

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
@TableName("wms_transfer_order")
public class WmsTransferOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 调拨单主键ID */
    @TableId(value = "transfer_id", type = IdType.ASSIGN_ID)
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

    /** 单据状态 */
    private Integer status;

    /** 调出操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outBy;

    /** 调出时间 */
    private LocalDateTime outTime;

    /** 调入操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inBy;

    /** 调入时间 */
    private LocalDateTime inTime;

    /** 审核人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditBy;

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
