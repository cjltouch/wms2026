package com.example.wms.business.office.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用品登记保存请求（新增/修改共用）
 */
@Data
public class OfficeRecordSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录ID（修改时必传） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;

    /** 日期（不传默认当天） */
    private LocalDate recordDate;

    /** 名称 */
    @NotBlank(message = "名称不能为空")
    private String itemName;

    /** 类型：1入库 2领取 3报损 */
    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型不合法")
    @Max(value = 3, message = "类型不合法")
    private Integer type;

    /** 单位 */
    private String unit;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于0")
    private Integer quantity;

    /** 姓名 */
    private String personName;

    /** 规格 */
    private String spec;

    /** 备注 */
    private String remark;
}
