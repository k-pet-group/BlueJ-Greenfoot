// BlueJ Java Grammar
//
// A note about conflicts.  There are four points in this grammar where
//   two tokens of lookahead cannot predict which alternative to select.  These
//   points are:
//   1) standard "else" ambiguity
//   2) index reference after array creation:
//        new int[1][2][3]  // is the "3" an index or dim spec?
//   3) {1,2,3,}    // does the last comma start a new init or is it garbage?
//   4) ((caseLabel|"default")+ (statement)* )*
//      nasty conflict, but proper code is generated
//
// Each of these conflicts are noted in the grammar where they occur, and 
// these are no worry as long as these are the only conflicts reported by 
// ANTLR
//


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
import bluej.utility.Debug;
import bluej.parser.symtab.SymbolTable;
import bluej.parser.symtab.JavaVector;
import bluej.parser.symtab.DummyClass;
import bluej.parser.symtab.ClassInfo;

import antlr.*;

import java.util.Vector;
import java.io.*;
}


//---------------------------------------------------------------------------
// Define the Parser.
//---------------------------------------------------------------------------

class ClassParser extends Parser;
options {
    k = 2;                           // two token lookahead
    importVocab=Java;            // Call its vocabulary "Java"
    codeGenMakeSwitchThreshold = 2;  // Some optimizations
    codeGenBitsetTestThreshold = 3;
    defaultErrorHandler = false;     // Don't generate parser error handlers
}


// Define some methods and variables to use in the generated parser.
{
    // these static variables are used to tell what kind of compound
    // statement is being parsed (see the compoundStatement rule
    static final int BODY          = 1;
    static final int CLASS_INIT    = 2;
    static final int INSTANCE_INIT = 3;
    static final int NEW_SCOPE     = 4;


    // We need a symbol table to track definitions
    private SymbolTable symbolTable;
    private TokenStreamHiddenTokenFilter filter;
    
    // the main entry point to parse a file
    public static ClassInfo parse(String filename, Vector classes)
        throws Exception 
    {
	// create a new symbol table
	SymbolTable symbolTable = new SymbolTable();
	doFile(new File(filename), symbolTable); // parse it

	// resolve the types of all symbols in the symbol table
	//  -- we don't need this for BlueJ
	// symbolTable.resolveTypes();

	ClassInfo info = new ClassInfo();

	// add existing classes to the symbol table
	symbolTable.addClasses(classes);

	symbolTable.getInfo(info);
	return info;
    }


    // This method decides what action to take based on the type of
    //   file we are looking at
    public static void doFile(File f, SymbolTable symbolTable)
	throws Exception 
    {
        // If this is a directory, walk each file/dir in that directory
        if (f.isDirectory()) {
            String files[] = f.list();
            for(int i=0; i < files.length; i++)
                doFile(new File(f, files[i]), symbolTable);
        }

        // otherwise, if this is a java file, parse it!
        else if (f.getName().endsWith(".java")) {
            symbolTable.setFile(f);
            parseFile(new BufferedInputStream(new FileInputStream(f)), symbolTable);
        }
    }

    // Here's where we do the real work...
    public static void parseFile(InputStream s,
                                 SymbolTable symbolTable)
	throws Exception 
    {
	// Create a scanner that reads from the input stream passed to us
	JavaLexer lexer = new JavaLexer(s);

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
	parser.setFilter(filter);

	// start parsing at the compilationUnit rule
	parser.compilationUnit();
    }

    // Tell the parser which symbol table to use
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void setFilter(TokenStreamHiddenTokenFilter filter) {
        this.filter = filter;
    }

    
    // redefined from antlr.LLkParser to supress error messages
    public void reportError(ParserException ex) {
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
				if (ctok.getLine() < startToken.getLine()-2)
					ctok = null;
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

    public void popScope()                 {symbolTable.popScope();}
    public void endFile()                  {symbolTable.popAllScopes();}
    public void defineBlock(JavaToken tok) {symbolTable.defineBlock(tok);}
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
			    JavaToken comment) {
        symbolTable.defineClass(theClass, superClass, interfaces, isAbstract, comment);
    }

    public void defineInterface(JavaToken theInterface,
                                JavaVector subInterfaces,
                                JavaToken comment) {
        symbolTable.defineInterface(theInterface, subInterfaces, comment);
    }

    public void defineVar(JavaToken theVariable, JavaToken type, JavaToken comment) {
        symbolTable.defineVar(theVariable, type, comment);
    }

    public void defineMethod(JavaToken theMethod, JavaToken type, JavaToken comment) {
        symbolTable.defineMethod(theMethod, type, comment);
    }

    public void addImport(JavaToken id, String className, String packageName) {
        symbolTable.addImport(id, className, packageName);
    }

}



