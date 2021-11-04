package com.zzy.diytomcat.http;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.zzy.diytomcat.catalina.Context;
import com.zzy.diytomcat.catalina.Engine;
import com.zzy.diytomcat.catalina.Service;
import com.zzy.diytomcat.util.MiniBrowser;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.Socket;
import java.util.*;

public class Request extends BaseRequest {
    // RequestをServletに渡すためにHttpServletRequestを継承する
    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;
    private Service service;
    private String method;
    private String queryString;
    private Map<String, String[]> parameterMap;
    private Map<String, String> headerMap;

    public Request(Socket socket, Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        parseHttpRequest();
        if (StrUtil.isEmpty(requestString)) return;
        parseUri();
        parseContext();
        parseMethod();

        // uri="/a/index.html" -> uri="/index"
        if (!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri)) uri = "/";
        }
        parseParameters();
        parseHeaders();
    }


    private void parseContext() {
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if (null != context) return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path) {
            path = "/";
        } else {
            path = "/" + path;
        }

        context = engine.getDefaultHost().getContext(path);
        if (null == context) {
            context = engine.getDefaultHost().getContext("/");
        }
    }

    private void parseHttpRequest() throws IOException {
        InputStream inputStream = this.socket.getInputStream();
        // HTTP1.1のデフォルトでは継続的接続のため，trueにするとreadBytes中のwhile文が止まらなくなる。
        byte[] bytes = MiniBrowser.readBytes(inputStream, false);
        requestString = new String(bytes, "utf-8");
        /**
         * 例： requestString
         * GET /b/index.html HTTP/1.1
         * Host: 127.0.0.1:18080
         * Upgrade-Insecure-Requests: 1
         * Accept: text/html,application/xhtml+xml,application/xml;q=0.9,asterisk/asterisk;q = 0.8
         * User - Agent:Mozilla / 5.0 (Macintosh; Intel Mac OS X 10_15_7)AppleWebKit / 605.1 .15 (KHTML, like
         Gecko)Version / 14.1 .2 Safari / 605.1 .15
         * Accept - Language:zh - cn
         * Accept - Encoding:gzip, deflate
         * Connection:keep - alive
         *
         * parameter=value
         * parameter=value
         */
    }

    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length) {
            return values[0];
        }

        return null;
    }

    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString) return;

        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                String[] values = parameterMap.get(name);
                if (null == values) {
                    values = new String[]{value};
                } else {
                    values = ArrayUtil.append(values, value);
                }
                parameterMap.put(name, values);
            }
        }
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public String getHeader(String name){
        if(null == name){
            return null;
        }
        name = name.toLowerCase();
        return headerMap.get(name);
    }

    public Enumeration getHeaderNames(){
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }

    public void parseHeaders(){
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for(int i = 1; i < lines.size(); i++){
            String line = lines.get(i);
            if(0 == line.length()) break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headerMap.put(headerName, headerValue);
        }
    }

    private void parseUri() {
        String temp;

        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }
}
