package com.example.wms.business.inventory.entity;

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
import java.time.LocalDateTime;

@Data
@TableName("wms_inventory_log")
public class WmsInventoryLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long logId;

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

    /** 商品内部编码（SKU明细innerCode，冗余落库便于流水直接展示） */
    private String innerCode;

    /** 单据业务类型（varchar，如 STOCK_IN/STOCK_OUT/TRANSFER/CHECK 等） */
    private String billType;

    /** 单据号 */
    private String billNo;

    /** 关联单据明细ID */
    @TableField("bill_item_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long billItemId;

    /** 变更类型（1入 -1出 0调整，对应 handler.ChangeType 的方向语义——用于前端展示入/出 tag） */
    private Integer changeType;

    /** 变动前数量 */
    private Integer beforeQty;

    /** 变动数量（正数=入，负数=出，统一取绝对值后结合 changeType 显示方向） */
    private Integer changeQty;

    /** 变动后数量 */
    private Integer afterQty;

    /** 变动前锁定数量 */
    private Integer beforeLocked;

    /** 变动锁定数量 */
    private Integer changeLocked;

    /** 变动后锁定数量 */
    private Integer afterLocked;

    /** 单价 */
    private BigDecimal costPrice;

    /** 变动金额 */
    private BigDecimal changeAmount;

    /** 操作备注 */
    private String remark;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operateBy;

    /** 操作人名称（DB 真实列，可能已落库） */
    private String operateName;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 逻辑删除 */
    @com.baomidou.mybatisplus.annotation.TableLogic
    @com.baomidou.mybatisplus.annotation.TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer deleted;

    // ============== 以下为 exist=false 前端展示辅助字段 ==============

    /** 仓库名称（非DB，JOIN 后填充） */
    @TableField(exist = false)
    private String warehouseName;

    /** SKU编码（非DB，JOIN 后填充） */
    @TableField(exist = false)
    private String skuCode;

    /** SKU名称（非DB，JOIN 后填充） */
    @TableField(exist = false)
    private String skuName;

    /** 单据类型中文名称（非DB，由 billType 字符串转义） */
    @TableField(exist = false)
    private String billTypeName;

    /** 变动方向（1入 -1出 0调整，由 changeQty 的正负推导后赋值，前端 log.vue direction 列使用） */
    @TableField(exist = false)
    private Integer direction;

    /** 变动方向名称（入库/出库/调整，非DB） */
    @TableField(exist = false)
    private String directionName;

    /** 变动数量绝对值（兼容前端 log.vue qtyChange 列） */
    @TableField(exist = false)
    private Integer qtyChange;

    /** 单价（兼容前端 log.vue unitPrice 列） */
    @TableField(exist = false)
    private BigDecimal unitPrice;

    /** 变动金额（兼容前端 log.vue amountChange 列） */
    @TableField(exist = false)
    private BigDecimal amountChange;

    /** 关联单据明细ID（兼容前端 itemId 别名） */
    @TableField(exist = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    // ==================== 兼容性 setter：供老的 InventoryChangeHandler 调用 ====================

    /**
     * 兼容老代码 log.setBillId(...)：数据库无 bill_id 列，这里直接丢弃（billNo 已作为单据标识即可）。
     * 保留方法仅用于编译兼容，不写入任何字段。
     */
    public void setBillId(Long billId) {
        // no-op（DB 无此字段）
    }

    /**
     * 兼容老代码 log.setBillType(Integer)（传的是 ChangeType.code 数字）。
     * 注意：方法名必须避开 setBillType，否则 MyBatis 按属性反射匹配时会把 String 的 bill_type 列
     * 错误绑定到这个 Integer 形参的 setter 上，导致 NumberFormatException。
     * 数据库 bill_type 列为 varchar，我们按 code 映射为业务字符串后写入；
     * 无法识别时直接回写数字字符串，避免抛异常。
     */
    public void setBillTypeCode(Integer changeTypeCode) {
        if (changeTypeCode == null) {
            this.billType = null;
            return;
        }
        switch (changeTypeCode) {
            case 1 -> this.billType = "PURCHASE_RETURN";
            case 2 -> this.billType = "STOCK_IN";
            case 3 -> this.billType = "STOCK_OUT_LOCK";
            case 4 -> this.billType = "STOCK_OUT";
            case 5 -> this.billType = "TRANSFER_OUT";
            case 6 -> this.billType = "TRANSFER_IN";
            case 7 -> this.billType = "LOSS";
            case 8 -> this.billType = "CHECK";
            default -> this.billType = String.valueOf(changeTypeCode);
        }
    }

    /**
     * 兼容老代码 log.setDirection(int)：同步更新 changeType（方向标志）。
     * DB 列没有 direction 字段，方向实际由 changeQty 正负决定。
     */
    public void setDirection(Integer dir) {
        this.direction = dir;         // 写入 exist=false 辅助字段（返回前端用）
        if (dir == null) {
            this.changeType = null;
        } else if (dir > 0) {
            this.changeType = 1;
        } else if (dir < 0) {
            this.changeType = -1;
        } else {
            this.changeType = 0;
        }
    }

    /**
     * 兼容老代码 log.setQtyChange(Integer)：直接写入 DB 列 changeQty。
     * 同时同步 exist=false 辅助字段 qtyChange（前端绝对值展示用）。
     */
    public void setQtyChange(Integer qtyChange) {
        this.changeQty = qtyChange;
        // 同时同步 exist=false 前端适配字段（保持绝对值语义，方便前端统一加 ±）
        if (qtyChange == null) {
            this.qtyChange = null;
        } else if (qtyChange >= 0) {
            this.qtyChange = qtyChange;
        } else {
            this.qtyChange = -qtyChange;
        }
    }

    /**
     * 兼容老代码 log.setUnitPrice(...)：同步写入 DB 列 costPrice，以及 exist=false 辅助字段 unitPrice。
     */
    public void setUnitPrice(BigDecimal price) {
        this.costPrice = price;
        this.unitPrice = price;
    }

    /**
     * 兼容老代码 log.setAmountChange(...)：同步写入 DB 列 changeAmount，以及 exist=false 辅助字段 amountChange。
     */
    public void setAmountChange(BigDecimal amount) {
        this.changeAmount = amount;
        this.amountChange = amount;
    }

    /**
     * 兼容老代码 log.setItemId(Long)（handler 传的是明细主键 id 非 bill_item_id）：
     * 直接写入 exist=false 字段即可，DB bill_item_id 可为空。
     */
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
