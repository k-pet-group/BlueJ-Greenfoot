header {
    package bluej.parser.ast;
}

/** Java 1.3 AST Recognizer Grammar
 *
 * Author: (see java.g preamble)
 *
 * This grammar is in the PUBLIC DOMAIN
 */
class UnitTestParser extends TreeParser;

options {
	importVocab = Java;
    buildAST = true;
}

{
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
        if(caht.getHiddenBefore() != null) {
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
