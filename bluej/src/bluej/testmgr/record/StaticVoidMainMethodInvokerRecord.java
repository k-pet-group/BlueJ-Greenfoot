package bluej.testmgr.record;

/**
 * Records a call to a programs static void main()
 * entry point. Because this does not return a result
 * and tests should not depend on side effects from
 * static statements, we create a record for this
 * but the record does not result in any unit test
 * code being created.
 *
 * @author  Andrew Patterson
 * @version $Id: StaticVoidMainMethodInvokerRecord.java 2223 2003-10-28 01:54:15Z ajp $
 */
public class StaticVoidMainMethodInvokerRecord extends InvokerRecord
{
    protected String command;
    
    public StaticVoidMainMethodInvokerRecord(String command)
    {
        this.command = command;
    }

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return null;
    }

	public String toTestMethod()
	{
        return null;
	}
}
