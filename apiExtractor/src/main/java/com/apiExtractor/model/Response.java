package com.apiExtractor.model;

import java.lang.reflect.Type;

import lombok.Data;

/**
 * @author shenjiafeng 16/11/23.
 */
@Data
public class Response {
    private Class<?> type;
    private String description;
    private Type genericParameterType;
}
