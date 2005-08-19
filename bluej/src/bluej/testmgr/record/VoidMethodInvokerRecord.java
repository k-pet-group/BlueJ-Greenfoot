package bluej.testmgr.record;


/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls with no result.
 *
 * @author  Andrew Patterson
 * @version $Id: VoidMethodInvokerRecord.java 3532 2005-08-19 06:01:30Z davmac $
 */
public class VoidMethodInvokerRecord extends InvokerRecord
{
    protected String command;
    private String [] argumentValues;
    
    public VoidMethodInvokerRecord(String command, String [] argVals)
    {
        this.command = command;
        this.argumentValues = argVals;
    }
    
    public String [] getArgumentValues()
    {
        return argumentValues;
    }

    /**
     * Construct a declaration for any objects constructed
     * by this invoker record.
     * 
     * @return null because a void method results in no objects
     */    
    public String toFixtureDeclaration()
    {
        return null;
    }
    
    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String reprenting the object initialisation
     *         src or null if there is none. 
     */    
    public String toFixtureSetup()
    {
    	// code for the fixture setup involves just inserting the method call
        return secondIndent + command + statementEnd;
    }

	public String toTestMethod()
	{
		// code for the test method involves just inserting the method call
		return secondIndent + command + statementEnd;
	}
}
