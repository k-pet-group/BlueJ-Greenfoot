header {
    package bluej.parser.ast.gen;
	
	import bluej.parser.ast.LocatableAST;
}

/** Java 1.5 Recognizer
 *
 * Run 'java Main [-showtree] directory-full-of-java-files'
 *
 * [The -showtree option pops up a Swing frame that shows
 *  the AST constructed from the parser.]
 *
 * Run 'java Main <directory full of java files>'
 *
 * Contributing authors:
 *		John Mitchell		johnm@non.net
 *		Terence Parr		parrt@magelang.com
 *		John Lilley			jlilley@empathy.com
 *		Scott Stanchfield	thetick@magelang.com
 *		Markus Mohnen       mohnen@informatik.rwth-aachen.de
 *      Peter Williams      pete.williams@sun.com
 *      Allan Jacobs        Allan.Jacobs@eng.sun.com
 *      Steve Messick       messick@redhills.com
 *      John Pybus			john@pybus.org
 *
 * Version 1.00 December 9, 1997 -- initial release
 * Version 1.01 December 10, 1997
 *		fixed bug in octal def (0..7 not 0..8)
 * Version 1.10 August 1998 (parrt)
 *		added tree construction
 *		fixed definition of WS,comments for mac,pc,unix newlines
 *		added unary plus
 * Version 1.11 (Nov 20, 1998)
 *		Added "shutup" option to turn off last ambig warning.
 *		Fixed inner class def to allow named class defs as statements
 *		synchronized requires compound not simple statement
 *		add [] after builtInType DOT class in primaryExpression
 *		"const" is reserved but not valid..removed from modifiers
 * Version 1.12 (Feb 2, 1999)
 *		Changed LITERAL_xxx to xxx in tree grammar.
 *		Updated java.g to use tokens {...} now for 2.6.0 (new feature).
 *
 * Version 1.13 (Apr 23, 1999)
 *		Didn't have (stat)? for else clause in tree parser.
 *		Didn't gen ASTs for interface extends.  Updated tree parser too.
 *		Updated to 2.6.0.
 * Version 1.14 (Jun 20, 1999)
 *		Allowed final/abstract on local classes.
 *		Removed local interfaces from methods
 *		Put instanceof precedence where it belongs...in relationalExpr
 *			It also had expr not type as arg; fixed it.
 *		Missing ! on SEMI in classBlock
 *		fixed: (expr) + "string" was parsed incorrectly (+ as unary plus).
 *		fixed: didn't like Object[].class in parser or tree parser
 * Version 1.15 (Jun 26, 1999)
 *		Screwed up rule with instanceof in it. :(  Fixed.
 *		Tree parser didn't like (expr).something; fixed.
 *		Allowed multiple inheritance in tree grammar. oops.
 * Version 1.16 (August 22, 1999)
 *		Extending an interface built a wacky tree: had extra EXTENDS.
 *		Tree grammar didn't allow multiple superinterfaces.
 *		Tree grammar didn't allow empty var initializer: {}
 * Version 1.17 (October 12, 1999)
 *		ESC lexer rule allowed 399 max not 377 max.
 *		java.tree.g didn't handle the expression of synchronized
 *		statements.
 * Version 1.18 (August 12, 2001)
 *      	Terence updated to Java 2 Version 1.3 by
 *		observing/combining work of Allan Jacobs and Steve
 *		Messick.  Handles 1.3 src.  Summary:
 *		o  primary didn't include boolean.class kind of thing
 *      	o  constructor calls parsed explicitly now:
 * 		   see explicitConstructorInvocation
 *		o  add strictfp modifier
 *      o  missing objBlock after new expression in tree grammar
 *		o  merged local class definition alternatives, moved after declaration
 *		o  fixed problem with ClassName.super.field
 *      o  reordered some alternatives to make things more efficient
 *		o  long and double constants were not differentiated from int/float
 *		o  whitespace rule was inefficient: matched only one char
 *		o  add an examples directory with some nasty 1.3 cases
 *		o  made Main.java use buffered IO and a Reader for Unicode support
 *		o  supports UNICODE?
 *		   Using Unicode charVocabulay makes code file big, but only
 *		   in the bitsets at the end. I need to make ANTLR generate
 *		   unicode bitsets more efficiently.
 * Version 1.19 (April 25, 2002)
 *		Terence added in nice fixes by John Pybus concerning floating
 *		constants and problems with super() calls.  John did a nice
 *		reorg of the primary/postfix expression stuff to read better
 *		and makes f.g.super() parse properly (it was METHOD_CALL not
 *		a SUPER_CTOR_CALL).  Also:
 *
 *		o  "finally" clause was a root...made it a child of "try"
 *		o  Added stuff for asserts too for Java 1.4, but *commented out*
 *		   as it is not backward compatible.
 *
 * Version 1.20 (October 27, 2002)
 *
 *      Terence ended up reorging John Pybus' stuff to
 *      remove some nondeterminisms and some syntactic predicates.
 *      Note that the grammar is stricter now; e.g., this(...) must
 *	be the first statement.
 *
 *      Trinary ?: operator wasn't working as array name:
 *          (isBig ? bigDigits : digits)[i];
 *
 *      Checked parser/tree parser on source for
 *          Resin-2.0.5, jive-2.1.1, jdk 1.3.1, Lucene, antlr 2.7.2a4,
 *	    and the 110k-line jGuru server source.
 *
 * Version 1.21 (October 17, 2003)
 *	Fixed lots of problems including:
 *	Ray Waldin: add typeDefinition to interfaceBlock in java.tree.g
 *  He found a problem/fix with floating point that start with 0
 *  Ray also fixed problem that (int.class) was not recognized.
 *  Thorsten van Ellen noticed that \n are allowed incorrectly in strings.
 *  TJP fixed CHAR_LITERAL analogously.
 *
 * Version 1.21.2 (March, 2003)
 *      Changes by Matt Quail to support generics (as per JDK1.5/JSR14)
 *      Notes:
 *      o We only allow the "extends" keyword and not the "implements"
 *        keyword, since thats what JSR14 seems to imply.
 *      o Thanks to Monty Zukowski for his help on the antlr-interest
 *        mail list.
 *      o Thanks to Alan Eliasen for testing the grammar over his
 *        Fink source base
 *
 * Version 1.22 (July, 2004)
 *      Changes by Michael Studman to support Java 1.5 language extensions
 *      Notes:
 *      o Added support for annotations types
 *      o Finished off Matt Quail's generics enhancements to support bound type arguments
 *      o Added support for new for statement syntax
 *      o Added support for static import syntax
 *      o Added support for enum types
 *      o Tested against JDK 1.5 source base and source base of jdigraph project
 *      o Thanks to Matt Quail for doing the hard part by doing most of the generics work
 *
 * Version 1.22.1 (July 28, 2004)
 *      Bug/omission fixes for Java 1.5 language support
 *      o Fixed tree structure bug with classOrInterface - thanks to Pieter Vangorpto for
 *        spotting this
 *      o Fixed bug where incorrect handling of SR and BSR tokens would cause type
 *        parameters to be recognised as type arguments.
 *      o Enabled type parameters on constructors, annotations on enum constants
 *        and package definitions
 *      o Fixed problems when parsing if ((char.class.equals(c))) {} - solution by Matt Quail at Cenqua
 *
 * This grammar is in the PUBLIC DOMAIN
 */

