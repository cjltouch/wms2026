package com.example.wms.system.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CaptchaRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uuid;

    private String img;

    private Boolean enabled;
}
