// include an ANTLR "header" so all generated code will be in
// package bluej.parser
header {
    package bluej.parser;
}

// tell ANTLR that we want to generate Java source code
options {
    language="Java";
}

// Import the necessary classes
{
    //import bluej.utility.Debug;
    import bluej.parser.symtab.SymbolTable;
    import bluej.parser.symtab.JavaVector;
    import bluej.parser.symtab.DummyClass;
    import bluej.parser.symtab.ClassInfo;
    import bluej.parser.symtab.Selection;

    import antlr.*;

    import java.util.*;
    import java.io.*;

    class JavaBitSet extends java.util.BitSet
    {
    }

}


//---------------------------------------------------------------------------
// Define the Parser.
//---------------------------------------------------------------------------

class ClassParser extends Parser;
options {
    k = 2;                           // two token lookahead
    exportVocab=Java;            // Call its vocabulary "Java"
    codeGenMakeSwitchThreshold = 2;  // Some optimizations
    codeGenBitsetTestThreshold = 3;
    defaultErrorHandler = false;     // Don't generate parser error handlers
}

tokens {
    BLOCK; MODIFIERS; OBJBLOCK; SLIST; CTOR_DEF; METHOD_DEF; VARIABLE_DEF;
    INSTANCE_INIT; STATIC_INIT; TYPE; CLASS_DEF; INTERFACE_DEF;
    PACKAGE_DEF; ARRAY_DECLARATOR; EXTENDS_CLAUSE; IMPLEMENTS_CLAUSE;
    PARAMETERS; PARAMETER_DEF; LABELED_STAT; TYPECAST; INDEX_OP;
    POST_INC; POST_DEC; METHOD_CALL; EXPR; ARRAY_INIT;
    IMPORT; UNARY_MINUS; UNARY_PLUS; CASE_GROUP; ELIST; FOR_INIT; FOR_CONDITION;
    FOR_ITERATOR; EMPTY_STAT; FINAL="final"; ABSTRACT="abstract";
    STRICTFP="strictfp"; SUPER_CTOR_CALL; CTOR_CALL;
}

// Define some methods and variables to use in the generated parser.
{
    // these static variables are used to tell what kind of compound
    // statement is being parsed (see the compoundStatement rule
    static final int BODY          = 1;
    static final int CLASS_INIT    = 2;
    static final int INSTANCE_INIT = 3;
    static final int NEW_SCOPE     = 4;

    // these static variables are used as indices into a BitSet which
    // shows the modifiers of a class
    static final int MOD_PRIVATE	= 0;
    static final int MOD_PUBLIC	= 1;
    static final int MOD_PROTECTED	= 2;
    static final int MOD_STATIC	= 3;
    static final int MOD_ABSTRACT	= 4;

    // We need a symbol table to track definitions
    private SymbolTable symbolTable;
    private TokenStreamHiddenTokenFilter filter;
    private ClassInfo info;

    /**
     * Counts the number of LT seen in the typeArguments production.
     * It is used in semantic predicates to ensure we have seen
     * enough closing '>' characters; which actually may have been
     * either GT, SR or BSR tokens.
     */
    private int ltCounter = 0;

    public static ClassInfo parse(String filename)
        throws Exception
    {
    	return parse(filename, null);
    }

    public static ClassInfo parse(File file)
        throws Exception
    {
    	return parse(file, null);
    }

    // the main entry point to parse a file
    public static ClassInfo parse(String filename, Vector classes)
        throws Exception
    {
    	return parse(new File(filename), classes);
    }

    public static ClassInfo parse(File file, Vector classes)
    	throws Exception
    {
    // create a new symbol table
	SymbolTable symbolTable = new SymbolTable();
        ClassInfo info = new ClassInfo();

	doFile(file, symbolTable, info); // parse it

	// resolve the types of all symbols in the symbol table
	//  -- we don't need this for BlueJ
	// symbolTable.resolveTypes();

	// add existing classes to the symbol table
	if(classes != null)
		symbolTable.addClasses(classes);

	symbolTable.getInfo(info);

	return info;
    }


    // This method decides what action to take based on the type of
    //   file we are looking at
    private static void doFile(File f, SymbolTable symbolTable, ClassInfo info)
	throws Exception
    {
        // If this is a directory, walk each file/dir in that directory
        if (f.isDirectory()) {
		throw new Exception("Attempt to parse directory");
        }

        // otherwise, if this is a java file, parse it!
        else if (f.getName().endsWith(".java")) {
            symbolTable.setFile(f);
            parseFile(new BufferedReader(new FileReader(f)), symbolTable, info);
        }
    }

    // Here's where we do the real work...
    private static void parseFile(Reader r,
                                 SymbolTable symbolTable, ClassInfo info)
	throws Exception
    {
	// Create a scanner that reads from the input stream passed to us
	JavaLexer lexer = new JavaLexer(r);

	// Tell the scanner to create tokens of class JavaToken
	lexer.setTokenObjectClass("bluej.parser.JavaToken");

	TokenStreamHiddenTokenFilter filter = new TokenStreamHiddenTokenFilter(lexer);

	// Tell the lexer to redirect all multiline comments to our
	// hidden stream
	filter.hide(ClassParser.ML_COMMENT);

	// Create a parser that reads from the scanner
	ClassParser parser = new ClassParser(filter);

	// Tell the parser to use the symbol table passed to us
	parser.setSymbolTable(symbolTable);
	parser.setClassInfo(info);
	parser.setFilter(filter);

	// start parsing at the compilationUnit rule
	parser.compilationUnit();
	
	//close the reader to allow class name changes in editor
	r.close();
    }

    // Tell the parser which symbol table to use
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void setClassInfo(ClassInfo info) {
    	this.info = info;
    }

    public void setFilter(TokenStreamHiddenTokenFilter filter) {
        this.filter = filter;
    }


    // redefined from antlr.LLkParser to supress error messages
    public void reportError(RecognitionException ex) {
        // do nothing
    }

	public JavaToken findAttachedComment(JavaToken startToken)
	{
		CommonHiddenStreamToken ctok = null;
		if (startToken != null) {
			ctok = filter.getHiddenBefore(startToken);

			if(ctok != null) {
				// if the last line of the comment is more than
				// two away from the start of the method/class
				// then we can't really say that its attached.
				// I believe it is part of the javadoc spec that
				// says that comments and their method have to
				// be right next to each other but I could be
				// wrong
				/* disabled by ajp. AS of antlr 2.7.1, getLine()
				   refers to the start line of the comment, not
				   the end line so the following logic doesn't work.
				   I don't think its important enough to bother
				   reimplementing */
				/* if (ctok.getLine() < startToken.getLine()-2)
					ctok = null; */
			}
		}

		return (JavaToken)ctok;
	}

    //------------------------------------------------------------------------
    // Symboltable adapter methods
    // The following methods are provided to give a single set of entry
    //   calls into the symbol table.  This makes it easy to add debugging
    //   code that will track all calls to symbolTable.popScope, for instance
    // These are direct pass-through calls to the symbolTable, but a
    //   few have special function.
    //------------------------------------------------------------------------

    public void popScope()                 {//System.out.println("pop");
                                            symbolTable.popScope();}
    public void endFile()                  {symbolTable.popAllScopes();}
    public void defineBlock(JavaToken tok) {//System.out.println("entering block" + tok);
                                            symbolTable.defineBlock(tok);}
    public void definePackage(JavaToken t) {symbolTable.definePackage(t);}
    public void defineLabel(JavaToken t)   {symbolTable.defineLabel(t);}
    public void useDefaultPackage()        {symbolTable.useDefaultPackage();}
    public void reference(JavaToken t)     {symbolTable.reference(t);}
    public void setNearestClassScope()     {symbolTable.setNearestClassScope();}

    public void endMethodHead(JavaVector exceptions) {
        symbolTable.endMethodHead(exceptions);
    }

    public DummyClass dummyClass(JavaToken theClass) {
        return symbolTable.getDummyClass(theClass);
    }


    public void defineClass(JavaToken theClass,
                            JavaToken superClass,
                            JavaVector interfaces,
			    boolean isAbstract,
			    boolean isPublic,
			    boolean isEnum,
			    JavaToken comment,
			    Selection extendsInsert, Selection implementsInsert,
			    Selection extendsReplace, Selection superReplace,
			    Selection typeParameterTextSelection,
			    Vector typeParameterSelections,
			    Vector interfaceSelections)
    {
        symbolTable.defineClass(theClass, superClass, interfaces, isAbstract, isPublic,
        			isEnum, comment, extendsInsert, implementsInsert,
        			extendsReplace, superReplace, typeParameterTextSelection,
        			typeParameterSelections,
        			interfaceSelections);
    }

    public void defineInterface(JavaToken theInterface,
                                JavaVector superInterfaces,
                                boolean isPublic,
                                JavaToken comment,
                                Selection extendsInsert,
                                Selection typeParameterTextSelection,
                                Vector typeParameterSelections,
                                Vector superInterfaceSelections)
    {
        symbolTable.defineInterface(theInterface, superInterfaces, isPublic, comment,
                                    extendsInsert, typeParameterTextSelection,
                                    typeParameterSelections,
                                    superInterfaceSelections);
    }

    public void defineVar(JavaToken theVariable, JavaToken type, boolean isVarargs, JavaToken comment) {
        symbolTable.defineVar(theVariable, type, isVarargs, comment);
    }


    public void defineMethod(JavaToken theMethod, JavaToken type, JavaToken comment, Selection typeParameterText) {
        //System.out.println("entering method" + theMethod);
        symbolTable.defineMethod(theMethod, type, comment, typeParameterText);
    }

    public void addImport(JavaToken id, String className, String packageName) {
        symbolTable.addImport(id, className, packageName);
    }

    // create a selection which consists of the location just after the token passed
    // in
    public Selection selectionAfterToken(JavaToken id)
    {
	    return new Selection(id.getFile(), id.getLine(),
                              id.getColumn() + id.getText().length());
    }
    
    // create a selection which consists of the location just after the token passed
    // in
    public Selection nextSelection(Selection sel)
    {
	    return new Selection(sel.getFile(), sel.getLine(),
                              sel.getColumn() + sel.getText().length());
    }
}



