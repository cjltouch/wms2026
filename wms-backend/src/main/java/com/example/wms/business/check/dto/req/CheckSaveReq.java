package com.example.wms.business.check.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CheckSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点单ID（新增时为空，编辑时必填） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long checkId;

    /** 盘点单号 */
    private String checkNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 库区ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long areaId;

    /** 库区名称 */
    private String areaName;

    /** 盘点类型 */
    private Integer checkType;

    /** 盘点日期 */
    private LocalDate checkDate;

    /** 盘点人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long checkerId;

    /** 盘点人姓名 */
    private String checkerName;

    /** 备注 */
    private String remark;
}
