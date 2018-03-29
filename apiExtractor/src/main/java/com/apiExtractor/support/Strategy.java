package com.apiExtractor.support;

import java.lang.reflect.Type;
import java.util.Map;
import com.alibaba.fastjson.JSONObject;

public interface Strategy {

	String parse(Class<?> type, Type genericParameterType, Map<Type, String> typeMap, JSONObject typesValue);
}
