package com.example.wms.common.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.io.Serial;

@Getter
public class ParamException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final BindingResult bindingResult;

    public ParamException(BindingResult bindingResult) {
        super();
        this.bindingResult = bindingResult;
    }

    public ParamException(String message, BindingResult bindingResult) {
        super(message);
        this.bindingResult = bindingResult;
    }
}
