package com.example.wms.business.stockout.dto.rsp;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 出库明细导出VO（只导出已审核 status=4 的出库单）
 */
@Data
@ColumnWidth(16)
@HeadStyle(fillForegroundColor = 22)
public class StockOutItemExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("出库单号")
    @ColumnWidth(22)
    private String stockOutNo;

    @ExcelProperty("出库类型")
    @ColumnWidth(10)
    private String typeText;

    @ExcelProperty("出库仓库")
    @ColumnWidth(16)
    private String warehouseName;

    @ExcelProperty("客户")
    @ColumnWidth(16)
    private String customerName;

    @ExcelProperty("审核时间")
    @ColumnWidth(18)
    private String auditTime;

    @ExcelProperty("行号")
    @ColumnWidth(6)
    private Integer lineNo;

    @ExcelProperty("SKU编码")
    private String skuCode;

    @ExcelProperty("内部编码")
    private String innerCode;

    @ExcelProperty("商品名称")
    @ColumnWidth(22)
    private String skuName;

    @ExcelProperty("规格")
    @ColumnWidth(18)
    private String specText;

    @ExcelProperty("单位")
    @ColumnWidth(8)
    private String unitName;

    @ExcelProperty("应出数量")
    @ColumnWidth(10)
    private Integer expectedQty;

    @ExcelProperty("实出数量")
    @ColumnWidth(10)
    private Integer actualQty;

    @ExcelProperty("批次号")
    @ColumnWidth(14)
    private String batchNo;

    @ExcelProperty("库位编码")
    @ColumnWidth(12)
    private String locationCode;

    @ExcelProperty("成本单价")
    @ColumnWidth(12)
    private BigDecimal costPrice;

    @ExcelProperty("成本小计")
    @ColumnWidth(12)
    private BigDecimal subtotalCost;

    @ExcelProperty("销售单价")
    @ColumnWidth(12)
    private BigDecimal salePrice;

    @ExcelProperty("销售小计")
    @ColumnWidth(12)
    private BigDecimal subtotalSale;
}
