package com.apiExtractor.persist;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.apiExtractor.model.API;

/**
 * @author shenjiafeng 16/11/23.
 */
public abstract class PersistService {

	private static final Logger LOG = LoggerFactory.getLogger(PersistService.class);
	
	private static final String  ENCODE = "UTF-8";

    @Setter
    private String storePath = System.getProperty("user.home") + "/api.json";

	public void persist(List<API> apiList) {
	}
    
	public boolean saveToFile(String jsonStr) {
		try {
            if (storePath == null || storePath.length() == 0) {
                LOG.error("api 存储路径为空，无法生成api文档！！！");
            }
			File dataFile = new File(storePath);
			if (!dataFile.exists()) {
				dataFile.createNewFile();
			}
			FileOutputStream out = null;
			BufferedWriter writer = null;
			try {
				out = new FileOutputStream(dataFile);
				BufferedOutputStream stream = new BufferedOutputStream(out);
				writer = new BufferedWriter(new OutputStreamWriter(stream, ENCODE));
				writer.write(jsonStr);
				writer.flush();
				LOG.info("save api jsonString success! path:"+storePath);
			} finally {
				if (writer != null)
					writer.close();
				if (out != null) {
					out.close();
				}
			}
			return true;
		} catch (IOException e) {
			LOG.error("save api jsonString fail!", e);
			return false;
		}
	}
}
