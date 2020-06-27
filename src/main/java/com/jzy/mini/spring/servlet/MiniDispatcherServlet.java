package com.jzy.mini.spring.servlet;

import com.jzy.mini.spring.annotation.MiniAutowired;
import com.jzy.mini.spring.annotation.MiniComponent;
import com.jzy.mini.spring.annotation.MiniController;
import com.jzy.mini.spring.annotation.MiniRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author jinziyu
 * @date 2020/6/23 15:34
 */
public class MiniDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMap = new HashMap<>();

    /**
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 2.扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));
        // 3.实例化相关类
        doInstance();
        // 4.依赖注入
        doAutowired();
        // 5.初始化 HandlerMapping
        initHandlerMapping();

        System.out.println("Initialization is complete.");
    }

    private void doLoadConfig(String contextConfigLocation) {
        // 在 classpath 路径下寻找
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
                continue;
            }
            if (!file.getName().endsWith(".class")) {
                continue;
            }
            String className = scanPackage + "." + file.getName().replace(".class", "");
            classNames.add(className);
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> cls = Class.forName(className);
                if (!cls.isAnnotationPresent(MiniComponent.class)) {
                    continue;
                }
                Object instance = cls.newInstance();

                // 1.bean's default name = 类名首字母小写
                String beanName = toLowerFirsLetter(cls.getSimpleName());
                // 2.不同包下重名
                MiniComponent component = cls.getAnnotation(MiniComponent.class);
                if (!"".equals(component.value())) {
                    beanName = component.value();
                }
                // 3.如果是接口, 存储接口的全类名
                for (Class<?> interfaceCls : cls.getInterfaces()) {
                    if (ioc.containsKey(interfaceCls.getName())) {
                        throw new Exception(String.format("%s has exist.", interfaceCls.getName()));
                    }
                    ioc.put(interfaceCls.getName(), instance);
                }
                if (ioc.containsKey(beanName)) {
                    throw new Exception(String.format("%s has exist.", beanName));
                }
                ioc.put(beanName, instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MiniAutowired.class)) {
                    continue;
                }
                MiniAutowired autowired = field.getAnnotation(MiniAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                // 强制访问
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> cls = entry.getValue().getClass();
            if (!cls.isAnnotationPresent(MiniController.class)) {
                continue;
            }
            String prefixUrl = "";
            if (cls.isAnnotationPresent(MiniRequestMapping.class)) {
                prefixUrl = cls.getAnnotation(MiniRequestMapping.class).value();
            }
            for (Method method : cls.getMethods()) {
                if (!method.isAnnotationPresent(MiniRequestMapping.class)) {
                    continue;
                }
                MiniRequestMapping requestMapping = method.getAnnotation(MiniRequestMapping.class);
                String targetUrl = ("/" + prefixUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMap.put(targetUrl, method);
                System.out.println(String.format("handlerMapping, Url [%s] to Method [%s]", targetUrl, method.getName()));
            }
        }
    }

    private String toLowerFirsLetter(String classSimpleName) {
        char[] chs = classSimpleName.toCharArray();
        chs[0] += 32;
        return String.valueOf(chs);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!handlerMap.containsKey(url)) {
            resp.getWriter().write("404 Not Found.");
        }
        Method method = handlerMap.get(url);
        Map<String, String[]> params = req.getParameterMap();

        method.invoke(, )
    }

}
