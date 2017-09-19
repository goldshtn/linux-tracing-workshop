package perros;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Get {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Post {
    String value() default "";
}

class Request {
    private HttpExchange exchange;
    private Map<String, String> queryParams;
    private Map bodyJson;
    private static ScriptEngine engine;

    public Request(HttpExchange exch) {
        exchange = exch;
        String query = exch.getRequestURI().getQuery();
        queryParams = parseQuery(query);
        bodyJson = parseBody(exch.getRequestBody());
    }

    public InputStream requestBody() {
        return exchange.getRequestBody();
    }

    public OutputStream responseBody() {
        return exchange.getResponseBody();
    }

    public Map<String, String> queryParams() {
        return queryParams;
    }

    public Map bodyJson() {
        return bodyJson;
    }

    public String method() {
        return exchange.getRequestMethod();
    }

    public void badRequest() throws IOException {
        finish(400);
    }

    public void ok() throws IOException {
        finish(200);
    }

    public void unauthorized() throws IOException {
        finish(403);
    }

    public void internalServerError(String error) throws IOException {
        finish(500, "Internal Server Error: " + error);
    }

    public void finish(int status) throws IOException {
        exchange.sendResponseHeaders(status, 0);
        exchange.getResponseBody().close();
    }

    public void finish(int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private Map parseBody(InputStream stream) {
        String bodyString = "";
        try {
            try (Scanner scanner = new Scanner(stream)) {
                bodyString = scanner.useDelimiter("\\A").next();
            }
        } catch (NoSuchElementException e) { // Empty body is OK
        }
        try {
            String script = "Java.asJSONCompatible(" + bodyString + ")";
            return (Map)engine.eval(script);
        } catch (ScriptException se) {
            se.printStackTrace();
            return new HashMap<String, String>();
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<String, String>();
        if (query == null) {
            return result;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] values = part.split("=");
            result.put(values[0], values[1]);
        }
        return result;
    }

    static {
        ScriptEngineManager sem = new ScriptEngineManager();
        engine = sem.getEngineByName("javascript");
    }
}

class Router implements HttpHandler {
    private Map<String, Object> routes = new HashMap<>();
    private Map<String, Class<?>> routeClasses = new HashMap<>();

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Object handler = resolve(path);
        if (handler == null) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close();
        } else {
            Request request = new Request(exchange);
            try {
                invoke(handler, request);
            } catch (InvocationTargetException e) {
                request.internalServerError(e.getCause().getClass().toString());
            } catch (Exception e2) {
                request.internalServerError(e2.getClass().toString());
            }
        }
    }

    public void addRoute(String route, Class<?> clazz) {
        routeClasses.put(route, clazz);
    }

    private void invoke(Object handler, Request request) throws Exception {
        Method handlerMethod = null;
        for (Method method : handler.getClass().getDeclaredMethods()) {
            for (Annotation ann : method.getDeclaredAnnotations()) {
                if (ann instanceof Get && request.method().equals("GET")) {
                    handlerMethod = method;
                } else if (ann instanceof Post && request.method().equals("POST")) {
                    handlerMethod = method;
                }
            }
        }
        if (handlerMethod != null) {
            handlerMethod.setAccessible(true);
            handlerMethod.invoke(handler, request);
        } else {
            request.badRequest();
        }
    }

    private Object resolve(String route) {
        Object result = routes.get(route);
        if (result != null) {
            return result;
        }
        Class<?> clazz = routeClasses.get(route);
        if (clazz != null) {
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                result = ctor.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                result = BadRoute.getInstance();
            }
            routes.put(route, result);
        }
        return result;
    }
}

class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("USAGE: <port>");
            System.exit(1);
        }

        Router router = new Router();
        router.addRoute("/auth", AuthHandler.class);
        router.addRoute("/register", RegisterHandler.class);
        router.addRoute("/admin", AdminHandler.class);
        router.addRoute("/users", UsersHandler.class);
        router.addRoute("/stats", StatsHandler.class);

        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", router);
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("Starting server...");
        server.start();
    }
}
