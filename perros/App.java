package perros;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Scanner;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

interface ApiHandler {
    void handle(HttpExchange request, Map<String, String> queryParams,
                Map bodyJson) throws IOException;
}

class Router implements HttpHandler {
    private Map<String, ApiHandler> routes = new HashMap<String, ApiHandler>();
    private ScriptEngine engine;

    public Router() {
        ScriptEngineManager sem = new ScriptEngineManager();
        engine = sem.getEngineByName("javascript");
    }

    public void handle(HttpExchange request) throws IOException {
        String path = request.getRequestURI().getPath();
        ApiHandler handler = routes.get(path);
        if (handler == null) {
            request.sendResponseHeaders(400, 0);
            request.getResponseBody().close();
        } else {
            String query = request.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQuery(query);
            Map bodyJson = parseBody(request.getRequestBody());
            handler.handle(request, queryParams, bodyJson);
        }
    }

    public void addRoute(String route, ApiHandler handler) {
        routes.put(route, handler);
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
}

class App {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("USAGE: <port>");
            System.exit(1);
        }

        Router router = new Router();
        router.addRoute("/auth", new AuthHandler());

        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", router);
        server.setExecutor(null);
        System.out.println("Starting server...");
        server.start();
    }
}
