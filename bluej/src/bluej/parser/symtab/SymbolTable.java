
package bluej.parser.symtab;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.StringTokenizer;
import bluej.parser.JavaToken;

/*******************************************************************************
 * A SymbolTable object keeps track of all symbols encountered while 
 *  parsing a file.  It's main components are a list of all packages
 *  that have been parsed or imported, and a stack of symbols that represent
 *  the syntactical scope of a source file as it is being parsed.
 ******************************************************************************/
public class SymbolTable        
{
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** a dummy scope to hold things like primitive types */
    private BlockDef baseScope = null;

    /** the "default" package to hold classes w/o package definitions */
    private PackageDef defaultPackage = null;
    
    /** A list of all strings encountered in the source */
    private StringTable   names = new StringTable();
    
    /** A stack of currently-active scopes */
    private JavaStack     activeScopes;
    
    /** A list of all packages that have been parsed or imported */
    private JavaHashtable packages;
    
    /** A specific scope to look in if the source code contains
     *  an explicitly-qualified identifier
     */
    private Definition    qualifiedScope;
    
    /** The file that is currently being parsed */
    private File          currentFile;
    
    /** The method header that is currently being parsed.
     *  This is used to associate variable definitions as
     *  parameters to a method
     */
    private MethodDef     currentMethod;
    
    /** The amount of spacing to use when printing a report line */
    private String        currentIndent = "";
    
    /** A list of packages that are being imported on demand */
    private JavaVector    demand;
    
    /** A list of classes that have been explicitly imported */
    private JavaHashtable importedClasses;
    
    /** The java.lang package */
    private PackageDef javaLang;

    /** The "java.lang.Object" class */
    private ClassDef object;

    /** The names of primitive type, which we want to ignore in uses list */
    private static Vector predefined;

    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new symbol table */ 
    public SymbolTable() {
        // allocate storage for the packages and scope lists
        packages        = new JavaHashtable();
        activeScopes    = new JavaStack();
        
        // Create a package object to represent java.lang
	Occurrence o = new Occurrence(null,0);
        javaLang        = new PackageDef(getUniqueName("java.lang"),
                                          new Occurrence(null,0),null);

        // Create a block to hold predefined types
        baseScope = new BlockDef(null, null, null);
        pushScope(baseScope);
        baseScope.setDefaultOrBaseScope(true);
        
        // define the predefined types
        // treat them as being an inheritance hierarchy to make widening
        //   conversions automatic. For example, a float can widen to a double,
        //   so we treat float as a _subclass_ of double.  This makes
        //   parameter matching for method lookup simple!
        //   (NOTE: the real parameter lookup routine that would be used
        //          for method resolution is not implemented in this simple
        //          cross-reference tool.  However, treating the primitives
        //          as an inheritance hierarchy is included to show a
        //          technique that might be used in a real cross-reference tool.)
        PrimitiveDef pBoolean = new PrimitiveDef(getUniqueName("boolean"),
                                                          getObject(), baseScope);
        PrimitiveDef pDouble  = new PrimitiveDef(getUniqueName("double"),
                                                          getObject(), baseScope);
        PrimitiveDef pFloat   = new PrimitiveDef(getUniqueName("float"),
                                                          pDouble,     baseScope);
        PrimitiveDef pLong    = new PrimitiveDef(getUniqueName("long"),
                                                          pFloat,      baseScope);
        PrimitiveDef pInt     = new PrimitiveDef(getUniqueName("int"),
                                                          pLong,       baseScope);
        PrimitiveDef pShort   = new PrimitiveDef(getUniqueName("short"),
                                                          pInt,        baseScope);
        PrimitiveDef pByte    = new PrimitiveDef(getUniqueName("byte"),
                                                          pShort,      baseScope);
        PrimitiveDef pChar    = new PrimitiveDef(getUniqueName("char"),
                                                          pInt,        baseScope);
        PrimitiveDef pVoid    = new PrimitiveDef(getUniqueName("void"),
                                                          getObject(), baseScope);
        
        baseScope.add(pBoolean);
        baseScope.add(pDouble);
        baseScope.add(pFloat);
        baseScope.add(pLong);
        baseScope.add(pInt);
        baseScope.add(pShort);
        baseScope.add(pByte);
        baseScope.add(pChar);
        baseScope.add(pVoid);

	predefined = new Vector(9);
        predefined.addElement("boolean");
        predefined.addElement("double");
        predefined.addElement("float");
        predefined.addElement("long");
        predefined.addElement("int");
        predefined.addElement("short");
        predefined.addElement("byte");
        predefined.addElement("char");
        predefined.addElement("void");
        predefined.addElement("String");
    }   


