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
public @interface ApiParams {

    /**
     * 参数说明，放在方法上，优先级低于放在参数上
     */
    ApiParam[] value();
}