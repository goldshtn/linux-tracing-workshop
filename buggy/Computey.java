import java.util.ArrayList;

class Primes {
        private final int from;
        private final int to;
        private final int numThreads;
        private ArrayList<Integer> primes = new ArrayList<>();
        private Object primesLock = new Object();

        public Primes(int from, int to, int numThreads) {
                this.from = from;
                this.to = to;
                this.numThreads = numThreads;
        }

        public int count() {
                return primes.size();
        }

        public void execute() throws Exception {
                Thread[] threads = new Thread[numThreads];
                int chunkSize = (to-from)/numThreads;
                for (int i = 0; i < numThreads; ++i) {
                        final int myStart = i*chunkSize;
                        final int myEnd = (i+1)*chunkSize - 1;
                        threads[i] = new Thread(new Runnable() {
                                public void run() {
                                        primesThread(myStart, myEnd);
                                }
                        });
                        threads[i].start();
                }
                for (Thread thread : threads) {
                        thread.join();
                }
        }

        private void primesThread(int from, int to) {
                for (int i = from; i <= to; ++i) {
                        if (isPrime(i)) {
                                synchronized (primesLock) {
                                        primes.add(i);
                                }
                        }
                }
        }

        private static boolean isPrime(int number) {
                // Obviously very inefficient
                if (number == 2) return true;
                if (number % 2 == 0) return false;
                for (int i = 3; i < number; i += 2)
                        if (number % i == 0)
                                return false;
                return true;
        }
}

class Computey {
        public static void main(String[] args) throws Exception {
                int from = Integer.parseInt(args[0]);
                int to = Integer.parseInt(args[1]);
                int numThreads = Integer.parseInt(args[2]);
                Primes primes = new Primes(from, to, numThreads);
                System.out.println("Press RETURN to start.");
                System.in.read();
                primes.execute();
                System.out.println("Found " + primes.count() + " primes");
        }
}
