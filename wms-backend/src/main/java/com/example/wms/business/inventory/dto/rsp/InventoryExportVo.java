package com.example.wms.business.inventory.dto.rsp;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存查询导出VO
 */
@Data
@ColumnWidth(16)
@HeadStyle(fillForegroundColor = 22)
public class InventoryExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("所属仓库")
    @ColumnWidth(18)
    private String warehouseName;

    @ExcelProperty("供应商")
    @ColumnWidth(18)
    private String supplierName;

    @ExcelProperty("SKU编码")
    private String skuCode;

    @ExcelProperty("内部编码")
    private String innerCode;

    @ExcelProperty("商品名称")
    @ColumnWidth(22)
    private String skuName;

    @ExcelProperty("规格")
    @ColumnWidth(22)
    private String specText;

    @ExcelProperty("单位")
    @ColumnWidth(8)
    private String unitName;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("库存数量")
    private Integer quantity;

    @ExcelProperty("锁定数量")
    private Integer lockedQty;

    @ExcelProperty("可用数量")
    private Integer availableQty;

    @ExcelProperty("成本单价")
    private BigDecimal costPrice;

    @ExcelProperty("库存金额")
    private BigDecimal totalAmount;

    @ExcelProperty("生产日期")
    private String produceDate;

    @ExcelProperty("过期日期")
    private String expireDate;
}
