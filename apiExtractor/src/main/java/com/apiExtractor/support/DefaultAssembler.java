package com.apiExtractor.support;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.apiExtractor.model.API;
import com.apiExtractor.model.Parameter;

/**
 * 按照前端格式要求装配json字符串
 * <p>
 *
 * @author shenjiafeng 16/11/23.
 */
public class DefaultAssembler {
	
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAssembler.class);
    
    public static JSONObject assemble(List<API> apiList) {

        JSONObject root = new JSONObject();
        root.put("version", 1);

        JSONObject apisValue = new JSONObject();
        JSONObject typesValue = new JSONObject();
        Map<Type, String> typeMap = new HashMap<Type, String>();
      
        for (API api : apiList) {
            if (CollectionUtils.isEmpty(api.getPaths())) {
                continue;
            }
            JSONObject apiObject = new JSONObject();
            if (!StringUtils.isEmpty(api.getDescription())) {
                apiObject.put("description", api.getDescription());
            }
            if (api.getMethods() != null && api.getMethods().size() == 1) {
                apiObject.put("method", String.valueOf(api.getMethods().iterator().next()));
            }
            if (!CollectionUtils.isEmpty(api.getParameters())) {
            	boolean hasRequestBodyAnno=false;
                Map<String, Map<String, Object>> params = new HashMap<String, Map<String, Object>>();
                for (Parameter parameter : api.getParameters()) {
                    Map<String, Object> param = new HashMap<String, Object>();
                    if (!StringUtils.isEmpty(parameter.getDescription())) {
                        param.put("description", parameter.getDescription());
                    }
                    if (parameter.getDefaultValue() != null) {
                        param.put("default", parameter.getDefaultValue());
                    }
                    if (parameter.getAllowedvalues()!= null&&parameter.getAllowedvalues().length>0) {
                        param.put("values", parameter.getAllowedvalues());
                    }
                    
                    if (!StringUtils.isEmpty(parameter.getMock())) {
                        param.put("mock", parameter.getMock());
                    }
                    if (!StringUtils.isEmpty(parameter.getValidate())) {
                        param.put("validate", parameter.getValidate());
                    }
                    if(parameter.getMax()!=null&&parameter.getMax()!=Integer.MAX_VALUE){
                    	param.put("max", parameter.getMax());
                    }
                    if(parameter.getMin()!=null&&parameter.getMin()!=Integer.MIN_VALUE){
                    	param.put("min", parameter.getMin());
                    }
                    if(parameter.isHasRequestBodyAnno()){
                    	hasRequestBodyAnno=true;
                    }
                    param.put("notNull", parameter.isRequired());
                    param.put("type", parseType(parameter.getType(),parameter.getGenericParameterType(), typeMap, typesValue));
                    params.put(parameter.getName(), param);
                }
                if(hasRequestBodyAnno){
                apiObject.put("contentType", "application/json");
                }
                apiObject.put("params", params);
            }
            if (api.getResponse() != null) {
                Map<String, String> response = new HashMap<String, String>();
                if (!StringUtils.isEmpty(api.getResponse().getDescription())) {
                    response.put("description", api.getResponse().getDescription());
                }
                response.put("type", parseType(api.getResponse().getType(),api.getResponse().getGenericParameterType(), typeMap, typesValue));
                apiObject.put("return", response);
            }
            for (String path : api.getPaths()) {
                if (apiObject.containsKey(path)) {
                    LOG.warn("apiExtractor:解析不准确，api重复:" + apiObject);
                    continue;
                }
                apisValue.put(path, apiObject.clone());
            }
        }
        
        root.put("apis", apisValue);
        root.put("types", typesValue);

        return root;
    }

    public static String parseType(Class<?> type,Type genericParameterType,Map<Type, String> typeMap, JSONObject typesValue) {
    	
        String value = typeMap.get(genericParameterType);
        if (value == null) {
            if (type.isArray()) {
                value = type.getComponentType().getCanonicalName();
                parseType(type.getComponentType(),null, typeMap, typesValue);
                value += "[]";
                typeMap.put(genericParameterType, value);
                return value;
            }
            if (type.isEnum()) {
                value = type.getCanonicalName();
                typeMap.put(genericParameterType, value);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("type", "enum");
                Map<String, Map> enumFields = new HashMap<String, Map>();
                for (Object obj : type.getEnumConstants()) {
                    enumFields.put(String.valueOf(obj), new HashMap());
                }
                map.put("fields", enumFields);
                typesValue.put(value, map);
                return value;
            }
            if (CharSequence.class.isAssignableFrom(type)) {
                value = type.getCanonicalName();
                typeMap.put(genericParameterType, value);
                Map<String, String> map = new HashMap<String, String>();
                map.put("native", "string");
                typesValue.put(value, map);
                return value;
            }
            if (Number.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(type))) {
                value = type.getCanonicalName();
                typeMap.put(genericParameterType, value);
                Map<String, String> map = new HashMap<String, String>();
                if(Short.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(type))
                	||Long.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(type))
                	||Integer.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(type))){
                	 map.put("native", "integer");
                }else{
                	 map.put("native", "number");
                }
                typesValue.put(value, map);
                return value;
            }
            if (Boolean.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(type))) {
                value = type.getCanonicalName();
                typeMap.put(genericParameterType, value);
                Map<String, String> map = new HashMap<String, String>();
                map.put("native", "boolean");
                typesValue.put(value, map);
                return value;
            }
            if (Collection.class.isAssignableFrom(type)) {
            	return new CollectionTypeStrategy().parse(type, genericParameterType, typeMap, typesValue);
            }
            if (Map.class.isAssignableFrom(type)) {
            	return new MapTypeStrategy().parse(type, genericParameterType, typeMap, typesValue);
            }
            if (HANDLE_AS_SIMPLE.contains(type)) {
                value = type.getCanonicalName();
                typeMap.put(genericParameterType, value);
                Map<String, String> map = new HashMap<String, String>();
                if(Date.class.isAssignableFrom(type)||java.sql.Date.class.isAssignableFrom(type)){
                   map.put("native", "date");
                }
                typesValue.put(value, map);
                return value;
            }
            if (!type.isPrimitive()) {
            	return new CustomOrRawTypeStrategy().parse(type, genericParameterType, typeMap, typesValue);
            }
        }
        return value;
    }

    
    private static Set<Class<?>> HANDLE_AS_SIMPLE = new HashSet<Class<?>>();
    
    static {
        HANDLE_AS_SIMPLE.add(Object.class);
        HANDLE_AS_SIMPLE.add(Date.class);
        HANDLE_AS_SIMPLE.add(java.sql.Date.class);
        HANDLE_AS_SIMPLE.add(URI.class);
        HANDLE_AS_SIMPLE.add(URL.class);
        HANDLE_AS_SIMPLE.add(Class.class);
        HANDLE_AS_SIMPLE.add(URL.class);
        HANDLE_AS_SIMPLE.add(Locale.class);
    }
}
