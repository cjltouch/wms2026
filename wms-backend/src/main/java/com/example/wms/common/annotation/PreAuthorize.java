package com.example.wms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreAuthorize {

    /**
     * 需要拥有的单一权限标识
     */
    String hasAuthority() default "";

    /**
     * 拥有其中任意一个权限即可通过
     */
    String[] hasAnyAuthority() default {};
}
