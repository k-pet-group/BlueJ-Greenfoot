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
 * @version $Id: InvokerRecord.java 1628 2003-02-13 00:21:54Z ajp $
 */
public abstract class InvokerRecord
{
    protected List assertions = new ArrayList();
    
    public abstract boolean hasReturnValue();
    
    public abstract Class getReturnType();
    
    public abstract boolean isConstructor();
    
    public abstract String toTestMethod();
    
    public abstract String toFixtureDeclaration();
    
    public abstract String toFixtureSetup();
    
    public void addAssertion(String assertion)
    {
        assertions.add(assertion);        
    }
}
