
package bluej.parser.symtab;

import java.util.Enumeration;
import bluej.parser.JavaToken;

/*******************************************************************************
 * An extended Vector class to provide simple lookup and type resolution
 * methods
 ******************************************************************************/
public class JavaVector extends java.util.Vector {
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** prevent nested resolutions... */
    private boolean resolvingRefs=false;
    private boolean resolvingTypes=false;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new Java vector */
    public JavaVector() {
        super();
    }


    /** Add a new element to the vector (used for debugging) */
    public void addElement(Definition o) {
        super.addElement(o);
        if (o == null)
            throw new IllegalArgumentException("null element added to vector");
        
    }   


    /** get an element from the */
    public Definition getElement(String name) {
        Enumeration e = elements();
        while(e.hasMoreElements()) {
            Definition d = (Definition)e.nextElement();
            if (d.getName().equals(name))
                return d;
        }
        return null;
    }   


    /** list the names of all elements in the vector */
    void list(IndentingPrintWriter out) {
        Enumeration e = elements();
        while(e.hasMoreElements())
            out.println(((Definition)e.nextElement()).getQualifiedName());
    }   


    /** list the names of all elements in the vector, but first print
     *  a title for the section and indent the following lines
     */
    void listIndented(IndentingPrintWriter out, String title) {
        if (title != null)
            out.println(title);
        out.indent();
        list(out);
        out.dedent();
    }


    /** Get info about references that are stored as JavaTokens */
    public void getUnresolvedInfo(ClassInfo info, SymbolTable symbolTable) 
    {
        if (!resolvingRefs) {
            resolvingRefs = true;

            // examine all tokens
            Enumeration e = elements();
            while(e.hasMoreElements()) {
                JavaToken t = (JavaToken)e.nextElement();

		// the token name can be something like "package.class.field"
		// split it into separate names
		String name = t.getText();
		int dotPos = name.indexOf(".");
		while(dotPos != -1) {
		    String partName = name.substring(0, dotPos);
		    // check whether the name is a known class
		    Definition d = symbolTable.lookup(partName);
		    if ((d != null) && (d instanceof ClassDef))
			info.addUsed(d.getQualifiedName());
		    dotPos = name.indexOf(".", dotPos+1);
		}
		Definition d = symbolTable.lookup(name);  // try full name
		if ((d != null) && (d instanceof ClassDef))
		    info.addUsed(d.getQualifiedName());
            } 
        }   
    }

    /** Resolve references that are stored as JavaTokens */
    public void resolveRefs(SymbolTable symbolTable) {
        if (!resolvingRefs) {
            resolvingRefs = true;
            // resolve each element in the list
            Enumeration e = elements();
            while(e.hasMoreElements()) {
                JavaToken t = (JavaToken)e.nextElement();
                Definition d = symbolTable.lookup(t.getText(),t.getParamCount());
                if (d == null)
                    d = symbolTable.findPackage(t.getText());
                if (d != null)
                    d.addReference(new Occurrence(t.getFile(),
                                                  t.getLine()));
            } 
        }   
    }


    /** Resolve the types of dummy elements in the vector */
    public void resolveTypes(SymbolTable symbolTable) {
        if (!resolvingTypes) {
            resolvingTypes = true;
            Enumeration e = elements();
            while(e.hasMoreElements()) {
                Definition d = (Definition)e.nextElement();
                if (d instanceof DummyClass) {
                    String pkg = ((DummyClass)d).getPackage();
                     Definition newD = symbolTable.lookupDummy(d);
                    if (newD != null) {
                        newD.addReference(d.getOccurrence());
                        removeElement(d);
                        addElement(newD);
                    }   
                }   
                else
                    d.resolveTypes(symbolTable);
            }   
        }   
    }
}
