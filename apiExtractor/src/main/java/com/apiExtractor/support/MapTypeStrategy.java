package com.apiExtractor.support;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class MapTypeStrategy implements Strategy {

	private static final Logger LOG = LoggerFactory.getLogger(MapTypeStrategy.class);
	
	@Override
	public String parse(Class<?> type, Type genericParameterType, Map<Type, String> typeMap, JSONObject typesValue) {
		String value = "";
		//Map<K,V>
		if (genericParameterType instanceof ParameterizedType) {
			
			//获取K所代表的Type对象
			ParameterizedType pt = (ParameterizedType) genericParameterType;
			Type kType = pt.getActualTypeArguments()[0];
			
			 //只支持key为String
			if(!(kType instanceof GenericArrayType)&&!(kType instanceof ParameterizedType)&& 
					!(kType instanceof TypeVariable)&&!(kType instanceof WildcardType)
					&&String.class.isAssignableFrom((Class<?>) kType)){
				
				//获取V所代表的Type对象
				Type vType = pt.getActualTypeArguments()[1];
				
				//根据V所代表的具体类型进行处理
				String type1=PreHandlerAnyActualType.handlerType(vType,typeMap,typesValue);
				value=type1+"{}";
				return value;
			}else {
				LOG.warn("apiExtractor:解析不准确  类型错误：只能为Map<String, ...>类型");
			}
			
		//特殊情况  为Map 没使用泛型 
		} else {
			value = type.getCanonicalName();
			typeMap.put(genericParameterType, value);
			typesValue.put(value, new HashMap());
			return value;
		}
		return value;
	}

}