    public static Vector getPredefined()
    {
	return predefined;
    }

    /**
     *  Add some classes to the current scope. 'classes' is a vector of
     *  Strings (the class names).
     */
    public void addClasses(Vector classes) 
    {
	if(classes != null) {
	    Enumeration e = classes.elements();
	    while(e.hasMoreElements()) {
		Definition def = new ClassDef((String)e.nextElement());

		// add the definition to the current scope
		getCurrentScope().add(def);
        
		// set the parent scope for the definition
		def.setParentScope(getCurrentScope());
	    }
	}
    }   


    /** Add a package to the list of packages available on demand
     *  ("On demand" refers to imports that use an "*" to mean "any class
     *  that resides in the package."  For example
     *     import java.awt.*;
     *  is an on-demand import that says "if we don't find a class anywhere
     *  else, try to find it in the java.awt.* package.
     */
    void addDemand(PackageDef pkg) {
        demand.addElement(pkg);
    }


    /** Add a package that has been imported */
    public void addImport(JavaToken tok, String className, String packageName) {
        if (importedClasses == null) // lazy instantiation
            importedClasses = new JavaHashtable();  
        
        // if there is no package name, use the default package
        if (packageName.equals(".") || packageName.equals("")) {
            importedClasses.put(getUniqueName("~default~"), getDefaultPackage());
            return;
        }   
            
        // otherwise, chop the extra "." that the parser adds...
        else
            packageName = packageName.substring(1);
            
        // if there is no class, we are importing a package on demand
        // so create a dummy package definition (if one doesn't already
        // exist)
        if (className == null) {
            Definition d = (Definition)packages.get(packageName);
            if (d == null) {
                d = new PackageDef(getUniqueName(packageName),
                                   new Occurrence(currentFile, tok.getLine()),
                                   null);
                packages.put(packageName, d);
            }   
            importedClasses.put(d.getName(), d);
            reference(tok);
        }   
        
        // otherwise, create a placeholder class for class/interface ref
        else {
            importedClasses.put(getUniqueName(className),
                new DummyClass(getUniqueName(className),
                               new Occurrence(currentFile, tok.getLine()),
                               getUniqueName(packageName)));
            reference(tok);
        }   
    }


    /** Add an element to the current scope */
    void addToCurrentScope(Definition def) {
        // add the definition to the current scope
        getCurrentScope().add(def);
        
        // set the parent scope for the definition
        def.setParentScope(getCurrentScope());
    }   


    /** We are done with imports, so clear the list */
    void closeImports() {
        demand          = null;
        importedClasses = null;
    }   


    /** Define a curly-brace-delimited block of code */
    public Definition defineBlock(JavaToken tok) {
        // create a new block definition and push it
        // as the current scope
        BlockDef def = new BlockDef(null, getOccurrence(tok), getCurrentScope());
        addToCurrentScope(def);
        return pushScope(def);
    }   


    /** Define a class object */
    public void defineClass(JavaToken theClass,      // class being created
			    JavaToken superClass,    // its superclass
			    JavaVector interfaces,   // implemented interfaces
			    boolean isAbstract)
    {
        // note -- we leave interfaces as a vector of JavaTokens for now
        //         we'll resolve them in pass 2.

        // create a new class definition for the class
        ClassDef def = new ClassDef(getUniqueName(theClass),
				    isAbstract,
				    getOccurrence(theClass),
				    superClass==null ?
				                null :
				                getDummyClass(superClass),
				    interfaces,
				    getCurrentScope());
                                         
        def.setType(ClassDef.CLASS);
                                         
        // add the imported classes/packages to the class
        def.setImports(importedClasses);
        
        // add the class to the current scope
        addToCurrentScope(def);
        
        // make the claa be the new current scope
        pushScope(def);
    }   


