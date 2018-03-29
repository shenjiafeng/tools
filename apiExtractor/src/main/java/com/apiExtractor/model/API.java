package com.apiExtractor.model;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;

import java.util.List;
import java.util.Set;

/**
 * @author shenjiafeng 16/11/23.
 */
@Data
public class API {

    private Set<String> paths;
    private Set<RequestMethod> methods;
    private Set<NameValueExpression<String>> params;
    private Set<NameValueExpression<String>> headers;
    private Set<MediaTypeExpression> consumes;
    private Set<MediaTypeExpression> produces;

    private String description;

    private List<Parameter> parameters;
    private Response response;
}
