package bluej.testmgr.record;

import bluej.utility.JavaUtils;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls with no result.
 *
 * @author  Bruce Quig
 * @version $Id: ExpressionInvokerRecord.java 5833 2008-08-13 15:48:14Z polle $
 */
public class ExpressionInvokerRecord extends MethodInvokerRecord 
{
    
	/**
	 * @param command
	 */
	public ExpressionInvokerRecord(String command) 
    {
		super(JavaUtils.getJavaUtils().genTypeFromClass(Object.class), command, null, null);
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