class JavaRecognizer extends Parser;
options {
	k = 2;                           // two token lookahead
	exportVocab=Java;                // Call its vocabulary "Java"
	codeGenMakeSwitchThreshold = 2;  // Some optimizations
	codeGenBitsetTestThreshold = 3;
	defaultErrorHandler = false;     // Don't generate parser error handlers
	buildAST = true;
}

tokens {
	BLOCK; MODIFIERS; OBJBLOCK; SLIST; CTOR_DEF; METHOD_DEF; VARIABLE_DEF;
	INSTANCE_INIT; STATIC_INIT; TYPE; CLASS_DEF; INTERFACE_DEF;
	PACKAGE_DEF; ARRAY_DECLARATOR; EXTENDS_CLAUSE; IMPLEMENTS_CLAUSE;
	PARAMETERS; PARAMETER_DEF; LABELED_STAT; TYPECAST; INDEX_OP;
	POST_INC; POST_DEC; METHOD_CALL; EXPR; ARRAY_INIT;
	IMPORT; UNARY_MINUS; UNARY_PLUS; CASE_GROUP; ELIST; FOR_INIT; FOR_CONDITION;
	FOR_ITERATOR; EMPTY_STAT; FINAL="final"; ABSTRACT="abstract";
	STRICTFP="strictfp"; SUPER_CTOR_CALL; CTOR_CALL; VARIABLE_PARAMETER_DEF;
	STATIC_IMPORT; ENUM_DEF; ENUM_CONSTANT_DEF; FOR; FOR_EACH; ANNOTATION_DEF; ANNOTATION;
	ANNOTATION_MEMBER_VALUE_PAIR; ANNOTATION_FIELD_DEF; ANNOTATION_ARRAY_INIT; TYPE_ARGUMENT;
	TYPE_PARAMETERS; WILDCARD_TYPE; TYPE_UPPER_BOUNDS; TYPE_LOWER_BOUNDS; COMMENT_DEF;

	ML_COMMENT;  // used by the lexer
}

{
    // Our nodes can also store "important" nodes. Ones that
    // don't interfere with the AST matching but which can be
    // retrieved in the tree processing code.
    // For instance, when processing a class, we need to know
    // where the opening and closing bracket tokens are, yet we
    // don't want to change the actual tree generated to include
    // bracket nodes. So we attach the bracket tokens to the
    // CLASS_DEF node in the tree.
    static void addImportantToken(AST h, Token t)
    {
        // we may want to use this grammar with different AST types
        // so we make sure it works if the nodes are not LocatableAST
        if (h instanceof bluej.parser.ast.LocatableAST)
            ((bluej.parser.ast.LocatableAST) h).addImportantToken(t);
    }
    
    // Keep track of the most recently hit type end (>, >>, or >>>)
    Token mostRecentTypeEnd;
    
    
    static void setEndToken(AST h, Token t)
    {
    	if (h instanceof bluej.parser.ast.LocatableAST) {
    		bluej.parser.ast.LocatableToken lt = (bluej.parser.ast.LocatableToken) t;
    		int l = lt.getLine();
    		int c = lt.getColumn() + lt.getText().length();
    		((bluej.parser.ast.LocatableAST) h).setEndPos(l, c);
    	}
    }
    
    static void setEndAst(AST h, AST e)
    {
    	if (h instanceof bluej.parser.ast.LocatableAST) {
    		bluej.parser.ast.LocatableAST le = (bluej.parser.ast.LocatableAST) e;
    		int l = le.getEndLine();
    		int c = le.getEndColumn();
    		((bluej.parser.ast.LocatableAST) h).setEndPos(l, c);
    	}
    }

    /**
     * Counts the number of LT seen in the typeArguments production.
     * It is used in semantic predicates to ensure we have seen
     * enough closing '>' characters; which actually may have been
     * either GT, SR or BSR tokens.
     */
    private int ltCounter = 0;
    /**
     * Counts the number of '>' characters that have been seen but
     * have not yet been associated with the end of a typeParameters or
     * typeArguments production. This is necessary because SR and BSR
     * tokens have significance (the extra '>' characters) not only for the production
     * that sees them but also productions higher in the stack (possibly right up to an outer-most
     * typeParameters production). As the stack of the typeArguments/typeParameters productions unwind,
     * any '>' characters seen prematurely through SRs or BSRs are reconciled.
     */
    private int gtToReconcile = 0;
}

// Compilation Unit: In Java, this is a single file.  This is the start
//   rule for this parser
compilationUnit
	:	// A compilation unit starts with an optional package definition
		(	(packageDefinition)=> packageDefinition
		|	/* nothing */
		)

		// Next we have a series of zero or more import statements
		( importDefinition )*

		// Wrapping things up with any number of class or interface
		//    definitions
		( typeDefinition )*

		EOF!
	;