// Compilation Unit: In Java, this is a single file.  This is the start
//   rule for this parser
compilationUnit
    :   // A compilation unit starts with an optional package definition
        (   (packageDefinition)=> packageDefinition
            // need above syntactic predicate to dis-amb the 'annotation' leading both
            // packageDefinition and typeDefinition
        |   /* nothing */     {useDefaultPackage();}
        )

        // Next we have a series of zero or more import statements
        ( importDefinition )*

        // Wrapping things up with any number of class or interface
        //    definitions
        ( typeDefinition )*

        // When we reach end-of-file, tell the symboltable
        EOF!
        {endFile();} // if a package were defined, pop its scope
    ;


// Package statement: "package" followed by an identifier.
packageDefinition
    options { defaultErrorHandler = true; } // let ANTLR handle errors

    { JavaToken id; }           // define an id for the package name
    :  (annotation)*  pkg:"package" id=identifier sem:SEMI
        {
            info.setPackageSelections(new Selection((JavaToken)pkg),
                                        new Selection(id), id.getText(),
                                        new Selection((JavaToken)sem));

            definePackage(id);  // tell the symbol table about the package
        }
    ;


// Import statement: import followed by a package or class name
//   or a "static" method import
//   Note that we do not perform any action here.  The action associated
//   with the import statement is performed in identifierStar.  (That
//   rule is only called from importDefinition, so it is safe to have it
//   assume it's an import.)
importDefinition
    options {defaultErrorHandler = true;}
    :   "import" ("static")? identifierStar SEMI
    ;

// A type definition in a file is either a class or interface definition.
// Note that modifiers are essentially thrown out.  If you want to handle
//   them, collect them in a Vector (returned from the modifiers rule)
//   and pass them _down_ to classDefinition and interfaceDefinition
//   to be associated with the class or interface definition
typeDefinition
    options {defaultErrorHandler = true;}
    {JavaBitSet mods;
     JavaToken commentToken = null; }
    : { commentToken = findAttachedComment((JavaToken)LT(1)); }
       mods=modifiers
        ( classDefinition[mods, commentToken]
          | interfaceDefinition[mods, commentToken]
          | enumDefinition[mods, commentToken]
          | annotationTypeDeclaration
        )
    |   SEMI
    ;

// A declaration is the creation of a reference or primitive-type variable
// Because we'll be doing the actual definition action inside
//   variableDefinitions, we need to _pass_ the type down to it.  This is
//   one of the big advantages to an LL tool over an LALR tool -- because
//   we build top-down, we can pass information _into_ a rule and return
//   information from a rule.  An LALR parser can only collect information
//   bottom-up
declaration
    {JavaToken type;}
    :   modifiers type=typeSpec variableDefinitions[type, null]
    ;

// A type specification is a type name with possible brackets afterwards
//   (which would make it an array type).
typeSpec returns [JavaToken t]
    {t=null;}
    :   t=classTypeSpec
    |   t=builtInTypeSpec
    ;

arraySpecOpt:
        (options{greedy=true;}: // match as many as possible
            lb:LBRACK^  RBRACK!
        )*
    ;


// A class type specification is a class type with possible brackets afterwards
//   (which would make it an array type).
// - generic type arguments after
classTypeSpec returns [JavaToken t]
        {t=null;}
        : t=classOrInterfaceType[true, null] (LBRACK RBRACK
	{
             if(t != null)
                   t.setText(t.getText() + "[]");
	} )*
	;

// Represents a class or interface type
// Type arguments may or not be desired depending on the usage.
// Token returned is a formatted text representation of the type
// boolean flag adds type args, List captures all actual tokens as selections
// which can be used if need to do any calculating on actual tokens
classOrInterfaceType[boolean includeTypeArgs, Vector typeArgTokens] returns [JavaToken t]
     {
     	t=null;
        JavaToken typeArg = null;
        
     }
	:   id1:IDENT 
	        {   
	        	t=(JavaToken)id1;
	        }
	    (typeArg = typeArguments[typeArgTokens]
	        {
	        	
	        	if(includeTypeArgs)
	    	    t.setText(t.getText() + typeArg.getText());	
	        }
	    
	    )? 
        (options{greedy=true;}: // match as many as possible
            DOT
            id2:IDENT {t.setText(t.getText() + "." + id2.getText());}
            (typeArg = typeArguments[typeArgTokens]
	            {
	            	if(includeTypeArgs)
	    	        t.setText(t.getText() + typeArg.getText());
	    	    }
	        )?  
        )*
    ;


// typeArgTokens may be null if not required by caller
typeArguments[Vector typeArgTokens] returns [JavaToken t]
    {   t = null;
    	JavaToken st = null;
    	JavaToken st1 = null;
    	JavaToken te = null;
    	int currentLtLevel = 0;
    }
    :
        {currentLtLevel = ltCounter;}
        lt:LT 
            {
            	ltCounter++;
            	t = (JavaToken)lt;
            	if(typeArgTokens != null)
        	       typeArgTokens.add(t);
            }
        st=singleTypeArgument[typeArgTokens]
        {
        	t.setText(t.getText() + st.getText());
        }
        (options{greedy=true;}: // match as many as possible
            co:COMMA st1=singleTypeArgument[typeArgTokens]
            {
                t.setText(t.getText() + ((JavaToken)co).getText() + ((JavaToken)st1).getText());
            }
        )*
        
        (   // turn warning off since Antlr generates the right code,
            // plus we have our semantic predicate below
            options{generateAmbigWarnings=false;}:
            te=typeArgumentsEnd
            {
            	if(typeArgTokens != null)
            	    typeArgTokens.add(te);
                t.setText(t.getText() + ((JavaToken)te).getText());
            }
        )?
        
        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?
    ;

