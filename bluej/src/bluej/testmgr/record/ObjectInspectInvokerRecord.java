package bluej.testmgr.record;

/**
 * Records a single user interaction with the 
 * object inspection mechanisms of BlueJ.
 * 
 * This record is for objects accessed through inspectors
 * (not currently working).
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectInspectInvokerRecord.java 5823 2008-08-06 11:07:18Z polle $
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    private InvokerRecord parentIr;
    private boolean isArray;

	/**
	 * Object inspection from an initial result.
	 * 
	 * @param type
	 * @param name
	 */    
    public ObjectInspectInvokerRecord(String type, String name, boolean isArray)
    {
        this.type = type;
        this.name = name;
        //POLLE create a ArrayInspectInvokerRecord?
        this.isArray = isArray;
    }

	/**
	 * Object inspection from another inspector.
	 * 
	 * @param type
	 * @param name
	 * @param ir
	 */
    public ObjectInspectInvokerRecord(String type, String name,  boolean isArray, InvokerRecord ir)
    {
        this.type = type;
        this.name = name;
        this.isArray = isArray;
        this.parentIr = ir;
    }

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return secondIndent + type + statementEnd;
    }

	public String toTestMethod()
	{
		return firstIndent + type + statementEnd;
	}
	
	@Override
    public String toExpression()
    {
	    if(parentIr instanceof MethodInvokerRecord) {
	        // This means we are inspecting a method result, and not a named object
	        // POLLE Maybe don't set the name and use that instead of instanceof
	        return parentIr.toExpression();
	    }
        else if(parentIr != null) {
            return parentIr.toExpression() + parentIr.getExpressionGlue() + name; 
        }
	    else {
	        return name;
	    }
	}

    @Override
    public String getExpressionGlue()
    {
        if(isArray) {
            return "";
        } else {
            return ".";
        }
    }
}
