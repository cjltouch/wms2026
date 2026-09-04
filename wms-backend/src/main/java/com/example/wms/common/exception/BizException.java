package com.example.wms.common.exception;

import com.example.wms.common.ResultCode;
import lombok.Getter;

import java.io.Serial;

@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ResultCode code;
    private final Object data;

    public BizException(ResultCode code) {
        super(code.getMsg());
        this.code = code;
        this.data = null;
    }

    public BizException(ResultCode code, String msg) {
        super(msg);
        this.code = code;
        this.data = null;
    }

    public BizException(ResultCode code, Object data) {
        super(code.getMsg());
        this.code = code;
        this.data = data;
    }

    public BizException(String msg) {
        super(msg);
        this.code = ResultCode.SYSTEM_ERROR;
        this.data = null;
    }
}
