package bluej.testmgr.record;

import java.util.*;

/**
 * Records a single user interaction with the object
 * construction/method call mechanisms of BlueJ.
 * 
 * Also contains static methods to help deal with the
 * construction and maintenance of assertion data.
 *
 * @author  Andrew Patterson
 * @version $Id: InvokerRecord.java 2287 2003-11-06 00:55:29Z ajp $
 */
public abstract class InvokerRecord
{
	final static String firstIndent = "\t";
	final static String secondIndent = "\t\t";
	final static String thirdIndent = "\t\t\t";
	final static String statementEnd = ";\n";
    
    final static String fieldDeclarationStart = firstIndent + "private ";

	/**
	 * Construct a declaration for any objects constructed
	 * by this invoker record.
	 * 
	 * @return a String representing the object declaration
	 *         src or null if there is none.
	 */    
	public abstract String toFixtureDeclaration();

	/**
	 * Construct a portion of an initialisation method for
	 * this invoker record.
	 *  
	 * @return a String reprenting the object initialisation
	 *         src or null if there is none. 
	 */    
	public abstract String toFixtureSetup();
    
	/**
	 * Construct a portion of a test method for this
	 * invoker record.
	 * 
	 * @return a String representing the test method src
	 */
	public abstract String toTestMethod();

    /**
     * A collection of assertion skeletons made about the invoker
     * record.
     */
    private ArrayList assertions = new ArrayList();
    
    /**
     * Add the skeleton of an assertion statement to our list of
     * assertions made about this invoker record.
     * 
     * @param assertion
     */
    public void addAssertion(String assertion)
    {
        assertions.add(assertion);        
    }
    
    public int getAssertionCount()
    {
    	return assertions.size();
    }
    
    public String getAssertion(int i)
    {
		return (String) assertions.get(i);
    }
    
    /**
     * Returns a statement representing this assertion
     * with an @@ at the point where code needs to be
     * inserted.
     * 
     * This case is for when there is only a single argument
     * for the assertion.

     * @return a String of the assertion statement.
     */
    public static String makeAssertionStatement(String assertName)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(assertName);
        sb.append("(@@)");
        return sb.toString();
    }

    /**
     * Returns a statement representing this assertion
     * with an @@ at the point where code needs to be
     * inserted.
     * 
     * This case is for when there are two arguements
     * for the assertion.
     * 
     * For example, if the user has selected equals and
     * has put a value of
     * 
     * new X(a,b)
     * 
     * in the edit box, we return the string
     * 
     * "assertEquals(new X(a,b), @@)"  
     *  
     * @return a String of the assertion statement.
     * 
     * There is a danger here if the characters @@ appear
     * in the user input. This is considered unlikely, but
     * when searching for the @@ location, the _last_ @@
     * in this string should be found.
     */
    public static String makeAssertionStatement(String assertName, String userData)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(assertName);
        sb.append("(");
        sb.append(userData);
        sb.append(", @@)");
        return sb.toString();    
    }

    /**
     * Returns a statement representing this assertion
     * with an @@ at the point where a statement needs to be
     * inserted.
     * 
     * This case is for when we have floating point or
     * double assertions with assertEquals()

     * @return a String of the assertion statement.
     */
    public static String makeAssertionStatement(String assertName, String userData, String deltaData)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(assertName);
        sb.append("(");
        sb.append(userData);
        sb.append(", @@, ");
        sb.append(deltaData);
        sb.append(")");
        return sb.toString();    
    }

    /**
     * Insert a command into an assertion.
     * 
     * Our assertions have an @@ wherever the command needs to go. We
     * find this and insert the string there.
     * 
     * @param  assertion   a String representing the assertion in the
     *                     form "assertXXXX(YYYY, @@)"
     * @param  command     a String representing the statement to make the
     *                     assertion against ie foo.bar(5,6) 
     * @return the combined string representing original assertion with the 
     *         insertion of the command or reference to bench object.
     *         ie assertXXXX(YYYY, foo.bar(5,6)) 
     */
    public static String insertCommandIntoAssertionStatement(String assertion, String command)
    {
        StringBuffer assertCommand = new StringBuffer(assertion);
        // search for the _last_ @@ in case the user has input some of these
        // @@ characters in the assertion
        int insertionSpot = assertion.lastIndexOf("@@");

        if(insertionSpot == -1)
            throw new IllegalArgumentException("the assertion must have an @@");

        assertCommand.replace(insertionSpot, insertionSpot + 2, command);
            
        return assertCommand.toString();
    }
}
