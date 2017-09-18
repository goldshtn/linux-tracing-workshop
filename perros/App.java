package perros;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Scanner;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

interface ApiHandler {
    void handle(Request request) throws IOException;
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
    private Map<String, ApiHandler> routes = new HashMap<String, ApiHandler>();

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        ApiHandler handler = routes.get(path);
        if (handler == null) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close();
        } else {
            Request request = new Request(exchange);
            try {
                handler.handle(request);
            } catch (Exception e) {
                request.internalServerError(e.getClass().toString());
            }
        }
    }

    public void addRoute(String route, ApiHandler handler) {
        routes.put(route, handler);
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
        router.addRoute("/register", new RegisterHandler());
        router.addRoute("/admin", new AdminHandler());

        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", router);
        server.setExecutor(null);
        System.out.println("Starting server...");
        server.start();
    }
}
