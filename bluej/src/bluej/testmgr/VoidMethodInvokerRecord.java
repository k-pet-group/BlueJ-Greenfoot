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
 * @version $Id: VoidMethodInvokerRecord.java 1626 2003-02-11 01:46:35Z ajp $
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

    public String toString()
    {
        return command + ";\n";        
    }
}
