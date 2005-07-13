package bluej.views;

import bluej.debugger.gentype.JavaType;

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
     * Get an array of Class objects representing parameter classes
     * @return  array of Class objects
     */
    public abstract Class[] getParameters();
    
    /**
     * Get an array of GenType objects representing parameter types. For a
     * generic method the types returned will be the base type of any type
     * parameters, rather than the type parameters themselves.<p>
     * 
     * For instance, <code>&lt;T extends Object&gt; T genMethod(T a)</code><p>
     * 
     * ... would return Object as the type parameter. 
     * 
     * @param raw  whether to return raw versions of the parameter types
     * @return  the parameter types
     */
    public abstract JavaType[] getParamTypes(boolean raw);

    /**
     * Gets an array of strings with the names of the parameters
     * @return
     */
    public String[] getParamNames()
    {
        Comment c = getComment();
        if( c == null )
            return null;
        return c.getParamNames();
    }
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters 
     */
    public abstract String[] getParamTypeStrings();
    
    public void print(FormattedPrintWriter out)
    {
        print(out, 0);
    }

    public void print(FormattedPrintWriter out, int indents)
    {
        Comment comment = getComment();
        if(comment != null)
            comment.print(out, indents);

        out.setItalic(false);
        out.setBold(true);
        for(int i=0; i<indents; i++)
            out.indentLine();
        out.println(getLongDesc());
    }

}