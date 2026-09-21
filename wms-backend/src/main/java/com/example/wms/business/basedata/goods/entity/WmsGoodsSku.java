package com.example.wms.business.basedata.goods.entity;

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
@TableName("wms_goods_sku")
public class WmsGoodsSku implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "sku_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long spuId;

    /** 主供应商ID（采购下单时按供应商过滤可选商品） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    private String skuCode;

    /** 内部编码 */
    private String innerCode;

    private String barcode;

    private String specText;

    /** 颜色 */
    private String color;

    @TableField("weight")
    private Integer weightG;

    @TableField("volume")
    private Integer volumeMl;

    @TableField(exist = false)
    private Integer batchFlag;

    @TableField(exist = false)
    private Integer expireFlag;

    @TableField(exist = false)
    private Integer snFlag;

    @TableField(exist = false)
    private Integer shelfLifeDays;

    @TableField("cost_price")
    private BigDecimal defaultCost;

    @TableField("sale_price")
    private BigDecimal defaultSale;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer deleted;

    @TableField(exist = false)
    private BigDecimal availableQty;

    private String skuName;

    /** 计量单位ID（单位定义在SPU上，查询时回填，非本表列） */
    @TableField(exist = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    @TableField(exist = false)
    private String unitName;
}
