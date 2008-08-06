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
 * @version $Id: StaticVoidMainMethodInvokerRecord.java 5823 2008-08-06 11:07:18Z polle $
 */
public class StaticVoidMainMethodInvokerRecord extends InvokerRecord
{
    public StaticVoidMainMethodInvokerRecord()
    {
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

    @Override
    public String toExpression()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }

    @Override
    public String getExpressionGlue()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }
}