singleTypeArgument[Vector typeArgTokens] returns [JavaToken t]
    {t=null;
     JavaToken t1 = null;
     JavaToken t2 = null;
     JavaToken t3 = null;
     JavaToken t4 = null;
     
    }
    :
        (
            t3=classTypeSpec {t = t3; if(typeArgTokens != null) typeArgTokens.add(t);}
            | t4=builtInTypeSpec {t = t4; if(typeArgTokens != null) typeArgTokens.add(t);}
            | qu:QUESTION {if(qu != null) t = (JavaToken)qu; if(typeArgTokens != null) typeArgTokens.add(t);}          
        )
           
        
        (   // I'm pretty sure Antlr generates the right thing here:
            options{generateAmbigWarnings=false;}:
            (id1:"extends"|id2:"super")
                {
                    if(id1 != null){ 
                        t.setText(t.getText() + " " + ((JavaToken)id1).getText());
                        if(typeArgTokens != null) typeArgTokens.add(id1);
                    }
                    else if(id2 != null) 
                        t.setText(t.getText() + " " + ((JavaToken)id2).getText());
                        if(typeArgTokens != null) typeArgTokens.add(id2);
                }
             
            
            ( t1=classTypeSpec {t.setText(t.getText() + " " + t1.getText()); if(typeArgTokens != null) typeArgTokens.add(t1);}
              | t2=builtInTypeSpec {t.setText(t.getText() + " " + t2.getText()); if(typeArgTokens != null) typeArgTokens.add(t2);}
              | qu1:QUESTION {t.setText(t.getText() + ((JavaToken)qu1).getText()); if(typeArgTokens != null) typeArgTokens.add(t3);}
            )
        )?
    ;

// this gobbles up *some* amount of '>' characters, and counts how many
// it gobbled.
protected typeArgumentsEnd returns [JavaToken t]
    { t = null; }
    :
        id1:GT { ltCounter-=1; t = (JavaToken)id1;}
        | id2:SR {ltCounter-=2; t = (JavaToken)id2;}
        | id3:BSR {ltCounter-=3; t = (JavaToken)id3;}
    ;

// A builtin type specification is a builtin type with possible brackets
// afterwards (which would make it an array type).
builtInTypeSpec returns [JavaToken t]
        {t=null;}
	: t=builtInType (LBRACK RBRACK
		{
		   if(t != null)
                   t.setText(t.getText() + "[]");
		} )*
	;

// A type name. which is either a (possibly qualified) class name or
//   a primitive (builtin) type
type returns [JavaToken t]
    {t=null;}
    :   t=classOrInterfaceType[false, null]
    |   t=builtInType
    ;


// The primitive types.
builtInType returns [JavaToken t]
    {t=null;}
    :   bVoid:"void"       {t = (JavaToken)bVoid;}
    |   bBoolean:"boolean" {t = (JavaToken)bBoolean;}
    |   bByte:"byte"       {t = (JavaToken)bByte;}
    |   bChar:"char"       {t = (JavaToken)bChar;}
    |   bShort:"short"     {t = (JavaToken)bShort;}
    |   bInt:"int"         {t = (JavaToken)bInt;}
    |   bFloat:"float"     {t = (JavaToken)bFloat;}
    |   bLong:"long"       {t = (JavaToken)bLong;}
    |   bDouble:"double"   {t = (JavaToken)bDouble;}
    ;

// A (possibly-qualified) java identifier.  We start with the first IDENT
//   and expand its name by adding dots and following IDENTS
identifier returns [JavaToken t]
    {t=null;}
    :   id1:IDENT          {t=(JavaToken)id1;}
        (   DOT
            id2:IDENT      {t.setText(t.getText() + "." + id2.getText());}
        )*
    ;

// This is the special identifer rule used by package statements.  We will
//   keep track of the name in two parts: packageName and className
// There are three cases to consider:
//   1) a single IDENT:  This will be assigned to className; packageName
//      will be an empty string.  This means that we are importing a class
//      from the default package
//   2) more than one IDENT: for each succesive (DOT IDENT) pair we find, we
//      append the _last_ className to the package name.  The idea is that
//      the _last_ qualifier in a name must be a class name (or "*" -- handled
//      in #3).  Each time we see a new qualifier, we know all the previous
//      qualifiers are actually part of the packageName.  So we'll end up
//      with a package and class name
//   3) one or more IDENTs with DOT STAR at the end: We follow the above two
//      rules until we hit the DOT STAR at the end. Once we do, we know this is
//      an "import-on-demand" for a certain package name.  The entire name
//      is put into packageName, and className is set to null.
// We then take the class and package name and tell the symbol table about
//   the import statement
identifierStar
    {String className=""; String packageName="";}
    :   id:IDENT        {className=id.getText();}
        ( DOT id2:IDENT
            {packageName += "."+className; className = id2.getText();} )*
        ( DOT STAR      {packageName += "."+className; className = null;} )?

        {
            // put the overall name in the token's text
            if (packageName.equals(""))
                id.setText(className);
            else if (className == null)
                id.setText(packageName.substring(1));
            else
                id.setText(packageName.substring(1) + "." + className);

            // tell the symbol table about the import
            addImport((JavaToken)id, className, packageName);
        }
    ;

// A list of zero or more modifiers.  We could have used (modifier)* in
//   place of a call to modifiers, but I thought it was a good idea to keep
//   this rule separate so they can easily be collected in a Vector if
//   someone so desires
modifiers returns [JavaBitSet mods]
    { mods = new JavaBitSet(); }
       :( options{warnWhenFollowAmbig = false;}
       : modifier[mods] | annotation)*
    ;

// modifiers for Java classes, interfaces, class/instance vars and methods
modifier[JavaBitSet mods]
    :   "private"
	{ mods.set(MOD_PRIVATE); }
    |   "public"
	{ mods.set(MOD_PUBLIC); }
    |   "protected"
	{ mods.set(MOD_PROTECTED); }
    |   "static"
	{ mods.set(MOD_STATIC); }
    |   "transient"
    |   "final"
    |   "abstract"
	{ mods.set(MOD_ABSTRACT); }
    |   "native"
    |   "threadsafe"
    |   "synchronized"
//  |   "const"                    // reserved word, but not valid
    |   "volatile"
    |	"strictfp"
    ;

// Definition of a Java class
classDefinition[JavaBitSet mods, JavaToken commentToken]
    {
    	JavaToken superClass=null;
    	JavaVector interfaces = new JavaVector();
        Vector interfaceSelections = new Vector();
        Vector typeParameterSelections = new Vector();
        Selection extendsInsert=null, implementsInsert=null,
                    extendsReplace=null, superReplace=null,
                    typeParameterTextSelection=null;
        // JavaTokens for type args
        Vector typeArguments = new Vector();
        
                    
    }
    : "class" id:IDENT    // aha! a class!
    (typeParameterTextSelection = typeParameters[typeParameterSelections]
    )?
        {
            // the place which we would want to insert an "extends" is at the
            // character just after the classname identifier
            // it is also potentially the place where we would insert a
            // "implements" so we will set that here and allow it to be overridden
            // later on if need be
            
            // Need to also allow for type parameters with java 1.5 generic types
            Selection lastTypeParamSelection = null;
            if(!typeParameterSelections.isEmpty()) {
            	lastTypeParamSelection = (Selection)typeParameterSelections.lastElement();
            	extendsInsert = implementsInsert = nextSelection(lastTypeParamSelection);
            }
            else
                extendsInsert = implementsInsert = selectionAfterToken((JavaToken)id);
        }

    // it might have a superclass...
    (
     ex:"extends" superClass=classOrInterfaceType[false, typeArguments]
        {
            extendsReplace = new Selection((JavaToken)ex);
            
            superReplace = new Selection(superClass);
            
             // may be more than just the superclasses name if there is a generic
             // type argument involved
            if(!typeArguments.isEmpty()){
                Iterator it = typeArguments.iterator();
                while(it.hasNext()){
                	superReplace.addToken((JavaToken)it.next());
                }	
            }

            // maybe we need to place "implements" lines after this superClass..
            // set it here. Factor in type arguments as well...
            if(!typeArguments.isEmpty()) {
            	JavaToken lastArg = (JavaToken)typeArguments.lastElement();
            	implementsInsert = selectionAfterToken((JavaToken)lastArg);
            }
            else
                implementsInsert = selectionAfterToken((JavaToken)superClass);
        }
    )?

    // it might implement some interfaces...
    (
     implementsInsert=implementsClause[interfaces, interfaceSelections]
    )?

        // tell the symbol table about it
        // Note that defineClass pushes the class' scope,
        // so we'll have to pop...
        { 
        	defineClass( (JavaToken)id, superClass,
            		  interfaces,
            		  mods.get(MOD_ABSTRACT), mods.get(MOD_PUBLIC),
            		  false, //not an enum
            		  commentToken,
            		  extendsInsert, implementsInsert,
            		  extendsReplace, superReplace,
            		  typeParameterTextSelection,
            		  typeParameterSelections,
            		  interfaceSelections); }

    // now parse the body of the class
    classBlock

        // tell the symbol table that we are exiting a scope
        { popScope(); }
    ;


