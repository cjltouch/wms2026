package com.example.wms.business.check.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class CheckPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点单号 */
    private String checkNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 盘点类型 */
    private Integer checkType;

    /** 单据状态 */
    private Integer status;
}
