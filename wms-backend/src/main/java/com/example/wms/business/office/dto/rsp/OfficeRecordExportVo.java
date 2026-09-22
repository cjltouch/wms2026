package com.example.wms.business.office.dto.rsp;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用品登记导出VO
 */
@Data
@ColumnWidth(14)
@HeadStyle(fillForegroundColor = 22)
public class OfficeRecordExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("日期")
    @ColumnWidth(12)
    private String recordDate;

    @ExcelProperty("名称")
    @ColumnWidth(20)
    private String itemName;

    @ExcelProperty("类型")
    @ColumnWidth(8)
    private String typeText;

    @ExcelProperty("单位")
    @ColumnWidth(8)
    private String unit;

    @ExcelProperty("数量")
    @ColumnWidth(8)
    private Integer quantity;

    @ExcelProperty("姓名")
    @ColumnWidth(10)
    private String personName;

    @ExcelProperty("规格")
    @ColumnWidth(16)
    private String spec;

    @ExcelProperty("备注")
    @ColumnWidth(24)
    private String remark;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private String createTime;
}
