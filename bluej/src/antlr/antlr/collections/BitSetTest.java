package antlr.collections;

import antlr.collections.impl.BitSet;

public class BitSetTest {


	public static void main(String[] args) {
		BitSet a = BitSet.of(1);
		System.out.println("BitSet.of(1) == " + a);
		a.add(2);
		a.add(3);
		a.add(4);
		System.out.println("a == "+a);
		System.out.println("a.clone() == "+a.clone());
		BitSet b = BitSet.of(2);
		System.out.println("b == "+b);
		System.out.println("b.not() == "+b.not());
		System.out.println("a.not() == "+a.not());
		System.out.println("a.equals(b) == "+a.equals(b));
		System.out.println("b.equals(a) == "+b.equals(a));
		System.out.println("b.equals(b) == "+b.equals(b));
		System.out.println("a.equals(a) == "+a.equals(a));
		System.out.println("a.size() == "+a.size());
		System.out.println("new BitSet().equals(new BitSet()) == "+new BitSet().equals(new BitSet()));
		System.out.println("a&b == "+a.and(b));
		System.out.println("b.or(BitSet.of(5)) == "+b.or(BitSet.of(5)));
		System.out.println("a.nil() == "+a.nil());
		System.out.println("new BitSet().nil() == "+new BitSet().nil());
		System.out.println("new BitSet().degree() == " + new BitSet().degree());
		System.out.println("BitSet.of(1).degree() == " + BitSet.of(1).degree());
		System.out.println("a.degree() == " + a.degree());
		System.out.println("b.subset(a) == " + b.subset(a));
		System.out.println("a.subset(b) == " + a.subset(b));
		System.out.println("new BitSet().subset(a) == " + new BitSet().subset(a));
		System.out.println("b.or(BitSet.of(4)).subset(a) == " + b.or(BitSet.of(4)).subset(a));
		System.out.println("b.or(BitSet.of(5)).subset(a) == " + b.or(BitSet.of(5)).subset(a));

		System.out.println("BitSet.of(85) == "+BitSet.of(85));
		System.out.println("a|BitSet.of(85) == "+a.or(BitSet.of(85)));
		System.out.println("BitSet.of(85).size() == "+BitSet.of(85).size());
		System.out.println("BitSet.of(85).degree() == "+BitSet.of(85).degree());

		BitSet z = a.or(BitSet.of(85));
		z.clear();
		System.out.println("a|BitSet.of(85).clear() == " + z);
	}
}
