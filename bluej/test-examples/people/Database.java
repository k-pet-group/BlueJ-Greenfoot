 import java.util.Vector;
import java.util.Enumeration;

/**
 ** A very simple database of people in a university. This class simply stores
 ** persons and, at request, lists them on standard output.
 **
 ** Written as a first demo program for JavaBlue.
 **
 ** Author:  Michael Kölling
 ** Version: 1.0
 ** Date:    January 1999
 **/

public class Database {

	// instance variables:

	private Vector personVector;
	public Person p;
	public String name;

	/**
	 ** Create a new, empty person database.
	 **/
	public Database() {
	    personVector = new Vector ();
	    name = "Kolling";
	}

	/**
	 ** Add a person to the database.
	 **/
	public void addPerson(Person p) {
	    personVector.addElement(p);
	    this.p = p;
	}

	/**
	 ** Add a person to the database.
	 **/
	public void doError()
	throws Exception
	{
	    throw new Exception("errrrorrr");
	  //p = null;
	  //p.setName("m");
	}


    /**
     * method comment here
     */
    public Person getP()
    {
        return p;
    }

    /**
     * method comment here
     */
    public Vector getS()
    {
        return (Vector)personVector.clone();
    }

    /**
     * method comment here
     */
    public String getS0()
    {
        return null;
    }

	/**
	 ** List all the persons currently in the database on standard out.
	 **/
	public void listAll () {
	    for (Enumeration e = personVector.elements(); e.hasMoreElements();) {
	        System.out.println(e.nextElement());
	    }
	}

}