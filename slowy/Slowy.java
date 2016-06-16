package slowy;

class App {

    private static boolean isDivisible(int n, int d) {
        return n % d == 0;
    }       

    private static boolean isSimplePrime(int n) {
        return n <= 2;
    }

    private static boolean isPrime(int n) {
        if (isSimplePrime(n)) return true;
        for (int d = 3; d < n; d += 2) {
            if (isDivisible(n, d)) return false;
        }
        return true;
    }

    public static void main(String[] args) throws java.io.IOException {
        System.out.println("Press ENTER to start.");
        System.in.read();
        int count = 0;
        for (int n = 2; n < 200000; ++n) {
            if (isPrime(n)) ++count;
        }
        System.out.println(String.format("Primes found: %d", count));
        System.out.println("Press ENTER to exit.");
        System.in.read();
    }

}
