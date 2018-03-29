package com.tool.webservice.web;

import com.tool.utils.Contant;
import com.tool.utils.FileUtils;
import com.tool.utils.WSDL2JavaUtils;
import com.tool.utils.ZipUtils;
import com.tool.webservice.vo.BaseVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import java.io.FileOutputStream;

@RestController
public class AxisController {

    @RequestMapping("/wsdl/0")
    @ResponseBody
    public BaseVO<String> index(@RequestParam String url, @RequestParam String p){
        BaseVO<String> data = new  BaseVO<String>();
        try{
            Long l = System.currentTimeMillis();
            String path = Contant.PATH+"/wsdl/"+l+"";
            String [] args = {"-o",path,"-p",p,url};
            WSDL2JavaUtils wsdl2java = new WSDL2JavaUtils();
            wsdl2java.parse(args);
            String pathzip = Contant.PATH+"/wsdl/"+l+".zip";
            ZipUtils.toZip(path,new FileOutputStream(pathzip),true);
            data.setData(pathzip);
        }catch (Throwable e){
            data.setCode(1);
            data.setMessage(e.getMessage());
        }
        return data;
    }
    @RequestMapping("/wsdl/1")
    @ResponseBody
    public BaseVO<String> index(@RequestParam CommonsMultipartFile file, @RequestParam String p) throws Exception{
        String url = FileUtils.saveFile(file.getOriginalFilename(),file.getInputStream());
        return index(url,p);
    }





}