package bluej.testmgr;

/**
 * Records a single user interaction with the object construction
 * mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: ConstructionInvokerRecord.java 1819 2003-04-10 13:47:50Z fisker $
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

    public boolean hasReturnValue()
    {
        return true;
    }
        
    public Class getReturnType()
    {
        return String.class;    
    }
    
    public boolean isConstructor()
    {
        return true;        
    }

    public String toTestMethod()
    {
        return "\t\t" + type + " " + name + " = " + command + ";\n";
    }

    public String toFixtureDeclaration()
    {
        return "\t" + type + " " + name + ";\n";       
    }
    
    public String toFixtureSetup()
    {
        return "\t\t" + name + " = " + command + ";\n";          
    }
}
