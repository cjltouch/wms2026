package com.example.wms.business.basedata.goods.dto.rsp;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SPU+SKU 导出 VO（SKU 维度，每 SKU 一行）。
 * 表头在 EasyExcel 写入时动态构造，本类字段用于 setter/getter 数据传递。
 */
@Data
@ColumnWidth(14)
@HeadStyle(fillForegroundColor = 22)
public class SpuExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ===== SPU 字段 =====
    private String spuCode;
    private String spuName;
    /** 分类完整路径（从根到叶子，每级一个元素） */
    private String[] categoryPath;
    private String brandName;
    private String unitName;
    private String origin;
    private String abcLevel;
    private String statusText;
    private String createTime;

    // ===== SKU 字段 =====
    private String skuCode;
    private String skuName;
    private String innerCode;
    private String barcode;
    private String specText;
    private String color;
    private Integer weightG;
    private Integer volumeMl;
    private BigDecimal defaultCost;
    private BigDecimal defaultSale;
    private String supplierName;

    public void setCreateTime(LocalDateTime t) {
        this.createTime = t == null ? "" : t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
