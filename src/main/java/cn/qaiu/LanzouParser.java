package cn.qaiu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v1.3.3 bate
 */
@WebServlet(urlPatterns = "/parser")
public class LanzouParser extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=utf-8");
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.3";
        String param = request.getParameter("url");
        String url = param.substring(0,param.lastIndexOf('/')+1);
        String id = param.substring(param.lastIndexOf('/')+1);
        Map<String,String> header = new HashMap<>();
        header.put("Accept-Language","zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        header.put("referer",url);
        PrintWriter out = null;
        try {
            out = response.getWriter();
            //第一次请求，获取iframe的地址
            String result = Jsoup.connect(url + id)
                    .userAgent(userAgent)
                    .get()
                    .select(".ifr2")
                    .attr("src");

            //第二次请求得到js里的json数据里的sign
            result = Jsoup.connect(url + result)
                    .headers(header)
                    .userAgent(userAgent)
                    .get()
                    .html();
            System.out.println(result);
            Matcher matcher = Pattern.compile("'[\\w]+_c_c'").matcher(result);
            Map<String, String> params = new LinkedHashMap();
            if (matcher.find()) {
                String sn = matcher.group().replace("'", "");
                params.put("action", "downprocess");
                params.put("sign", sn);
                params.put("ves", "1");
                System.out.println(sn);

            } else {
                throw new IOException();
            }
            //第三次请求 通过参数发起post请求,返回json数据
            result = Jsoup
                    .connect(url + "ajaxm.php")
                    .headers(header)
                    .userAgent(userAgent)
                    .data(params)
                    .post()
                    .text()
                    .replace("\\", "");
            //json转为map
            params = new ObjectMapper().readValue(result, Map.class);
            System.out.println(params);
            //通过json的数据拼接出最终的URL发起第最终请求,并得到响应信息头
            url = params.get("dom") + "/file/" + params.get("url");
            Map<String, String> headers = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent(userAgent)
                    .headers(header)
                    .followRedirects(false)
                    .execute()
                    .headers();
            //得到重定向的地址进行重定向
            url = headers.get("Location");
            System.out.println(url);
            response.sendRedirect(url);
        }catch (Exception e){
            out.println("解析失败,请重试!");
            out.close();
        }
    }
}
