import java.security.Security;
import java.net.URL;
import java.io.InputStream;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

class Fetchy {
        static {
               Security.setProperty(
                               "networkaddress.cache.negative.ttl", "0");
        }

        private String url;

        public Fetchy(String url) {
                this.url = url;
        }

        public String fetch() throws Exception {
                InputStream res = new URL(url).openStream();
                Scanner scn = new Scanner(res);
                return scn.useDelimiter("\\A").next();
        }
}

class Crawley {
        private List<String> urls;

        public Crawley(List<String> urls) {
                this.urls = urls;
        }

        public void crawl() {
                try {
                        for (String url : urls) {
                                Fetchy fetchy = new Fetchy(url);
                                fetchy.fetch();
                        }
                } catch (Exception e) {
                }
        }
}

class Clienty {
        public static void main(String[] args) {
                ArrayList<String> urls = new ArrayList<String>();
                if (args[0].equals("bad")) {
                        urls.add("https://i-dont-exist-at-all-20170126.com");
                } else {
                        urls.add("https://facebook.com");
                }
                urls.add("https://google.com");
                urls.add("https://example.org");
                Crawley crawley = new Crawley(urls);
                while (true) {
                        long start = System.currentTimeMillis();
                        crawley.crawl();
                        long end = System.currentTimeMillis();
                        System.out.println("Crawl complete, elapsed: " +
                                (end-start) + " milliseconds.");
                }
        }
}
