package xyz.kumaraswamy.sketch;

public class Test {
    public static void main(String[] args) {
        long current = System.currentTimeMillis();
        System.out.println(fib(40));
        System.out.println(System.currentTimeMillis() - current);
    }

    public static int fib(int n) {
        return n < 2 ? n : fib(n - 1) + fib(n - 2);
    }

}
