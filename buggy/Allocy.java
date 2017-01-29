class ResponseBuilder {
        private String response;

        public ResponseBuilder(String template) {
                response = template;
        }

        public void addLine(String line) {
                response += line;
        }

        public String getResponse() {
                return response;
        }
}

class Allocy {
        public static void main(String[] args) {
                while (true) {
                        ResponseBuilder rb = new ResponseBuilder("Result:");
                        for (int i = 0; i < 100000; ++i) {
                                rb.addLine("#" + i + ": OK");
                        }
                        rb.addLine("End of response.");
                }
        }
}
