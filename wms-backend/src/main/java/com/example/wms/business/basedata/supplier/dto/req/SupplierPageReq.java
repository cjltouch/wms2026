package com.example.wms.business.basedata.supplier.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String supplierName;

    private String supplierCode;

    private String contact;

    private String phone;

    private String status;
}
