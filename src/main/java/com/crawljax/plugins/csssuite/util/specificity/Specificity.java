package com.crawljax.plugins.csssuite.util.specificity;

public class Specificity
{
	private final int value;

	Specificity(int value)
	{
		this.value = value;
	}

	public int getValue()
	{
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Specificity)) {
			return false;
		}

		Specificity other = (Specificity) object;
		return value == other.getValue();
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public String toString() {
		int v = value;
		final int base = 100;


		int d = v % base;
		v = v / base;
		int c = v % base;
		v = v / base;
		int b = v % base;
		v = v / base;
		int a = v % base;

		return "{" + a + ", " + b + ", " + c + ", " + d + "}";
	}
}