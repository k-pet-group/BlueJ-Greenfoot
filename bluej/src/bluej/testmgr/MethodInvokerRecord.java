package bluej.testmgr;

import bluej.utility.JavaNames;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls that return a result.
 *
 * @author  Andrew Patterson
 * @version $Id: MethodInvokerRecord.java 1941 2003-05-05 06:07:49Z ajp $
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
				sb.append(getAssertion(0));
				sb.append(command);
				sb.append(")");
				sb.append(statementEnd);
			}
			else {
				sb.append(secondIndent);
				sb.append(getAssertion(0));
				sb.append(benchName);
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
				sb.append(getAssertion(i));
				sb.append(assertAgainstName);
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
