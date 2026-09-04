package com.example.wms.business.basedata.unit.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class UnitPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String unitName;

    private String unitCode;

    private String status;
}
