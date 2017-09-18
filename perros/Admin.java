package perros;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class AdminHandler implements ApiHandler {
    private static final String CONFIG_FILE = "/etc/perros.conf";

    public void handle(Request request) throws IOException {
        String config = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
        String username = request.queryParams().get("username");
        if (config.contains("approve: " + username)) {
            request.ok(); 
        } else {
            request.unauthorized();
        }
    }
}