    /** Define an interface object */
    public void defineInterface(JavaToken theInterface,
                                      JavaVector superInterfaces) {
        // note -- we leave superInterfaces as a vector of JavaTokens for now.
        //         we'll resolve in pass 2.
        
        // create the new interface object
        ClassDef def = new ClassDef(getUniqueName(theInterface),
				    false,
                                    getOccurrence(theInterface),
                                    null, // no super class...
                                    superInterfaces,
                                    getCurrentScope());
                                             
        def.setType(ClassDef.INTERFACE);
        
        // add it to the current scope
        addToCurrentScope(def);
        
        // make the interface the current scope
        pushScope(def);
    }   


    /** Define a new label object */
    public void defineLabel(JavaToken theLabel) {
        addToCurrentScope(new LabelDef(getUniqueName(theLabel),
                                getOccurrence(theLabel),
                                getCurrentScope()));
    }   


    /** Define a new method object */
    public void defineMethod(JavaToken theMethod, JavaToken type) {
        // if there is no type, this is a constructor
        String name;
        if (type == null)
            name = "~constructor~";
            
        // otherwise use its real name
        else {
            if (theMethod == null) {
                theMethod = type;
                type = null;
            }   
            name = theMethod.getText();         
        }   

        // create the method definition
        currentMethod = new MethodDef(getUniqueName(name),
                                             getOccurrence(theMethod),
                                             getDummyClass(type),
                                             getCurrentScope());
                                            
        // add the method to the current scope
        addToCurrentScope(currentMethod);
        
        // make the method be the current scope
        pushScope(currentMethod);
    }


    /** Define a new package object 
     *  This is an adapter version to get the name of the package from a token
     */
    public void definePackage(JavaToken tok) {
        definePackage(getUniqueName(tok));
    }   


    /** Define a new package object */
    PackageDef definePackage(String name) {
        // try to find thew package
        PackageDef pkg = (PackageDef)packages.get(name);
        
        // if we don't already have the package, define it
        if (pkg == null) {
            pkg = new PackageDef(getUniqueName(name), null, null);
            packages.put(name, pkg);
        }   
        
        // make the package be the current scope
        pushScope(pkg);
        return pkg;
    }


    /** create a variable definition */
    public void defineVar(JavaToken theVariable, JavaToken type) {
        // create the variable definition
        VariableDef v = new VariableDef(getUniqueName(theVariable),
                                        getOccurrence(theVariable),
                                        getDummyClass(type),
                                        getCurrentScope());
                                            
        // if we are in a method's parameter def, add to its parameters
        if (currentMethod != null)
            currentMethod.add(v);
            
        // otherwise, add to the current scope
        else
            addToCurrentScope(v);
    }   


    /** State that we are done processing the method header */
    public void endMethodHead(JavaVector exceptions) {
        // set its thrown exception list
        currentMethod.setExceptions(exceptions);
        
        // reset the method indicator
        // NOTE:  this is not a problem for inner classes; you cannot define an
        //        inner class inside a formal parameter list, so we don't need a
        //        stack of methods here...
        currentMethod = null;
    }   


    /** look for the name in the import list for this class */
    Definition findInImports(String name) {
        Definition def=null;
        
        // look at the stuff we imported
        // (the name could be a class name)
        if (importedClasses != null)
            def = (Definition)importedClasses.get(name);
        
        // otherwise, take a look in the import-on-demand packages to
        //   see if the class is defined
        if (def==null && demand != null && name.charAt(0) != '~') {
            Enumeration e = demand.elements();
            while(def==null && e.hasMoreElements())
                def = ((PackageDef)e.nextElement()).lookup(name);
        }
        
        return def;
    }   


    /** Lookup a package in the list of all parsed packages */
    Definition findPackage(String name) {
        return (Definition)packages.get(name);
    }       


    /** Return the currently-active scope */
    ScopedDef getCurrentScope() {
        if (activeScopes.empty())
            return null;
        return (ScopedDef)activeScopes.peek();
    }   


    /** Define a new package object */
    PackageDef getDefaultPackage() {
        // if the default package has not yet been defined, create it
        // (lazy instantiation)
        if (defaultPackage == null) {
            defaultPackage = new PackageDef(getUniqueName("~default~"), null, null);
            packages.put(getUniqueName("~default~"), defaultPackage);
            defaultPackage.setDefaultOrBaseScope(true);
        }   
        return defaultPackage;
    }


    /** Create a new dummy class object */
    public DummyClass getDummyClass(JavaToken tok) {
        if (tok == null) return null;
        return new DummyClass(getUniqueName(tok), getOccurrence(tok));
    }   