// Definition of a Java Interface
interfaceDefinition[JavaBitSet mods, JavaToken commentToken]
    {
        JavaVector superInterfaces = new JavaVector();
        Vector superInterfaceSelections = new Vector();
        Vector typeParameterSelections = new Vector();
        Selection extendsInsert = null;
        //Selection typeParamsInsert = null;
        Selection typeParameterTextSelection = null;
        //JavaToken typeParameterText = null;
    }
    : "interface" id:IDENT   // aha! an interface!
        // it _might_ have type paramaters
        (typeParameterTextSelection = typeParameters[typeParameterSelections])?
        {
	        // the place which we would want to insert an "extends" is at the
	        // character just after the interfacename identifier
	        Selection lastTypeParamSelection = null;
            if(!typeParameterSelections.isEmpty()) {
            	lastTypeParamSelection = (Selection)typeParameterSelections.lastElement();
            	extendsInsert = nextSelection(lastTypeParamSelection);
            }
            else
               extendsInsert = selectionAfterToken((JavaToken)id);
        }

    // it might extend some other interfaces
    (
     extendsInsert=interfaceExtends[superInterfaces, superInterfaceSelections]
    )?

        // tell the symbol table about it!
        // Note that defineInterface pushes the interface scope, so
        //   we'll have to pop it...
        { 
        	defineInterface((JavaToken)id,
		            superInterfaces,
		            mods.get(MOD_PUBLIC), commentToken,
		            extendsInsert,
		            typeParameterTextSelection,
		            typeParameterSelections,
            		superInterfaceSelections); }

    // now parse the body of the interface (looks like a class...)
    classBlock

        // tell the symboltable that we are done in that scope
        { popScope(); }
    ;

    // Definition of a Java enum, based on classDefinition
enumDefinition[JavaBitSet mods, JavaToken commentToken]
    {
    	JavaToken superClass=null;
        JavaVector interfaces = new JavaVector();
        Vector interfaceSelections = new Vector();
        Selection implementsInsert=null;
    }
    : "enum" id:IDENT    // aha! an enum!
        {
            // the place which we would want to insert an "implements" is at the
            // character just after the classname identifier
            implementsInsert = selectionAfterToken((JavaToken)id);
        }

    // it might implement some interfaces...
    (
     implementsInsert=implementsClause[interfaces, interfaceSelections]
    )?
        // should there be a separate defineEnum??? BQ
        // tell the symbol table about it
        // Note that defineClass pushes the class' scope,
        // so we'll have to pop...
        { defineClass( (JavaToken)id, superClass,
            		  interfaces,
            		  mods.get(MOD_ABSTRACT), mods.get(MOD_PUBLIC),
            		  true, // yes, it is an enum
            		  commentToken,
            		  null, implementsInsert,
            		  null, null,
            		  null, null,
            		  interfaceSelections); }

    // now parse the body of the class
    enumBlock

        // tell the symbol table that we are exiting a scope
        { popScope(); }
    ;
    
annotationTypeDeclaration
    :
        AT "interface" IDENT annotationTypeBody
    ;

typeParameters[Vector typeParameterSelections] returns [Selection typeParamInsert]
    {    int currentLtLevel = 0;
         typeParamInsert = null;
         JavaToken typeParameterText = null;
         JavaToken typeParam = null;
         JavaToken paramEnd = null;
         // the full token for the type parameter selection
         //JavaToken typeParameters = null;     
    }
    :
        {currentLtLevel = ltCounter;}
        id:LT 
            {ltCounter++;
                typeParameterText = (JavaToken)id;
                //build up selection to be returned at the end
                typeParamInsert = new Selection(typeParameterText);
                // add this as first selection
                typeParameterSelections.add(typeParamInsert);
                                
            }
        typeParam = typeParameter[typeParameterSelections]
            {
            	Selection s = new Selection(typeParam);
            	typeParameterSelections.add(s);
            	typeParameterText.setText(typeParameterText.getText() + typeParam.getText());
            	typeParamInsert = new Selection(typeParameterText);     	    
            } 
        (co:COMMA typeParam = typeParameter[typeParameterSelections]
            {
            	Selection s = new Selection((JavaToken)co);
            	typeParameterSelections.add(s);
            	s = new Selection((JavaToken)typeParam);
            	typeParameterSelections.add(s);
            	typeParameterText.setText(typeParameterText.getText() + ((JavaToken)co).getText() + typeParam.getText());
            	typeParamInsert = new Selection(typeParameterText);     	    
            }         
        )*
        (paramEnd = typeArgumentsEnd
            {
            	if(paramEnd != null) {
            		Selection s = new Selection((JavaToken)paramEnd);
            		typeParameterSelections.add(s);
            		typeParameterText.setText(typeParameterText.getText() + paramEnd.getText());
            		typeParamInsert = new Selection(typeParameterText);
            	}
            }
        )?
        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?
    ;

typeParameter[Vector typeParameterSelections] returns [JavaToken paramInsert]
	{   paramInsert = null;
		JavaToken id = null;
		JavaToken xtend = null;
	}
	:
        (id2:IDENT|id3:QUESTION)
        {
        	if(id2 != null)
        	    paramInsert = (JavaToken)id2;
        	else if(id3 != null)
        	    paramInsert = (JavaToken)id3;
        	typeParameterSelections.add(new Selection(paramInsert));    
        }
        (   // I'm pretty sure Antlr generates the right thing here:
            options{generateAmbigWarnings=false;}:
            ex:"extends" id=classOrInterfaceType[false, null] 
            { 
            	typeParameterSelections.add(new Selection((JavaToken)ex));
            	typeParameterSelections.add(new Selection((JavaToken)id));
            	paramInsert.setText(paramInsert.getText() + " " + ex.getText());
            	paramInsert.setText(paramInsert.getText() + " " + id.getText());
            	Selection s = new Selection((JavaToken)paramInsert);
            }
            (band:BAND id=classOrInterfaceType[false, null] 
            { 
            	typeParameterSelections.add(new Selection((JavaToken)band));
            	typeParameterSelections.add(new Selection((JavaToken)id));
            	paramInsert.setText(paramInsert.getText() + " " + band.getText());
            	paramInsert.setText(paramInsert.getText() + " " + id.getText());
            	Selection s = new Selection((JavaToken)paramInsert);
            })*
        )?
    ;




// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
classBlock
    :   LCURLY
            ( field | SEMI )*
        RCURLY
    ;

enumBlock
	:	LCURLY!
        enumConstant
        (
            // CONFLICT: does a COMMA after an enumConstant start a new
            //           constant or start the optional ',' at end?
            //           ANTLR generates proper code by matching
            //			 the comma as soon as possible.
            options {warnWhenFollowAmbig = false;}:
            COMMA enumConstant
        )*
        (COMMA!)?
        
        (
            SEMI
            (  field | SEMI! )*
        )?
		RCURLY!
	;

enumConstant
    :
        (annotation)*
        IDENT ( LPAREN argList RPAREN )? (classBlock)?
        
    ;

annotationTypeBody
    :
        LCURLY
        (annotationTypeMemberDeclaration)*
        RCURLY
    ;
annotationTypeMemberDeclaration
    :
        m:modifiers
        (
            typeSpec IDENT LPAREN RPAREN (annDefaultValue)?  SEMI
        |   typeDefinition
        )
        
    ;

protected
annDefaultValue:
        "default" annMemberValue
    ;

