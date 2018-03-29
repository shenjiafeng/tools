package com.apiExtractor.annotation;

import java.lang.annotation.*;

/**
 * @author shenjiafeng 16/11/23.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Api {

    /**
     * 接口描述   
     *
     * @return
     */
    String description();
}
