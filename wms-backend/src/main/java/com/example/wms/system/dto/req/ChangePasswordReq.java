package com.example.wms.system.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自助修改密码请求
 */
@Data
public class ChangePasswordReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原密码 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