// Package statement: "package" followed by an identifier.
packageDefinition
	options {defaultErrorHandler = false;} // let ANTLR handle errors
	:	(annotation)* p:"package"^ {#p.setType(PACKAGE_DEF);} identifier s:SEMI!
	    {   addImportantToken(#p, s);   }
	;


// Import statement: import followed by a package or class name
importDefinition
	options {defaultErrorHandler = false;}
    { boolean isStatic = false; }
	:	i:"import"^ ( "static"! {isStatic = true;} )? identifierStar s:SEMI!
	    {  
	        if (isStatic)
	        {
	            #i.setType(STATIC_IMPORT);
	        }
	        else
	        {
	            #i.setType(IMPORT);
	        }
            addImportantToken(#i, s);   
	    }
	;

// A type definition in a file is either a class or interface definition.
typeDefinition
	options {defaultErrorHandler = false;}
	:	m:modifiers!
		( classDefinition[#m]
		| interfaceDefinition[#m]
		| enumDefinition[#m]
		| annotationDefinition[#m]
		)
	|	SEMI!
	;

/** A declaration is the creation of a reference or primitive-type variable
 *  Create a separate Type/Var tree for each var in the var list.
 */
declaration!
	:	m:modifiers t:typeSpec[false] v:variableDefinitions[#m,#t]
		{#declaration = #v;}
	;

// A type specification is a type name with possible brackets afterwards
//   (which would make it an array type).
typeSpec[boolean addImagNode]
	: classTypeSpec[addImagNode]
	| builtInTypeSpec[addImagNode]
	;

arraySpecOpt:
        (options{greedy=true;}: // match as many as possible
            lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!
        )*
    ;

// A class type specification is a class type with either:
// - possible brackets afterwards
//   (which would make it an array type).
// - generic type arguments after
classTypeSpec[boolean addImagNode]
	:   classOrInterfaceType[addImagNode]
        arraySpecOpt
		{
			if ( addImagNode ) {
				#classTypeSpec = #(#[TYPE,"TYPE"], #classTypeSpec);
			}
		}
	;

classOrInterfaceType[boolean addImagNode]
	:	nameWithTypeArgs[addImagNode]
        (options{greedy=true;}: // match as many as possible
            DOT^
			nameWithTypeArgs[addImagNode]
        )*
    ;
    
nameWithTypeArgs[boolean addImagNode]
	:	IDENT^ (typeArguments[addImagNode])?
	;

// A generic type argument is a class type, a possibly bounded wildcard type or a built-in type array
typeArgument[boolean addImagNode]
	:   (   (classOrInterfaceType[addImagNode] | builtInTypeSpec[addImagNode]) arraySpecOpt
	        | wildcardType[addImagNode]
	    )
		{#typeArgument = #(#[TYPE_ARGUMENT,"TYPE_ARGUMENT"], #typeArgument);}
    ;

wildcardType[boolean addImagNode]
    :   q:QUESTION^ {#q.setType(WILDCARD_TYPE);}
        (("extends" | "super")=> typeArgumentBounds[addImagNode])?
    ;

// A series of type arguments for a generic type. The first will have the
// closing '>' (or '>>' or '>>>') token as an important token.
typeArguments[boolean addImagNode]
{int currentLtLevel = 0;}
    :
        {currentLtLevel = ltCounter;}
        LT! {ltCounter++;}
        typeArgument[addImagNode]
        (options{greedy=true;}: // match as many as possible
            // If there are any '>' to reconcile
            // (i.e. we've recently encountered a GT, SR or BSR
            // - the end of one or more type arguments and
            // possibly an enclosing type parameter)
            // then further type arguments are not possible
            {gtToReconcile == 0}? COMMA! typeArgument[addImagNode]
        )*

        (   // turn warning off since Antlr generates the right code,
            // plus we have our semantic predicate below
            options{generateAmbigWarnings=false;}:
            typeArgumentsEnd!
        )?
        
        {
			if (#typeArguments instanceof LocatableAST)
				((LocatableAST) #typeArguments).addImportantToken(mostRecentTypeEnd);
		}

        // As we are leaving a typeArguments production, the enclosing '>'
        // we've just read (and we've possibly seen more than one in the
        // case of SRs and BSRs) can now be marked as reconciled with a '<'
        // but we still leave unreconciled the count for any excess '>'
        // for other typeArguments or typeParameters productions higher in
        // the stack
        {if (gtToReconcile > 0) gtToReconcile-=1;}
        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?
    ;

// this gobbles up *some* amount of '>' characters, and counts how many
// it gobbled.
protected typeArgumentsEnd!:
        gt:GT {ltCounter-=1;gtToReconcile+=1;mostRecentTypeEnd=gt;}
    |   sr:SR {ltCounter-=2;gtToReconcile+=2;mostRecentTypeEnd=sr;}
    |   bsr:BSR {ltCounter-=3;gtToReconcile+=3;mostRecentTypeEnd=bsr;}
    ;

typeArgumentBounds[boolean addImagNode]
    {boolean isUpperBounds = false;}
    :
        ( "extends"! {isUpperBounds=true;} | "super"! ) classTypeSpec[addImagNode]
		{
		    if (isUpperBounds)
		    {
		        #typeArgumentBounds = #(#[TYPE_UPPER_BOUNDS,"TYPE_UPPER_BOUNDS"], #typeArgumentBounds);
		    }
		    else
		    {
		        #typeArgumentBounds = #(#[TYPE_LOWER_BOUNDS,"TYPE_LOWER_BOUNDS"], #typeArgumentBounds);
		    }
		}
    ;

// A builtin type specification is a builtin type with possible brackets
// afterwards (which would make it an array type).
builtInTypeSpec[boolean addImagNode]
	:	builtInType arraySpecOpt
		{
			if ( addImagNode ) {
				#builtInTypeSpec = #(#[TYPE,"TYPE"], #builtInTypeSpec);
			}
		}
	;

// A type name. which is either a (possibly qualified and parameterized)
// class name or a primitive (builtin) type
type
	:	classOrInterfaceType[false]
	|	builtInType
	;

// The primitive types.
builtInType
	:	"void"
	|	"boolean"
	|	"byte"
	|	"char"
	|	"short"
	|	"int"
	|	"float"
	|	"long"
	|	"double"
	;

// A (possibly-qualified) java identifier.  We start with the first IDENT
//   and expand its name by adding dots and following IDENTS
identifier
	:	IDENT  ( DOT^ IDENT )*
	;

identifierStar
	:	IDENT
		( DOT^ IDENT )*
		( DOT^ STAR  )?
	;

// A list of zero or more modifiers.  We could have used (modifier)* in
//   place of a call to modifiers, but I thought it was a good idea to keep
//   this rule separate so they can easily be collected in a Vector if
//   someone so desires
modifiers
	:
	    (
	        //hush warnings since the semantic check for "@interface" solves the non-determinism
	        options{generateAmbigWarnings=false;}:

	        modifier
	        |
	        //Semantic check that we aren't matching @interface as this is not an annotation
	        //A nicer way to do this would be nice
	        {LA(1)==AT && !LT(2).getText().equals("interface")}? annotation
	    )*

		{#modifiers = #([MODIFIERS, "MODIFIERS"], #modifiers);}
	;

// modifiers for Java classes, interfaces, class/instance vars and methods
modifier
	:	"private"
	|	"public"
	|	"protected"
	|	"static"
	|	"transient"
	|	"final"
	|	"abstract"
	|	"native"
//	|	"threadsafe"
	|	"synchronized"
//	|	"const"			// reserved word, but not valid
	|	"volatile"
	|	"strictfp"
	;

annotation!
    :   AT! i:identifier ( LPAREN! ( args:annotationArguments )? RPAREN! )?
        {#annotation = #(#[ANNOTATION,"ANNOTATION"], i, args);}
    ;

annotations
    :   (annotation)*
    ;

annotationArguments
    :   annotationMemberValueInitializer | anntotationMemberValuePairs
    ;

anntotationMemberValuePairs
    :   annotationMemberValuePair ( COMMA! annotationMemberValuePair )*
    ;

annotationMemberValuePair!
    :   i:IDENT ASSIGN! v:annotationMemberValueInitializer
        {#annotationMemberValuePair = #(#[ANNOTATION_MEMBER_VALUE_PAIR,"ANNOTATION_MEMBER_VALUE_PAIR"], i, v);}
    ;

annotationMemberValueInitializer
    :
        conditionalExpression | annotation | annotationMemberArrayInitializer
    ;

// This is an initializer used to set up an annotation member array.
annotationMemberArrayInitializer
	:	lc:LCURLY^ {#lc.setType(ANNOTATION_ARRAY_INIT);}
			(	annotationMemberArrayValueInitializer
				(
					// CONFLICT: does a COMMA after an initializer start a new
					//           initializer or start the option ',' at end?
					//           ANTLR generates proper code by matching
					//			 the comma as soon as possible.
					options {
						warnWhenFollowAmbig = false;
					}
				:
					COMMA! annotationMemberArrayValueInitializer
				)*
				(COMMA!)?
			)?
		RCURLY!
	;

// The two things that can initialize an annotation array element are a conditional expression
//   and an annotation (nested annotation array initialisers are not valid)
annotationMemberArrayValueInitializer
	:	conditionalExpression
	|	annotation
	;

// Definition of a Java class
classDefinition![AST modifiers]
	:	c:"class" IDENT
		// it _might_ have type paramaters
		(tp:typeParameters)?
		// it _might_ have a superclass...
		sc:superClassClause
		// it might implement some interfaces...
		ic:implementsClause
		// now parse the body of the class
		cb:classBlock
		{
			#classDefinition = #(#[CLASS_DEF,"CLASS_DEF"],
							   modifiers,IDENT,tp,sc,ic,cb);
	    	addImportantToken(#classDefinition, c);
	    	setEndAst(#classDefinition, #cb);
	    }
	;

superClassClause!
	:	( e:"extends" c:classOrInterfaceType[false] )?
		{#superClassClause = #(#[EXTENDS_CLAUSE,"EXTENDS_CLAUSE"],c);
	     addImportantToken(#superClassClause, e);   }
	;

// Definition of a Java Interface
interfaceDefinition![AST modifiers]
	:	i:"interface" IDENT
        // it _might_ have type paramaters
        (tp:typeParameters)?
		// it might extend some other interfaces
		ie:interfaceExtends
		// now parse the body of the interface (looks like a class...)
		cb:classBlock
		{#interfaceDefinition = #(#[INTERFACE_DEF,"INTERFACE_DEF"],
									modifiers,IDENT,tp,ie,cb);
         addImportantToken(#interfaceDefinition, i);   }
	;

enumDefinition![AST modifiers]
	:	"enum" IDENT
		// it might implement some interfaces...
		ic:implementsClause
		// now parse the body of the enum
		eb:enumBlock
		{#enumDefinition = #(#[ENUM_DEF,"ENUM_DEF"],
							   modifiers,IDENT,ic,eb);}
    ;

annotationDefinition![AST modifiers]
	:	AT "interface" IDENT
		// now parse the body of the annotation
		ab:annotationBlock
		{#annotationDefinition = #(#[ANNOTATION_DEF,"ANNOTATION_DEF"],
							        modifiers,IDENT,ab);}
    ;

typeParameters
{int currentLtLevel = 0;}
    :
        {currentLtLevel = ltCounter;}
        lt:LT! {ltCounter++;}
        lastTp:typeParameter (COMMA! tp:typeParameter {#lastTp = #tp;})*
        (tae:typeArgumentsEnd!)?

		{
			if (#typeParameters instanceof LocatableAST)
				((LocatableAST) #typeParameters).addImportantToken(mostRecentTypeEnd);
		}
		
        // There should be only one '>' to reconcile - the enclosing
        // '>' for the type parameter. Any other adjacent '>' seen should
        // have been reconciled with type arguments for the last type parameter
        // hence we can assert here that there is but one unaccounted '>'.
        {gtToReconcile==1}?
        //And then there were none..
        {gtToReconcile=0;}
        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?
        {
			// DM: in BlueJ we introduce a new node type to vastly
			// simplify the representation
        	#typeParameters = #(#[TYPE_PARAMETERS,"TYPE_PARAMETERS"], #typeParameters);
        	addImportantToken(#typeParameters, lt);
        	setEndToken(#typeParameters, mostRecentTypeEnd);
        }
    ;

typeParameter
    :
        // I'm pretty sure Antlr generates the right thing here:
        (id:IDENT^) ( options{generateAmbigWarnings=false;}: typeParameterBounds )?
		// {#typeParameter = #(#[TYPE_PARAMETER,"TYPE_PARAMETER"], #typeParameter);}
    ;

typeParameterBounds
    :
        "extends"! classOrInterfaceType[true]
        (BAND! classOrInterfaceType[true])*
        {#typeParameterBounds = #(#[TYPE_UPPER_BOUNDS,"TYPE_UPPER_BOUNDS"], #typeParameterBounds);}
    ;

// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
classBlock
	:	lc:LCURLY!
			( field | SEMI! )*
		rc:RCURLY!
		{
			#classBlock = #([OBJBLOCK, "OBJBLOCK"], #classBlock);
		 	addImportantToken(#classBlock, lc);
		 	addImportantToken(#classBlock, rc);
		 	setEndToken(#classBlock, rc);
		}
	;

// This is the body of an annotation. You can have annotation fields and extra semicolons,
// That's about it (until you see what an annoation field is...)
annotationBlock
	:	LCURLY!
	    ( annotationField | SEMI! )*
		RCURLY!
		{#annotationBlock = #([OBJBLOCK, "OBJBLOCK"], #annotationBlock);}
	;

// An annotation field
annotationField!
    :   mods:modifiers
		(	cd:classDefinition[#mods]       // inner class
			{#annotationField = #cd;}

		|	id:interfaceDefinition[#mods]   // inner interface
			{#annotationField = #id;}

		|	ed:enumDefinition[#mods]        // inner enum
			{#annotationField = #id;}

		|	ad:annotationDefinition[#mods]   // inner annotation
			{#annotationField = #ad;}

		|   t:typeSpec[false]               // annotation field
			(	i:IDENT  // the name of the field

				LPAREN! RPAREN!

				rt:declaratorBrackets[#t]

                ( "default" amvi:annotationMemberValueInitializer )?

				SEMI

				{#annotationField =
				    #(#[ANNOTATION_FIELD_DEF,"ANNOTATION_FIELD_DEF"],
                         mods,
                         #(#[TYPE,"TYPE"],rt),
                         i,amvi
                         );}
			|	v:variableDefinitions[#mods,#t] SEMI    // variable
				{#annotationField = #v;}
			)
		)
    ;

// This is the body of an enum. You can have zero or more enum constants
// followed by any number of fields like a regular class
enumBlock
	:	LCURLY!
	        ( enumConstant ( options{greedy=true;}: COMMA! enumConstant )* ( COMMA! )? )?
	        ( SEMI! ( field | SEMI! )* )?
		RCURLY!
		{#enumBlock = #([OBJBLOCK, "OBJBLOCK"], #enumBlock);}
	;

//An enum constant may have optional parameters and may have a
//a body
enumConstant!
    :   an:annotations
        i:IDENT
        (	LPAREN!
            a:argList
            RPAREN!
        )?
        ( b:enumConstantBlock )?
        {#enumConstant = #([ENUM_CONSTANT_DEF, "ENUM_CONSTANT_DEF"], an, i, a, b);}
    ;

//The class-like body of an enum constant
enumConstantBlock
    :   LCURLY!
        ( enumConstantField | SEMI! )*
        RCURLY!
        {#enumConstantBlock = #([OBJBLOCK, "OBJBLOCK"], #enumConstantBlock);}
    ;

//An enum constant field is just like a class field but without
//the posibility of a constructor definition or a static initializer
enumConstantField!
    :   mods:modifiers
		(	cd:classDefinition[#mods]       // inner class
			{#enumConstantField = #cd;}

		|	id:interfaceDefinition[#mods]   // inner interface
			{#enumConstantField = #id;}

		|	ed:enumDefinition[#mods]        // inner enum
			{#enumConstantField = #id;}

		|	ad:annotationDefinition[#mods]   // inner annotation
			{#enumConstantField = #ad;}

		|	// A generic method has the typeParameters before the return type.
            // This is not allowed for variable definitions, but this production
            // allows it, a semantic check could be used if you wanted.
            (tp:typeParameters)? t:typeSpec[false]  // method or variable declaration(s)
			(	IDENT  // the name of the method

				// parse the formal parameter declarations.
				LPAREN! param:parameterDeclarationList RPAREN!

				rt:declaratorBrackets[#t]

				// get the list of exceptions that this method is
				// declared to throw
				(tc:throwsClause)?

				( s2:compoundStatement | SEMI )
				{#enumConstantField = #(#[METHOD_DEF,"METHOD_DEF"],
						     mods,
						     tp,
							 #(#[TYPE,"TYPE"],rt),
							 IDENT,
							 param,
							 tc,
							 s2);}
			|	v:variableDefinitions[#mods,#t] SEMI
				{#enumConstantField = #v;}
			)
		)

    // "{ ... }" instance initializer
	|	s4:compoundStatement
		{#enumConstantField = #(#[INSTANCE_INIT,"INSTANCE_INIT"], s4);}
	;

// An interface can extend several other interfaces...
interfaceExtends
	:	(
		e:"extends"!
		classOrInterfaceType[false] ( COMMA! classOrInterfaceType[false] )*
		)?
		{#interfaceExtends = #(#[EXTENDS_CLAUSE,"EXTENDS_CLAUSE"],
							#interfaceExtends);
	     addImportantToken(#interfaceExtends, e);  }
	;

// A class can implement several interfaces...
implementsClause
	:	(
			imp:"implements"! classOrInterfaceType[false] ( c:COMMA! i:classOrInterfaceType[false] { addImportantToken(#i, c); } )*
		)?
		{#implementsClause = #(#[IMPLEMENTS_CLAUSE,"IMPLEMENTS_CLAUSE"],
								 #implementsClause);
	     addImportantToken(#implementsClause, imp);  }
	;

// Now the various things that can be defined inside a class or interface...
// Note that not all of these are really valid in an interface (constructors,
//   for example), and if this grammar were used for a compiler there would
//   need to be some semantic checks to make sure we're doing the right thing...
field!
	:	// method, constructor, or variable declaration
		mods:modifiers
		(	cd:classDefinition[#mods]       // inner class
			{#field = #cd;}

		|	id:interfaceDefinition[#mods]   // inner interface
			{#field = #id;}

		|	ed:enumDefinition[#mods]        // inner enum
			{#field = #ed;}

		|	ad:annotationDefinition[#mods]   // inner annotation
			{#field = #ad;}

        |   (tp:typeParameters)?
            (
                h:ctorHead s:constructorBody // constructor
                {#field = #(#[CTOR_DEF,"CTOR_DEF"], mods, tp, h, s);}

                |
            
            // A generic method/ctor has the typeParameters before the return type.
            // This is not allowed for variable definitions, but this production
            // allows it, a semantic check could be used if you wanted.
                
                t:typeSpec[false]  // method or variable declaration(s)
				(
					IDENT  // the name of the method

					// parse the formal parameter declarations.
					LPAREN! param:parameterDeclarationList RPAREN!

					rt:declaratorBrackets[#t]

					// get the list of exceptions that this method is
					// declared to throw
					(tc:throwsClause)?

					( s2:compoundStatement | SEMI )
					{#field = #(#[METHOD_DEF,"METHOD_DEF"],
						     mods,
						     tp,
							 #(#[TYPE,"TYPE"],rt),
							 IDENT,
							 param,
							 tc,
							 s2);}
				|	v:variableDefinitions[#mods,#t] se:SEMI
					{#field = #v;
                 	addImportantToken(#field, se);  }
				)
			)
		)

    // "static { ... }" class initializer
	|	"static" s3:compoundStatement
		{#field = #(#[STATIC_INIT,"STATIC_INIT"], s3);}

    // "{ ... }" instance initializer
	|	s4:compoundStatement
		{#field = #(#[INSTANCE_INIT,"INSTANCE_INIT"], s4);}
	;

constructorBody
    :   lc:LCURLY^ {#lc.setType(SLIST);}
            ( options { greedy=true; } : explicitConstructorInvocation)?
            (statement)*
        RCURLY!
    ;

/** Catch obvious constructor calls, but not the expr.super(...) calls */
explicitConstructorInvocation
    :   "this"! lp1:LPAREN^ argList RPAREN! SEMI!
		{#lp1.setType(CTOR_CALL);}
    |   "super"! lp2:LPAREN^ argList RPAREN! SEMI!
		{#lp2.setType(SUPER_CTOR_CALL);}
    ;

variableDefinitions[AST mods, AST t] { AST lastVarDeclarator = null; }
	:	vd:variableDeclarator[getASTFactory().dupTree(mods),
						   getASTFactory().dupList(t)]
			{
				lastVarDeclarator = #vd;
			}
		(	comma:COMMA! { setEndToken(lastVarDeclarator, comma); }
			vdn:variableDeclarator[getASTFactory().dupTree(mods),
							   getASTFactory().dupTree(t)] { lastVarDeclarator = #vdn; }
		)*
	;

/** Declaration of a variable.  This can be a class/instance variable,
 *   or a local variable in a method
 * It can also include possible initialization.
 */
variableDeclarator![AST mods, AST t]
	:	id:IDENT d:declaratorBrackets[t] v:varInitializer
		{#variableDeclarator = #(#[VARIABLE_DEF,"VARIABLE_DEF"], mods, #(#[TYPE,"TYPE"],d), id, v);}
	;

declaratorBrackets[AST typ]
	:	{#declaratorBrackets=typ;}
		(lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!)*
	;

varInitializer
	:	( ASSIGN^ initializer )?
	;

// This is an initializer used to set up an array.
arrayInitializer
	:	lc:LCURLY^ {#lc.setType(ARRAY_INIT);}
			(	initializer
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
				(COMMA!)?
			)?
		RCURLY!
	;


// The two "things" that can initialize an array element are an expression
//   and another (nested) array initializer.
initializer
	:	expression
	|	arrayInitializer
	;

// This is the header of a method.  It includes the name and parameters
//   for the method.
//   This also watches for a list of exception classes in a "throws" clause.
ctorHead
	:	IDENT  // the name of the method

		// parse the formal parameter declarations.
		LPAREN! parameterDeclarationList RPAREN!

		// get the list of exceptions that this method is declared to throw
		(throwsClause)?
	;

// This is a list of exception classes that the method is declared to throw
throwsClause
	:	"throws"^ identifier ( COMMA! identifier )*
	;

// A list of formal parameters
//     Zero or more parameters
//     If a parameter is variable length (e.g. String... myArg) it is the right-most parameter
parameterDeclarationList
    // The semantic check in ( .... )* block is flagged as superfluous, and seems superfluous but
    // is the only way I could make this work. If my understanding is correct this is a known bug
    :   (   ( parameterDeclaration )=> parameterDeclaration
            ( options {warnWhenFollowAmbig=false;} : ( COMMA! parameterDeclaration ) => COMMA! parameterDeclaration )*
            ( COMMA! variableLengthParameterDeclaration )?
        |
            variableLengthParameterDeclaration
        )?
		{#parameterDeclarationList = #(#[PARAMETERS,"PARAMETERS"],
									#parameterDeclarationList);}
	;

// A formal parameter.
parameterDeclaration!
	:	pm:parameterModifier t:typeSpec[false] id:IDENT
		pd:declaratorBrackets[#t]
		{#parameterDeclaration = #(#[PARAMETER_DEF,"PARAMETER_DEF"],
									pm, #([TYPE,"TYPE"],pd), id);}
	;

variableLengthParameterDeclaration!
    {
        boolean isVariable = false;
    }
	:	pm:parameterModifier t:typeSpec[false] TRIPLE_DOT! id:IDENT
		pd:declaratorBrackets[#t]
		{#variableLengthParameterDeclaration = #(#[VARIABLE_PARAMETER_DEF,"VARIABLE_PARAMETER_DEF"],
                                                pm, #([TYPE,"TYPE"],pd), id);}
    ;

parameterModifier
    //final can appear amongst annotations in any order - greedily consume any preceding
    //annotations to shut nond-eterminism warnings off
	:	(options{greedy=true;} : annotation)* (f:"final")? (annotation)*
		{#parameterModifier = #(#[MODIFIERS,"MODIFIERS"], #parameterModifier);}
	;

// Compound statement.  This is used in many contexts:
//   Inside a class definition prefixed with "static":
//      it is a class initializer
//   Inside a class definition without "static":
//      it is an instance initializer
//   As the body of a method
//   As a completely indepdent braced block of code inside a method
//      it starts a new scope for variable definitions

compoundStatement
	:	lc:LCURLY^ {#lc.setType(SLIST);}
			// include the (possibly-empty) list of statements
			(statement)*
		rc:RCURLY!
        {
		     addImportantToken(#lc, rc);
		     setEndToken(#lc, rc);
        }
	;


// A single statement. The location of the exact end of the statement is stored
// (if the AST is a LocatableAST). In most cases the statement ends with the ';'
// character, but sometimes it is a closing curly brace ('}'), and in the case
// of a multiple variable declaration it can be a comma.
//
// For instance "int i, j, k;" is broken up into three statements:
//     "int i,"
//     "j,"
//     "k;"
statement
	// A list of statements in curly braces -- start a new scope!
	:	compoundStatement

	// declarations are ambiguous with "ID DOT" relative to expression
	// statements.  Must backtrack to be sure.  Could use a semantic
	// predicate to test symbol table to see what the type was coming
	// up, but that's pretty hard without a symbol table ;)
	|	(declaration)=> decl:declaration semi:SEMI!
		{
			AST nn = #decl;
			AST ns = nn.getNextSibling();
			while (ns != null) {
				nn = ns;
				ns = nn.getNextSibling();
			}
			setEndToken(nn, semi);
		}

	// An expression statement.  This could be a method call,
	// assignment statement, or any other expression evaluated for
	// side-effects.
	|	expr:expression semi2:SEMI! { setEndToken(#expr, semi2); }

	// class definition
	|	m:modifiers! classDefinition[#m]

	// Attach a label to the front of a statement
	|	IDENT c:COLON^ {#c.setType(LABELED_STAT);} stmt:statement { setEndAst(#c, #stmt); }

	// If-else statement
	|	ifstmt:"if"^ LPAREN! expression RPAREN! stmt2:statement { setEndAst(#ifstmt, #stmt2); }
		(
			// CONFLICT: the old "dangling-else" problem...
			//           ANTLR generates proper code matching
			//			 as soon as possible.  Hush warning.
			options {
				warnWhenFollowAmbig = false;
			}
		:
			"else"! stmt3:statement { setEndAst(#ifstmt, #stmt3); }
		)?

	// For statement
	|	forStatement

	// While statement
	|	wstmt:"while"^ LPAREN! expression RPAREN! stmt4:statement
	{
		setEndAst(#wstmt, #stmt4);
	}

	// do-while statement
	|	dostmt:"do"^ statement "while"! LPAREN! expression RPAREN! semi3:SEMI!
	{
		setEndToken(#dostmt, semi3);
	}

	// get out of a loop (or switch)
	|	brstmt:"break"^ (IDENT)? semi4:SEMI!  { setEndToken(#brstmt, semi4); }

	// do next iteration of a loop
	|	contstmt:"continue"^ (IDENT)? semi5:SEMI!  { setEndToken(#contstmt, semi5); }

	// Return an expression
	|	retstmt:"return"^ (expression)? semi6:SEMI!  { setEndToken(#retstmt, semi6); }

	// switch/case statement
	|	swstmt:"switch"^ LPAREN! expression RPAREN! LCURLY!
			( casesGroup )*
		rcurly:RCURLY!
		{
			setEndToken(#swstmt, rcurly);
		}

	// exception try-catch block
	|	tryBlock

	// throw an exception
	|	throwkw:"throw"^ expression semi7:SEMI!  { setEndToken(#throwkw, semi7); }

	// synchronize a statement
	|	synckw:"synchronized"^ LPAREN! expression RPAREN! stmt5:compoundStatement
		{
			setEndAst(#synckw, #stmt5);
		}

	// asserts (uncomment if you want 1.4 compatibility)
	|	assertkw:"assert"^ expression ( COLON! expression )? semi8:SEMI!
		{
			setEndToken(#assertkw, semi8);
		}

	// empty statement
	|	s:SEMI { #s.setType(EMPTY_STAT); setEndToken(#s,s); }
	;

forStatement
    :   f:"for"^
        LPAREN!
            ( (forInit SEMI)=>traditionalForStatement {#f.setType(FOR);}
              |
              forEachStatement {#f.setType(FOR_EACH);})
        RPAREN!
        stmt:statement { setEndAst(#f, #stmt); }   // statement to loop over
    ;

traditionalForStatement
    :
        forInit SEMI!   // initializer
        forCond SEMI!   // condition test
        forIter         // updater
    ;

forEachStatement
    :
        p:parameterDeclaration COLON! expression
    ;

casesGroup
	:	(	// CONFLICT: to which case group do the statements bind?
			//           ANTLR generates proper code: it groups the
			//           many "case"/"default" labels together then
			//           follows them with the statements
			options {
				greedy = true;
			}
			:
			aCase
		)+
		caseSList
		{#casesGroup = #([CASE_GROUP, "CASE_GROUP"], #casesGroup);}
	;

aCase
	:	("case"^ expression | "default") COLON!
	;

caseSList
	:	(statement)*
		{#caseSList = #(#[SLIST,"SLIST"],#caseSList);}
	;

// The initializer for a for loop
forInit
		// if it looks like a declaration, it is
	:	((declaration)=> declaration
		// otherwise it could be an expression list...
		|	expressionList
		)?
		{#forInit = #(#[FOR_INIT,"FOR_INIT"],#forInit);}
	;

forCond
	:	(expression)?
		{#forCond = #(#[FOR_CONDITION,"FOR_CONDITION"],#forCond);}
	;

forIter
	:	(expressionList)?
		{#forIter = #(#[FOR_ITERATOR,"FOR_ITERATOR"],#forIter);}
	;

// an exception handler try/catch block
tryBlock
	:	trykw:"try"^ compoundStatement
		(hndlr:handler { setEndAst(#trykw, #hndlr); } )*
		( fclause:finallyClause { setEndAst(#trykw, #fclause); } )?
	;

finallyClause
	:	fc:"finally"^ cs:compoundStatement  { setEndAst(#fc, #cs); }
	;

// an exception handler
handler
	:	catchkw:"catch"^ LPAREN! parameterDeclaration RPAREN! cs:compoundStatement
		{
			setEndAst(#catchkw, #cs);
		}
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
	:	assignmentExpression
		{#expression = #(#[EXPR,"EXPR"],#expression);}
	;


// This is a list of expressions.
expressionList
	:	expression (COMMA! expression)*
		{#expressionList = #(#[ELIST,"ELIST"], expressionList);}
	;


// assignment expression (level 13)
assignmentExpression
	:	conditionalExpression
		(	(	ASSIGN^
            |   PLUS_ASSIGN^
            |   MINUS_ASSIGN^
            |   STAR_ASSIGN^
            |   DIV_ASSIGN^
            |   MOD_ASSIGN^
            |   SR_ASSIGN^
            |   BSR_ASSIGN^
            |   SL_ASSIGN^
            |   BAND_ASSIGN^
            |   BXOR_ASSIGN^
            |   BOR_ASSIGN^
            )
			assignmentExpression
		)?
	;


// conditional test (level 12)
conditionalExpression
	:	logicalOrExpression
		( QUESTION^ assignmentExpression COLON! conditionalExpression )?
	;


// logical or (||)  (level 11)
logicalOrExpression
	:	logicalAndExpression (LOR^ logicalAndExpression)*
	;


// logical and (&&)  (level 10)
logicalAndExpression
	:	inclusiveOrExpression (LAND^ inclusiveOrExpression)*
	;


// bitwise or non-short-circuiting or (|)  (level 9)
inclusiveOrExpression
	:	exclusiveOrExpression (BOR^ exclusiveOrExpression)*
	;


// exclusive or (^)  (level 8)
exclusiveOrExpression
	:	andExpression (BXOR^ andExpression)*
	;


// bitwise or non-short-circuiting and (&)  (level 7)
andExpression
	:	equalityExpression (BAND^ equalityExpression)*
	;


// equality/inequality (==/!=) (level 6)
equalityExpression
	:	relationalExpression ((NOT_EQUAL^ | EQUAL^) relationalExpression)*
	;


// boolean relational expressions (level 5)
relationalExpression
	:	shiftExpression
		(	(	(	LT^
				|	GT^
				|	LE^
				|	GE^
				)
				shiftExpression
			)*
		|	"instanceof"^ typeSpec[true]
		)
	;


// bit shift expressions (level 4)
shiftExpression
	:	additiveExpression ((SL^ | SR^ | BSR^) additiveExpression)*
	;


// binary addition/subtraction (level 3)
additiveExpression
	:	multiplicativeExpression ((PLUS^ | MINUS^) multiplicativeExpression)*
	;


// multiplication/division/modulo (level 2)
multiplicativeExpression
	:	unaryExpression ((STAR^ | DIV^ | MOD^ ) unaryExpression)*
	;

unaryExpression
	:	INC^ unaryExpression
	|	DEC^ unaryExpression
	|	MINUS^ {#MINUS.setType(UNARY_MINUS);} unaryExpression
	|	PLUS^  {#PLUS.setType(UNARY_PLUS);} unaryExpression
	|	unaryExpressionNotPlusMinus
	;

unaryExpressionNotPlusMinus
	:	BNOT^ unaryExpression
	|	LNOT^ unaryExpression
	|	(	// subrule allows option to shut off warnings
			options {
				// "(int" ambig with postfixExpr due to lack of sequence
				// info in linear approximate LL(k).  It's ok.  Shut up.
				generateAmbigWarnings=false;
			}
		:	// If typecast is built in type, must be numeric operand
			// Have to backtrack to see if operator follows
		(LPAREN builtInTypeSpec[true] RPAREN unaryExpression)=>
        lpb:LPAREN^ {#lpb.setType(TYPECAST);} builtInTypeSpec[true] RPAREN!
        unaryExpression

        // Have to backtrack to see if operator follows.  If no operator
        // follows, it's a typecast.  No semantic checking needed to parse.
        // if it _looks_ like a cast, it _is_ a cast; else it's a "(expr)"
    |	(LPAREN classTypeSpec[true] RPAREN unaryExpressionNotPlusMinus)=>
        lp:LPAREN^ {#lp.setType(TYPECAST);} classTypeSpec[true] RPAREN!
        unaryExpressionNotPlusMinus

    |	postfixExpression
    )
	;

// qualified names, array expressions, method invocation, post inc/dec
postfixExpression
	:
    /*
    "this"! lp1:LPAREN^ argList RPAREN!
		{#lp1.setType(CTOR_CALL);}

    |   "super"! lp2:LPAREN^ argList RPAREN!
		{#lp2.setType(SUPER_CTOR_CALL);}
    |
    */
        primaryExpression

		(
            /*
            options {
				// the use of postfixExpression in SUPER_CTOR_CALL adds DOT
				// to the lookahead set, and gives loads of false non-det
				// warnings.
				// shut them off.
				generateAmbigWarnings=false;
			}
		:	*/
		    //type arguments are only appropriate for a parameterized method
		    //semantic check may be needed here to ensure this
            DOT^ (typeArguments[false])? IDENT
			(	lp:LPAREN^ {#lp.setType(METHOD_CALL);}
				argList
				RPAREN!
			)?
		|	DOT^ "this"

		|	DOT^ "super"
            (   // (new Outer()).super()  (create enclosing instance)
                lp3:LPAREN^ argList RPAREN!
                {#lp3.setType(SUPER_CTOR_CALL);}
			|   DOT^ (typeArguments[false])? IDENT
                (	lps:LPAREN^ {#lps.setType(METHOD_CALL);}
                    argList
                    RPAREN!
                )?
            )
		|	DOT^ newExpression
		|	lb:LBRACK^ {#lb.setType(INDEX_OP);} expression RBRACK!
		)*

		(   // possibly add on a post-increment or post-decrement.
            // allows INC/DEC on too much, but semantics can check
			in:INC^ {#in.setType(POST_INC);}
	 	|	de:DEC^ {#de.setType(POST_DEC);}
		)?
 	;

// the basic element of an expression
primaryExpression
	:	identPrimary ( options {greedy=true;} : DOT^ "class" )?
    |   constant
	|	"true"
	|	"false"
	|	"null"
    |   newExpression
	|	"this"
	|	"super"
	|	LPAREN! assignmentExpression RPAREN!
		// look for int.class and int[].class
	|	builtInType
		( lbt:LBRACK^ {#lbt.setType(ARRAY_DECLARATOR);} RBRACK! )*
		DOT^ "class"
	;

/** Match a, a.b.c refs, a.b.c(...) refs, a.b.c[], a.b.c[].class,
 *  and a.b.c.class refs.  Also this(...) and super(...).  Match
 *  this or super.
 */
identPrimary
	:	IDENT
		(
            options {
				// .ident could match here or in postfixExpression.
				// We do want to match here.  Turn off warning.
				greedy=true;
			}
		:	DOT^ IDENT
		)*
		(
            options {
				// ARRAY_DECLARATOR here conflicts with INDEX_OP in
				// postfixExpression on LBRACK RBRACK.
				// We want to match [] here, so greedy.  This overcomes
                // limitation of linear approximate lookahead.
				greedy=true;
		    }
		:   ( lp:LPAREN^ {#lp.setType(METHOD_CALL);} argList RPAREN! )
		|	( options {greedy=true;} :
              lbc:LBRACK^ {#lbc.setType(ARRAY_DECLARATOR);} RBRACK!
            )+
		)?
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
newExpression
	:	"new"^ type
		(	LPAREN! argList RPAREN! (classBlock)?

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

argList
	:	(	expressionList
		|	/*nothing*/
			{#argList = #[ELIST,"ELIST"];}
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
			lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);}
				(expression)?
			RBRACK!
		)+
	;

constant
	:	NUM_INT
	|	CHAR_LITERAL
	|	STRING_LITERAL
	|	NUM_FLOAT
	|	NUM_LONG
	|	NUM_DOUBLE
	;


//----------------------------------------------------------------------------
// The Java scanner
//----------------------------------------------------------------------------
class JavaLexer extends Lexer;

options {
	exportVocab=Java;      // call the vocabulary "Java"
	testLiterals=false;    // don't automatically test for literals
    defaultErrorHandler=false;
	k=4;                   // four characters of lookahead
	charVocabulary='\u0000'..'\uFFFF';
	// without inlining some bitset tests, couldn't do unicode;
	// I need to make ANTLR generate smaller bitsets; see
	// bottom of JavaLexer.java
	codeGenBitsetTestThreshold=20;
}



// OPERATORS
QUESTION		:	'?'		;
LPAREN			:	'('		;
RPAREN			:	')'		;
LBRACK			:	'['		;
RBRACK			:	']'		;
LCURLY			:	'{'		;
RCURLY			:	'}'		;
COLON			:	':'		;
COMMA			:	','		;
//DOT  			:	'.'		;
ASSIGN			:	'='		;
EQUAL			:	"=="	;
LNOT			:	'!'		;
BNOT			:	'~'		;
NOT_EQUAL		:	"!="	;
DIV				:	'/'		;
DIV_ASSIGN		:	"/="	;
PLUS			:	'+'		;
PLUS_ASSIGN		:	"+="	;
INC				:	"++"	;
MINUS			:	'-'		;
MINUS_ASSIGN	:	"-="	;
DEC				:	"--"	;
STAR			:	'*'		;
STAR_ASSIGN		:	"*="	;
MOD				:	'%'		;
MOD_ASSIGN		:	"%="	;
SR				:	">>"	;
SR_ASSIGN		:	">>="	;
BSR				:	">>>"	;
BSR_ASSIGN		:	">>>="	;
GE				:	">="	;
GT				:	">"		;
SL				:	"<<"	;
SL_ASSIGN		:	"<<="	;
LE				:	"<="	;
LT				:	'<'		;
BXOR			:	'^'		;
BXOR_ASSIGN		:	"^="	;
BOR				:	'|'		;
BOR_ASSIGN		:	"|="	;
LOR				:	"||"	;
BAND			:	'&'		;
BAND_ASSIGN		:	"&="	;
LAND			:	"&&"	;
SEMI			:	';'		;


// Whitespace -- ignored
WS	:	(	' '
		|	'\t'
		|	'\f'
			// handle newlines
		|	(	options {generateAmbigWarnings=false;}
			:	"\r\n"  // Evil DOS
			|	'\r'    // Macintosh
			|	'\n'    // Unix (the right way)
			)
			{ newline(); }
		)+
		// { _ttype = Token.SKIP; }
	;

// Single-line comments
SL_COMMENT
	:	"//"
		(~('\n'|'\r'))* 
		{$setType(Token.SKIP);}
	;

// multiple-line comments
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
		|	'\n'			{newline();}
		|	~('*'|'\n'|'\r')
		)*
		"*/"
		// We could skip multiline comments in the lexer, but that
		// would prevent us from seeing javadoc comments. We can use
		// a TokenStream filter instead.
		//{$setType(Token.SKIP);}
	;


// character literals
CHAR_LITERAL
	:	'\'' ( ESC | ~('\''|'\n'|'\r'|'\\') ) '\''
	;

// string literals
STRING_LITERAL
	:	'"' (ESC|~('"'|'\\'|'\n'|'\r'))* '"'
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
	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
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
	:	('0'..'9'|'A'..'F'|'a'..'f')
	;


// a dummy rule to force vocabulary to be all characters (except special
//   ones that ANTLR uses internally (0 to 2)
//protected
//VOCAB
//	:	'\3'..'\377'
//	;


//protected
//IDENT_LETTER
//    :   ( '$' | 'A'..'Z' | '_' | 'a'..'z' |
//          '\u00c0'..'\u00d6' |
//          '\u00d8'..'\u00f6' |
//          '\u00f8'..'\u00ff' |
//          '\u0100'..'\u1fff' |
//          '\u3040'..'\u318f' |
//          '\u3300'..'\u337f' |
//          '\u3400'..'\u3d2d' |
//          '\u4e00'..'\u9fff' |
//          '\uf900'..'\ufaff' )
//    ;
    
// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT
	options {testLiterals=true; paraphrase = "an identifier";}
    :   ('a'..'z'|'A'..'Z'|'_'|'$'|'\u00c0'..'\ufffe')
        ('a'..'z'|'A'..'Z'|'_'|'$'|'\u00c0'..'\ufffe'|'0'..'9')*
	;

//TOKEN :
//{
// < IDENTIFIER: <LETTER> (<LETTER>|<DIGIT>)* >
//|
//  < #LETTER:
//       "\u0024",
//       "\u0041"-"\u005a",
//       "\u005f",
//       "\u0061"-"\u007a",
//       "\u00c0"-"\u00d6",
//       "\u00d8"-"\u00f6",
//       "\u00f8"-"\u00ff",
//       "\u0100"-"\u1fff",
//       "\u3040"-"\u318f",
//       "\u3300"-"\u337f",
//       "\u3400"-"\u3d2d",
//       "\u4e00"-"\u9fff",
//       "\uf900"-"\ufaff"
//|
 // < #DIGIT:
  //     "\u0030"-"\u0039",
//       "\u0660"-"\u0669",
//       "\u06f0"-"\u06f9",
//       "\u0966"-"\u096f",
//       "\u09e6"-"\u09ef",
//       "\u0a66"-"\u0a6f",
//       "\u0ae6"-"\u0aef",
//       "\u0b66"-"\u0b6f",
//       "\u0be7"-"\u0bef",
//       "\u0c66"-"\u0c6f",
//       "\u0ce6"-"\u0cef",
//       "\u0d66"-"\u0d6f",
//       "\u0e50"-"\u0e59",
//       "\u0ed0"-"\u0ed9",
//       "\u1040"-"\u1049"

// a numeric literal
NUM_INT
    options {paraphrase = "a number";}
	{boolean isDecimal=false; Token t=null; }
    :   '.' {_ttype = DOT;}
            (
                (('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
                {
				if (t != null && t.getText().toUpperCase().indexOf('F')>=0) {
                	_ttype = NUM_FLOAT;
				}
				else {
                	_ttype = NUM_DOUBLE; // assume double
				}
				})
				|
				// JDK 1.5 token for variable length arguments
				(".." {_ttype = TRIPLE_DOT;})
            )?

	|	(	'0' {isDecimal = true;} // special case for just '0'
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
		|	('1'..'9') ('0'..'9')*  {isDecimal=true;}		// non-zero decimal
		)
		(	('l'|'L') { _ttype = NUM_LONG; }

		// only check to see if it's a float if looks like decimal so far
		|	{isDecimal}?
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

// JDK 1.5 token for annotations and their declarations
AT
    :   '@'
    ;

// a couple protected methods to assist in matching floating point numbers
protected
EXPONENT
	:	('e'|'E') ('+'|'-')? ('0'..'9')+
	;


protected
FLOAT_SUFFIX
	:	'f'|'F'|'d'|'D'
	;
