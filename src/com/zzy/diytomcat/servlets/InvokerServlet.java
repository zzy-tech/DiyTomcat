package com.zzy.diytomcat.servlets;

import cn.hutool.core.util.ReflectUtil;
import com.zzy.diytomcat.catalina.Context;
import com.zzy.diytomcat.http.Request;
import com.zzy.diytomcat.http.Response;
import com.zzy.diytomcat.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Tomcatでの処理：
 *  1. InvokerServletでServletを処理
 *  2. DefaultServletで静的リソースを処理
 *  3. JspServletでjspファイルを処理
 */
public class InvokerServlet extends HttpServlet {
    // シングルトン
    private static InvokerServlet instance = new InvokerServlet();

    public static InvokerServlet getInstance() {
        return instance;
    }

    private InvokerServlet() {
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        try {
            Class<?> servletClass = context.getWebClassLoader().loadClass(servletClassName);
            Object servletObject = context.getServlet(servletClass);
            ReflectUtil.invoke(servletObject, "service", request, response);

            if(null != response.getRedirectPath()){
                response.setStatus(Constant.CODE_302);
            } else {
                response.setStatus(Constant.CODE_200);
            }

        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
