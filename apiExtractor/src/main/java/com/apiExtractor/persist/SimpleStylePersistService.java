package com.apiExtractor.persist;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.apiExtractor.model.API;
import com.apiExtractor.support.CustomOrRawTypeStrategy;
import com.apiExtractor.support.DefaultAssembler;

/**
 * 输出格式简化处理
 */
public class SimpleStylePersistService extends PersistService {

	private static final Logger LOG = LoggerFactory.getLogger(SimpleStylePersistService.class);

	public void persist(List<API> apiList) {
		JSONObject root = DefaultAssembler.assemble(apiList);
		JSONObject types = root.getJSONObject("types");
		// 存放需要添加的
		Map<String, Object> needAdd = new HashMap<String, Object>();
		Iterator<Map.Entry<String, Object>> it = types.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			String key = entry.getKey();
			@SuppressWarnings("unchecked")
			Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
			JSONObject valueObject = JSONObject.parseObject(JSONObject.toJSONString(valueMap));
			if (key.contains("<")) {
				JSONObject filedsJSONObject = valueObject.getJSONObject("fields");
				if (filedsJSONObject != null) {
					String className = key.substring(0, key.indexOf("<"));
					String newKey = getNewKey(className);
					if (!needAdd.containsKey(newKey)) {
						valueObject.put("fields", getFieldsInfo(className));
						needAdd.put(newKey, valueObject);
					}
					it.remove();
				}
			}
		}

		for (Map.Entry<String, Object> needAddEntry : needAdd.entrySet()) {
			types.put(needAddEntry.getKey(), needAddEntry.getValue());
		}
		String simpleStyle = JSON.toJSONString(JSON.parseObject(root.toJSONString()), true);
		//LOG.debug("apiExtractor simple style:" + simpleStyle);
		saveToFile(simpleStyle);
	}

	private Map<String, Map<String, Object>> getFieldsInfo(String className) {
		Class<?> ss = TypeUtils.loadClass(className);
		Map<String, Type> actualTypeByTypeVariableNameMap = new HashMap<String, Type>();
		TypeVariable<?>[] typeVariableArr = ss.getTypeParameters();
		for (TypeVariable<?> typeVariable : typeVariableArr) {
			actualTypeByTypeVariableNameMap.put(typeVariable.getName(), typeVariable);
		}
		JSONObject tempTypesValue = new JSONObject();
		Map<Type, String> tempTypeMap = new HashMap<Type, String>();
		CustomOrRawTypeStrategy cus = new CustomOrRawTypeStrategy();
		Map<String, Map<String, Object>> fieldsMap = cus.buildFieldsMap(ss, tempTypeMap, tempTypesValue,
				actualTypeByTypeVariableNameMap);
		return fieldsMap;
	}

	private String getNewKey(String className) {
		Class<?> ss = TypeUtils.loadClass(className);
		TypeVariable<?>[] typeVariableArr = ss.getTypeParameters();
		StringBuilder newkey = new StringBuilder();
		newkey.append(className + "<");
		for (TypeVariable<?> typeVariable : typeVariableArr) {
			newkey.append(typeVariable.getName());
			newkey.append(",");
		}
		newkey.deleteCharAt(newkey.lastIndexOf(","));
		newkey.append(">");
		return newkey.toString();
	}
	
	
	
	

}
