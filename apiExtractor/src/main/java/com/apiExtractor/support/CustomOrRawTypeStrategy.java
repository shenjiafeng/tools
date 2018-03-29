package com.apiExtractor.support;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.apiExtractor.annotation.ApiParam;
import com.apiExtractor.annotation.ApiResponseObject;

/**
 * @author shenjiafeng
 * 处理原始类型  和自定义类（含自定义泛型）
 * 
 */
public class CustomOrRawTypeStrategy implements Strategy {

	private static final Logger LOG = LoggerFactory.getLogger(CustomOrRawTypeStrategy.class);
	
	public String parse(Class<?> type, Type genericParameterType, Map<Type, String> typeMap, JSONObject typesValue) {
		String value ="";
		Map<String, Type> actualTypeByTypeVariableNameMap = null;
		// 如果为参数化类型，解析出类型变量与实际类型type的对应关系
		if (genericParameterType instanceof ParameterizedType) {
			actualTypeByTypeVariableNameMap = buildActualTypeByTypeVariableNameMap(type, genericParameterType);
			//构造json格式type字段对应的value值
			value = getValue(type, typeMap, typesValue, actualTypeByTypeVariableNameMap);
		}else{
			value=type.getCanonicalName();
		}
		
		typeMap.put(genericParameterType, value);
		Map<String, Object> map = new HashMap<String, Object>();
		Annotation anno=type.getAnnotation(ApiResponseObject.class);
		if(anno!=null){
			map.put("description", (String)AnnotationUtils.getValue(anno, "description"));
		}
		
		Map<String, Map<String, Object>> fieldsMap = buildFieldsMap(type, typeMap, typesValue,actualTypeByTypeVariableNameMap);
		map.put("fields", fieldsMap);
		typesValue.put(value, map);
		return value;
	}
	
	public String getValue(Class<?> type, Map<Type, String> typeMap, JSONObject typesValue,
			Map<String, Type> actualTypeByTypeVariableNameMap) {
		StringBuilder value=new StringBuilder();
		value.append(type.getCanonicalName());
		value.append("<");
		for(Type actualType:actualTypeByTypeVariableNameMap.values()){
			String value1=PreHandlerAnyActualType.handlerType(actualType, typeMap, typesValue);
		    value.append(value1+",");
		}
		value.deleteCharAt(value.lastIndexOf(","));
		value.append(">");
		return value.toString();
	}
	
