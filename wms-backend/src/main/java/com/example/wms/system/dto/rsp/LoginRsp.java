package com.example.wms.system.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LoginRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;
}
