package bluej.testmgr;

import java.util.*;

/**
 * Records a single user interaction with the object
 * construction/method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: InvokerRecord.java 1941 2003-05-05 06:07:49Z ajp $
 */
public abstract class InvokerRecord
{
	final static String firstIndent = "\t";
	final static String secondIndent = "\t\t";
	final static String thirdIndent = "\t\t\t";
	final static String statementEnd = ";\n";

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
}
