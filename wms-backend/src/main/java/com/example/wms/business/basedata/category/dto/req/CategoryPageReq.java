package com.example.wms.business.basedata.category.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String categoryName;

    private String categoryCode;

    private String status;
}
