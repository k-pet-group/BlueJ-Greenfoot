package bluej.testmgr;

import java.util.*;

/**
 * Records a single user interaction with the object
 * construction/method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: InvokerRecord.java 1882 2003-04-24 06:28:11Z ajp $
 */
public abstract class InvokerRecord
{
	final static String firstIndent = "\t\t";
	final static String secondIndent = "\t\t\t";
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


    protected ArrayList assertions = new ArrayList();
    
    public void addAssertion(String assertion)
    {
        assertions.add(assertion);        
    }
    
    public String getAllAssertions()
    {
		StringBuffer sb = new StringBuffer();
		
		Iterator it = assertions.iterator();
		
		while(it.hasNext()) {
			sb.append(secondIndent);
			sb.append(it.next());
			sb.append(statementEnd);
		}
    	
    	return sb.toString();
    }
}
