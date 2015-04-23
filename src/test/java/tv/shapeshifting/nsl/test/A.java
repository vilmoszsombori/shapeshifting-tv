package tv.shapeshifting.nsl.test;

public class A {
	public static int g(int a, int b) {
		if(b > 0) {
			return a * g(a, b-1);
		} else {
			return 1;
		}
	}
	public static int f(int a, int b, int n) {
		if(n > 0) {
			return  g(a, b) * f(a, b, n-1);
		} else {
			return 1;
		}
	}
	
	public static void main(String[] args) {
		System.out.println(f(2,2,2));
		System.out.println(f(3,2,2));
	}
}
