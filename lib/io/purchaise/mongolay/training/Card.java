package io.purchaise.mongolay.training;

import java.util.List;

/**
 * Created by agonlohaj on 25 Apr, 2022
 */
public class Card {
	protected String owner;
	protected double balance;
	protected String indicator;

	public Card (String owner, double balance, String indicator) {
		this.owner = owner;
		this.balance = balance;
		this.indicator = indicator;
	}

	public double getBalance () {
		return this.balance;
	}

	public String getOwner() {
		return owner;
	}

	public String getIndicator() {
		return indicator;
	}

	public void withdraw (double amount) {
		if (amount > this.getBalance()) {
			System.out.println("The amount is bigger than your balance");
			return;
		}
		this.balance -= amount;
		System.out.println("Your current balance is: " + this.balance);
	}

	public void deposit (double amount) {
		System.out.println("Your current balance is: " + this.balance);
		this.balance += amount;
	}

	public double calcAverage (List<Card> cards) {
		if (cards.size() == 0) {
			return 0;
		}
		double min = cards.get(0).getBalance();
		double max = cards.get(0).getBalance();
		// this
		for (int i = 1; i < cards.size(); i++) {
			Card next = cards.get(i);
			min = Math.min(min, next.getBalance());
			max = Math.min(max, next.getBalance());
		}
		// or
		for (Card next: cards) {
			min = Math.min(min, next.getBalance());
			max = Math.min(max, next.getBalance());
		}
		return (min + max) / 2;
	}
}
