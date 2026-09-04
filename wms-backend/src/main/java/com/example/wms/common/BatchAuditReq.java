package com.example.wms.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class BatchAuditReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> ids;
    private String remark;
    private Integer status;
}
