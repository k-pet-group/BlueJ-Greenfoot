package bluej.testmgr;

/**
 * Records a single user interaction with the object construction
 * mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectInspectInvokerRecord.java 1727 2003-03-26 04:23:18Z ajp $
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    
    public ObjectInspectInvokerRecord(String type, String name)
    {
        this.type = type;
        this.name = name;
    }

    public ObjectInspectInvokerRecord(String type, String name, InvokerRecord ir)
    {
        this.type = type;
        this.name = name;
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
        return false;        
    }

    public String toTestMethod()
    {
        return "\t\t" + type + ";\n";
    }

    public String toFixtureDeclaration()
    {
        return "";
    }
    
    public String toFixtureSetup()
    {
        return "\t\t" + type + ";\n";
    }
}
