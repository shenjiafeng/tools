package com.apiExtractor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.apiExtractor.persist.SimpleStylePersistService;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

import com.alibaba.fastjson.JSON;
import com.apiExtractor.annotation.Api;
import com.apiExtractor.annotation.ApiIgnore;
import com.apiExtractor.annotation.ApiParam;
import com.apiExtractor.annotation.ApiParams;
import com.apiExtractor.annotation.ApiResponse;
import com.apiExtractor.model.API;
import com.apiExtractor.model.Parameter;
import com.apiExtractor.model.Response;
import com.apiExtractor.persist.PersistService;

/**
 * @author shenjiafeng 16/11/23.
 */
@Service
public class SpringMvcApiExtractor implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringMvcApiExtractor.class);

    private static final String NOT_COMPATIBLE_WARN = "apiExtractor:****************SpringMvcApiExtractor bean未配置于spring mvc servlet的配置文件中，或版本不兼容,无法生成API*****************";

    @Setter
    private PersistService persistService = new SimpleStylePersistService();

    private Map<String, RequestMappingHandlerMapping> handlerMappingMap;

    private List<HandlerMethodArgumentResolver> argumentResolvers;

    private ParameterNameDiscoverer parameterNameDiscoverer;

    /**
     * 解决spring4.*版本可能无法正常生成文档问题 
     * （spring4.*以上版本中api所在类可能使用RestController注解替代Controller和ResponseBody注解）  
     */
    private static Class<Annotation> restControllerAnnotation;
    
	static {
		try {
			restControllerAnnotation = (Class<Annotation>) Class
					.forName("org.springframework.web.bind.annotation.RestController");
		} catch (Exception e) {
			//ignore
		}
	}
	
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!init(event.getApplicationContext())) {
            LOG.warn(NOT_COMPATIBLE_WARN);
            return;
        }
        try {
            final List<API> apiList = extract();

            if (persistService != null) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            persistService.persist(apiList);
                        } catch (Exception ex) {
                            LOG.error("apiExtractor Persist meet error:", ex);
                        }
                    }
                }).start();
            }
        } catch (Throwable ex) {
            LOG.error("", ex);
        }

    }

    private boolean init(ApplicationContext applicationContext) {
        Map<String, RequestMappingHandlerMapping> requestMappingHandlerMappingMap = applicationContext
                .getBeansOfType(RequestMappingHandlerMapping.class);
        if (CollectionUtils.isEmpty(requestMappingHandlerMappingMap)) {
            return false;
        }
        RequestMappingHandlerAdapter handlerAdapter = applicationContext
                .getBean(RequestMappingHandlerAdapter.class);
        if (handlerAdapter == null) {
            return false;
        }
        HandlerMethodArgumentResolverComposite resolverComposite = null;
        try {
            Field argumentResolversField = handlerAdapter.getClass().getDeclaredField("argumentResolvers");
            argumentResolversField.setAccessible(true);
            resolverComposite = (HandlerMethodArgumentResolverComposite) argumentResolversField.get(handlerAdapter);
        } catch (NoSuchFieldException e) {
            //ignore
        } catch (IllegalAccessException e) {
            //ignore
        }
        if (resolverComposite == null) {
            return false;
        }
        List<HandlerMethodArgumentResolver> handlerMethodArgumentResolverList = resolverComposite.getResolvers();
        if (CollectionUtils.isEmpty(handlerMethodArgumentResolverList)) {
            return false;
        }
        ParameterNameDiscoverer parameterNameDiscovererFromAdapter = getParameterNameDiscoverer(handlerAdapter);
        if (parameterNameDiscovererFromAdapter == null) {
            return false;
        }
        this.handlerMappingMap = requestMappingHandlerMappingMap;
        this.argumentResolvers = handlerMethodArgumentResolverList;
        this.parameterNameDiscoverer = parameterNameDiscovererFromAdapter;
        return true;
    }

    private List<API> extract() {

        List<API> apiList = new ArrayList<API>();

        for (RequestMappingHandlerMapping handlerMapping : handlerMappingMap.values()) {
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
                RequestMappingInfo mappingInfo = entry.getKey();
                HandlerMethod handlerMethod = entry.getValue();
                if (AnnotationUtils.getAnnotation(handlerMethod.getBeanType(), ApiIgnore.class) != null
                        || AnnotationUtils.getAnnotation(handlerMethod.getMethod(), ApiIgnore.class) != null) {
                    continue;
                }
                apiList.add(extractAPI(mappingInfo, handlerMethod));
            }
        }
        return apiList;
    }

    private API extractAPI(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {

        API api = new API();

        api.setDescription((String) getAnnotationValue(handlerMethod.getMethodAnnotation(Api.class)));

        api.setPaths(mappingInfo.getPatternsCondition().getPatterns());
        api.setMethods(mappingInfo.getMethodsCondition().getMethods());
        api.setParams(mappingInfo.getParamsCondition().getExpressions());
        api.setHeaders(mappingInfo.getHeadersCondition().getExpressions());
        api.setConsumes(mappingInfo.getConsumesCondition().getExpressions());
        api.setProduces(mappingInfo.getProducesCondition().getExpressions());
        api.setParameters(extractParameters(handlerMethod));

		mergeApiParams(api, handlerMethod);
		if (handlerMethod.getMethodAnnotation(ResponseBody.class) != null
				|| (restControllerAnnotation != null && AnnotationUtils.findAnnotation(handlerMethod.getBeanType(),
						restControllerAnnotation) != null)) {
			api.setResponse(extractResponse(handlerMethod));
		}
		return api;
    }

    private List<Parameter> extractParameters(HandlerMethod handlerMethod) {

        List<Parameter> parameterList = new ArrayList<Parameter>();
        MethodParameter[] methodParameters = getMethodParameters(handlerMethod.getMethodParameters());
        for (MethodParameter methodParameter : methodParameters) {
            if (ServletRequest.class.isAssignableFrom(methodParameter.getParameterType())
                    || ServletResponse.class.isAssignableFrom(methodParameter.getParameterType())
                    || Model.class.isAssignableFrom(methodParameter.getParameterType())
                    || ModelMap.class.isAssignableFrom(methodParameter.getParameterType())
                    || methodParameter.hasParameterAnnotation(ApiIgnore.class)) {
                continue;
            }

            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            GenericTypeResolver.resolveParameterType(methodParameter, handlerMethod.getBean().getClass());

            for (HandlerMethodArgumentResolver resolver : argumentResolvers) {
                if (resolver.supportsParameter(methodParameter)) {

                    if (resolver instanceof AbstractNamedValueMethodArgumentResolver) {

                        Parameter parameter = new TempResolver(
                                (AbstractNamedValueMethodArgumentResolver) resolver)
                                .getMyParameter(methodParameter);

                        parameter.setType(methodParameter.getParameterType());
                        parameter.setGenericParameterType(methodParameter.getGenericParameterType());
                        Annotation anno = methodParameter.getParameterAnnotation(ApiParam.class);
                        RequestBody annot = methodParameter.getParameterAnnotation(RequestBody.class);
                        if(annot!=null&&annot.required()){
                        	parameter.setHasRequestBodyAnno(true);
                        }
                        fillOtherAttributeValue(parameter, anno);
                        parameterList.add(parameter);

                    } else if (resolver instanceof ServletModelAttributeMethodProcessor
                            || resolver instanceof RequestResponseBodyMethodProcessor) {
                        Parameter parameter = new Parameter();
                        parameter.setName(methodParameter.getParameterName());
                        parameter.setType(methodParameter.getParameterType());
                        parameter.setGenericParameterType(methodParameter.getGenericParameterType());
                        Annotation anno = methodParameter.getParameterAnnotation(ApiParam.class);
                        RequestBody annot = methodParameter.getParameterAnnotation(RequestBody.class);
                        if(annot!=null&&annot.required()){
                        	parameter.setHasRequestBodyAnno(true);
                        }
                        fillOtherAttributeValue(parameter, anno);
                        parameterList.add(parameter);
                    } else {
                        LOG.warn("apiExtractor参数未成功解析：" + JSON.toJSONString(methodParameter));
                    }
                    break;
                }
            }
        }

        return parameterList;
    }

	private void fillOtherAttributeValue(Parameter parameter, Annotation anno) {
		parameter.setDescription((String) getAnnotationValue(anno));
		parameter.setAllowedvalues((String[])AnnotationUtils.getValue(anno, "allowedvalues"));
		parameter.setMock((String)AnnotationUtils.getValue(anno, "mock"));
		parameter.setValidate((String)AnnotationUtils.getValue(anno, "validate"));
		parameter.setMax((Integer) AnnotationUtils.getValue(anno, "max"));
		parameter.setMin((Integer) AnnotationUtils.getValue(anno, "min"));
	}

    private void mergeApiParams(API api, HandlerMethod handlerMethod) {
        Annotation paramsAnno = handlerMethod.getMethodAnnotation(ApiParams.class);
        if (!CollectionUtils.isEmpty(api.getParameters()) && paramsAnno != null) {
            Annotation[] params = (Annotation[]) AnnotationUtils.getValue(paramsAnno);
            for (Parameter parameter : api.getParameters()) {
                if (StringUtils.isEmpty(parameter.getDescription())) {
                    for (Annotation anno : params) {
                        if (parameter.getName().equals(AnnotationUtils.getValue(anno, "name"))) {
                        	 fillOtherAttributeValue(parameter, anno);
                        }
                    }
                }
            }
        }
    }

    private Response extractResponse(HandlerMethod handlerMethod) {
        Response response = new Response();
        MethodParameter returnParameter = handlerMethod.getReturnType();
        if (returnParameter != null) {
            response.setGenericParameterType(returnParameter.getGenericParameterType());
            response.setType(returnParameter.getParameterType());
        }
        response.setDescription((String) getAnnotationValue(handlerMethod.getMethodAnnotation(ApiResponse.class)));

        return response;
    }

    private Object getAnnotationValue(Annotation anno) {
        if (anno != null) {
            return AnnotationUtils.getValue(anno, "description");
        }
        return null;
    }

    private MethodParameter[] getMethodParameters(MethodParameter[] parameters) {
        MethodParameter[] result = new MethodParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            result[i] = new MethodParameter(parameters[i]);
        }
        return result;
    }

    private ParameterNameDiscoverer getParameterNameDiscoverer(
            RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        try {
            Field field = RequestMappingHandlerAdapter.class.getDeclaredField("parameterNameDiscoverer");
            field.setAccessible(true);
            return (ParameterNameDiscoverer) field.get(requestMappingHandlerAdapter);
        } catch (Exception e) {
            LOG.error("", e);
            return null;
        }
    }

    private class TempResolver extends RequestParamMethodArgumentResolver {
        private AbstractNamedValueMethodArgumentResolver resolver;

        public TempResolver(
                AbstractNamedValueMethodArgumentResolver resolver) {
            super(null, false);
            this.resolver = resolver;
        }

        public Parameter getMyParameter(MethodParameter parameter) {
            try {
                Method getNamedValueInfo = AbstractNamedValueMethodArgumentResolver.class
                        .getDeclaredMethod("getNamedValueInfo", MethodParameter.class);
                getNamedValueInfo.setAccessible(true);
                NamedValueInfo namedValueInfo = (NamedValueInfo) getNamedValueInfo.invoke(resolver, parameter);

                Field nameField = NamedValueInfo.class.getDeclaredField("name");
                nameField.setAccessible(true);
                Field requiredField = NamedValueInfo.class.getDeclaredField("required");
                requiredField.setAccessible(true);
                Field defaultValueField = NamedValueInfo.class.getDeclaredField("defaultValue");
                defaultValueField.setAccessible(true);
                Parameter myParameter = new Parameter();
                myParameter.setName((String) nameField.get(namedValueInfo));
                myParameter.setRequired(requiredField.getBoolean(namedValueInfo));
                myParameter.setDefaultValue((String) defaultValueField.get(namedValueInfo));
                return myParameter;
            } catch (Exception e) {
                LOG.error("", e);
            }
            return null;
        }
    }

}
