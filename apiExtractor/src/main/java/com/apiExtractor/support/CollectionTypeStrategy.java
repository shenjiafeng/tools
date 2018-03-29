package com.apiExtractor.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class CollectionTypeStrategy implements Strategy {

	public String parse(Class<?> type, Type genericParameterType, Map<Type, String> typeMap, JSONObject typesValue) {
		String value = "";
		//Collection<E>
		if (genericParameterType instanceof ParameterizedType) {
			//取得 表示此collection实际类型参数的 Type对象 (即E所表示的)
			ParameterizedType pt = (ParameterizedType) genericParameterType;
			Type eType = pt.getActualTypeArguments()[0];
			//根据E所代表的具体类型进行处理
			String type1=PreHandlerAnyActualType.handlerType(eType,typeMap,typesValue);
			value=type1+"[]";
			return value;
		//特殊情况  为 Collection 不包含泛型
		} else {
			value = type.getCanonicalName();
			typeMap.put(genericParameterType, value);
			typesValue.put(value, new HashMap());
			return value;
		}
	}

}
