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
 * @version $Id: InvokerRecord.java 1626 2003-02-11 01:46:35Z ajp $
 */
public abstract class InvokerRecord
{
    public abstract boolean hasReturnValue();
    
    public abstract Class getReturnType();
    
    public abstract boolean isConstructor();
    
}