    /** Get the current indentation string */
    String getIndent() {
        return currentIndent;
    }


    /** Get the java.lang.Object object */
    ClassDef getObject() {
        if (object == null) { // lazy instantiation
            object = new DummyClass();
            object.setType(ClassDef.CLASS);
            // add it to package java.lang
            javaLang.add(object);
        }   
        return object;  
    }   


    /** Create a new occurrence object */
    Occurrence getOccurrence(JavaToken tok) {
        if (tok == null)
            return new Occurrence(null, 0);
        else
            return new Occurrence(currentFile, tok.getLine());
    }   


    /** return the current qualified scope for lookup.  */
    Definition getScope() {
        return qualifiedScope;
    }

    /** Get a unique occurrence of a String that has the name we want */
    String getUniqueName(JavaToken tok) {
        return getUniqueName(tok.getText());
    }   


    /** Get a unique occurrence of a String that has the specified name */
    String getUniqueName(String name) {
        return names.getName(name);
    }   


    /** Lookup a non-method in the symbol table 
     *  This version of lookup is a convenience wrapper that just passes -1
     *  as the parameter count to the real lookup routine
     */
    Definition lookup(String name) {
        return lookup(name, -1);
    }   


    /** Lookup a name in the symbol table */
    Definition lookup(String name, int numParams) {
        Definition def     = null;
        StringTokenizer st = null;
        String afterPackage = null;
        
        // If we have a dot ('.') in the name, we must first resolve the package,
        //  class or interface that starts the name, the progress through the
        //  name
        if (name.indexOf('.') > 0) {
            ///NOTE: class names can have the same name as a package and
            //       context will determine the result!!!!
            //  eg.  pkg com.magelang.xref  and class xref in
            //       com.magelang can coexist...
            //  The lookup algorithm use here is far simpler, and may
            //    resolve improperly (ie, if we have packages 
            //    com.magelang.xref and com.magelang and the string we are
            //   testing is com.magelang.xref, we will always assume that it
            //   is a package)
            // A better implementation of this routine would
            //   try to find the proper name by trying to match the entire
            //   name at each stage rather than just finding the longest
            //   valid package name.
            String testName     = null;

            // break up the string into an enumerartion of substrings that were
            //   separated by dots ('.')
            st  = new StringTokenizer(name, ".");

            // We'll walk through to find the longest package name that we
            //   know about, then start checking to see if the rest of the
            //   elements are validly scoped within that package
            boolean doneWithPackage = false;
            while (st.hasMoreElements()) {
                String id = (String)st.nextElement();
                Definition testIt = null;
                if (testName == null) {
                    testName = id;
                    // see if the first part of the name is an imported class
                    def = findInImports(testName);
                    if (def != null) {
                        doneWithPackage = true;
                        id = (String)st.nextElement();
                    }   
                }   
                else
                    testName += "." + id;
                
                // keep track of the longest name that is a package
                if (!doneWithPackage &&
                    ((testIt = (PackageDef)packages.get(name)) != null)) {
                    def = testIt;
                    afterPackage = null;
                }   
                else if (afterPackage == null)
                    afterPackage = id;
                else
                    afterPackage += "." + id;
            }
        }   
        
        // otherwise, just try to find the name in the imported classes/packages
        else if (numParams == -1) {
            def = findInImports(name);                  
            if (def != null)
                return def;
            }   


        // At this point, we may have a definition that represents the
        //   part of the name to the left of the rightmost dot ('.')
        // If we have such a definition, there might be something after it;
        //   a final part of the name. If so, we need to push the scope of the
        //   leftmost part of the identifier.  If not, we just want to analyze
        //   the entire name as a unit.
        if (def != null && afterPackage != null)
            setScope(def);
        else
            afterPackage = name;


        // Here we know we have more to look at...              
        if (afterPackage != null) { // more to chew on...
            // check to see if we still have any DOTs in the name
            // if so, we'll need to figure out where certain names start/end
            st = new StringTokenizer(afterPackage, ".");
         
            // find and load the classes up to the last
            while (st.hasMoreElements()) {
                String id = (String)st.nextElement();
                // if a explicit scope qualification was specified, look only there
                if (qualifiedScope != null) {
                    def = qualifiedScope.lookup(id, numParams);
                    resetScope();
                }

                // Otherwise, first try a scoped lookup
                else
                    def = activeScopes.lookup(id, numParams);
                
                if (def == null) break;

                if (st.hasMoreElements())
                    setScope(def);
            }
        }

        return def;
    }


