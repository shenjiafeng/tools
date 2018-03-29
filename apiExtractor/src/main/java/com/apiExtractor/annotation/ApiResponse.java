package com.apiExtractor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author shenjiafeng 16/11/23.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResponse {

    /**
     * 响应结果说明
     * @return
     */
    String description() default "";
}
