package bluej.testmgr;

import bluej.utility.JavaNames;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls that return a result.
 *
 * @author  Andrew Patterson
 * @version $Id: MethodInvokerRecord.java 2222 2003-10-27 02:19:26Z bquig $
 */
public class MethodInvokerRecord extends VoidMethodInvokerRecord
{
    private Class returnType;
	private String benchType;
	private String benchName;
	
    public MethodInvokerRecord(Class returnType, String command)
    {
    	super(command);
    	
        this.returnType = returnType;
        this.benchType = returnType.getName();
        this.benchName = null;
    }

	public void setBenchName(String name, String type)
	{
		benchName = name;
		benchType = type;
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
		// if it hasn't been assigned a name there is nothing to do for
		// fixture declaration
		if (benchName == null)
			return null;

		// declare the variable		
		StringBuffer sb = new StringBuffer();
		sb.append(firstIndent);
		sb.append(benchDeclaration());
		sb.append(benchName);
		sb.append(statementEnd);

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
		if (benchName == null) {
			return secondIndent + command + statementEnd;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(secondIndent);
		sb.append(benchAssignmentTypecast());
        sb.append(statementEnd);
		
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
		StringBuffer sb = new StringBuffer();

		// an assignment to the bench changes the way we do things
		// first we construct an assignment statement for it
		if (benchName != null) {
			sb.append(secondIndent);
			sb.append(benchDeclaration());
			sb.append(benchAssignmentTypecast());
			sb.append(statementEnd);		
		}

		// first, with no assertions we either need to just do the
		// assignment made above, or just do the statement by itself
		if (getAssertionCount() == 0) {
			if (benchName == null)
				sb.append(secondIndent + command + statementEnd);
		}
		
		// with only one assertion we can merge the assertion
		// with the statement or the name
		if (getAssertionCount() == 1) {		
			if (benchName == null) {
				sb.append(secondIndent);
                sb.append(insertCommand(getAssertion(0), command));
				sb.append(")");
				sb.append(statementEnd);
			}
			else {
				sb.append(secondIndent);
                sb.append(insertCommand(getAssertion(0), benchName));
				sb.append(")");
				sb.append(statementEnd);
			}
		}			

		// with multiple assertions we need to do some fancy
		// scoping
		if (getAssertionCount() > 1) {
			String indentLevel;
			String assertAgainstName;

			if (benchName == null) {
				indentLevel = thirdIndent;
				assertAgainstName = "result";
			} else {
				indentLevel = secondIndent;
				assertAgainstName = benchName;
			}

			// with no bench assignment, introduce a new scope
			if (benchName == null) {
				sb.append(secondIndent);
				sb.append("{\n");			

				sb.append(thirdIndent);
				sb.append(JavaNames.typeName(returnType.getName()));
				sb.append(" result = ");
				sb.append(command);
				sb.append(";\n");

			}
			
			// here are all the assertions
			for(int i=0; i<getAssertionCount(); i++) {
				sb.append(indentLevel);
                sb.append(insertCommand(getAssertion(i), assertAgainstName));
				sb.append(")");
				sb.append(statementEnd);
			}

			// with no bench assignment, end the result scope
			if (benchName == null) {
				sb.append(secondIndent);
				sb.append("}\n");		
			}			
		}

		return sb.toString();
	}
    
    /**
     * insert the command into the assertion.  The position may alter if using
     * a floating point assertion for "assertEquals" which uses a second parameter
     * to specify delta range. 
     * @return the combined string representing original assertion with the 
     * insertion of the command or reference to bench object. 
     */
    private String insertCommand(String assertion, String command)
    {
        StringBuffer assertCommand = new StringBuffer(assertion);
        int firstComma = assertion.indexOf(',');
        if(firstComma!= -1)
            assertCommand.insert(firstComma + 1, command);
        else
            assertCommand.append(command);
        return assertCommand.toString();
    }

	private String benchDeclaration()
	{
		return JavaNames.typeName(benchType) + " ";
	}
	
	private String benchAssignmentTypecast()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append(benchName);
		sb.append(" = ");

		// check if a typecast is required
		if (!benchType.equals(returnType.getName())) {
			sb.append("(");
			sb.append(benchType);
			sb.append(")");
		}

		sb.append(command);

		return sb.toString();
	}
}
