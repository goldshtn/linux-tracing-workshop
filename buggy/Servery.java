import java.io.File;
import java.util.Scanner;

class Initializer {
        private static boolean initializationComplete = false;
        private static final Object initLock = new Object();

        public static void initializeAsync(String configFile) {
                new Thread(new Runnable() {
                        public void run() {
                                synchronized (initLock) {
                                        while (!openConfigFile(configFile))
                                                ;
                                        initializationComplete = true;
                                        initLock.notifyAll();
                                }
                        }
                }).start();
        }

        public static void waitForInitialization() {
                while (true) {
                        synchronized (initLock) {
                                if (initializationComplete)
                                        return;
                                try { initLock.wait(); }
                                catch (InterruptedException e) { }
                        }
                }
        }

        private static boolean openConfigFile(String configFile) {
                System.out.println("[*] Opening config file.");
                try {
                        String content = new Scanner(new File(configFile))
                                .useDelimiter("\\Z").next();
                } catch (Exception e) {
                        try { Thread.sleep(1000); }
                        catch (InterruptedException e2) {}
                        return false;
                }
                System.out.println("[*] Config file read successfully.");
                return true;
        }
}

class Servery {
        public static void main(String[] args) throws Exception {
                Initializer.initializeAsync("/etc/acme-svr.config");
                System.out.println("[*] Server started, initializing.");
                Initializer.waitForInitialization();
                System.out.println("[*] Initialization complete!");
                System.in.read();
        }
}