// An interface can extend several other interfaces, so we collect a vector
//   of all the superinterfaces and return it
// We also collect a vector of all the positions of the tokens so we can
//   add and remove them from the source
// We also collect a vector of all the text of the tokens so that we can find
// a particular name for deletion
interfaceExtends[JavaVector interfaces, Vector interfaceSelections] returns [Selection extendsInsert]
    { JavaToken id;
      Vector typeArgs = new Vector();
      extendsInsert = null;
    }
    : ex:"extends" id=classOrInterfaceType[false, typeArgs]
       {
       	  if(!typeArgs.isEmpty()) {
          	  JavaToken lastArg = (JavaToken)typeArgs.lastElement();
          	  extendsInsert = selectionAfterToken(lastArg);
          }
          else	
              extendsInsert = selectionAfterToken((JavaToken)id);

          interfaceSelections.addElement(new Selection((JavaToken)ex));
    	  interfaces.addElement(dummyClass(id));
    	  //deal with type args if they exist
    	  Selection s = new Selection((JavaToken)id);
          if(!typeArgs.isEmpty()) {
          	  Iterator it = typeArgs.iterator();
              while(it.hasNext()){
                  s.addToken((JavaToken)it.next());
              }	
          }
    	  interfaceSelections.addElement(s);
    	  //clear typeArgs in case it is re-used in optional section below
    	  typeArgs.removeAllElements();
       }
        ( co:COMMA id=classOrInterfaceType[false, typeArgs]
        {
          extendsInsert = selectionAfterToken((JavaToken)id);

          interfaceSelections.addElement(new Selection((JavaToken)co));
          interfaces.addElement(dummyClass(id));
          Selection s = new Selection((JavaToken)id);
          if(!typeArgs.isEmpty()) {
          	  Iterator it = typeArgs.iterator();
              while(it.hasNext()){
                  s.addToken((JavaToken)it.next());
              }
          }
    	  interfaceSelections.addElement(s);
    	  typeArgs.removeAllElements();
          interfaceSelections.addElement(new Selection((JavaToken)id));
        }
        )*
    ;


// A class can implement several interfaces, so we collect a vector of
//   all the implemented interfaces and return it
// We also collect a vector of all the positions of the tokens so we can
//   add and remove them from the source
// We return the position where should insert a new implements clause
//  in the source
implementsClause[JavaVector interfaces, Vector interfaceSelections] returns [Selection implementsInsert]
    { JavaToken id;
      Vector typeArgs = new Vector();
      implementsInsert = null;
    }
    : im:"implements" id=classOrInterfaceType[false, typeArgs]
        {
          if(!typeArgs.isEmpty()) {
          	  JavaToken lastArg = (JavaToken)typeArgs.lastElement();
          	  implementsInsert = selectionAfterToken(lastArg);
          }	
          else
              implementsInsert = selectionAfterToken((JavaToken)id);

    	  interfaceSelections.addElement(new Selection((JavaToken)im));
          interfaces.addElement(dummyClass(id));
          Selection s = new Selection((JavaToken)id);
          if(!typeArgs.isEmpty()) {
          	  Iterator it = typeArgs.iterator();
              while(it.hasNext()){
                  s.addToken((JavaToken)it.next());
              }	
          }
    	  interfaceSelections.addElement(s);
    	  //clear typeArgs in case it is re-used in optional section below
    	  typeArgs.removeAllElements();
        }
    ( co:COMMA id=classOrInterfaceType[false, typeArgs]
        {
           if(!typeArgs.isEmpty()) {
          	  JavaToken lastArg = (JavaToken)typeArgs.lastElement();
          	  implementsInsert = selectionAfterToken(lastArg);
          }	
          else
              implementsInsert = selectionAfterToken((JavaToken)id);

          interfaceSelections.addElement(new Selection((JavaToken)co));
          interfaces.addElement(dummyClass(id));
          Selection s = new Selection((JavaToken)id);
          if(!typeArgs.isEmpty()) {
          	  Iterator it = typeArgs.iterator();
              while(it.hasNext()){
                  s.addToken((JavaToken)it.next());
              }
          }
    	  interfaceSelections.addElement(s);
    	  typeArgs.removeAllElements();
        }
    )*

    ;

// Now the various things that can be defined inside a class or interface...
// Note that not all of these are really valid in an interface (constructors,
//   for example), and if this grammar were used for a compiler there would
//   need to be some semantic checks to make sure we're doing the right thing...
field
    {
        JavaToken  type, commentToken = null;
        JavaVector exceptions = null;           // track thrown exceptions
        JavaBitSet mods = null;
        //Selection typeParameterSelection = null;
        Vector typeParameterSelections = new Vector();
        Selection typeParameterTextSelection = null;
        //JavaToken typeParameterText = null;
    }
    :   // method, constructor, or variable declaration
	{ commentToken = findAttachedComment((JavaToken)LT(1)); }

        mods=modifiers

        (
            ctorHead[null, commentToken] constructorBody    // constructor
        |
            classDefinition[new JavaBitSet(), null]         // inner class
        |
            enumDefinition[new JavaBitSet(), null]     // inner enum
        |
            interfaceDefinition[new JavaBitSet(), null]     // inner interface
        |
            // A generic method has the typeParameters before the return type.
            // This is not allowed for variable definitions, but this production
            // allows it, a semantic check could be used if you wanted.
            (typeParameterTextSelection = typeParameters[typeParameterSelections])?
            type=typeSpec  // method or variable declaration(s)
            (
                method:IDENT  // the name of the method
                {
                    // tell the symbol table about it.  Note that this signals that
        	        // we are in a method header so we handle parameters appropriately
        	        
        	        defineMethod((JavaToken)method, type, commentToken, typeParameterTextSelection);
                }

                // parse the formal parameter declarations.
                LPAREN parameterDeclarationList RPAREN

                (LBRACK RBRACK )*

                // get the list of exceptions that this method is
                // declared to throw
                (exceptions=throwsClause)?

                // tell the symbol table we are done with the method header. Note that
                // this will tell the symbol table to handle variables normally
                {endMethodHead(exceptions);}

		    ( compoundStatement[BODY] | SEMI {popScope();} )
		|
                    variableDefinitions[type,commentToken] SEMI
            )
        )


    // "static { ... }" class initializer
    |   "static" compoundStatement[CLASS_INIT]

    // "{ ... }" instance initializer
    |   compoundStatement[INSTANCE_INIT]
    ;

constructorBody
    :   LCURLY
        // Predicate might be slow but only checked once per constructor def
        // not for general methods.
        ( (explicitConstructorInvocation) => explicitConstructorInvocation
        |
        )
        (statement)*
        // tell the symbol table we're leaving a scope
        {popScope();}
        RCURLY
    ;

explicitConstructorInvocation
    :   (	options {
				// this/super can begin a primaryExpression too; with finite
				// lookahead ANTLR will think the 3rd alternative conflicts
				// with 1, 2.  I am shutting off warning since ANTLR resolves
				// the nondeterminism by correctly matching alts 1 or 2 when
				// it sees this( or super(
				generateAmbigWarnings=false;
			}
		:	"this" LPAREN argList RPAREN SEMI

	    |   "super" LPAREN argList RPAREN SEMI

			// (new Outer()).super()  (create enclosing instance)
		|	primaryExpression DOT "super" LPAREN argList RPAREN SEMI
		)
    ;

variableDefinitions[JavaToken type, JavaToken commentToken]
    :   variableDeclarator[type, commentToken]
        (COMMA variableDeclarator[type, commentToken] )*
   
    ;

// Declaration of a variable.  This can be a class/instance variable,
//   or a local variable in a method
// It can also include possible initialization.  Note again that the
//   array brackets are ignored...
variableDeclarator[JavaToken type, JavaToken commentToken]
    :   id:IDENT (LBRACK RBRACK  { if(type != null)
    					type.setText(type.getText() + "[]");
    				   } )* varInitializer
        {defineVar((JavaToken)id, type, false, commentToken);}
    ;

varInitializer
	:	( ASSIGN initializer )?
	;

// This is an initializer used to set up an array.
arrayInitializer
    :   LCURLY
            (   initializer
                (
		    // CONFLICT: does a COMMA after an initializer start a new
		    //           initializer or start the option ',' at end?
		    //           ANTLR generates proper code by matching
		    //			 the comma as soon as possible.
		    options {
			warnWhenFollowAmbig = false;
			}
		   :
			COMMA! initializer
		)*
                (COMMA)?
            )?
        RCURLY
    ;


// The two "things" that can initialize an array element are an expression
//   and another (nested) array initializer.
initializer
    :   expression
    |   arrayInitializer
    ;

// This is the header of a constructor.  It includes the name and parameters
//   for the method.

//  Note that the type is passed into this method.  This
//   was necessary to resolve a conflict that several types of fields in a
//   class started with a type and/or modifier, so they had to be left-factored
//   This also watches for a list of exception classes in a "throws" clause.
ctorHead[JavaToken type, JavaToken commentToken]
    {
        JavaVector exceptions=null;
    }
    :   method:IDENT  // the name of the method

        {
		// tell the symbol table about it.  Note that this signals that
        	// we are in a method header so we handle parameters appropriately
        	defineMethod((JavaToken)method, type, commentToken, null);
        }

        // parse the formal parameter declarations.  These are sent to the
        // symbol table as _variables_.  Because the symbol table knows we
        // are in a method header, it collects these definitions as parameters
        // for the method.
        LPAREN parameterDeclarationList RPAREN

        // get the list of exceptions that this method is declared to throw
        (exceptions=throwsClause)?

        // tell the symbol table we are done with the method header. Note that
        // this will tell the symbol table to handle variables normally
        {
            endMethodHead(exceptions);
        }
    ;

