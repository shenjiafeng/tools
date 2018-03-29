package com.apiExtractor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author shenjiafeng 16/11/23.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResponseObject {

    /**
     * 响应结果对象说明
     * @return
     */
    String description() default "";
}
