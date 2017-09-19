package perros;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RegisterHandler {
    @Post
    public void handle(Request request) throws IOException {
        String username = request.bodyJson().get("username").toString();
        String email = request.bodyJson().get("email").toString();
        String password = request.bodyJson().get("password").toString();

        if (!validateEmail(email) || !validatePassword(password)) {
            request.badRequest();
            return;
        }

        request.finish(201); // Created
    }

    private boolean validateEmail(String email) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9]+@[A-Za-z0-9]+\\.com");
        Matcher matcher = pattern.matcher(email);
        return matcher.find();
    }

    private boolean validatePassword(String password) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9]+[A-Za-z0-9]+)+[!^&*#]");
        Matcher matcher = pattern.matcher(password);
        return matcher.find();
    }
}
