package bluej.testmgr;

import java.util.List;
import java.util.*;

import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.views.*;
import bluej.debugger.*;
import bluej.runtime.*;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ (assuming it returns a
 * value).
 *
 * @author  Andrew Patterson
 * @version $Id: MethodInvokerRecord.java 1629 2003-02-13 02:03:35Z ajp $
 */
public class MethodInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    private String command;

    public MethodInvokerRecord(String type, String command)
    {
        this.type = type;
        this.command = command;
    }

    public void setResultName(String name)
    {
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
    
    public boolean isLocal()
    {
        return false;    
    }

    public String toTestMethod()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append("\t\t{\n");
        sb.append("\t\t\t" + type + " result = " + command + ";\n");

        if (assertions.size() > 0) {
            sb.append(assertions.get(0));
        }

        sb.append("\t\t}\n");
                    
        return sb.toString();
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
