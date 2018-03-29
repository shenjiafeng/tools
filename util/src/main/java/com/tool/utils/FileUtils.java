/**
 * Copyright (C), 2011-2018, 微贷网.
 */
package com.tool.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @author jiafengshen 2018/3/29.
 */
public class FileUtils {

    private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String saveFile(String fileName, InputStream in){

        FileOutputStream out  = null;
        String path = Contant.PATH+"/upload/"+System.currentTimeMillis()+"/"+fileName;
        try{
             out  = new FileOutputStream(path);
            byte[] b = new byte[1024];
            int len = 0;
            while ((len = in.read(b)) != -1) {
                out.write(b, 0, len);
            }
        }catch (Exception e){
            logger.error("",e);
            path = null;
        }finally {
            if(out != null){
                try{
                    out.flush();
                    out.close();
                }catch (Exception e){
                    logger.error("",e);
                }
            }
        }
        return path ;
    }

}