// Compilation Unit: In Java, this is a single file.  This is the start
//   rule for this parser
compilationUnit
    :   // A compilation unit starts with an optional package definition
        //   If there is no package definition, any classes are in the
        //   "default" package
        (   packageDefinition
        |   /* nothing */     {useDefaultPackage();}
        )

        // Next we have a series of zero or more import statements
        ( importDefinition )*

        // Wrapping things up with any number of class or interface
        //    definitions
        ( typeDefinition )*

        // When we reach end-of-file, tell the symboltable
        EOF
        {endFile();} // if a package were defined, pop its scope
    ;


// Package statement: "package" followed by an identifier.
packageDefinition
    options {defaultErrorHandler = true;} // let ANTLR handle errors
    {JavaToken id;} // define an id for the package name
    :   "package" id=identifier SEMI
        {definePackage(id); }  // tell the symbol table about the package
    ;


// Import statement: import followed by a package or class name
//   Note that we do not perform any action here.  The action associated
//   with the import statement is performed in identifierStar.  (That
//   rule is only called from importDefinition, so it is safe to have it
//   assume it's an import.)
importDefinition
    options {defaultErrorHandler = true;}
    :   "import" identifierStar SEMI
    ;


// A type definition in a file is either a class or interface definition.
// Note that modifiers are essentially thrown out.  If you want to handle
//   them, collect them in a Vector (returned from the modifiers rule)
//   and pass them _down_ to classDefinition and interfaceDefinition
//   to be associated with the class or interface definition
typeDefinition
    options {defaultErrorHandler = true;}
    {boolean isAbstract;
     JavaToken commentToken = null; }
    : { commentToken = findAttachedComment((JavaToken)LT(1)); }
       isAbstract=modifiers
        ( classDefinition[isAbstract, commentToken]
        | interfaceDefinition[commentToken]
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


// A list of zero or more modifiers.  We could have used (modifier)* in
//   place of a call to modifiers, but I thought it was a good idea to keep
//   this rule separate so they can easily be collected in a Vector if
//   someone so desires
modifiers returns [boolean isAbstract]
    {isAbstract = false;
     boolean abs;}
    :   ( abs=modifier 
	  {isAbstract|=abs;} 
	)*
    ;


// A type specification is a type name with possible brackets afterwards
//   (which would make it an array type).  Currently we ignore arrays to
//   give the "interested reader" something simple to try to add.
typeSpec returns [JavaToken t]
    {t=null;}
    :   t=type (LBRACK RBRACK )*
    ;


// A type name. which is either a (possibly qualified) class name or
//   a primitive (builtin) type
type returns [JavaToken t]
    {t=null;}
    :   t=identifier
    |   t=builtInType
    ;


// The primitive types.  In this version of the cross reference tool we
//   just pass the token that contained the name on up for later lookup
//   in the symbol table
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


// modifiers for Java classes, interfaces, class/instance vars and methods
// For this version of the cross reference tool we ignore them...
modifier returns [boolean isAbstract]
    {isAbstract = false;}
    :   "private"
    |   "public"
    |   "protected"
    |   "static"
    |   "transient"
    |   "final"
    |   "abstract"
	{isAbstract = true;}
    |   "native"
    |   "threadsafe"
    |   "synchronized"
    |   "const"
    ;


// Definition of a Java class
classDefinition[boolean isAbstract, JavaToken commentToken]
    {JavaToken superClass=null; JavaVector interfaces=null;}
    :   "class" id:IDENT // aha! a class!

            // it _might_ have a superclass...
            ("extends" superClass=identifier
            { }
            )?

            // it might implement some interfaces...
            (interfaces=implementsClause)?

            // tell the symbol table about it
            // Note that defineClass pushes tyhe class' scope,
            //   so we'll have to pop...
            {defineClass((JavaToken)id, superClass, interfaces, isAbstract, commentToken);}

            // now parse the body of the class
            classBlock

        // tell the symbol table that we are exiting a scope
        {popScope();}
    ;



// Definition of a Java Interface
interfaceDefinition[JavaToken commentToken]
    {JavaVector superInterfaces = null;}
    :   "interface" id:IDENT // aha! an interface!
            
            // it might extend some other interfaces
            (superInterfaces=interfaceExtends)? 
            
            // tell the symbol table about it!
            // Note that defineInterface pushes the interface scope, so
            //   we'll have to pop it...
            { defineInterface((JavaToken)id, superInterfaces, commentToken);}

            // now parse the body of the interface (looks like a class...)
            classBlock

        // tell the symboltable that we are done in that scope
        {popScope();}
    ;


// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
classBlock
    :   LCURLY
            ( field | SEMI )*
        RCURLY
    ;


// An interface can extend several other interfaces, so we collect a vector
//   of all the superinterfaces and return it
interfaceExtends returns [JavaVector supers]
    {JavaToken id; supers = new JavaVector();}
    :   "extends" id=identifier
    	{ supers.addElement(dummyClass(id)); }
        (COMMA id=identifier
        { supers.addElement(dummyClass(id)); }
         )*
    ;


// A class can implement several interfaces, so we collect a vector of
//   all the implemented interfaces and return it
implementsClause returns [JavaVector inters]
    {inters = new JavaVector(); JavaToken id;}
    :   "implements" id=identifier  {inters.addElement(dummyClass(id));}
        ( COMMA id=identifier       {inters.addElement(dummyClass(id));} )*
    ;



// Now the various things that can be defined inside a class or interface...
// Note that not all of these are really valid in an interface (constructors,
//   for example), and if this grammar were used for a compiler there would
//   need to be some semantic checks to make sure we're doing the right thing...
field
    {JavaToken type;
     JavaToken commentToken = null; }
    :   // method, constructor, or variable declaration
	{ commentToken = findAttachedComment((JavaToken)LT(1)); }

        modifiers

        (   methodHead[null, commentToken]            // no type to pass...
                compoundStatement[BODY] // constructor

        |   classDefinition[false, null]      // inner class
        |   interfaceDefinition[null]         // inner interface

        |   type=typeSpec  // method or variable declaration(s)
            (   methodHead[type, commentToken]
                    ( compoundStatement[BODY] | SEMI {popScope();})

            |   variableDefinitions[type, commentToken] SEMI
            )
        ) 
          

    // "static { ... }" class initializer
    |   "static" compoundStatement[CLASS_INIT]

    // "{ ... }" instance initializer
    |   compoundStatement[INSTANCE_INIT]         
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
    :   id:IDENT (LBRACK RBRACK)* ( ASSIGN initializer )?
        {defineVar((JavaToken)id, type, commentToken);}
    ;


// This is an initializer used to set up an array.  For our
//   purposes, it is simply ignored...
// CONFLICT: does a COMMA after an initializer start a new
//           (...)* or start the (...)?
//           ANTLR generates proper code due to LA(2)
arrayInitializer
    :   LCURLY
            (   initializer ( COMMA initializer )*
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


// This is the header of a method.  It includes the name and parameters
//   for the method.  Note that the type is passed into this method.  This
//   was necessary to resolve a conflict that several types of fields in a
//   class started with a type and/or modifier, so they had to be left-factored
//   This also watches for a list of exception classes in a "throws" clause.
methodHead[JavaToken type, JavaToken commentToken]
    {JavaVector exceptions=null;} // to keep track of thrown exceptions
    :   method:IDENT  // the name of the method

        {
		// tell the symbol table about it.  Note that this signals that 
        	// we are in a method header so we handle parameters appropriately
        	defineMethod((JavaToken)method, type, commentToken);
        }

        // parse the formal parameter declarations.  These are sent to the
        // symbol table as _variables_.  Because the symbol table knows we
        // are in a method header, it collects these definitions as parameters
        // for the method.
        LPAREN (parameterDeclarationList)? RPAREN

        // again, the array specification is skipped...
        (LBRACK RBRACK)*

        // get the list of exceptions that this method is declared to throw
        (exceptions=throwsClause)?

        // tell the symbol table we are done with the method header. Note that
        // this will tell the symbol table to handle variables normally
        {endMethodHead(exceptions);}
    ;


// This is a list of exception classes that the method is declared to throw
// We just collect them in a vector and pass them back to the method header
throwsClause returns [JavaVector exceptions]
    {JavaToken id; exceptions = new JavaVector();}
    :   "throws" id=identifier   {exceptions.addElement(dummyClass(id));}
        (COMMA id=identifier     {exceptions.addElement(dummyClass(id));} )*
    ;


// A list of formal parameters
parameterDeclarationList
    :   parameterDeclaration ( COMMA parameterDeclaration )*
    ;


// A formal parameter.  We pass this to the symbol table as a variable, and
//   the symbol table adds it to the parameter list of the current method
//   header.
parameterDeclaration
    {JavaToken type;}
    :   ("final")? type=typeSpec id:IDENT (LBRACK RBRACK)*
        {defineVar((JavaToken)id, type, null);}
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
                        defineMethod(null, (JavaToken)lc, null);
                        endMethodHead(null);
                        break;
                    case INSTANCE_INIT:
                        lc.setText("~instance-init~");
                        defineMethod(null, (JavaToken)lc, null);
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


// Here are all the wonderful Java statements...
statement
    {int count = -1;} // used for parameter counts in method calls

    // A list of statements in curly braces -- start a new scope!
    :   compoundStatement[NEW_SCOPE]

    // If it _looks_ like a decl, it's a decl...
    |   (declaration)=> declaration SEMI

    // Attach a label to the front of a statement
    |   id:IDENT COLON statement  {defineLabel((JavaToken)id);}

    // An expression statement.  This could be a method call, assignment
    //   statement, or any other expression evaluated for side-effects.
    |   expression SEMI

    // If-else statement
    // CONFLICT: the old "dangling-else" problem...
    //           ANTLR generates proper code by just making the "else"
    //           optional!
    |   "if" LPAREN expression RPAREN statement
        ( "else" statement )?

        // the "else" part above is ambiguous.  The intent
        // is to keep it as close to the corresponding "if"
        // as possible.  The generated code will do this,
        // so we can live with the ambiguity.  We could do
        //      (   ("else")=> "else" statement 
        //      |   // no else clause
        //      )
        // instead, but that's less efficient...

    // For statement
    |   "for"
            LPAREN
                (forInit)?        SEMI    // initializer
                (expression)?     SEMI    // condition test
                (count=expressionList)?   // updater
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
    // CONFLICT: to which "cases" does the statement bind?
    //           ANTLR generates proper code as it groups as
    //           many "case"/"default" labels together then
    //           follows them with the statements
    |   "switch" LPAREN expression RPAREN
            LCURLY
                (   (("case" expression | "default") COLON)+ (statement)*
                    // ambiguous but proper code will be generated...
                )*
            RCURLY

    // exception try-catch block
    |   tryBlock

    // throw an exception
    |   "throw" expression SEMI

    // synchronize a statement
    |   "synchronized" LPAREN expression RPAREN statement

    // empty statement
    |   SEMI
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


// expressions -- the FUN stuff!
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
        ( QUESTION conditionalExpression COLON conditionalExpression )?
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
        (   (   LT
            |   GT
            |   LE
            |   GE
            )
            shiftExpression
        )*
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
    :   castExpression ((STAR | DIV | MOD ) castExpression)*
    ;


// cast/unary (level 1)
castExpression
    {JavaToken t;}
    // if it _looks_ like a cast, it _is_ a cast
    :   ( LPAREN t=typeSpec RPAREN castExpression )=>
            LPAREN t=typeSpec RPAREN c:castExpression
            {reference(t);}

    // otherwise it's a unary expression
    |   INC castExpression
    |   DEC castExpression
    |   MINUS castExpression
    |   BNOT castExpression
    |   LNOT castExpression
    |   postfixExpression ( "instanceof" t=typeSpec {reference(t);})?
        // instanceof should not allow just primitives (x instanceof int)
        // need a semantic check if we're compiling...
    ;


// qualified names, array expressions, method invocation, post inc/dec
postfixExpression
    {JavaToken t; int count=-1;}

    :   t=primaryExpression // start with a primary

        
        (   // qualified id (id.id.id.id...) -- buid the name
            DOT ( id:IDENT {if (t!=null) t.setText(t.getText()+"."+id.getText());}
                | "this"   {if (t!=null) t.setText(t.getText()+".this");}
                | "class"  {if (t!=null) t.setText(t.getText()+".class");}
                )
            // the above line needs a semantic check to make sure "class"
            //   is the _last_ qualifier.  Could also add it as a dummy
            //   data member in all classes in the symbol table...

        // an array indexing operation (not handled)
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
                ( count=expressionList
                | /*nothing*/ {count=0;}
                )
            RPAREN
            {
                if (t!=null)
                    t.setParamCount(count);
            }
        )*

        // if we have a reference, tell the symbol table
        {if (t != null) reference(t);}

        // possibly add on a post-increment or post-decrement
        (   INC
        |   DEC
        |   // nothing
        )
    ;


// the basic element of an expression
primaryExpression returns [JavaToken t]
    {t=null;}
    :   id:IDENT {t = (JavaToken)id;}
    |   t=builtInType DOT "class" {t.setText(t.getText()+".class");}
    |   t=newExpression
    |   constant
    |   s:"super"        {t = (JavaToken)s;}
    |   "true"
    |   "false"
    |   th:"this"        {t = (JavaToken)th; setNearestClassScope();}
    |   "null"
    |   LPAREN expression RPAREN
    ;


// object instantiation.
newExpression returns [JavaToken t]
    {t=null; int count=-1;}
    :   "new" t=type
        (   LPAREN
                ( count=expressionList
                | /*nothing*/ {count=0;}
                )
            RPAREN 
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
        |   (
                // CONFLICT:
                // newExpression is a primaryExpression which can be
                // followed by an array index reference.  This is ok,
                // as the generated code will stay in this loop as
                // long as it sees an LBRACK (proper behavior)
                LBRACK
                    (expression)?
                RBRACK
            )+
            (arrayInitializer)?

        )
    ;


constant
    :   NUM_INT
    |   CHAR_LITERAL
    |   STRING_LITERAL
    |   NUM_FLOAT
    ;


//----------------------------------------------------------------------------
// The Java scanner
//----------------------------------------------------------------------------
class JavaLexer extends Lexer;
options {
    importVocab=Java;		// call the vocabulary "Java"
    testLiterals=false;		// don't automatically test for literals
    k=4;			// four characters of lookahead
}

{
	protected int tokColumn = 1;
	protected int column = 1;

	public void consume() throws IOException
	{
//		if ( inputState.guessing==0 ) {
		if (text.length()==0) {
			// remember token start column
			tokColumn = column;
		}
//		}
		column++;
		super.consume();
	}

	public void newline() { super.newline(); column = 1; }

	protected Token makeToken(int t)
	{
		Token tok = super.makeToken(t);
		tok.setColumn(tokColumn);
		return tok;
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


// Whitespace -- ignored
WS
    :   (   ' '
        |   '\t'
        |   '\f'
        // handle newlines
        |   (   "\r\n"  // Evil DOS
            |   '\r'    // Macintosh
            |   '\n'    // Unix (the right way)
            )
            { newline(); }
        )
        { $setType(Token.SKIP); }
    ;


// Single-line comments
SL_COMMENT
    :   "//" (~('\n'|'\r'))* ('\n'|'\r'('\n')?)
            { $setType(Token.SKIP); newline(); }
    ;


// multiple-line comments
// we are using a filter stream so these are not set to be
// Token.SKIP, rather they are redirected with the filter stream
ML_COMMENT
    :   '/'! '*'
        (   { LA(2)!='/' }? '*'
        |   '\n' { newline(); }
        |   ~('*'|'\n')
        )*
        '*' '/'!
            { }
    ;


// character literals
CHAR_LITERAL
    :   '\'' ( ESC | ~'\'' ) '\''
    ;

// string literals
STRING_LITERAL
    :   '"' (ESC|~'"')* '"'
    ;


// escape sequence -- note that this is protected; it can only be called
//   from another lexer rule -- it will not ever directly return a token to
//   the parser
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
        |   ('0'..'3') ( ('0'..'9') ('0'..'9')? )?
        |   ('4'..'7') ('0'..'9')?
        )
    ;


// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
    :   ('0'..'9'|'A'..'F'|'a'..'f')
    ;


// a dummy rule to force vocabulary to be all characters (except special
//   ones that ANTLR uses internally (0 to 2)
protected
VOCAB
    :   '\3'..'\377'
    ;


// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT
    options {testLiterals=true;}
    :   ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|'$')*
    ;


// a numeric literal
NUM_INT
    {boolean isDecimal=false;}
    :   '.' {_ttype = DOT;}
            (('0'..'9')+ (EXPONENT)? (FLOAT_SUFFIX)? { _ttype = NUM_FLOAT; })?
    |   (   '0' {isDecimal = true;} // special case for just '0'
            (   ('x'|'X') (HEX_DIGIT)+                      // hex
            |   ('0'..'8')+                                 // octal
            )?
        |   ('1'..'9') ('0'..'9')*  {isDecimal=true;}       // non-zero decimal
        )
        (   ('l'|'L')
        
        // only check to see if it's a float if looks like decimal so far
        |   {isDecimal}?
            (   '.' ('0'..'9')* (EXPONENT)? (FLOAT_SUFFIX)?
            |   EXPONENT (FLOAT_SUFFIX)?
            |   FLOAT_SUFFIX
            )
            { _ttype = NUM_FLOAT; }
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

