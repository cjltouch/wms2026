package com.example.wms.business.loss.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LossHandleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报损单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 处理备注 */
    private String remark;
}
