package bluej.testmgr.record;

/**
 * Records a single user interaction with the 
 * object inspection mechanisms of BlueJ.
 * 
 * This record is for objects accessed through inspectors
 * (not currently working).
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectInspectInvokerRecord.java 5833 2008-08-13 15:48:14Z polle $
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String name;
    private InvokerRecord parentIr;
    private boolean isArray;

	/**
	 * Object inspection from an initial result.
	 * 
	 * @param type
	 * @param name
	 */    
    public ObjectInspectInvokerRecord(String name, boolean isArray)
    {
        this.name = name;
        //TODO create an ArrayInspectInvokerRecord instead?
        this.isArray = isArray;
    }

	/**
	 * Object inspection from another inspector.
	 * 
	 * @param type
	 * @param name
	 * @param ir
	 */
    public ObjectInspectInvokerRecord(String name,  boolean isArray, InvokerRecord ir)
    {
        this.name = name;
        this.isArray = isArray;
        this.parentIr = ir;
    }

    public String toFixtureDeclaration()
    {
        throw new UnsupportedOperationException();
    }
    
    public String toFixtureSetup()
    {
        throw new UnsupportedOperationException();
    }

	public String toTestMethod()
	{
        throw new UnsupportedOperationException();
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

    public void incUsageCount()
    {
        parentIr.incUsageCount();        
    }
    
    public String toTestMethodInit() 
    {
        return parentIr.toTestMethodInit();
    }
}
