package inf.ed.graph.structure.auxiliary;

public class Quantifier {

	public enum Type {
		PERCENT, COUNT
	}

	private Type type;
	private String predict;
	private int value;
	private double pvalue;

	public Quantifier(String predict, String valueString) {
		this.predict = predict;

		if (valueString.contains("%")) {
			this.type = Type.PERCENT;
			this.value = Integer.parseInt(valueString.substring(0, valueString.length() - 1));
			this.pvalue = this.value * 1.0 / 100;
		} else {
			this.type = Type.COUNT;
			this.value = Integer.parseInt(valueString);
		}
	}

	public boolean isExistential() {
		return (type == Type.COUNT) && predict.equals(">=") && value == 1;
	}

	public boolean isNegation() {
		return (type == Type.COUNT) && predict.equals("=") && value == 0;
	}

	public boolean isPercentage() {
		return type == Type.PERCENT;
	}

	public boolean isCount() {
		return type == Type.COUNT;
	}

	public boolean isValid(int number) {
		if (type == Type.COUNT) {
			if (predict.equals("=")) {
				return number == value;
			} else if (predict.equals(">")) {
				return number > value;
			} else if (predict.equals(">=")) {
				return number >= value;
			} else if (predict.equals("<")) {
				return number < value;
			} else if (predict.equals("<=")) {
				return number <= value;
			} else {
				throw new IllegalArgumentException("Unsupported predict.");
			}
		} else {
			throw new IllegalArgumentException("Unsupported quantifier type.");
		}
	}

	public boolean isValid(int numerator, int denominator) {
		if (type == Type.PERCENT) {
			double percentage = numerator * 1.0 / denominator;
			if (predict.equals("=")) {
				return percentage == pvalue;
			} else if (predict.equals(">")) {
				return percentage > pvalue;
			} else if (predict.equals(">=")) {
				return percentage >= pvalue;
			} else if (predict.equals("<")) {
				return percentage < pvalue;
			} else if (predict.equals("<=")) {
				return percentage <= pvalue;
			} else {
				throw new IllegalArgumentException("Unsupported predict.");
			}
		} else {
			throw new IllegalArgumentException("Unsupported quantifier type.");
		}
	}

	public int getRequiredCount() {
		if (!this.isNegation()) {
			if (this.isCount()) {
				return this.value;
			} else {
				return 1;
			}
		} else
			return 0;
	}

	@Override
	public String toString() {
		if (type == Type.COUNT) {
			return "{" + predict + " " + value + "}";
		} else
			return "{" + predict + " " + value + "%}";
	}
}
