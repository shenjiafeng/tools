package com.tool.webservice.web;

import com.tool.utils.FileUtils;
import com.tool.webservice.vo.BaseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jiafengshen 2018/3/29.
 */
@Controller
public class FileController {

    private static Logger logger = LoggerFactory.getLogger(FileController.class);

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void getApi(HttpServletRequest req, HttpServletResponse resp,
                       @RequestParam() String fp) throws Exception {
        req.setCharacterEncoding("UTF-8");
        String name = req.getParameter("name");//获取要下载的文件名
        //第一步：设置响应类型
        resp.setContentType("application/force-download");//应用程序强制下载
        //第二读取文件
        InputStream in = new FileInputStream(fp);
        //设置响应头，对文件进行url编码
        name = URLEncoder.encode(name, "UTF-8");
        resp.setHeader("Content-Disposition", "attachment;filename=" + name);
        resp.setContentLength(in.available());
        //第三步：老套路，开始copy
        OutputStream out = resp.getOutputStream();
        byte[] b = new byte[1024];
        int len = 0;
        while ((len = in.read(b)) != -1) {
            out.write(b, 0, len);
        }
        out.flush();
        out.close();
        in.close();
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    @ResponseBody
    public BaseVO<List<String>> uploadImg(@RequestParam List<CommonsMultipartFile> file){
        BaseVO<List<String>> baseVO = new BaseVO<List<String>>();
        try {
            List<String> data = new ArrayList<String>();
            for(CommonsMultipartFile ffile:file){
                String url = FileUtils.saveFile(ffile.getOriginalFilename(),ffile.getInputStream());
                data.add(url);
            }
            baseVO.setCode(0);
            baseVO.setData(data);
            baseVO.setMessage("上传图片成功");
        }catch (Exception e){
            baseVO.setCode(1);
            baseVO.setMessage("上传图片失败");
            logger.error("上传图片失败", e);
        }
        return baseVO;
    }
}
