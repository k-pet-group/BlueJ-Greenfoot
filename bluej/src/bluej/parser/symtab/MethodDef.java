
package bluej.parser.symtab;

import java.util.Enumeration;

/*******************************************************************************
 * A definition of a method in a class
 ******************************************************************************/
class MethodDef extends ScopedDef implements TypedDef
{
    //==========================================================================
    //==  Class Variables
    //==========================================================================

    /** The comment attached to this method definition */
    private String comment;

    /** The return type of the method */
    private Definition type = null;

    /** A list of formal parameters to the method */
    private JavaVector parameters;

    /** A list of exceptions that can be thrown */
    private JavaVector exceptions;
    
    /** used if method is a generic method */
    private String genericTypeArgument;


    //==========================================================================
    //==  Methods
    //==========================================================================


    /** Constructor to create a method definition object */
    MethodDef(String name,               // the name of the method
              Occurrence occ,            // where it was defined
              ClassDef type,             // the return type of the method
              ScopedDef parentScope,     // which scope owns it
              String typeArgument) {     // typeArgument if generic method 
        super(name, occ, parentScope);
        this.type = type;
        this.genericTypeArgument = typeArgument;
    }


    /** Add a thrown exception to the method's exception list */
    void add(ClassDef excep) {
        if (exceptions == null) // lazy instantiation
            exceptions = new JavaVector();
        exceptions.addElement(excep);
    }


    /** Add a parameter to the method's parameter list */
    void add(VariableDef param) {
        if (parameters == null) // lazy instantiation
            parameters = new JavaVector();
        parameters.addElement(param);
    }


    /** Find out how many parameters this method has */
    int getParamCount() {
        if (parameters == null)
            return 0;
        return parameters.size();
    }


    /** Return the return type of the method */
    public Definition getType() {
        return type;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /** lookup the name as a local variable or local class in this class */
    Definition lookup(String name, int numParams) {
        if (numParams == -1) {
            // look for it in the method's scope
            Definition d = super.lookup(name, numParams);
            if (d != null) return d;

            // otherwise, look in the parameters for the method
            if (parameters != null) {
                Enumeration e = parameters.elements();
                while(e.hasMoreElements()) {
                    d = (Definition)e.nextElement();
                    if (d.getName().equals(name))
                        return d;
                }
            }
        }
        return null;
    }


    /** Collect information about this method */
    public void getInfo(ClassInfo info, SymbolTable symbolTable)
    {
        StringBuffer target = new StringBuffer();  // the method signature

        if(genericTypeArgument != null) {
            target.append(genericTypeArgument);
            target.append(" ");
        }
        // if it has a return type, add it
        if (type != null) {
            info.addUsed(type.getQualifiedName());
            target.append(type.getName());
            target.append(" ");
        }

        target.append(getName());
        target.append("(");
        // if it has parameters, list them
        StringBuffer paramnames = new StringBuffer();

        if (parameters != null) {
    	    Enumeration e = parameters.elements();
    	    while(e.hasMoreElements()) {
    	        VariableDef vd = ((VariableDef)e.nextElement());
                paramnames.append(vd.getName());
                paramnames.append(" ");

    	        vd.getInfo(info, symbolTable);

    	        target.append(vd.getType().getName());

                for(int i=0; i<vd.getArrayLevel(); i++)
                    target.append("[]");
                
                // if it is a vararg    
                if(vd.isVarargs())
    	            target.append(" ...");
                
    	        target.append(",");
            }
        }

        int lastchar = paramnames.length()-1;
        if (lastchar >= 0 && paramnames.charAt(lastchar) == ' ')
            paramnames.deleteCharAt(lastchar);

        lastchar = target.length()-1;
        if (target.charAt(lastchar) == ',')
            target.deleteCharAt(lastchar);
        target.append(")");

        // if it throws exceptions, list them
        if (exceptions != null) {
    	    Enumeration e = exceptions.elements();
    	    while(e.hasMoreElements())
        		info.addUsed(((ClassDef)e.nextElement()).getQualifiedName());
        }

        info.addComment(target.toString(), comment, paramnames.toString());

        super.getInfo(info, symbolTable);
    }


    /** Resolve references to other symbols for pass 2 */
    void resolveTypes(SymbolTable symbolTable) {
        // if we have parameters and/or exceptions, resolve them
        if (parameters != null) parameters.resolveTypes(symbolTable);
        if (exceptions != null) exceptions.resolveTypes(symbolTable);

        // if we have a return type, resolve it
        if (type != null && type instanceof DummyClass) {
            Definition newType = symbolTable.lookupDummy(type);
            if (newType != null) {
                newType.addReference(type.getOccurrence());
                type = newType;
            }
        }

        // perform resolution for our superclass
        super.resolveTypes(symbolTable);
    }


    /** set the list of exceptions that this method can throw */
    void setExceptions(JavaVector exceptions) {
        this.exceptions = exceptions;
    }
}
