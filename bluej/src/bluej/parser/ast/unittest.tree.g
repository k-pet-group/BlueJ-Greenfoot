header {
    package bluej.parser.ast;
    
    import bluej.parser.SourceSpan;
    import bluej.parser.SourceLocation;
    
    import java.util.*;
    import antlr.BaseAST;
}

/** 
 * Author: (see java.g preamble)
 * Author: Andrew Patterson
 *
 * This grammar takes a tree constructed using the standard antlr Java
 * tree creator, and uses it to perform operations needed for constructing
 * unit tests (i.e. identifying fields of a class, finding methods called 
 * testXXX etc).
 *
 * This parser requires that the nodes of the tree we work on are 
 * LocatableAST nodes.
 */
class UnitTestParser extends TreeParser;

options {
	importVocab = Java;
    buildAST = true;
}

{
	/**
	 * Given an OBJBLOCK (generally from a class definition), we extract
	 * a list of fields declared in the OBJBLOCK
	 * ie
	 *
	 * class FooBar {
	 *    private int a = 10;
	 *    java.util.HashMap h,i,j = null;
	 *    public String aString;
	 * }
	 * gives us a list with SourceSpan objects encompassing
	 *   p in private to ;
	 *   j in java to ;
	 *   p in public to ;
	 */
    public static List getVariableSelections(AST objBlock)
    {
        if (!(objBlock instanceof LocatableAST))
            throw new IllegalArgumentException("using unit test parser with wrong AST type");

		// we are creating a list of AST nodes
        LinkedList l = new LinkedList();

		// the first AST in this OBJBLOCK
        LocatableAST childAST = (LocatableAST) ((BaseAST)objBlock).getFirstChild();

        // the children in an object block are a list of variable definitions
        // and method definitions
        while(childAST != null) {
            // we are only interested in variable definitions
            if(childAST.getType() == UnitTestParserTokenTypes.VARIABLE_DEF) {
				// potentially VARIABLE_DEF could look like this
				// we need to find the first type token (in this case
				// "java") and the semicolon
				
				// ( VARIABLE_DEF ( . ( . java lang ) String ) ; )

            	// find the complete span of nodes for this variable definition
                LocatableAST startSibling = null, endSibling = null;
                
                startSibling = (LocatableAST) childAST.getFirstChild();
                if(startSibling != null) {
                	// the semicolon is always the sibling of the first child found
                    endSibling = (LocatableAST) startSibling.getNextSibling();
                    
                    // however, we need to keep going down with startSibling to find
                    // the left most token
					while (startSibling.getFirstChild() != null)
						startSibling = (LocatableAST) startSibling.getFirstChild();
				}
				                    
                if (startSibling != null && endSibling != null) {                    
					l.add(new SourceSpan(new SourceLocation(startSibling.getLine(),
					                                        startSibling.getColumn()),
					                     new SourceLocation(endSibling.getLine(),
					                                        endSibling.getColumn())));
                }
            }               
            childAST = (LocatableAST) childAST.getNextSibling();            
        }            
        return l;
    }

	/**
	 * Given an OBJBLOCK (generally from a class definition), we extract
	 * information about a unitTestSetup method in the OBJBLOCK
	 * ie
	 *
	 * class FooBar {
	 *    private int a = 10;
	 *    java.util.HashMap h = null;
	 *    public String aString;
	 * }
	 * gives us a list with AST nodes for  -> private , ; , java , ; , public , ; -<
	 */
    public static List getSetupMethodSelections(AST objBlock)
    {
        if (!(objBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        LinkedList l = new LinkedList();
        LocatableAST childAST = (LocatableAST) ((BaseAST)objBlock).getFirstChild();

        // the children on a class' object block are a list of variable definitions
        // and method definitions
        while(childAST != null) {
            // we are only interested in method definitions
            if(childAST.getType() == UnitTestParserTokenTypes.METHOD_DEF) {
                LocatableAST firstSib = null, secondSib = null, thirdSib = null;
                
                firstSib = (LocatableAST) childAST.getFirstChild();
                if(firstSib != null && firstSib.getText().equals("setUp"))
                    secondSib = (LocatableAST) firstSib.getNextSibling();
                else
                    continue;

                if (secondSib != null) {
                    thirdSib = (LocatableAST) secondSib.getNextSibling();
                }
                    
                if (secondSib != null && thirdSib != null) {                    
                    l.addFirst(thirdSib);
                    l.addFirst(secondSib);

                    return l;
                }
            }               
            childAST = (LocatableAST) childAST.getNextSibling();            
        }            
        return l;
    }

    public static LocatableAST getOpeningBracketSelection(AST classBlock)
    {
        if (!(classBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        return (LocatableAST) classBlock.getNextSibling();
    }

    public static LocatableAST getMethodInsertSelection(AST classBlock)
    {
        if (!(classBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        return (LocatableAST) classBlock.getNextSibling();
    }

    /**
     * Locate the comment token associated with a method/class.
     * We do this by looking at the hidden tokens associated with
     * both the modifiers (public, static etc) of the method/class
     * and then keyword associated (ie class for classes, the method
     * name for methods).
     */
    static antlr.CommonToken helpFindComment(AST mods, AST keyword)
    {
        if (mods != null && mods.getFirstChild() != null) {
		    antlr.CommonASTWithHiddenTokens caht = (antlr.CommonASTWithHiddenTokens) mods.getFirstChild();
            if(caht.getHiddenBefore() != null) {
                antlr.CommonHiddenStreamToken chst = caht.getHiddenBefore();
                return chst;
            }
        }

	    antlr.CommonASTWithHiddenTokens caht = (antlr.CommonASTWithHiddenTokens) keyword.getFirstChild();
        if(caht != null && caht.getHiddenBefore() != null) {
            antlr.CommonHiddenStreamToken chst = caht.getHiddenBefore();
            return chst;
        }
        
        return null;       
    }

    /**
     * Return the first AST we can find out of the modifiers and type
     * definition of a method or variable declaration.
     */
    static AST findFirstChild(AST mods, AST type)
    {
        if (mods.getFirstChild() != null) {
            return mods.getFirstChild();
        } else if (type.getFirstChild() != null) {
            return type.getFirstChild();
        }
        else
            return null;       
    }
}

compilationUnit 
	:	(packageDefinition!)?
		(importDefinition!)*
		(typeDefinition)*
	;

packageDefinition
	:	#( PACKAGE_DEF identifier )
	;

importDefinition
	:	#( IMPORT identifierStar )
	;

typeDefinition!
	:	#(CLASS_DEF m:modifiers i:IDENT ec:extendsClause implementsClause ob:objBlock )
	    {
            Token commentToken = helpFindComment(m, i);
            AST comment;
            
            if (commentToken != null)
                comment = new LocatableAST(commentToken);
            else
                comment = null;

            LocatableAST lc = new LocatableAST(((LocatableAST) ob).getImportantToken(0));
            LocatableAST rc = new LocatableAST(((LocatableAST) ob).getImportantToken(1));
            
            #typeDefinition = #(i, lc, rc, m, ec ,ob, ([COMMENT_DEF, "COMMENT_DEF"], comment) ); 
	    }
	|	#(INTERFACE_DEF im:modifiers ii:IDENT iec:extendsClause ib:interfaceBlock )
	;

typeSpec
	:	#(TYPE typeSpecArray)
	;

typeSpecArray
	:	#( ARRAY_DECLARATOR typeSpecArray )
	|	type
	;

type:	identifier
	|	builtInType
	;

builtInType
    :   "void"
    |   "boolean"
    |   "byte"
    |   "char"
    |   "short"
    |   "int"
    |   "float"
    |   "long"
    |   "double"
    ;

modifiers
	:	#( MODIFIERS (modifier)* )
	;

modifier
    :   "private"
    |   "public"
    |   "protected"
    |   "static"
    |   "transient"
    |   "final"
    |   "abstract"
    |   "native"
    |   "threadsafe"
    |   "synchronized"
    |   "const"
    |   "volatile"
	|	"strictfp"
    ;

extendsClause
	:	#(EXTENDS_CLAUSE (id:identifier)* )
	;

implementsClause
	:	#(IMPLEMENTS_CLAUSE (identifier)* )
	;


interfaceBlock
	:	#(	OBJBLOCK
			(	methodDecl
			|	variableDef
			)*
		)
	;
	
objBlock
	:	#(	OBJBLOCK
			(!	ctorDef
			|	methodDef
			|	variableDef
			|!	typeDefinition
			|!  #(STATIC_INIT slist)
			|!	#(INSTANCE_INIT slist)
			)*
		)
	;

ctorDef
	:	#(CTOR_DEF modifiers methodHead ctorSList)
	;

methodDecl
	:	#(METHOD_DEF modifiers typeSpec methodHead)
	;

methodDef!
	:	#(METHOD_DEF m:modifiers t:typeSpec mh:methodHead
	        ( sl:slist
                {
            Token commentToken = helpFindComment(m, t);
            AST comment;
            
            if (commentToken != null)
                comment = new LocatableAST(commentToken);
            else
                comment = null;

                    LocatableAST bracketholder = (LocatableAST) sl;
                
                    if (bracketholder.getImportantTokenCount() > 0) {
                        AST bracket = new LocatableAST(bracketholder.getImportantToken(0));
                        #methodDef = #([METHOD_DEF,"METHOD_DEF"], mh, sl, bracket,
                                             ([COMMENT_DEF, "COMMENT_DEF"], comment) );
                    }
    	        }
	        )?
		)
	;

variableDef
	:	#(VARIABLE_DEF m:modifiers t:typeSpec variableDeclarator varInitializer)
	    {
            LocatableAST semiholder = (LocatableAST) #variableDef;
            
            if (semiholder.getImportantTokenCount() > 0) {
	            AST semi = new LocatableAST(semiholder.getImportantToken(0));
  
                #variableDef = #(VARIABLE_DEF, findFirstChild(m, t), semi); 
            }
            else {
                // note: we get here when there are multiple declarations of a
                // variable on a line ie int x=2,y=3;
                // will come here for the y=3 declaration. As this is encompassed
                // in the int x=2...; declaration we can throw this away.
                #variableDef = null; 
            }
	    }
	;

parameterDef
	:	#(PARAMETER_DEF modifiers typeSpec IDENT )
	;

objectinitializer
	:	#(INSTANCE_INIT slist)
	;

variableDeclarator!
	:	i:IDENT
	|	LBRACK variableDeclarator
	{
	    #variableDeclarator = #i;
	}
	;

varInitializer
	:	#(ASSIGN initializer)
	|
	;

initializer
	:	expression
	|	arrayInitializer
	;

arrayInitializer
	:	#(ARRAY_INIT (initializer)*)
	;

methodHead
	:	i:IDENT^ #(PARAMETERS (pd:parameterDef)* ) (throwsClause)?
/*        {
           #methodHead = #(i, methodHead);
        } */

	;

throwsClause
	:	#( "throws" (identifier)* )
	;

identifier
	:	IDENT
	|	#( DOT identifier IDENT )
	;

identifierStar
	:	IDENT
	|	#( DOT identifier (STAR|IDENT) )
	;

ctorSList
	:	#( SLIST (ctorCall)? (stat)* )
	;

slist
	:	#( SLIST (stat!)* )
	;

stat:	typeDefinition
	|	variableDef
	|	expression
	|	#(LABELED_STAT IDENT stat)
	|	#("if" expression stat (stat)? )
	|	#(	"for"
			#(FOR_INIT (variableDef | elist)?)
			#(FOR_CONDITION (expression)?)
			#(FOR_ITERATOR (elist)?)
			stat
		)
	|	#("while" expression stat)
	|	#("do" stat expression)
	|	#("break" (IDENT)? )
	|	#("continue" (IDENT)? )
	|	#("return" (expression)? )
	|	#("switch" expression (caseGroup)*)
	|	#("throw" expression)
	|	#("synchronized" expression stat)
	|	tryBlock
	|	slist // nested SLIST
	|	EMPTY_STAT
	;

caseGroup
	:	#(CASE_GROUP (#("case" expression) | "default")+ slist)
	;

tryBlock
	:	#( "try" slist (handler)* (#("finally" slist))? )
	;

handler
	:	#( "catch" parameterDef slist )
	;

elist
	:	#( ELIST (expression)* )
	;

expression
	:	#(EXPR expr)
	;

expr:	#(QUESTION expr expr expr)	// trinary operator
	|	#(ASSIGN expr expr)			// binary operators...
	|	#(PLUS_ASSIGN expr expr)
	|	#(MINUS_ASSIGN expr expr)
	|	#(STAR_ASSIGN expr expr)
	|	#(DIV_ASSIGN expr expr)
	|	#(MOD_ASSIGN expr expr)
	|	#(SR_ASSIGN expr expr)
	|	#(BSR_ASSIGN expr expr)
	|	#(SL_ASSIGN expr expr)
	|	#(BAND_ASSIGN expr expr)
	|	#(BXOR_ASSIGN expr expr)
	|	#(BOR_ASSIGN expr expr)
	|	#(LOR expr expr)
	|	#(LAND expr expr)
	|	#(BOR expr expr)
	|	#(BXOR expr expr)
	|	#(BAND expr expr)
	|	#(NOT_EQUAL expr expr)
	|	#(EQUAL expr expr)
	|	#(LT expr expr)
	|	#(GT expr expr)
	|	#(LE expr expr)
	|	#(GE expr expr)
	|	#(SL expr expr)
	|	#(SR expr expr)
	|	#(BSR expr expr)
	|	#(PLUS expr expr)
	|	#(MINUS expr expr)
	|	#(DIV expr expr)
	|	#(MOD expr expr)
	|	#(STAR expr expr)
	|	#(INC expr)
	|	#(DEC expr)
	|	#(POST_INC expr)
	|	#(POST_DEC expr)
	|	#(BNOT expr)
	|	#(LNOT expr)
	|	#("instanceof" expr expr)
	|	#(UNARY_MINUS expr)
	|	#(UNARY_PLUS expr)
	|	primaryExpression
	;

primaryExpression
    :   IDENT
    |   #(	DOT
			(	expr
				(	IDENT
				|	arrayIndex
				|	"this"
				|	"class"
				|	#( "new" IDENT elist )
				|   "super"
				)
			|	#(ARRAY_DECLARATOR typeSpecArray)
			|	builtInType ("class")?
			)
		)
	|	arrayIndex
	|	#(METHOD_CALL primaryExpression elist)
	|	#(TYPECAST typeSpec expr)
	|   newExpression
	|   constant
    |   "super"
    |   "true"
    |   "false"
    |   "this"
    |   "null"
	|	typeSpec // type name used with instanceof
	;

ctorCall
	:	#( CTOR_CALL elist )
	|	#( SUPER_CTOR_CALL
			(	elist
			|	primaryExpression elist
			)
		 )
	;

arrayIndex
	:	#(INDEX_OP primaryExpression expression)
	;

constant
    :   NUM_INT
    |   CHAR_LITERAL
    |   STRING_LITERAL
    |   NUM_FLOAT
    |   NUM_DOUBLE
    |   NUM_LONG
    ;

newExpression
	:	#(	"new" type
			(	newArrayDeclarator (arrayInitializer)?
			|	elist (objBlock)?
			)
		)
			
	;

newArrayDeclarator
	:	#( ARRAY_DECLARATOR (newArrayDeclarator)? (expression)? )
	;
