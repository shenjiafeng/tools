package com.tool.webservice.vo;


import lombok.Data;

/**
 * @author jiafengshen 2018/3/29.
 */
@Data
public class BaseVO<T> {

    private int code;

    private String message;

    private T data;



}
