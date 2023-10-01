package io.purchaise.mongolay.training;

/**
 * Created by agonlohaj on 25 Apr, 2022
 */
public class CreditCard extends Card {
	public CreditCard(String owner, double balance, String indicator) {
		super(owner, balance, indicator);
	}

	@Override
	public void withdraw (double amount) {
		this.balance -= amount;
		System.out.println("Your current balance is: " + this.balance);
	}
}
