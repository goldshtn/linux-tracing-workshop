package perros;

import java.io.IOException;

class BadRoute implements ApiHandler {
    private static BadRoute instance = new BadRoute();

    public static BadRoute getInstance() { return instance; }

    public void handle(Request request) throws IOException {
        request.badRequest();
    }
}
