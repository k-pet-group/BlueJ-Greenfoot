package antlr.collections.impl;

public class IntRange {
	int begin, end;


	public IntRange(int begin, int end) {
		this.begin = begin;
		this.end = end;
	}
	public String toString() {
		return begin+".."+end;
	}
}
