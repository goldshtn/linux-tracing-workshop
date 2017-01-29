class DataFetcher {
        public void fetchData(String datum, int index) {
                if (index == 0 || index % 37 != 0) {
                        processIt();
                        return;
                }
                System.out.println("Error fetching data, cleaning up.");
                System.gc();
        }

        private void processIt() {
        }
}

class RequestProcessor {
        private DataFetcher fetcher;

        public RequestProcessor() {
                fetcher = new DataFetcher();
        }

        public void processRequest(String url, int index) throws Exception {
                if (index != 0 && index % 43 == 0)
                        fetcher.fetchData(url, index);
                else
                        Thread.sleep(1);
        }
}

class Collecty {
        public static void main(String[] args) throws Exception {
                RequestProcessor rp = new RequestProcessor();
                for (int i = 0; ; ++i) {
                        rp.processRequest("/api/all", i);
                }
        }
}
