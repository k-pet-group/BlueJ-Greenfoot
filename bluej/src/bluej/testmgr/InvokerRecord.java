package bluej.testmgr;

import java.util.*;

/**
 * Records a single user interaction with the object construction/
 * method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: InvokerRecord.java 1727 2003-03-26 04:23:18Z ajp $
 */
public abstract class InvokerRecord
{
    protected ArrayList assertions = new ArrayList();
    
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
    
    protected String getAllAssertions()
    {
		StringBuffer sb = new StringBuffer();
		
		Iterator it = assertions.iterator();
		
		while(it.hasNext()) {
			sb.append("\t\t\t");
			sb.append(it.next());
			sb.append("\n");
		}
    	
    	return sb.toString();
    }
}
