package io.purchaise.mongolay.training;

/**
 * Created by agonlohaj on 25 Apr, 2022
 */
public class GCD {
	public int calculateGCD(int a, int b) {
		if (a == b) {
			return a;
		}

		if (a > b) {
			return this.calculateGCD(a - b, b);
		}
		return this.calculateGCD(a, b - a);
	}
}
