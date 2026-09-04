package com.example.wms.business.loss.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class LossPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报损单号 */
    private String lossNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 报损类型 */
    private Integer lossType;

    /** 单据状态 */
    private Integer status;
}
