package bluej.views;

/**
 * A "callable" is the generalisation of a Constructor and a Method. This class
 * contains aspects common to both of those.
 * 
 * @author Michael Kolling
 *  
 */
public abstract class CallableView extends MemberView
{
    /**
     * Constructor.
     */
    public CallableView(View view) {
        super(view);
    }

    /**
     * @returns a boolean indicating whether this method has parameters
     */
    public abstract boolean hasParameters();
    
    /**
     * @returns a boolean indicating whether this method uses var args
     */
    public abstract boolean isVarArgs();

    /**
     * Build the signature string. Format: name(type,type,type)
     */
    protected String makeSignature(String name, Class[] params) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            String typeName = View.getTypeName(params[j]);
            if(isVarArgs() && j==(params.length-1)) {
                typeName = createVarArg(typeName);
            }                
            sb.append(typeName);
            if (j < (params.length - 1))
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Make the description string. Format: name(type name, type name, type
     * name) or name(name, name, name)
     */
    protected String makeDescription(String name, Class[] params, boolean includeTypeNames) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            if (includeTypeNames) {
                String typeName = View.getTypeName(params[j]);
                if(isVarArgs() && j==(params.length-1)) {
                    typeName = createVarArg(typeName);
                }                
                sb.append(typeName);
                sb.append(" ");
            }
            String paramname = null;
            if (getComment() != null)
                paramname = getComment().getParamName(j);
            else if (!includeTypeNames) {
                //Debug.message("substitute type for name");
                String typeName = View.getTypeName(params[j]);
                if(isVarArgs() && j==(params.length-1)) {
                    typeName = createVarArg(typeName);
                }             
                paramname = typeName;
            }
            if (paramname != null) {
                sb.append(paramname);
            }
            if (j < (params.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Changes an array type name (Object[]) to a var arg (Object ...)
     * 
     * @param typeName The name of the type
     * @return A var arg representation of the type
     */
    private String createVarArg(String typeName) {
        String lastArrayStripped = typeName.substring(0,typeName.length()-2);
        return lastArrayStripped + " ...";        
    }

    /**
     * Count of parameters
     * @returns the number of parameters
     */
    public int getParameterCount() {
        return getParameters().length;
    }

    /**
     * Get an array of Class objects representing parameters
     * @returns array of Class objects
     */
    public abstract Class[] getParameters();
}