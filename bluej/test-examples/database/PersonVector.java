  import java.util.Vector;
import java.util.Enumeration;

/**
 ** @author Morten Knudsen
 ** @author Kent Hansen
 ** @version 1.0
 ** @date July 1998
 **
 ** PersonVector is a Vector for storing Persons.
 ** 
 ** This class is part of a simple Database program, made to demonstrate
 ** JavaBlue by Michael Kolling.
 **/
class PersonVector {

	private Vector theVector = new Vector();

	public void addPerson (Person aPerson) {
		theVector.addElement (aPerson);
	}

	public Enumeration persons () {
		return theVector.elements ();
	}
}
