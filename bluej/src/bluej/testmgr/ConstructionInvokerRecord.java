package bluej.testmgr;

/**
 * Records a single user interaction with the object construction
 * mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: ConstructionInvokerRecord.java 1941 2003-05-05 06:07:49Z ajp $
 */
public class ConstructionInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    private String command;
    
    public ConstructionInvokerRecord(String type, String name, String command)
    {
        this.type = type;
        this.name = name;
        this.command = command;
    }

	/**
	 * Construct a declaration for any objects constructed
	 * by this invoker record.
	 * 
	 * @return a String representing the object declaration
	 *         src or null if there is none.
	 */    
    public String toFixtureDeclaration()
    {
        return firstIndent + type + " " + name + statementEnd;       
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
        return secondIndent + name + " = " + command + ";\n";          
    }

	/**
	 * Construct a portion of a test method for this
	 * invoker record.
	 * 
	 * @return a String representing the test method src
	 */
	public String toTestMethod()
	{
		return secondIndent + type + " " + name + " = " + command + ";\n";
	}
}
