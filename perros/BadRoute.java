package perros;

import java.io.IOException;

class BadRoute {
    private static BadRoute instance = new BadRoute();

    public static BadRoute getInstance() { return instance; }
}
