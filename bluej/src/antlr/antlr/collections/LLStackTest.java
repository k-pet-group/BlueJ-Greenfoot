package antlr.collections;

import antlr.collections.Stack;
import antlr.collections.impl.LList;
import java.util.Enumeration;

public class LLStackTest {


	public static void main(String[] args) {
		// create linked list, but treat it like a Stack
		LList list = new LList();
		Stack s = list;
		s.push(new Integer(3));
		s.push(new Integer(4));
		s.push(new Integer(5));

		// Test height()
		if ( s.height()!=3 )
			System.out.println("incorrect height");
		else
			System.out.println("correct: height is 3");
		
		// Test the enumeration (pretend it's a list again)
		// Note how different perspectives on the same object are useful.
		Enumeration e = list.elements();
		for (; e.hasMoreElements();) {
			System.out.println(e.nextElement());
		}

		// Test pop(): compute 3*(4+5) via "3 4 5 + *" in RPN notation
		int a = ((Integer)s.pop()).intValue();
		int b = ((Integer)s.pop()).intValue();
		s.push( new Integer(a+b) );
		a = ((Integer)s.pop()).intValue();
		b = ((Integer)s.pop()).intValue();
		int result = a * b;
		System.out.println("result, " + result + ", should be 27");
		
		if ( s.height()!=0 ) System.out.println("incorrect stack height");
		
		/* Code that is commented here out won't compile since a Stack
		 * is a limited perspective on a LLStack implementation.
		 *      Stack s2 = s;
		 *      s2.includes("Frank");
		 */
	}
}
