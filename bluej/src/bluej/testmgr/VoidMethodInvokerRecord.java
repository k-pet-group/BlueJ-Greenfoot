package bluej.testmgr;

/**
 * Records a single user interaction with the object construction/
 * method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: VoidMethodInvokerRecord.java 1727 2003-03-26 04:23:18Z ajp $
 */
public class VoidMethodInvokerRecord extends InvokerRecord
{
    private String command;
    
    public VoidMethodInvokerRecord(String command)
    {
        this.command = command;
    }

    public boolean hasReturnValue()
    {
        return false;
    }
        
    public Class getReturnType()
    {
        return null;    
    }
    
    public boolean isConstructor()
    {
        return false;        
    }

    public String toTestMethod()
    {
        return "\t\t" + command + ";\n";
    }

    public String toFixtureDeclaration()
    {
        return "";
    }
    
    public String toFixtureSetup()
    {
        return "\t\t" + command + ";\n";
    }
}
