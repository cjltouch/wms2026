package com.example.wms.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.inventory.entity.WmsInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface WmsInventoryMapper extends BaseMapper<WmsInventory> {

    @Update("UPDATE wms_inventory SET quantity = quantity + #{qty}, available_qty = available_qty + #{qty}, " +
            "total_amount = total_amount + #{amount}, last_in_time = #{lastInTime}, version = version + 1 " +
            "WHERE inventory_id = #{inventoryId} AND version = #{version}")
    int addStock(@Param("inventoryId") Long inventoryId, @Param("qty") Integer qty,
                 @Param("amount") BigDecimal amount, @Param("lastInTime") LocalDateTime lastInTime,
                 @Param("version") Integer version);

    @Update("UPDATE wms_inventory SET quantity = quantity - #{qty}, available_qty = available_qty - #{qty}, " +
            "last_out_time = #{now}, version = version + 1 " +
            "WHERE warehouse_id = #{whId} AND sku_id = #{skuId} " +
            "AND (location_id = #{locId} OR (#{locId} IS NULL AND location_id IS NULL)) " +
            "AND ((batch_no = #{batchNo}) OR (#{batchNo} IS NULL AND batch_no IS NULL)) " +
            "AND available_qty >= #{qty} AND deleted = 0")
    int deductStock(@Param("whId") Long whId, @Param("skuId") Long skuId,
                    @Param("locId") Long locId, @Param("batchNo") String batchNo,
                    @Param("qty") Integer qty, @Param("now") LocalDateTime now);

    @Update("UPDATE wms_inventory SET quantity = quantity + #{qty}, available_qty = available_qty + #{qty}, " +
            "total_amount = total_amount + #{amount}, last_out_time = #{lastOutTime}, version = version + 1 " +
            "WHERE inventory_id = #{inventoryId} AND version = #{version} AND available_qty + #{qty} >= 0")
    int adjustStock(@Param("inventoryId") Long inventoryId, @Param("qty") Integer qty,
                    @Param("amount") BigDecimal amount, @Param("lastOutTime") LocalDateTime lastOutTime,
                    @Param("version") Integer version);

    @Update("UPDATE wms_inventory SET quantity = quantity - #{qty}, available_qty = available_qty - #{qty}, " +
            "total_amount = total_amount - #{amount}, last_out_time = #{lastOutTime}, version = version + 1 " +
            "WHERE inventory_id = #{inventoryId} AND version = #{version} AND available_qty >= #{qty}")
    int subStock(@Param("inventoryId") Long inventoryId, @Param("qty") Integer qty,
                 @Param("amount") BigDecimal amount, @Param("lastOutTime") LocalDateTime lastOutTime,
                 @Param("version") Integer version);

    /** 锁定库存：可用量转锁定量（quantity 不变），可用不足时影响行数为 0 */
    @Update("UPDATE wms_inventory SET locked_qty = locked_qty + #{qty}, available_qty = available_qty - #{qty}, " +
            "version = version + 1 " +
            "WHERE warehouse_id = #{whId} AND sku_id = #{skuId} " +
            "AND (location_id = #{locId} OR (#{locId} IS NULL AND location_id IS NULL)) " +
            "AND ((batch_no = #{batchNo}) OR (#{batchNo} IS NULL AND batch_no IS NULL)) " +
            "AND available_qty >= #{qty} AND deleted = 0")
    int lockStock(@Param("whId") Long whId, @Param("skuId") Long skuId,
                  @Param("locId") Long locId, @Param("batchNo") String batchNo,
                  @Param("qty") Integer qty);

    /** 出库确认（锁定转出库）：quantity 与 locked_qty 同减，available_qty 锁定时已扣不再变动；total_amount 用新值计算 */
    @Update("UPDATE wms_inventory SET quantity = quantity - #{qty}, locked_qty = locked_qty - #{qty}, " +
            "last_out_time = #{now}, version = version + 1, " +
            "total_amount = cost_price * (quantity - #{qty}) " +
            "WHERE warehouse_id = #{whId} AND sku_id = #{skuId} " +
            "AND (location_id = #{locId} OR (#{locId} IS NULL AND location_id IS NULL)) " +
            "AND ((batch_no = #{batchNo}) OR (#{batchNo} IS NULL AND batch_no IS NULL)) " +
            "AND locked_qty >= #{qty} AND deleted = 0")
    int confirmLockStock(@Param("whId") Long whId, @Param("skuId") Long skuId,
                         @Param("locId") Long locId, @Param("batchNo") String batchNo,
                         @Param("qty") Integer qty, @Param("now") LocalDateTime now);

    /** 解锁：锁定量释放回可用量（作废已锁定的出库单时调用） */
    @Update("UPDATE wms_inventory SET locked_qty = locked_qty - #{qty}, available_qty = available_qty + #{qty}, " +
            "version = version + 1 " +
            "WHERE warehouse_id = #{whId} AND sku_id = #{skuId} " +
            "AND (location_id = #{locId} OR (#{locId} IS NULL AND location_id IS NULL)) " +
            "AND ((batch_no = #{batchNo}) OR (#{batchNo} IS NULL AND batch_no IS NULL)) " +
            "AND locked_qty >= #{qty} AND deleted = 0")
    int unlockStock(@Param("whId") Long whId, @Param("skuId") Long skuId,
                    @Param("locId") Long locId, @Param("batchNo") String batchNo,
                    @Param("qty") Integer qty);

    /** 出库反审核回补：quantity 与 locked_qty 同加，库存回到锁定占用状态 */
    @Update("UPDATE wms_inventory SET quantity = quantity + #{qty}, locked_qty = locked_qty + #{qty}, " +
            "version = version + 1 " +
            "WHERE warehouse_id = #{whId} AND sku_id = #{skuId} " +
            "AND (location_id = #{locId} OR (#{locId} IS NULL AND location_id IS NULL)) " +
            "AND ((batch_no = #{batchNo}) OR (#{batchNo} IS NULL AND batch_no IS NULL)) " +
            "AND deleted = 0")
    int restoreLockedStock(@Param("whId") Long whId, @Param("skuId") Long skuId,
                           @Param("locId") Long locId, @Param("batchNo") String batchNo,
                           @Param("qty") Integer qty);
}
