package bluej.testmgr.record;


/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls with no result.
 *
 * @author  Andrew Patterson
 * @version $Id: VoidMethodInvokerRecord.java 2223 2003-10-28 01:54:15Z ajp $
 */
public class VoidMethodInvokerRecord extends InvokerRecord
{
    protected String command;
    
    public VoidMethodInvokerRecord(String command)
    {
        this.command = command;
    }

    public String toFixtureDeclaration()
    {
        return null;
    }
    
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
