package antlr.collections;

import antlr.collections.List;
import antlr.collections.impl.LList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class LListTest {


	public static void main(String[] args) {
		// create a linked list, but treat it like a List
		LList l = new LList();
		List list = l;
		list.add("Hi there");
		list.add("Frank");
	   list.add("Zappa");
		list.add(new Integer(4));
		
		// Test length()
		if ( list.length()!=4 )
			System.out.println("incorrect length");
		else
			System.out.println("correct: length is 4");
		
		// Test the enumeration (view it as a LList)
		Enumeration e = l.elements();
		for (; e.hasMoreElements();) {
			System.out.println(e.nextElement());
		}

		// Test includes()
		if ( list.includes("Frank") )
			System.out.println("correct: contains Frank");
		else
			System.out.println("incorrect: does not contain Frank");
	
		// Test elementAt()
		Object o;
		o = list.elementAt(2);
		System.out.println("elementAt(2) replies: "+o);
		try {
			o = list.elementAt(200);
			System.out.println("elementAt(200) replies: "+o);
		} catch (NoSuchElementException ex) {
			System.out.println("correct: no such element: 200");
		}
	}
}
