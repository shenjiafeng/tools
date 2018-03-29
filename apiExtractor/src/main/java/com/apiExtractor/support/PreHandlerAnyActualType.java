package com.apiExtractor.support;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import com.alibaba.fastjson.JSONObject;

/**
 * @author shenjiafeng
 *
 */
public class PreHandlerAnyActualType {
	/**
	 * @param actualType
	 *            此参数必须包含泛型真实信息方可调用此方法
	 * @param typeMap
	 * @param typesValue
	 * @return
	 */
	public static String handlerType(Type actualType, Map<Type, String> typeMap, JSONObject typesValue) {
		String value = "";
		// 参数化类型
		if (actualType instanceof ParameterizedType) {
			value = getParameterizedType(actualType, typeMap, typesValue);
			return value;
		}

		if (actualType instanceof WildcardType) {
			value = actualType.toString();
			return value;
		}

		if (actualType instanceof TypeVariable) {
			value = getTypeVariableType(actualType);
			return value;
		}

		if (actualType instanceof GenericArrayType) {
			value = getGenericArrayType((GenericArrayType) actualType, typeMap, typesValue);
			return value;
		}

		Class<?> genericClazz = (Class<?>) actualType;
		value = DefaultAssembler.parseType(genericClazz, actualType, typeMap, typesValue);
		return value;
	}

	private static String getParameterizedType(Type eType, Map<Type, String> typeMap, JSONObject typesValue) {
		ParameterizedType ptt = (ParameterizedType) eType;
		Class<?> cl = (Class<?>) ptt.getRawType();
		return DefaultAssembler.parseType(cl, eType, typeMap, typesValue);
	}

	/**
	 * 递归获取
	 */
	private static String getGenericArrayType(GenericArrayType gt, Map<Type, String> typeMap, JSONObject typesValue) {
		// 返回表示此数组的组件类型的 Type 对象。
		Type componenTtype = gt.getGenericComponentType();
		if (componenTtype instanceof GenericArrayType) {
			return getGenericArrayType((GenericArrayType) componenTtype, typeMap, typesValue) + "[]";
		} else {
			// T[]
			if (componenTtype instanceof TypeVariable) {
				return getTypeVariableType(componenTtype) + "[]";
				// List<String>[]类
			} else {
				return getParameterizedType((ParameterizedType) componenTtype, typeMap, typesValue) + "[]";
			}
		}
	}

	private static String getTypeVariableType(Type type) {
		return type.toString();
	}

}
