package com.example.wms.business.check.entity;

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
@TableName("wms_check_order")
public class WmsCheckOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点单ID */
    @TableId(value = "check_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long checkId;

    /** 盘点单号 */
    private String checkNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 库区ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long areaId;

    /** 库区名称 */
    private String areaName;

    /** 盘点类型 */
    private Integer checkType;

    /** SKU总数 */
    private Integer totalSkuCount;

    /** 盘盈数量 */
    private Integer profitQty;

    /** 盘亏数量 */
    private Integer lossQty;

    /** 盘盈金额 */
    private BigDecimal profitAmount;

    /** 盘亏金额 */
    private BigDecimal lossAmount;

    /** 盘点日期 */
    private LocalDate checkDate;

    /** 单据状态 */
    private Integer status;

    /** 盘点人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long checkerId;

    /** 盘点人姓名 */
    private String checkerName;

    /** 盘点开始时间 */
    private LocalDateTime checkStartTime;

    /** 盘点结束时间 */
    private LocalDateTime checkEndTime;

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

    /** 逻辑删除标志 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer deleted;
}
