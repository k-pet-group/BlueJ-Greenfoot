package bluej.testmgr.record;

/**
 * Records a single user interaction with the 
 * object inspection mechanisms of BlueJ.
 * 
 * This record is for objects accessed through inspectors
 * (not currently working).
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectInspectInvokerRecord.java 2223 2003-10-28 01:54:15Z ajp $
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;

	/**
	 * Object inspection from an initial result.
	 * 
	 * @param type
	 * @param name
	 */    
    public ObjectInspectInvokerRecord(String type, String name)
    {
        this.type = type;
        this.name = name;
    }

	/**
	 * Object inspection from another inspector.
	 * 
	 * @param type
	 * @param name
	 * @param ir
	 */
    public ObjectInspectInvokerRecord(String type, String name, InvokerRecord ir)
    {
        this.type = type;
        this.name = name;
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
}