    /** Lookup a class based on a placeholder for that class */
    Definition lookupDummy(Definition d) {
        String pkg = ((DummyClass)d).getPackage();
        return lookup((pkg==null?"":pkg+".")+d.getName());
    }   


    /** Set up the list of imported packages for use in symbol lookup */
    void openImports(JavaHashtable imports) {
        // start a new demand list
        demand = new JavaVector();
        
        // add package java.lang to the demand list...
        demand.addElement(javaLang);
        importedClasses = new JavaHashtable();
        
        // if this class has something to import...
        if (imports != null) {
            // walk through the list of imports
            Enumeration e = imports.elements();
            while(e.hasMoreElements()) {
                // add the package or class to the demand or import list
                //   based on the type of import it was
                Definition d = (Definition)e.nextElement();
                if (d instanceof PackageDef)
                    addDemand((PackageDef)d);
                else {
                    if (d instanceof DummyClass) {
                        Definition newD = lookupDummy(d);
                        if (newD != null) 
                            d = newD;
                    }   
                    importedClasses.put(d.getName(), d);
                }   
            }
        }   
    }


    /** Clear the scope stack (except the base scope for primitives) */
    public void popAllScopes() {
        while(activeScopes.peek() != baseScope)
            activeScopes.pop();
        importedClasses = null;
    }   


    /** Pop off the current scope from the stack */
    public void popScope() {
        activeScopes.pop();
    }   


    /** Push a scope on the stack for symbol lookup */
    Definition pushScope(Definition scope) {
        if (!(scope instanceof ScopedDef))
            throw new RuntimeException("Not a ScopedDef");
        activeScopes.push(scope);
        return scope;
    }   


    /** Add an unresolved reference to the current scope */
    public void reference(JavaToken t) {
        t.setFile(currentFile);
        getCurrentScope().addUnresolved(t);
    }   


    /** Get some info about this class */
    public void getInfo(ClassInfo info) {
        Enumeration e = packages.elements();
        while(e.hasMoreElements()) {
            PackageDef p = (PackageDef)e.nextElement();
            p.getInfo(info, this);
        }   
    }   


    /** unset the qualifiedScope so normal scoped lookup applies on the next
     *  name to look up
     */
    void resetScope() {
        setScope((Definition)null);
    }   


    /** resolve types of anything that needs resolution in the symbol table */
    public void resolveTypes() {
        // for each package, resolve its references
        if (defaultPackage != null)
            defaultPackage.resolveTypes(this);
        packages.resolveTypes(this);
    }


    /** Mark the current file that is being parsed */
    public void setFile(File file) {
        currentFile = file;
    }   


    /** set the lookup scope to the nearest enclosing class (for "this.x") */
    public void setNearestClassScope() {
        // find the nearest class scope
        setScope(activeScopes.findTopmostClass());
    }   


    /** Set the qualified scope for the next name lookup.  Names will only be
     *  searched for within that scope.  This version of setScope looks up
     *  the definition to set based on its name as received from a token...
     */
    void setScope(JavaToken t) {
        Definition def = lookup(t.getText());
	System.out.println("   using class (scope): " + def.getName());
	
        if (def != null) {
            def.addReference(getOccurrence(t));
            setScope(def);
        }   
    }   


    /** Set the qualified scope for the next name lookup.  Names will only be
     *  searched for within that scope
     */
    void setScope(Definition d) {
        while (d != null && (d instanceof TypedDef))
            d = ((TypedDef)d).getType();
        if (d == null || d instanceof ScopedDef)
            qualifiedScope = d;
    }


    /** Set the qualified scope for the next name lookup.  Names will only be
     *  searched for within that scope.  This version of setScope looks up
     *  the definition to set based on its name...
     */
    void setScope(String name) {
	System.out.println("   setting scope: " + name);
        Definition def = lookup(name);
        if (def != null)
            setScope(def);
    }   


    /** Return a String representation for the entire symbol table */
    public String toString() {
        return "Symbol Table";
    }   


    /** Used to push the scope of the default package.  This is used by the
     *    parser if the source file being parsed does not contain a package
     *    specification.
     */
    public void useDefaultPackage() {
        pushScope(getDefaultPackage());
    }   
}
