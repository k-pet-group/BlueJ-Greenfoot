package bluej.testmgr;

import java.util.List;
import java.util.*;

import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.views.*;
import bluej.debugger.*;
import bluej.runtime.*;

/**
 * Records a single user interaction with the object construction/
 * method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: VoidMethodInvokerRecord.java 1628 2003-02-13 00:21:54Z ajp $
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
