package io.purchaise.mongolay.training;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Created by agonlohaj on 25 Apr, 2022
 */
@NoArgsConstructor
@AllArgsConstructor
public class Permuter {
	String characters;

	public void draw (int nrLines) {
		for (int i = 0; i < nrLines; i++) {
			for (int j = nrLines - 1; j > i; j--) {
				System.out.print(" ");
			}
			for (int j = 0; j < i; j++) {
				System.out.print("*");
			}
			System.out.println();
		}
	}

	public void permute() {
		this.permute("", characters);
	}


	// abc -> bac, cba
	// abc -> acb, bac
	// abc -> cba, acb
	public void permute(String prefix, String what) {
		int length = characters.length();
		String remainder = what.substring(1);
		for (int i = 0; i < length; i++) {
			char next = characters.charAt(i);
			String whole = prefix + next;
			if (what.length() == 1) {
				System.out.println(whole);
				continue;
			}
			this.permute(whole, remainder);
		}
	}
}
