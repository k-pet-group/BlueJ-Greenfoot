package bluej.testmgr.record;

/**
 * Records a single user interaction with the 
 * class inspection mechanisms of BlueJ.
 * 
 * This record is for classes accessed through inspectors
 * (not currently working).
 *
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassInspectInvokerRecord.java 5823 2008-08-06 11:07:18Z polle $
 *
 */
public class ClassInspectInvokerRecord extends InvokerRecord
{   
    private String className;

    /**
     * Class inspection 
     * 
     * @param className name of the class
     */    
    public ClassInspectInvokerRecord(String className)
    {
        this.className = className;        
    }   

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return secondIndent + className + statementEnd;
    }

    public String toTestMethod()
    {
        return firstIndent + className + statementEnd;
    }
    
    @Override
    public String toExpression()
    {
        return className;       
    }
    

    @Override
    public String getExpressionGlue()
    {
        return ".";
    }

}