// This is a list of exception classes that the method is declared to throw
throwsClause returns [JavaVector exceptions]
    {JavaToken id; exceptions = new JavaVector();}
    :   "throws" id=identifier   {exceptions.addElement(dummyClass(id));}
        (COMMA id=identifier     {exceptions.addElement(dummyClass(id));} )*
    ;


// A list of formal parameters
parameterDeclarationList
    :   ( parameterDeclaration ( COMMA! parameterDeclaration )* )?
    ;

// A formal parameter.  We pass this to the symbol table as a variable, and
//   the symbol table adds it to the parameter list of the current method
//   header.
parameterDeclaration
    {JavaToken type;
     boolean isVarargs = false; }
    :   parameterModifier type=typeSpec (ELLIPSES {isVarargs=true;})? id:IDENT
                      (LBRACK RBRACK
         		{ if(type != null)
			       type.setText(type.getText() + "[]"); } )*
        {defineVar((JavaToken)id, type, isVarargs, null);}
    ;

parameterModifier
	:    (annotation)*
             (f:"final" (annotation)* )?
	;

// Compound statement.  This is used in many contexts:
//   Inside a class definition prefixed with "static":
//      it is a class initializer, and is passed here with scopeType CLASS_INIT
//   Inside a class definition without "static":
//      it is an instance initializer, passed here with scopeType INSTANCE_INIT
//   As the body of a method, passed here with scopeType BODY
//   As a completely indepdent braced block of code inside a method
//      it starts a new scope for variable definitions, and is passed here
//      with NEW_SCOPE as its scopeType

compoundStatement[int scopeType]
    :   lc:LCURLY
            {   // based on the scopeType we are processing
                switch(scopeType) {
                    // if it's a new block, tell the symbol table
                    case NEW_SCOPE:
                        defineBlock((JavaToken)lc);
                        break;

                    // if it's a class initializer or instance initializer,
                    //   treat it like a method with a special name
                    case CLASS_INIT:
                        lc.setText("~class-init~");
                        defineMethod(null, (JavaToken)lc, null, null);
                        endMethodHead(null);
                        break;
                    case INSTANCE_INIT:
                        lc.setText("~instance-init~");
                        defineMethod(null, (JavaToken)lc, null, null);
                        endMethodHead(null);
                        break;

                    // otherwise, it's a body, so do nothing special
                }
            }

            // include the (poosibly-empty) list of statements
            (statement)*

            // tell the symbol table we're leaving a scope
            {popScope();}
        RCURLY
    ;


statement
    {int count = -1; // used for parameter counts in method calls
     JavaBitSet mods; }

    // A list of statements in curly braces -- start a new scope!
    :   compoundStatement[NEW_SCOPE]

	// declarations are ambiguous with "ID DOT" relative to expression
	// statements.  Must backtrack to be sure.  Could use a semantic
	// predicate to test symbol table to see what the type was coming
	// up, but that's pretty hard without a symbol table ;)
	|	(declaration)=> declaration SEMI

    // An expression statement.  This could be a method call,
	// assignment statement, or any other expression evaluated for
	// side-effects.
	|	expression SEMI

	// class definition
	// use syntactic predicate as it otherwise seems to produce non-determinism
	// warnings now that enum has been added
	|	mods=modifiers (mods=modifiers classDefinition[mods, null]) => classDefinition[mods, null]
	
	// enum definition
    // this was legal with Java 1.5 beta1 but not beta2
    // need to wait for updated JLS enum spec 
	//|	mods=modifiers enumDefinition[mods, null]

    // Attach a label to the front of a statement
    |   id:IDENT COLON statement  {defineLabel((JavaToken)id);}

    // If-else statement
    |   "if" LPAREN expression RPAREN statement
        (
			// CONFLICT: the old "dangling-else" problem...
			//           ANTLR generates proper code matching
			//			 as soon as possible.  Hush warning.
			options {
				warnWhenFollowAmbig = false;
			}
        :
                 "else" statement
        )?

    // For statement
    |   "for"
            LPAREN
(
                ( parameterDeclaration COLON ) => enhancedForClause
                |   forClause
                )
            
            RPAREN
            statement                     // statement to loop over

    // While statement
    |   "while" LPAREN expression RPAREN statement

    // do-while statement
    |   "do" statement "while" LPAREN expression RPAREN SEMI

    // get out of a loop (or switch)
    // tell the symbol table that we are (possibly) referencing a label
    |   "break" (bid:IDENT {reference((JavaToken)bid);})? SEMI

    // do next iteration of a loop
    // tell the symbol table that we are (possibly) referencing a label
    |   "continue" (cid:IDENT {reference((JavaToken)cid);})? SEMI

    // Return an expression
    |   "return" (expression)? SEMI

    // switch/case statement
	|	"switch"^ LPAREN! expression RPAREN! LCURLY!
			( casesGroup )*
		RCURLY!

    // exception try-catch block
    |   tryBlock

    // throw an exception
    |   "throw" expression SEMI

    // synchronize a statement
    |   "synchronized" LPAREN expression RPAREN statement

    // assertion statement
    |  "assert" expression (COLON expression)? SEMI

    // empty statement
    |   SEMI
    ;


casesGroup
	:	(	// CONFLICT: to which case group do the statements bind?
			//           ANTLR generates proper code: it groups the
			//           many "case"/"default" labels together then
			//           follows them with the statements
			options {
				warnWhenFollowAmbig = false;
			}
			:
			aCase
		)+
		caseSList
	;

aCase
	:	("case" expression | "default") COLON
	;

caseSList
	:	(statement)*
	;

protected forClause
    :
        (forInit)?        SEMI    // initializer
        (expression)?     SEMI    // condition test
        (expressionList)?   // updater
    ;
protected enhancedForClause
    :
        parameterDeclaration
        COLON
        expression
    ;

// The initializer for a for loop
forInit
    {int count = -1;}
    // if it looks like a declaration, it is
    :   (declaration)=> declaration
    // otherwise it could be an expression list...
    |   count=expressionList
    ;


// an exception handler try/catch block
tryBlock
    :   "try" compoundStatement[NEW_SCOPE]
        (handler)*
        ( "finally" compoundStatement[NEW_SCOPE] )?
    ;


// an exception handler
// note that this does not properly handle its parameters!  We should make
// the "catch" be a method with a special name (~catch1~, ~catch2~...)
// The way this is now, the parameters will be added as local variables to
//   the method containing them...
handler
    :   "catch" LPAREN parameterDeclaration RPAREN compoundStatement[NEW_SCOPE]
    ;


// expressions
// Note that most of these expressions follow the pattern
//   thisLevelExpression :
//       nextHigherPrecedenceExpression
//           (OPERATOR nextHigherPrecedenceExpression)*
// which is a standard recursive definition for a parsing an expression.
// The operators in java have the following precedences:
//    lowest  (13)  = *= /= %= += -= <<= >>= >>>= &= ^= |=
//            (12)  ?:
//            (11)  ||
//            (10)  &&
//            ( 9)  |
//            ( 8)  ^
//            ( 7)  &
//            ( 6)  == !=
//            ( 5)  < <= > >=
//            ( 4)  << >>
//            ( 3)  +(binary) -(binary)
//            ( 2)  * / %
//            ( 1)  ++ -- +(unary) -(unary)  ~  !  (type)
//                  []   () (method call)  . (dot -- identifier qualification)
//                  new   ()  (explicit parenthesis)
//
// the last two are not usually on a precedence chart; I put them in
// to point out that new has a higher precedence than '.', so you
// can validy use
//     new Frame().show()
//
// Note that the above precedence levels map to the rules below...
// Once you have a precedence chart, writing the appropriate rules as below
//   is usually very straightfoward



// the mother of all expressions
expression
    :   assignmentExpression
    ;


// This is a list of expressions.  We return just a count for use in
//   method lookup
expressionList returns [int count]
    {count=1;}
    :   expression (COMMA expression {count++;})*
    ;


