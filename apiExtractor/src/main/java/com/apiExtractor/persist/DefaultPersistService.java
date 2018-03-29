package com.apiExtractor.persist;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.apiExtractor.model.API;
import com.apiExtractor.support.DefaultAssembler;

/**
 * @author shenjiafeng 16/11/23.
 */
public class DefaultPersistService extends PersistService {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAssembler.class);

	@Override
	public void persist(List<API> apiList) {

		String jsonString = DefaultAssembler.assemble(apiList).toJSONString();
		String prettyStyle=JSON.toJSONString(JSON.parseObject(jsonString), true);
		//LOG.debug("apiExtractor pretty style:" + prettyStyle);
		saveToFile(prettyStyle);
	}

}
