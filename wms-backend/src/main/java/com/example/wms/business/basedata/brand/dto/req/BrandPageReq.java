package com.example.wms.business.basedata.brand.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class BrandPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String brandName;

    private String brandCode;

    private String status;
}
