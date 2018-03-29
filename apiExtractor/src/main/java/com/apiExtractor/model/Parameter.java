package com.apiExtractor.model;

import java.lang.reflect.Type;

import lombok.Data;

/**
 * @author shenjiafeng 16/11/23.
 */
@Data
public class Parameter {

    private String name;
    private Class<?> type;
    private boolean required;
    private String defaultValue;
    private String[] allowedvalues;
    private String mock;
    private String validate;
    private Integer max;
    private Integer min;
    private String description;
    private Type genericParameterType;
    private boolean hasRequestBodyAnno;
    
}
