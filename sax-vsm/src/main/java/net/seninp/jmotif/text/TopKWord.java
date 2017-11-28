package net.seninp.jmotif.text;

import java.util.Comparator;

public class TopKWord implements Comparable<TopKWord> {

	private String class_label;
	private String str_literal;
	private Double tfIDFWeights;

	public TopKWord(String class_label, String str_literal, Double tfIDFWeights) {
		super();
		this.class_label = class_label;
		this.str_literal = str_literal;
		this.tfIDFWeights = tfIDFWeights;
	}

	public String getClass_label() {
		return class_label;
	}

	@Override
	public String toString() {

		return "Class: " + class_label + " Literal :" + str_literal + " Weight :" + tfIDFWeights.toString();

	}

	public void setClass_label(String class_label) {
		this.class_label = class_label;
	}

	public String getStr_literal() {
		return str_literal;
	}

	public void setStr_literal(String str_literal) {
		this.str_literal = str_literal;
	}

	public Double getTfIDFWeights() {
		return tfIDFWeights;
	}

	public void setTfIDFWeights(Double tfIDFWeights) {
		this.tfIDFWeights = tfIDFWeights;
	}


	public static Comparator<TopKWord> TopKWordsComparator = new Comparator<TopKWord>() {
		public int compare(TopKWord word1, TopKWord word2) {
			return (word1.compareTo(word2));
		}

	};

	@Override
	public int compareTo(TopKWord word) {
		double weight1 = word.getTfIDFWeights();
		double weight2 = this.getTfIDFWeights();

		// ascending order
		if (weight1 < weight2)
			return -1;
		else if (weight1 > weight2)
			return 1;
		else
			return 0;

	}

}
