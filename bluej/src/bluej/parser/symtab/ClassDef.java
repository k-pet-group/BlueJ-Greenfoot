
package bluej.parser.symtab;

import java.util.Enumeration;

/*****************************************************************************
 * Definition of a Java class OR interface
 *  These are merged together because there are places where we just don't
 *  know if something is an interface or class (because we are not looking
 *  at the classes/interfaces that are imported.)
 ****************************************************************************/

class ClassDef extends HasImports       
{ 
    //========================================================================
    //==  Class Variables
    //========================================================================
    
    static final int CLASS = 0;
    static final int INTERFACE = 1;
    static final int EITHER = 2;
    
    /** The type of object this represents 
     *  We may not initially know, as a statement like
     *    import java.awt.Color
     *  _could_ be referring to a class _or_ interface.
     *  Of course a full implementation of a cross-reference
     *    tool would either parse the imports at this point,
     *    or read information from the class file...
     */
    private int classOrInterface=EITHER;

    /** indicating whether we have an abstract class */
    private boolean isAbstract=false;
    
    /** The class from which this class was extended 
     *  (This only applies if this represents a CLASS)
     */
    private ClassDef superClass;
    
    /** A list of classes that extend this class, OR
     *  interfaces that extend this interface
     */
    private JavaVector subClasses;
    
    /** A list of interfaces that this class implements, OR
     *  a list of super interfaces for this interface
     */
    private JavaVector interfaces;
    
    /** A list of classes that implement this interface
     *  (This only applies if this represents an INTERFACE)
     */
    private JavaVector implementers;        


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor for java.lang.Object
     *  This was the easiest way to avoid a nasty endless recursion
     */
    ClassDef() {
        super("Object", null, null);
    }   


    /** Constructor for named, but otherwise unknown class
     */
    ClassDef(String name) {
        super(name, null, null);
    }   


    /** Constructor to set up a class */
    ClassDef(String name,               // thhe name of the class
             boolean isAbstract,	// is this class abstract?
	     Occurrence occ,            // where it was defined
             ClassDef superClass,       // its superclass (if applicable)
             JavaVector interfaces,     // interfaces that it implements
             ScopedDef parentScope) {   // which scope owns it
        super(name, occ, parentScope);
        
	this.isAbstract = isAbstract;
        // if a superclass was specified, set it
        if (superClass != null)
            this.superClass = superClass;
            
        // keep track of implemented interfaces
        this.interfaces = interfaces;
    }   


    /** Adds a reference to the list of classes that implement this interface */
    void addImplementer(ClassDef def) {
        getImplementers().addElement(def);
        setType(INTERFACE);
        def.setType(CLASS);
    }   


    /** Add a reference to a class that extends this class 
     *  (or an interface that extends this interface
     */
    void addSubclass(ClassDef subclass) {
        getSubClasses().addElement(subclass);
    }   


    /** get the list of classes that implement this interface */
    JavaVector getImplementers() {
        if (implementers == null) // lazy instantiation
            implementers = new JavaVector();
        return implementers;
    }   


    /** Return the list of interfaces that this class implements
     *  (or the interfaces that this interface extends)
     */
    JavaVector getInterfaces() {
        return interfaces;
    }


    /** return a list of all subclasses/subinterfaces of this */
    JavaVector getSubClasses() {
        if (subClasses == null)
            subClasses = new JavaVector();
        return subClasses;
    }   


    /** Return a reference to the superclass of this class */
    ClassDef getSuperClass() {
        return superClass;
    }


    /** Does this represent a Java class? */
    boolean isClass() {
        return classOrInterface == CLASS;
    }   


    /** Does this represent a Java interface? */
    boolean isInterface() {
        return classOrInterface == INTERFACE;
    }   


    /** Lookup a method in the class or its superclasses */
    Definition lookup(String name, int numParams) {
        // try to look it up in our scope
        Definition d = super.lookup(name, numParams);
        
        // if not found, look it up in our superclass (if we have one)
        if (d == null)
            if (getSuperClass() != null) {
                setType(CLASS);
                getSuperClass().setType(CLASS);
                d = getSuperClass().lookup(name, numParams);
            }   
                
        // if still not found, look for it in any of our implemented interfaces
        if (d == null && interfaces != null) {
            Enumeration e = interfaces.elements();
            while(d == null && e.hasMoreElements())
                d = ((ClassDef)e.nextElement()).lookup(name, numParams);
        }   
        
        return d;
    }   


    /** Collect information about the class */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {

	info.setName(getName());

	if(isInterface())
	    info.setInterface(true);

	if(isAbstract)
	    info.setAbstract(true);

	// get info about the superclass
        if (getSuperClass() != null)
	    info.setSuperclass(getSuperClass().getQualifiedName());

	// get info about the interfaces which are implemented or extended
	// by this class/interface
        if(interfaces != null) {
	    Enumeration e = interfaces.elements();
	    while(e.hasMoreElements())
		info.addImplements(((ClassDef)e.nextElement()).getName());
	}

	// get info about imported classes/packages
        getImportInfo(info);

	// get info about definitions within this class
	getElementInfo(info, symbolTable);
    }   


    /** resolve referenced symbols */
    void resolveTypes(SymbolTable symbolTable) {
        System.err.println("resolving: "+getQualifiedName());
        // resolve superclass laundry
        super.resolveTypes(symbolTable);
        
        // if we have subclasses, resolve them to their symbols
        if (subClasses != null)
            subClasses.resolveTypes(symbolTable);
            
        // if we have a superclass, resolve it
        ClassDef newSuperClass = getSuperClass();
        if (newSuperClass != null && newSuperClass instanceof DummyClass) {
            // get its package name and look up the class/interace
            String pkg = ((DummyClass)newSuperClass).getPackage();
            newSuperClass = (ClassDef)symbolTable.lookupDummy(newSuperClass);
            if (newSuperClass == null)
                newSuperClass = new
                    DummyClass(symbolTable.getUniqueName(getSuperClass().getName()),
                               null, symbolTable.getUniqueName(pkg));

            // if we were able to resolve the superclass, add the reference
            // to its reference list and make it this class' superclass
            if (newSuperClass != null) {
                newSuperClass.addReference(getSuperClass().getOccurrence());
                setSuperClass(newSuperClass);
                newSuperClass.addSubclass(this);
                newSuperClass.setType(ClassDef.CLASS);
            }   
            setType(CLASS);
        }   

        // if this class implements any interfaces, resolve those interfaces
        if (interfaces != null) {
            interfaces.resolveTypes(symbolTable);
            
            // add this class to the list of implementers for each interface
            Enumeration e = interfaces.elements();
            while(e.hasMoreElements())          
                ((ClassDef)e.nextElement()).addImplementer(this);
        }
        
        // we're done, so toss the packages (only for top-level classes)
        if (isTopLevel())
            closeImports(symbolTable);
    }   


    /** Set the list of interfaces that this class implements */
    void setInterfaces(JavaVector interfaces) {
        this.interfaces = interfaces;
    }


    /** set the superclass of this class */
    void setSuperClass(ClassDef superClass) {
        this.superClass = superClass;
        setType(CLASS);
        superClass.setType(CLASS);
    }   


    /** Specify if this is a class or interface once we know */
    void setType(int type) {
        classOrInterface = type;
    }   
}