	public Map<String, Map<String, Object>> buildFieldsMap(Class<?> type, Map<Type, String> typeMap,
			JSONObject typesValue, Map<String, Type> actualTypeByTypeVariableNameMap) {
		Map<String, Map<String, Object>> fieldsMap = new HashMap<String, Map<String, Object>>();
		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(type)) {
			if (!"class".equals(pd.getName()) && pd.getReadMethod() != null
					&& Modifier.isPublic(pd.getReadMethod().getModifiers())) {
				Field field = ReflectionUtils.findField(type, pd.getName());
				if (field != null) {
					Map<String, Object> fieldMap = buildFieldMap(type,typeMap, typesValue, actualTypeByTypeVariableNameMap,field);
					fieldsMap.put(pd.getName(), fieldMap);
				}
			}
		}
		return fieldsMap;
	}

	/*
	 * 属性可能为原始类型、参数化类型、数组类型(其组件类型为参数化类型或类型变量)、类型变量和基本类型。
	 */ 
	private Map<String, Object> buildFieldMap(Class<?> classType, Map<Type, String> typeMap, JSONObject typesValue,
			Map<String, Type> actualTypeByTypeVariableNameMap, Field field) {
		Map<String, Object> fieldMap = new HashMap<String, Object>();
		
		// 属性为类型变量 例如：T
		if (field.getGenericType() instanceof TypeVariable) {
			// 获取属性真实类型
			Type actualFieldType =null;
			if(actualTypeByTypeVariableNameMap!=null){
				actualFieldType = actualTypeByTypeVariableNameMap.get(field.getGenericType().toString());
			}
			if(actualFieldType==null){
				actualFieldType=field.getGenericType();
				LOG.error("apiExtractor 生成结果不准确！请检查自定义泛型类的使用（泛型类的申明侧需指明泛型的真实类型，如方法返回值，否则无法获取泛型真实类型）,当前类信息为："+classType);
			}
			String value1 = PreHandlerAnyActualType.handlerType(actualFieldType, typeMap, typesValue);
			fieldMap.put("type", value1);
			// 属性为数组类型(其组件类型为参数化类型或类型变量)
		} else if (field.getGenericType() instanceof GenericArrayType) {
			GenericArrayType gt = (GenericArrayType) field.getGenericType();
			String type = getGenericArrayTypeFieldType(gt, actualTypeByTypeVariableNameMap, typeMap, typesValue);
			fieldMap.put("type", type);
			// 属性为参数化类型
		} else if ((field.getGenericType() instanceof ParameterizedType)) {
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			String type = getParameterizedTypeFieldMap(pt, actualTypeByTypeVariableNameMap, typeMap, typesValue);
			fieldMap.put("type", type);
		} else {
			fieldMap.put("type",DefaultAssembler.parseType(field.getType(), field.getGenericType(), typeMap, typesValue));
		}
		ApiParam apiParamAnno = field.getAnnotation(ApiParam.class);
		if (apiParamAnno != null) {
			if(!StringUtils.isEmpty(apiParamAnno.description())){
				fieldMap.put("description",apiParamAnno.description());
			}
			
			if(apiParamAnno.max()!=Integer.MAX_VALUE){
				fieldMap.put("max", apiParamAnno.max());
			}
			
			if(apiParamAnno.min()!=Integer.MIN_VALUE){
				fieldMap.put("min", apiParamAnno.min());
			}
			
			if(apiParamAnno.allowedvalues().length>0){
				fieldMap.put("values", apiParamAnno.allowedvalues());
			}
			
			if(!StringUtils.isEmpty(apiParamAnno.mock())){
				fieldMap.put("mock", apiParamAnno.mock());
			}
         
			if(!StringUtils.isEmpty(apiParamAnno.validate())){
				fieldMap.put("validate", apiParamAnno.validate());
			}
		}
		return fieldMap;
	}


	/**
	 * 递归获取
	 * @param classType 
	 */
	private String getGenericArrayTypeFieldType(GenericArrayType gt, Map<String, Type> actualTypeByTypeVariableNameMap,
			Map<Type, String> typeMap, JSONObject typesValue) {
		// 返回表示此数组的组件类型的 Type 对象。
		Type componenTtype = gt.getGenericComponentType();
		if (componenTtype instanceof GenericArrayType) {
			return getGenericArrayTypeFieldType((GenericArrayType) componenTtype, actualTypeByTypeVariableNameMap,
					typeMap, typesValue) + "[]";
		} else {
			// T[]
			if (componenTtype instanceof TypeVariable) {
				// 获取T真实类型
				Type actualTType =null;
				if(actualTypeByTypeVariableNameMap!=null){
					actualTType = actualTypeByTypeVariableNameMap.get(componenTtype.toString());
				}
				if(actualTType==null){
					LOG.error("apiExtractor 生成结果不准确！请检查自定义泛型类的使用（泛型类的申明侧需指明泛型的真实类型，如方法返回值，否则无法获取泛型真实类型）");
					actualTType=componenTtype;
				}
				return PreHandlerAnyActualType.handlerType(actualTType, typeMap, typesValue) + "[]";
				// List<String>[]类
			} else {
				//暂忽略此处actualTypeByTypeVariableNameMap为null情况
				return getParameterizedTypeFieldMap((ParameterizedType) componenTtype, actualTypeByTypeVariableNameMap,
						typeMap, typesValue) + "[]";
			}
		}
	}

	
	/**
	 * 根据 类的actualTypeByTypeVariableNameMap 递归方式获取参数化类型属性（里面可能包含该类的类型变量信息）的真实类型
	 */
	private String getParameterizedTypeFieldMap(ParameterizedType pt, Map<String, Type> actualTypeByTypeVariableNameMap,Map<Type, String> typeMap, JSONObject typesValue) {
		  Class<?> cl = (Class<?>) pt.getRawType();
		  if (Collection.class.isAssignableFrom(cl)) {
			  Type temp=pt.getActualTypeArguments()[0];
			  Type actualFieldType =null;
			  
			  if(actualTypeByTypeVariableNameMap!=null&&actualTypeByTypeVariableNameMap.size()>0){
				  actualFieldType = actualTypeByTypeVariableNameMap.get(temp.toString());
			  }
			  
			  if(actualFieldType==null){
				    if(temp instanceof ParameterizedType){
				    	return getParameterizedTypeFieldMap((ParameterizedType)temp,actualTypeByTypeVariableNameMap,typeMap, typesValue)+"[]";
				    }else if(temp instanceof GenericArrayType){
				    	 GenericArrayType gt = (GenericArrayType)temp;
						 return getGenericArrayTypeFieldType(gt, actualTypeByTypeVariableNameMap, typeMap, typesValue)+"[]";
				    }else if(temp instanceof WildcardType){
				    	//类似 List<Map<String,? extends E>> 作为泛型类的属性  暂不予解析此情况E的具体类型，过于复杂
				    	return temp.toString()+"[]";
					}else if(temp instanceof TypeVariable){
						return temp.toString()+"[]";
					}else{
				    	Class<?> cll = (Class<?>) pt.getActualTypeArguments()[0];
				    	return DefaultAssembler.parseType(cll, temp, typeMap, typesValue)+"[]";
				    }
			  }else{
				  return PreHandlerAnyActualType.handlerType(actualFieldType, typeMap, typesValue)+"[]";
			  }
		  }else if(Map.class.isAssignableFrom(cl)) {
			   Type kType = pt.getActualTypeArguments()[0];
				 //只支持key为String
				if(!(kType instanceof GenericArrayType)&&!(kType instanceof ParameterizedType)&& 
						!(kType instanceof TypeVariable)&&!(kType instanceof WildcardType)
						&&String.class.isAssignableFrom((Class<?>) kType)){
					//获取V所代表的Type对象
					Type vType = pt.getActualTypeArguments()[1];
					
				    Type actualVType =null;
				    if(actualTypeByTypeVariableNameMap!=null&&actualTypeByTypeVariableNameMap.size()>0){
				    	actualVType = actualTypeByTypeVariableNameMap.get(vType.toString());
				    }
				    
					if(actualVType==null){
							//此时vType不可能为 TypeVariable类型
						    if(vType instanceof ParameterizedType){
						    	return getParameterizedTypeFieldMap((ParameterizedType)vType,actualTypeByTypeVariableNameMap,typeMap, typesValue)+"{}";
						    }else if(vType instanceof GenericArrayType){
						    	 GenericArrayType gt = (GenericArrayType)vType;
								 return getGenericArrayTypeFieldType(gt, actualTypeByTypeVariableNameMap, typeMap, typesValue)+"{}";
						    }else if(vType instanceof WildcardType){
						    	//类似 List<Map<String,? extends E>> 作为泛型类的属性  暂不予解析此情况E的具体类型，过于复杂
						    	return vType.toString()+"{}";
							}else if(vType instanceof TypeVariable){
								return vType.toString()+"[]";
							}else{
						    	Class<?> cll = (Class<?>) pt.getActualTypeArguments()[0];
						    	return DefaultAssembler.parseType(cll, vType, typeMap, typesValue)+"{}";
						    }
					}else{
						 return PreHandlerAnyActualType.handlerType(actualVType, typeMap, typesValue)+"{}";
					}
				}else {
					LOG.warn("apiExtractor:解析不准确  类型错误：只能为Map<String, ...>类型");
					return "";
				}
		  }else{
			// 返回表示此类型实际类型参数的 Type 对象的数组
			Type[] typetType = pt.getActualTypeArguments();
			StringBuilder type = new StringBuilder();
			type.append(cl.getCanonicalName());
			type.append("<");
			for (Type typetType1 : typetType) {
				
				Type actualFieldType=null;
				if(actualTypeByTypeVariableNameMap!=null&&actualTypeByTypeVariableNameMap.size()>0){
					actualFieldType = actualTypeByTypeVariableNameMap.get(typetType1.toString());
				}
				
				if (actualFieldType == null) {
					if (typetType1 instanceof ParameterizedType) {
						String temp = getParameterizedTypeFieldMap((ParameterizedType) typetType1,
								actualTypeByTypeVariableNameMap, typeMap, typesValue);
						type.append(temp);
					}else if(typetType1 instanceof GenericArrayType){
				    	GenericArrayType gt = (GenericArrayType)typetType1;
						String temp= getGenericArrayTypeFieldType(gt, actualTypeByTypeVariableNameMap, typeMap, typesValue);
						type.append(temp);
				    }else if(typetType1 instanceof WildcardType){
				    	//类似 List<Map<String,? extends E>> 作为泛型类的属性  暂不予解析此情况E的具体类型，过于复杂
				    	type.append(typetType1.toString());
					}else if(typetType1 instanceof TypeVariable){
						type.append(typetType1.toString());
					}else {
						Class<?> cll = (Class<?>) pt.getActualTypeArguments()[0];
						String temp = DefaultAssembler.parseType(cll, typetType1, typeMap, typesValue);
						type.append(temp);
					}
				} else {
					String temp = PreHandlerAnyActualType.handlerType(actualFieldType, typeMap, typesValue);
					type.append(temp);
				}
				type.append(",");
			}
			type.deleteCharAt(type.lastIndexOf(","));
			type.append(">");
			
			Map<String, Object> map = new HashMap<String, Object>();
			Annotation anno=cl.getAnnotation(ApiResponseObject.class);
			if(anno!=null){
				map.put("description", (String)AnnotationUtils.getValue(anno, "description"));
			}
			
			Map<String, Map<String, Object>> fieldsMap = buildFieldsMap(cl, typeMap, typesValue,actualTypeByTypeVariableNameMap);
			map.put("fields", fieldsMap);
			typesValue.put(type.toString(), map);
		
			return type.toString();
		}
	}
	
	
	private Map<String, Type> buildActualTypeByTypeVariableNameMap(Class<?> type, Type genericParameterType) {
		Map<String, Type> actualTypeByTypeVariableNameMap = new HashMap<String, Type>();
		TypeVariable<?>[] typeVariableArr = type.getTypeParameters();
		ParameterizedType pt = (ParameterizedType) genericParameterType;
		Type[] actualType = pt.getActualTypeArguments();
		for (int i = 0; i < typeVariableArr.length; i++) {
			actualTypeByTypeVariableNameMap.put(typeVariableArr[i].getName(), actualType[i]);
		}
		return actualTypeByTypeVariableNameMap;
	}
	
}
