package com.xq.learn.servlet;

import com.alibaba.fastjson.JSONObject;
import com.xq.learn.annotation.Autowired;
import com.xq.learn.annotation.Controller;
import com.xq.learn.annotation.RequestBody;
import com.xq.learn.annotation.RequestParam;
import com.xq.learn.annotation.ResponseBody;
import com.xq.learn.model.HandlerMapping;
import com.xq.learn.annotation.RequestMapping;
import com.xq.learn.annotation.Service;
import com.xq.learn.model.RequestMethod;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Dispatcher Servlet
 *
 * @author xiaoqiang
 * @date 2019/8/14 1:21
 */
public class DispatcherServlet extends HttpServlet
{
    private static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    private Properties contextConfig = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, List<HandlerMapping>> mappingMap = new HashMap<>();

    @Override

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        processRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        doDispatcher(req, resp);
    }

    /**
     * 分发请求，将相应的请求分发到对应的controller进行处理。
     * @param req req
     * @param resp resp
     * @throws IOException IOException
     */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        if (mappingMap.isEmpty())
        {
            return;
        }
        RequestMethod reqMethod = RequestMethod.resolve(req.getMethod());
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url.replace(contextPath, "").replaceAll("/+", "/");
        // 请求的URL不匹配，返回404
        if (!mappingMap.containsKey(url))
        {
            PrintWriter writer = resp.getWriter();
            writer.write("404 Not Found");
            writer.flush();
            return;
        }

        // 获取URL匹配的HandlerMapping对象
        List<HandlerMapping> handlerMappings = mappingMap.get(url);
        for (HandlerMapping mapping : handlerMappings)
        {
            for (RequestMethod rm : mapping.getRequestMethod())
            {
                if (rm.equals(reqMethod))
                {
                    // 获取方法
                    Method method = mapping.getMethod();
                    // 获取当前对象
                    String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
                    // 获取参数列表，spring通过读取字节码获取方法参数名称，JDK1.8提供了通过反射获取方法参数名称的方法
                    Object[] args = getMethodParameters(req, resp, method);
                    try
                    {
                        // 反射调用方法，这里可以加入interceptor
                        Object result = method.invoke(ioc.get(beanName), args);
                        if (method.isAnnotationPresent(ResponseBody.class))
                        {
                            // @ResponseBody注解的方法，返回值为json
                            resp.setContentType(CONTENT_TYPE);
                            PrintWriter writer = resp.getWriter();
                            writer.write(JSONObject.toJSONString(result));
                            writer.flush();
                            return;
                        }
                    }
                    catch (IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                    catch (InvocationTargetException e)
                    {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        // 1. 加载配置文件
        this.doLoadConfig(config);

        // 2. 解析配置文件，扫描相关的类
        this.doScanner(contextConfig.getProperty("scan.packages"));

        // 3. 初始化所有相关的类，并且报存到IOC容器中
        this.doInstance();

        // 4. 自动依赖注入，DI
        try
        {
            this.doAutowired();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
            throw new ServletException("Failed to autowired");
        }

        // 5. 创建HandlerMapping，建立URL和method的映射关系
        this.initHandlerMapping();
    }

    private void initHandlerMapping()
    {
        if (ioc.isEmpty())
        {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet())
        {
            Class<?> clazz = entry.getValue().getClass();
            // @Controller注解的类中，@RequestMapping的值作为URL
            String baseUrl = "";
            if (!clazz.isAnnotationPresent(Controller.class))
            {
                continue;
            }
            if (clazz.isAnnotationPresent(RequestMapping.class))
            {
                RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = mapping.value();
            }
            // 获取public方法
            Method[] methods = clazz.getMethods();
            List<HandlerMapping> mappings = new ArrayList<>();
            for (Method method : methods)
            {
                if (method.isAnnotationPresent(RequestMapping.class))
                {
                    RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                    // 将多余的/替换掉
                    String url = ("/" + baseUrl + "/" + mapping.value()).replaceAll("/+", "/");
                    HandlerMapping handlerMapping = new HandlerMapping();
                    handlerMapping.setMethod(method);
                    handlerMapping.setRequestMethod(mapping.method());
                    mappings.add(handlerMapping);
                    // 将URL和方法的映射关系保存到map中
                    mappingMap.put(url, mappings);
                }
            }
        }
    }

    private void doAutowired() throws IllegalAccessException
    {
        if (ioc.isEmpty())
        {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet())
        {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            // 将带有@Autowired的属性自动注入到bean中
            for (Field field : fields)
            {
                if (field.isAnnotationPresent(Autowired.class))
                {
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    String beanName = autowired.value();
                    if ("".equals(beanName))
                    {
                        // 接口注入
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    // 给当前对象赋值
                    field.set(entry.getValue(), ioc.get(beanName));
                }
            }
        }

    }

    private void doInstance() throws ServletException
    {
        if (classNames.isEmpty())
        {
            return;
        }
        try
        {
            // 实例化@Controller和@Service注解的类
            for (String className : classNames)
            {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class))
                {
                    Object instance = clazz.newInstance();
                    // Spring默认情况下，以类名小写作为beanName, 当指定了bean名称时，使用指定的beanName
                    String value = clazz.getAnnotation(Controller.class).value();
                    String beanName = "".equals(value) ? lowerFirstCase(clazz.getSimpleName()) : value;
                    // 将bean和对应的实例载入IOC容器，这是spring经典的注册式单例
                    ioc.put(beanName, instance);
                }
                else if (clazz.isAnnotationPresent(Service.class))
                {
                    // @Service注解注入时，需要考虑到接口注入，因此需要将接口的全称作为key
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = "".equals(service.value()) ? lowerFirstCase(clazz.getSimpleName()) : service.value();
                    Object instance = clazz.newInstance();
                    // 使用指定的beanName或者默认类名小写作为beanName
                    ioc.put(beanName, instance);
                    // 使用接口全称作为beanName
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class face : interfaces)
                    {
                        beanName = face.getName();
                        // spring不支持一个接口有多个实现类
                        if (ioc.containsKey(beanName))
                        {
                            throw new ServletException("The bean name is already exists");
                        }
                        ioc.put(beanName, instance);
                    }
                }
                else
                {
                    // doNothing
                }
            }

        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void doScanner(String packages)
    {
        // 扫描指定包下面的所有类
        URL url = this.getClass().getClassLoader().getResource("/" + packages.replaceAll("\\.", "/"));
        File classDir = new File(url.getPath().replace("%20", " "));
        // 递归扫描
        for (File file : classDir.listFiles())
        {
            if (file.isDirectory())
            {
                doScanner(packages + "." + file.getName());
            }
            else
            {
                if (file.getName().endsWith(".class"))
                {
                    String className = (packages + "." + file.getName().replaceAll(".class", "")).trim();
                    // 将扫描得到的类，保存到list中，供doInstance方法反射实例化
                    classNames.add(className);
                }
            }
        }
    }

    private void doLoadConfig(ServletConfig config)
    {
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        // 从类路径下面加载配置文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replace("classpath:", ""));
        try
        {
            contextConfig.load(in);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != in)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取方法参数列表
     * @param req req
     * @param resp resp
     * @param method method
     * @return args
     */
    private Object[] getMethodParameters(HttpServletRequest req, HttpServletResponse resp, Method method) throws ServletException, IOException
    {
        // 获取请求参数
        Map<String, String[]> parameters = req.getParameterMap();
        // 获取方法参数列表
        Parameter[] methodParameters = method.getParameters();
        int paramCount = method.getParameterCount();
        Object[] args = new Object[paramCount];
        for (int i = 0; i < paramCount; i++)
        {
            Parameter parameter = methodParameters[i];
            // 根据类型，注入req和resp
            if (parameter.getType().getTypeName().equals(HttpServletRequest.class.getName()))
            {
                args[i] = req;
                continue;
            }
            if (parameter.getType().getTypeName().equals(HttpServletResponse.class.getName()))
            {
                args[i] = resp;
                continue;
            }
            // 默认注入请求参数名称和方法参数名称相同的参数
            if (parameters.containsKey(parameter.getName()))
            {
                args[i] = String.join(",", parameters.get(parameter.getName()));
            }
            // 根据注解，注入参数
            if (parameter.isAnnotationPresent(RequestParam.class))
            {
                // 注入@RequestParam注解的参数
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                String name = "".equals(requestParam.value()) ? parameter.getName() : requestParam.value();
                if (parameters.containsKey(name))
                {
                    args[i] = String.join(",", parameters.get(name));
                }
                else
                {
                    if (requestParam.required())
                    {
                        // @RequestParam直接的参数为必须值时，没有指定则报错
                        throw new ServletException("Parameter" + name + " is not present.");
                    }
                }
            }
            else if (parameter.isAnnotationPresent(RequestBody.class))
            {
                // 注入@ResponseBody注解的参数, 将req中的值反序列化到object中
                Object object = JSONObject.parseObject(req.getInputStream(), StandardCharsets.UTF_8, parameter.getType());
                args[i] = object;
            }
            else
            {
                // doNothing
            }
        }

        return args;
    }

    private String lowerFirstCase(String name)
    {
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