// assignment expression (level 13)
assignmentExpression
    :   conditionalExpression
        (   (   ASSIGN
            |   PLUS_ASSIGN
            |   MINUS_ASSIGN
            |   STAR_ASSIGN
            |   DIV_ASSIGN
            |   MOD_ASSIGN
            |   SR_ASSIGN
            |   BSR_ASSIGN
            |   SL_ASSIGN
            |   BAND_ASSIGN
            |   BXOR_ASSIGN
            |   BOR_ASSIGN
            )
            assignmentExpression
        )?
    ;


// conditional test (level 12)
conditionalExpression
    :   logicalOrExpression
        ( QUESTION assignmentExpression COLON conditionalExpression )?
    ;


// logical or (||)  (level 11)
logicalOrExpression
    :   logicalAndExpression (LOR logicalAndExpression)*
    ;


// logical and (&&)  (level 10)
logicalAndExpression
    :   inclusiveOrExpression (LAND inclusiveOrExpression)*
    ;


// bitwise or non-short-circuiting or (|)  (level 9)
inclusiveOrExpression
    :   exclusiveOrExpression (BOR exclusiveOrExpression)*
    ;


// exclusive or (^)  (level 8)
exclusiveOrExpression
    :   andExpression (BXOR andExpression)*
    ;


// bitwise or non-short-circuiting and (&)  (level 7)
andExpression
    :   equalityExpression (BAND equalityExpression)*
    ;


// equality/inequality (==/!=) (level 6)
equalityExpression
    :   relationalExpression ((NOT_EQUAL | EQUAL) relationalExpression)*
    ;


// boolean relational expressions (level 5)
relationalExpression
    :   shiftExpression
        (   (   (   LT
                |   GT
                |   LE
                |   GE
                )
                shiftExpression
            )*
        | "instanceof" typeSpec
        )
    ;


// bit shift expressions (level 4)
shiftExpression
    :   additiveExpression ((SL | SR | BSR) additiveExpression)*
    ;


// binary addition/subtraction (level 3)
additiveExpression
    :   multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;


// multiplication/division/modulo (level 2)
multiplicativeExpression
    :   unaryExpression ((STAR | DIV | MOD ) unaryExpression)*
    ;

unaryExpression
	:	INC^ unaryExpression
	|	DEC^ unaryExpression
	|	MINUS^ unaryExpression
	|	PLUS^  unaryExpression
	|	unaryExpressionNotPlusMinus
	;

/*// cast/unary (level 1)
castExpression
    {JavaToken t;}
    // if it _looks_ like a cast, it _is_ a cast
    :   ( LPAREN t=typeSpec RPAREN castExpression )=>
            LPAREN t=typeSpec RPAREN c:castExpression
            {reference(t);} */

unaryExpressionNotPlusMinus
    {JavaToken t;}
    :	BNOT^ unaryExpression
    |	LNOT^ unaryExpression

    |   (   // subrule allows option to shut off warnings
		options {
			// "(int" ambig with postfixExpr due to lack of sequence
			// info in linear approximate LL(k).  It's ok.  Shut up.
			generateAmbigWarnings=false;
		}
	:	// If typecast is built in type, must be numeric operand
		// Also, no reason to backtrack if type keyword like int, float...
		LPAREN t=builtInTypeSpec RPAREN unaryExpression
                {
                    reference(t);
                }

		// Have to backtrack to see if operator follows.  If no operator
		// follows, it's a typecast.  No semantic checking needed to parse.
		// if it _looks_ like a cast, it _is_ a cast; else it's a "(expr)"
	|	(LPAREN t=classTypeSpec RPAREN unaryExpressionNotPlusMinus)
		   => LPAREN t=classTypeSpec RPAREN unaryExpressionNotPlusMinus
                {
                    reference(t);
                }

	|	postfixExpression
        )
        ;

// qualified names, array expressions, method invocation, post inc/dec
postfixExpression
    {JavaToken t; int count=-1;}

    :   t=primaryExpression // start with a primary

	        (   // qualified id (id.id.id.id...) -- build the name
	            DOT ( id:IDENT {if (t!=null) t.setText(t.getText()+"."+id.getText());}
	                | "this"   {if (t!=null) t.setText(t.getText()+".this");}
	                | "class"  {if (t!=null) t.setText(t.getText()+".class");}
	                | newExpression
	                | "super"
	                )
	            // the above line needs a semantic check to make sure "class"
	            //   is the _last_ qualifier.  Could also add it as a dummy
	            //   data member in all classes in the symbol table...

			// allow ClassName[].class
		|  ( LBRACK RBRACK )+ DOT "class"

	        // an array indexing operation
	        |   LBRACK expression RBRACK

	        // method invocation - keep number of parameters
	        // note that this will not quite work correctly in the cross reference
	        //   tool -- we really need to evaluate the method call's return
	        //   type and use that as the base for the next qualifier...
	        // but this works fine for example use, in method calls like
	        //   x.y(4);

	        // The next line is not strictly proper; it allows x(3)(4) or
	        //   x[2](4) which are not valid in Java.  If this grammar were used
	        //   to validate a Java program a semantic check would be needed, or
	        //   this rule would get really ugly...
	        |   LPAREN
                        count=argList
	            RPAREN
	            {
	                if (t!=null)
	                    t.setParamCount(count);
	            }
	        )*

	        // if we have a reference, tell the symbol table
	        {if (t != null) reference(t);}

	        // possibly add on a post-increment or post-decrement
		// allows INC/DEC on too much, but semantics can check
	        (   INC^
	        |   DEC^
	        |   // nothing
	        )
    ;


// the basic element of an expression
primaryExpression returns [JavaToken t]
    {t=null;}
    :   id:IDENT        {t = (JavaToken)id;}
    |   constant
    |   "true"
    |   "false"
    |   th:"this"       {t = (JavaToken)th; setNearestClassScope();}
    |   "null"
    |   t=newExpression
    |   LPAREN expression RPAREN
    |   s:"super"       {t = (JavaToken)s;}
        // look for int.class and int[].class
    |	builtInType ( LBRACK RBRACK )* DOT "class"
    ;

/** object instantiation.
 *  Trees are built as illustrated by the following input/tree pairs:
 *
 *  new T()
 *
 *  new
 *   |
 *   T --  ELIST
 *           |
 *          arg1 -- arg2 -- .. -- argn
 *
 *  new int[]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *
 *  new int[] {1,2}
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR -- ARRAY_INIT
 *                                  |
 *                                EXPR -- EXPR
 *                                  |      |
 *                                  1      2
 *
 *  new int[3]
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *                |
 *              EXPR
 *                |
 *                3
 *
 *  new int[1][2]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *               |
 *         ARRAY_DECLARATOR -- EXPR
 *               |              |
 *             EXPR             1
 *               |
 *               2
 *
 */

newExpression returns [JavaToken t]
    {t=null; int count=-1;}
    :   "new" t=type
        (   LPAREN count=argList RPAREN
                {
                    t.setText(t.getText()+".~constructor~");
                    t.setParamCount(count);
                }
                // java 1.1
                (classBlock)?

        //java 1.1
        // Note: This will allow bad constructs like
        //    new int[4][][3] {exp,exp}.
        //    There needs to be a semantic check here...
        // to make sure:
        //   a) [ expr ] and [ ] are not mixed
        //   b) [ expr ] and an init are not used together

		|	newArrayDeclarator (arrayInitializer)?
		)
	;

argList returns [int count]
	{count=0;}
	:	(	count=expressionList
		|	/*nothing*/
			{count=0;}
		)
	;

newArrayDeclarator
	:	(
                // CONFLICT:
                // newExpression is a primaryExpression which can be
                // followed by an array index reference.  This is ok,
                // as the generated code will stay in this loop as
                // long as it sees an LBRACK (proper behavior)
                options {
				warnWhenFollowAmbig = false;
			}
                :
                     LBRACK
                         (expression)?
                     RBRACK
            )+
    ;

constant
    :   NUM_INT
    |   CHAR_LITERAL
    |   STRING_LITERAL
    |   NUM_FLOAT
    |   NUM_LONG
    |   NUM_DOUBLE
    ;

// annotations (JSR-175: Metadata facility)
protected
annotation
    :
        AT annTypeName
        (
            LPAREN RPAREN // normalAnnotationRest
        |   (LPAREN IDENT ASSIGN )=> LPAREN annMemberValuePair (COMMA annMemberValuePair)* RPAREN // normalAnnotation
        |   LPAREN annMemberValue RPAREN // singleMemberAnnotation
           // none means just a markerAnnotation
        )?
    ;

