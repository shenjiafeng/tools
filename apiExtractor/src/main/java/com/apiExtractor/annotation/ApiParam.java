package com.apiExtractor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author shenjiafeng 16/11/23.
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiParam {

    /**
     *  参数名称，加在方法入参上或者类属性上则不需要填写
     * @return
     */
    String name() default "";

    /**
     *  参数简介
     * @return
     */
    String description();

    /**
	 * An array representing the allowed values this parameter can have. Default value is *
	 * @return
	 */
	String[] allowedvalues() default {};
    
	/**
     *  mock数据
     * @return
     */
	String mock() default "";
	
	/**
     *  可选：数据校验类型。比如 "email" | "phone" | "money" | "username" | "password" | "address" | "idcard" | "passport"| "gps"
     * @return
     */
	String validate() default "";
	
	/**
     *  这个参数允许的最大值  如果参数为字符串类型为字符串的最大长度
     * @return
     */
	int max() default Integer.MAX_VALUE;
	
	/**
     *  这个参数允许的最小值  如果参数为字符串类型为字符串的最小长度
     * @return
     */
	int min() default Integer.MIN_VALUE;
}