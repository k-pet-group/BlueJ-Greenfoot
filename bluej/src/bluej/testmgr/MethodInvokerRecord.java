package bluej.testmgr;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls that return a result.
 *
 * @author  Andrew Patterson
 * @version $Id: MethodInvokerRecord.java 1882 2003-04-24 06:28:11Z ajp $
 */
public class MethodInvokerRecord extends VoidMethodInvokerRecord
{
    private String type;

    public MethodInvokerRecord(String type, String command)
    {
    	super(command);
    	
        this.type = type;
    }

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return firstIndent + command + statementEnd;
    }

	public String toTestMethod()
	{
		StringBuffer sb = new StringBuffer();
        
		if (assertions.size() > 0) {
			sb.append(firstIndent + "{\n");
			sb.append(secondIndent + type + " result = " + command + ";\n");
			sb.append(assertions.get(0));
			sb.append(firstIndent + "}\n");
		} else {
			sb.append(firstIndent + command + statementEnd);
		}
                    
		return sb.toString();
	}
}
