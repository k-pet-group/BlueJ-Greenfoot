package bluej.views;

/**
 ** @author Michael Kolling
 **
 ** A "callable" is the generalisation of a Constructor and a Method.
 ** This class contains aspects common to both of those.
 **/
public abstract class CallableView extends MemberView
{
    /**
     * Constructor.
     */
    public CallableView(View view)
    {
	super(view);
    }
	
    /**
     * @returns a boolean indicating whether this method has parameters
     */
    public abstract boolean hasParameters();
	
    /**
     *  Build the signature string. Format:
     *    name(type,type,type)
     */
    protected String makeSignature(String name, Class[] params)
    {
	StringBuffer sb = new StringBuffer();

	sb.append(name);
	sb.append("(");
	for(int j = 0; j < params.length; j++) {
	    sb.append(View.getTypeName(params[j]));
	    if (j < (params.length - 1))
		sb.append(",");
	}
	sb.append(")");
	return sb.toString();
    }

    /**
     * Make the description string. Format:
     *    name(type name, type name, type name) or
     *    name(name, name, name)
     */
    protected String makeDescription(String name, Class[] params, 
				   boolean includeTypeNames)
    {
	StringBuffer sb = new StringBuffer();

	sb.append(name);
	sb.append("(");
	for(int j = 0; j < params.length; j++) {
	    if(includeTypeNames) {
		sb.append(View.getTypeName(params[j]));
		sb.append(" ");
	    }
	    
	    String paramname = null;
	    if(getComment() != null)
		paramname = getComment().getParamName(j);
	    else if(!includeTypeNames) {
		//Debug.message("substitute type for name");
		paramname = View.getTypeName(params[j]);
	    }
	    if(paramname != null) {
		sb.append(paramname);
	    }
	    if (j < (params.length - 1))
		sb.append(", ");
	}
	sb.append(")");
	return sb.toString();
    }

    /**
     * Count of parameters
     * @returns the number of parameters
     */
    public int getParameterCount()
    {
	return getParameters().length;
    }


    /**
     * Get an array of Class objects representing parameters
     * @returns array of Class objects
     */
    public abstract Class[] getParameters();

}
