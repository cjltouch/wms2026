package com.example.wms.business.basedata.supplier.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SupplierSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private String shortName;

    private String contact;

    private String phone;

    private String tel;

    private String email;

    private String fax;

    private String qq;

    private String wechat;

    private String address;

    private String taxNo;

    private String bankName;

    private String bankAccount;

    private BigDecimal taxRate;

    private String paymentTerms;

    private Integer creditLevel;

    private String status;
}
