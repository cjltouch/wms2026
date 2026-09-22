package com.example.wms.business.office.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 办公用品/消耗品出入库登记实体（仅作记录，不关联供应商/品牌/库存）
 */
@Data
@TableName("wms_office_record")
public class WmsOfficeRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(value = "record_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;

    /** 日期 */
    @TableField("record_date")
    private LocalDate recordDate;

    /** 名称 */
    @TableField("item_name")
    private String itemName;

    /** 类型：1入库 2领取 3报损 */
    @TableField("type")
    private Integer type;

    /** 单位 */
    @TableField("unit")
    private String unit;

    /** 数量 */
    @TableField("quantity")
    private Integer quantity;

    /** 姓名 */
    @TableField("person_name")
    private String personName;

    /** 规格 */
    @TableField("spec")
    private String spec;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 创建人 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer deleted;
}