protected annTypeName
    :
        IDENT (DOT IDENT)*
    ;

protected
annMemberValuePair
    :
        IDENT ASSIGN annMemberValue
    ;

protected
annMemberValue
    :
        conditionalExpression
    |   annotation
    |   annMemberValueArrayInitializer
    ;

protected
annMemberValueArrayInitializer
    :
        LCURLY (annMemberValues)? (COMMA)? RCURLY
    ;

protected
annMemberValues
    :
        annMemberValue (COMMA annMemberValue)*
    ;

//----------------------------------------------------------------------------
// The Java scanner
//----------------------------------------------------------------------------
class JavaLexer extends Lexer;

options {
    exportVocab=Java;		// call the vocabulary "Java"
    testLiterals=false;		// don't automatically test for literals
    k=4;			// four characters of lookahead
	charVocabulary='\u0003'..'\uFFFD';
	// without inlining some bitset tests, couldn't do unicode;
	// I need to make ANTLR generate smaller bitsets; see
	// bottom of JavaLexer.java
	codeGenBitsetTestThreshold=20;
}

{
	public void reportError(RecognitionException ex)
	{
		// do nothing
    }

    /** Parser error-reporting function can be overridden in subclass */
    public void reportError(String s)
    {
      	// do nothing
    }
}





// OPERATORS
QUESTION        :   '?'     ;
LPAREN          :   '('     ;
RPAREN          :   ')'     ;
LBRACK          :   '['     ;
RBRACK          :   ']'     ;
LCURLY          :   '{'     ;
RCURLY          :   '}'     ;
COLON           :   ':'     ;
COMMA           :   ','     ;
//DOT               :   '.'     ;
ASSIGN          :   '='     ;
EQUAL           :   "=="    ;
LNOT            :   '!'     ;
BNOT            :   '~'     ;
NOT_EQUAL       :   "!="    ;
DIV             :   '/'     ;
DIV_ASSIGN      :   "/="    ;
PLUS            :   '+'     ;
PLUS_ASSIGN     :   "+="    ;
INC             :   "++"    ;
MINUS           :   '-'     ;
MINUS_ASSIGN    :   "-="    ;
DEC             :   "--"    ;
STAR            :   '*'     ;
STAR_ASSIGN     :   "*="    ;
MOD             :   '%'     ;
MOD_ASSIGN      :   "%="    ;
SR              :   ">>"    ;
SR_ASSIGN       :   ">>="   ;
BSR             :   ">>>"   ;
BSR_ASSIGN      :   ">>>="  ;
GE              :   ">="    ;
GT              :   ">"     ;
SL              :   "<<"    ;
SL_ASSIGN       :   "<<="   ;
LE              :   "<="    ;
LT              :   '<'     ;
BXOR            :   '^'     ;
BXOR_ASSIGN     :   "^="    ;
BOR             :   '|'     ;
BOR_ASSIGN      :   "|="    ;
LOR             :   "||"    ;
BAND            :   '&'     ;
BAND_ASSIGN     :   "&="    ;
LAND            :   "&&"    ;
SEMI            :   ';'     ;

// annotation token
AT : '@' ;

// Whitespace -- ignored
WS  :   (   ' '
        |   '\t'
        |   '\f'
        // handle newlines
        |	(	options {generateAmbigWarnings=false;}
            :   "\r\n"  // Evil DOS
            |   '\r'    // Macintosh
            |   '\n'    // Unix (the right way)
            )
            { newline(); }
        )+
        { $setType(Token.SKIP); }
    ;

// original SL_COMMENT from standard antlr java.g, causes error on comment on last
// line of file    
// Single-line comments
//SL_COMMENT
//    :       "//"
//            (~('\n'|'\r'))* ('\n'|'\r'('\n')?)
//            { $setType(Token.SKIP); newline(); }
//    ;    

// Single-line comments
SL_COMMENT
    :       "//"
            (~('\n'|'\r'))* 
            { $setType(Token.SKIP); newline(); }
    ;

// multiple-line comments
// we are using a filter stream so these are not set to be
// Token.SKIP, rather they are redirected with the filter stream
ML_COMMENT
	:	"/*"
		(	/*	'\r' '\n' can be matched in one alternative or by matching
				'\r' in one iteration and '\n' in another.  I am trying to
				handle any flavor of newline that comes in, but the language
				that allows both "\r\n" and "\r" and "\n" to all be valid
				newline is ambiguous.  Consequently, the resulting grammar
				must be ambiguous.  I'm shutting this warning off.
			 */
			options {
				generateAmbigWarnings=false;
			}
		:
			{ LA(2)!='/' }? '*'
		|	'\r' '\n'		{newline();}
		|	'\r'			{newline();}
        |   '\n' { newline(); }
		|	~('*'|'\n'|'\r')
        )*
		"*/"
		{ }
    ;


// character literals
CHAR_LITERAL
    :   '\'' ( ESC | ~'\'' ) '\''
    ;

// string literals
STRING_LITERAL
	:	'"' (ESC|~('"'|'\\'))* '"'
    ;


// escape sequence -- note that this is protected; it can only be called
//   from another lexer rule -- it will not ever directly return a token to
//   the parser
// There are various ambiguities hushed in this rule.  The optional
// '0'...'9' digit matches should be matched here rather than letting
// them go back to STRING_LITERAL to be matched.  ANTLR does the
// right thing by matching immediately; hence, it's ok to shut off
// the FOLLOW ambig warnings.
protected
ESC
    :   '\\'
        (   'n'
        |   'r'
        |   't'
        |   'b'
        |   'f'
        |   '"'
        |   '\''
        |   '\\'
        |   ('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
		|	'0'..'3'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:	'0'..'7'
				)?
			)?
		|	'4'..'7'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
			)?
        )
    ;


// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
    :   ('0'..'9'|'A'..'F'|'a'..'f')
    ;


// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT
    options {testLiterals=true;}
    :   ('a'..'z'|'A'..'Z'|'_'|'$'|'\u00C0'..'\ufffe')
        ('a'..'z'|'A'..'Z'|'_'|'$'|'\u00C0'..'\ufffe'|'0'..'9')*
    ;


// a numeric literal
NUM_INT
    {boolean isDecimal=false; Token t=null;}
    :   '.' {_ttype = DOT;}
        ( '.' '.' {_ttype = ELLIPSES;}
        |
            (	('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
                {
				if (t != null && t.getText().toUpperCase().indexOf('F')>=0) {
                	_ttype = NUM_FLOAT;
				}
				else {
                	_ttype = NUM_DOUBLE; // assume double
				}
				}
            )?
     )

    |   (   '0' {isDecimal = true;} // special case for just '0'
			(	('x'|'X')
				(											// hex
					// the 'e'|'E' and float suffix stuff look
					// like hex digits, hence the (...)+ doesn't
					// know when to stop: ambig.  ANTLR resolves
					// it correctly by matching immediately.  It
					// is therefor ok to hush warning.
					options {
						warnWhenFollowAmbig=false;
					}
				:	HEX_DIGIT
				)+

			|	//float or double with leading zero
				(('0'..'9')+ ('.'|EXPONENT|FLOAT_SUFFIX)) => ('0'..'9')+

			|	('0'..'7')+									// octal
            )?
        |   ('1'..'9') ('0'..'9')*  {isDecimal=true;}       // non-zero decimal
        )
		(	('l'|'L') { _ttype = NUM_LONG; }

        // only check to see if it's a float if looks like decimal so far
        |   {isDecimal}?
            (   '.' ('0'..'9')* (EXPONENT)? (f2:FLOAT_SUFFIX {t=f2;})?
            |   EXPONENT (f3:FLOAT_SUFFIX {t=f3;})?
            |   f4:FLOAT_SUFFIX {t=f4;}
            )
            {
			if (t != null && t.getText().toUpperCase() .indexOf('F') >= 0) {
                _ttype = NUM_FLOAT;
			}
            else {
	           	_ttype = NUM_DOUBLE; // assume double
			}
			}
        )?
    ;


// a couple protected methods to assist in matching floating point numbers
protected
EXPONENT
    :   ('e'|'E') ('+'|'-')? ('0'..'9')+
    ;


protected
FLOAT_SUFFIX
    :   'f'|'F'|'d'|'D'
    ;

