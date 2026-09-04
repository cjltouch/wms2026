package com.example.wms.business.inventory.entity;

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
@TableName("wms_inventory")
public class WmsInventory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 库存ID */
    @TableId(value = "inventory_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inventoryId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** SKUID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** 库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 过期日期 */
    private LocalDate expireDate;

    /** 供应商批次 */
    private String supplierBatch;

    /** 库存数量 */
    private Integer quantity;

    /** 锁定数量 */
    private Integer lockedQty;

    /** 可用数量 */
    private Integer availableQty;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 库存总金额 */
    private BigDecimal totalAmount;

    /** 最后入库时间 */
    private LocalDateTime lastInTime;

    /** 最后出库时间 */
    private LocalDateTime lastOutTime;

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

    /** 仓库名称（非DB字段，JOIN 查询后填充） */
    @TableField(exist = false)
    private String warehouseName;

    /** SKU编码（非DB字段，JOIN 查询后填充） */
    @TableField(exist = false)
    private String skuCode;

    /** SKU名称（非DB字段，JOIN 查询后填充） */
    @TableField(exist = false)
    private String skuName;

    /** SKU规格文本（非DB字段，JOIN 查询后填充） */
    @TableField(exist = false)
    private String specText;

    /** 供应商名称（非DB字段，按SKU主数据填充） */
    @TableField(exist = false)
    private String supplierName;

    /** 计量单位名称（非DB字段，按SKU主数据填充） */
    @TableField(exist = false)
    private String unitName;

    /** 库位编码（非DB字段，展示用） */
    @TableField(exist = false)
    private String locationCode;

    /** SKU内部编码（非DB字段，JOIN 查询后填充） */
    @TableField(exist = false)
    private String innerCode;
}
