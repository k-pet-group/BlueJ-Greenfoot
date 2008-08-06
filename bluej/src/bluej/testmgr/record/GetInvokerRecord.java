package bluej.testmgr.record;

/**
 * Records a "Get" from the inspector window. Not from a result inspector
 * though, since that is handled by the MethodInvocationRecord. 
 * 
 * @author Poul Henriksen
 */
public class GetInvokerRecord extends InvokerRecord
{
    /** The invoker record for the inspector where the Get button was pressed, that resulted in the creation of this GetInvokerRecord. */
    private InvokerRecord parentIr;
    
    /** Name of the field to Get */
    private String fieldName;
    
    /** Type of the field */
    private String fieldType;
    
    /** Name of the object as it appears on the object bench */
    private String objName;
    
    /** Type of the object */
    private String objType;


    public GetInvokerRecord(String fieldType, String fieldName, InvokerRecord parentIr)
    {
        this.parentIr = parentIr;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    /**
     * Give this method invoker record a name on the object bench.
     * 
     * @param name Name of the object a it appears on the object bench.
     * @param type The type that the object is on the actual bench.
     */
    public void setBenchName(String name, String type)
    {
        objName = name;
        objType = type;
    }

    /**
     * Construct a declaration for any objects constructed
     * by this invoker record.
     * 
     * @return a String representing the object declaration
     *         src or null if there is none.
     */    
    @Override
    public String toFixtureDeclaration()
    {
        return fieldDeclarationStart + objType + " " + objName + statementEnd;    
    }

    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String representing the object initialisation
     *         src or null if there is none. 
     */    
    @Override
    public String toFixtureSetup()
    {
        return secondIndent + objName + " = " + toExpression() + statementEnd;          
    }

    /**
     * Construct a portion of a test method for this
     * invoker record.
     * 
     * @return a String representing the test method src
     */
    @Override
    public String toTestMethod()
    {
        return secondIndent + objType + " " + objName + " = " + toExpression() + statementEnd;
    }
    
    @Override
    public String toExpression()
    {
        if(! objType.equals(fieldType)) {
            return "((" + objType + ") " + parentIr.toExpression() + parentIr.getExpressionGlue() + fieldName + ")";
        }
        else {
            return parentIr.toExpression() + parentIr.getExpressionGlue() + fieldName;
        }
    }

    @Override
    public String getExpressionGlue()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }
}
