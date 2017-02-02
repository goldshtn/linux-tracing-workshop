import java.io.FileOutputStream;

class Writey {
        private static void doWrite(int size) throws Exception {
                FileOutputStream fos = new FileOutputStream("db-wal");
                byte[] data = new byte[size];
                fos.write(data);
                fos.close();
        }

        private static void flushData() throws Exception {
                doWrite((int)(Math.random() * 4 * 1048576 + 1));
        }

        private static void writer() throws Exception {
                flushData();
        }
        
        public static void main(String[] args) throws Exception {
                Thread t = new Thread(new Runnable() {
                        public void run() {
                                while (true) {
                                        try {
                                                Thread.sleep(100);
                                                doWrite(1024);
                                        } catch (Exception e) {
                                        }
                                }
                        }
                });
                t.start();
                while (true) {
                        Thread.sleep(1000);
                        writer();
                }
        }
}
