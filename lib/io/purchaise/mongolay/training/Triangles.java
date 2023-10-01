package io.purchaise.mongolay.training;

/**
 * Created by agonlohaj on 25 Apr, 2022
 */
public class Triangles {
	int nrRows;

	public Triangles (int nrRows) {
		this.nrRows = nrRows;
	}

	public void draw (int nrRows) {
		for (int i = 0; i < this.nrRows; i++) {
			for (int j = this.nrRows - 1; j > i; j--) {
				System.out.print(" ");
			}
			for (int j = 0; j < i + 1; j++) {
				System.out.print("*");
			}
			System.out.println();
		}
	}
}
