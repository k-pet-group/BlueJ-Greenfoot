package bluej.testmgr.record;

import java.util.*;

/**
 * From an existing unit test we create an invoker record
 * that represents all the existing fixture declarations
 * and setup code.
 * 
 * @author  Andrew Patterson
 * @version $Id: ExistingFixtureInvokerRecord.java 5823 2008-08-06 11:07:18Z polle $
 */
public class ExistingFixtureInvokerRecord extends InvokerRecord
{
    private List<String> fieldsSrc;
    private String setUpSrc;
    
    /**
     * Records a method call that returns a result to the user.
     * 
     * @param returnType  the Class of the return type of the method
     * @param command     the method statement to execute
     */
    public ExistingFixtureInvokerRecord()
    {
        fieldsSrc = new ArrayList<String>();
    }

    public void addFieldDeclaration(String fieldDecl)
    {
        fieldsSrc.add(fieldDecl);
    }
    
    public void setSetupMethod(String setupMethodSrc)
    {
        setUpSrc = setupMethodSrc;
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
        StringBuffer sb = new StringBuffer();

        ListIterator<String> it = fieldsSrc.listIterator();
                
        while(it.hasNext()) {
            String fieldDecl = (String) it.next();
                    
            sb.append(firstIndent);
            sb.append(fieldDecl);
            sb.append('\n');
        }

        return sb.toString();
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
        StringBuffer sb = new StringBuffer();
        sb.append(secondIndent);
        sb.append(setUpSrc);
        sb.append('\n');

        return sb.toString();
    }

	/**
	 * Construct a portion of a test method for this
	 * invoker record.
	 * 
	 * @return a String representing the test method src
	 */
	public String toTestMethod()
	{
	    return null;
	}
	
    @Override
    public String toExpression()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }

    @Override
    public String getExpressionGlue()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }
}
