package bluej.testmgr.record;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls with no result.
 *
 * @author  Bruce Quig
 * @version $Id: ExpressionInvokerRecord.java 2376 2003-11-19 23:16:33Z bquig $
 */
public class ExpressionInvokerRecord extends MethodInvokerRecord 
{
    

	/**
	 * @param command
	 */
	public ExpressionInvokerRecord(String command) 
    {
		super(Object.class, command);
	}
    
    /**
     * @return A string representing the assignment statement
     *         
     */
    protected String benchAssignmentTypecast()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append(benchName);
        sb.append(" = ");
        sb.append(command);

        return sb.toString();
    }   

}
