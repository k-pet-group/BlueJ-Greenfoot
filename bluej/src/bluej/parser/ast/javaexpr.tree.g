header {
    package bluej.parser.ast;

    import java.lang.reflect.*;
    import bluej.utility.JavaNames;
}

/**
 * Perform semantic checks on a Java expression
 */
class ExpressionSemanticParser extends TreeParser;

options {
	importVocab = Java;
    buildAST = true;
    ASTLabelType = "TypeInfoAST";
}

{

    public Class symbolLookupType(String name)
    {
        // try identifier by itself
        try {
            Class cl = Class.forName(name);
            return cl;
        } catch (ClassNotFoundException cnfe) { }

        // try current package

        // not implemented

        // try automatic import of java.lang classes
        // but only if the name is a simple name ie. no .
        if (name.indexOf(".") < 0) {
            try {
                Class cl = Class.forName("java.lang." + name);
                return cl;
            } catch (ClassNotFoundException cnfe) { }
        }
        return null;
//      throw new Error("compile error: unknown class " + name);
    } 

    public Class symbolLookupLocalType(Class parent, String name)
    {
        Class[] locals = parent.getClasses();
        
        if (locals != null) {
            for(int i=0; i<locals.length; i++) {
                if (!locals[i].getName().startsWith(parent.getName()))
                    continue; //throw new IllegalStateException("parent " + parent.getName() + " child " + locals[i].getName());

                String shortName = locals[i].getName().substring(parent.getName().length()+1);
                if (shortName.equals(name)) {
                    return locals[i];
                }
            }
        }
        return null;    
    }

    public Class symbolLookupLocalField(Class parent, String name)
    {
        try {
            Field f = parent.getField(name);
            return f.getType();
        }
        catch (Exception e) {
//            System.out.println(e);
        }
        return null;    
    }

    public Class symbolLookupLocalMethod(Class parent, String name)
    {
        try {
            Class args[] = new Class[] {};
            Method m = parent.getMethod(name,args);
            System.out.println(m);
            return m.getReturnType();
        }
        catch (Exception e) {
//            System.out.println(e);
        }  
        return null;    
    }  

        
    public Class symbolLookupVariable(String name)
    {
        if (name.equals("str"))
            return String.class;
        if (name.equals("v"))
            return java.util.Vector.class;    
        if (name.equals("i"))
            return int.class;
        if (name.equals("sys"))
            return System.class;
        if (name.equals("a"))
            return int[].class;
        return null;
    }

    public static int inMethodCall = 0;
    public static boolean exceptionHack = true;     
}


initializer
	:	expression
	|	arrayInitializer
	;

arrayInitializer
	:	#(ARRAY_INIT (initializer)*)
	;

elist
    {
        java.util.List el = new java.util.ArrayList();
    }
	:	#( ELIST (e:expression {el.add(#e.getTypeInfoClass());} )* )
        {
            #elist.setTypeInfo(el);
        }
	;

expression
	:	#(EXPR e:expr)
	    { #expression.setTypeInfo(#e); }
	;

expr:
	// trinary operator

     	#(QUESTION expr expr expr)
     	
    // binary operators...

	|	#(ASSIGN ass_l:expr ass_r:expr)
        { #expr.setTypeInfo(#ass_r); }
	|	#(PLUS_ASSIGN plusass_l:expr expr)
	|	#(MINUS_ASSIGN minusass_l:expr expr)
	|	#(STAR_ASSIGN starass_l:expr expr)
	|	#(DIV_ASSIGN divass_l:expr expr)
	|	#(MOD_ASSIGN modass_l:expr expr)
	|	#(SR_ASSIGN srass_l:expr expr)
	|	#(BSR_ASSIGN bsrass_l:expr expr)
	|	#(SL_ASSIGN expr expr)
	|	#(BAND_ASSIGN expr expr)
	|	#(BXOR_ASSIGN expr expr)
	|	#(BOR_ASSIGN expr expr)
	
	|	#(LOR lor_l:expr lor_r:expr)        { #expr.performBinaryBoolOp(#lor_l, #lor_r); }
	|	#(LAND land_l:expr land_r:expr)     { #expr.performBinaryBoolOp(#land_l, #land_r); }
	
	|	#(NOT_EQUAL ne_l:expr ne_r:expr)    { #expr.performBinaryBoolOp(#ne_l, #ne_r); }
	|   #(EQUAL eq_l:expr eq_r:expr)        { #expr.performBinaryBoolOp(#eq_l, #eq_r); }

    // the numerical comparison operators (JLS 15.20.1)
	|	#(LT lt_l:expr lt_r:expr)           { #expr.performBinaryBoolOp(#lt_l, #lt_r); }
	|	#(GT gt_l:expr gt_r:expr)           { #expr.performBinaryBoolOp(#gt_l, #gt_r); }
	|	#(LE le_l:expr le_r:expr)           { #expr.performBinaryBoolOp(#le_l, #le_r); }
	|	#(GE ge_l:expr ge_r:expr)           { #expr.performBinaryBoolOp(#ge_l, #ge_r); }

	|	#(SL sl_l:expr sl_r:expr)
	|	#(SR sr_l:expr sr_r:expr)
	|	#(BSR bsr_l:expr bsr_r:expr)

	|	#(BOR bor_l:expr bor_r:expr)        { #expr.performBinaryNumOp(#bor_l, #bor_r); }
	|	#(BXOR bxor_l:expr bxor_r:expr)     { #expr.performBinaryNumOp(#bxor_l, #bxor_r); }
	|	#(BAND band_l:expr band_r:expr)     { #expr.performBinaryNumOp(#band_l, #band_r); }

	|	#(PLUS plus_l:expr plus_r:expr)     { #expr.performBinaryNumOp(#plus_l, #plus_r); }
	|	#(MINUS minus_l:expr minus_r:expr)  { #expr.performBinaryNumOp(#minus_l, #minus_r); }

    // the multiplicative operators (JLS 15.17)
	|	#(DIV div_l:expr div_r:expr)        { #expr.performBinaryNumOp(#div_l, #div_r); }
	|	#(MOD mod_l:expr mod_r:expr)        { #expr.performBinaryNumOp(#mod_l, #mod_r); }
	|	#(STAR star_l:expr star_r:expr)     { #expr.performBinaryNumOp(#star_l, #star_r); }

    // unary numerical operators	
	|	#(INC expr)
	|	#(DEC expr)
	|	#(POST_INC expr)
	|	#(POST_DEC expr)
	|	#(BNOT expr)

	|	#(LNOT expr)
	    { #expr.setTypeInfo(Boolean.TYPE); }
    // the instanceof operator (JLS 15.20.2)
	|	#("instanceof" expr expr)           
	    { #expr.setTypeInfo(Boolean.TYPE); }
	|	#(UNARY_MINUS expr)
	|	#(UNARY_PLUS expr)
	|	pE:primaryExpression
	    { #expr.setTypeInfo(#pE); if(!#pE.hasTypeInfo()) System.out.println("woops"); }
	;

primaryExpression
    :   ident:IDENT
        {
            // potentially a variable in our scope ie. str.trim()
            //                                         ^^^
            // or else a type in our scope ie. System.out
            //                                 ^^^^^^
            // or else the start of a longer . expression ie. java.lang.System
            //                                                ^^^^
            
            Class cl = symbolLookupVariable(#ident.getText());
            
            if (cl != null)
                #primaryExpression.setTypeInfo(cl);                     // resolve variable
            else {
                cl = symbolLookupType(#ident.getText());
                System.out.println(#ident.getText() + " " + cl);

                if (cl != null)           
                    #primaryExpression.setTypeInfo(cl);                 // resolve type
                else {
                    #primaryExpression.setTypeText(#ident.getText());   // defer doing type resolution
                }
            }
        }
    |   #(	DOT
			(	dE:expr
				(	i2:IDENT
                {
                    // if the expression on the left has type information then
                    // we need to look up the right hand side as a field or
                    // type
                    // unless we are doing a method in which case the right hand
                    // side is the name of the method and should be ignored
                    if (#dE.hasTypeInfo()) {
                        Class cl, newcl;
                        String name = #i2.getText();
                        
                        cl = #dE.getTypeInfoClass();

                        newcl = symbolLookupLocalField(cl, name);
                        if (newcl == null) {
                            newcl = symbolLookupLocalType(cl, name);
                        }

                        if (inMethodCall > 0) {
                            System.out.println("in method call " + inMethodCall + " with " + cl + " " + name);
                            #primaryExpression.setTypeInfo(cl);
                            #primaryExpression.setTypeText(name);
                        }
                        else {
                            if (newcl == null)
				                throw new SemanticException("Unknown symbol '" + name + "' in the type " + cl);
                                
                            #primaryExpression.setTypeInfo(newcl);
                        }
                   }
                    else {
                        // try to resolve the . expression we have so far
                        String fullName = #dE.getTypeText() + "." + #i2.getText();
                        Class cl;
                        
                        cl = symbolLookupType(fullName);
                        
                        if (cl != null)
                            #primaryExpression.setTypeInfo(cl);         // resolve type
                        else
                            #primaryExpression.setTypeText(fullName);   // defer resolution
                    }
                }

				|	arrayIndex
                {
                
                }

				|	"class"                         // java.lang.String.class
                {
                    // dE must be a class, interface, array or primitive type JLS 15.8.2
                    #primaryExpression.setTypeInfo(java.lang.Class.class);
                }

				|	#( "new" newId:IDENT elist )          // Outer.new Local("a")
				{
				    // dE must be a typeName
				    Class cl;
                    if (#dE.hasTypeInfo()) {
                        cl = #dE.getTypeInfoClass();
                    }
                    else {
                        cl = symbolLookupType(#dE.getTypeText());
                    }
                    if (cl != null) {
                        #primaryExpression.setTypeInfo(symbolLookupLocalType(cl, #newId.getText()));
                    }
				}

				|	"this"
				{ if (exceptionHack)
				      throw new ExpressionSemanticException("The 'this' keyword is not allowed in object bench expressions"); }
				|   "super"
				{ if (exceptionHack)
				      throw new ExpressionSemanticException("The 'super' keyword is not allowed in object bench expressions"); }
		        
				)
		    // . []
			|	#(ARRAY_DECLARATOR typeSpecArray)
			// . boolean (or .int) ie
			|	bIT:builtInType ("class" {#primaryExpression.setTypeInfo(#bIT);} )?
			)
		)
	|	aI:arrayIndex
        {
            #primaryExpression.setTypeInfo(#aI);    
        }
	|	#(METHOD_CALL { inMethodCall++; } mc_pE:primaryExpression { inMethodCall--; } mc_e:elist)
	    {
	        if (!#mc_pE.hasTypeInfo()) { 
//                    if (inMethodCall > 0)
//                        throw new ExpressionSemanticException("An object to perform the method call on must be specified");
//                    else
                throw new SemanticException("Unknown type or variable " + #mc_pE.getTypeText());
            }	        

	        Class cl;
	        String methodName;

            cl = #mc_pE.getTypeInfoClass();
            methodName = #mc_pE.getTypeText();

            #primaryExpression.setTypeInfo(symbolLookupLocalMethod(cl,methodName));
	    }
	|	#(TYPECAST tc_tS:typeSpec tc_e:expr)
	    {
	        #primaryExpression.setTypeInfo(#tc_tS);
            // must turn it into value (not variable)	        
	    }
	|   nE:newExpression
	    { #primaryExpression.setTypeInfo(#nE); }
	|   c:constant
        { #primaryExpression.setTypeInfo(#c); }
    |   "true"
        { #primaryExpression.setTypeInfo(Boolean.TYPE); }
    |   "false"
        { #primaryExpression.setTypeInfo(Boolean.TYPE); }
    |   "null"
        {  }
	|	tS:typeSpec                                 // type name used with instanceof
	    { #primaryExpression.setTypeInfo(#tS); }
    |   "super"
    	{ if (exceptionHack) throw new ExpressionSemanticException("The 'super' keyword is not allowed in object bench expressions"); }
    |   "this"
    	{ if (exceptionHack) throw new ExpressionSemanticException("The 'this' keyword is not allowed in object bench expressions"); }
	;

arrayIndex
	:	#(INDEX_OP pE:primaryExpression e:expression)
	    {
	        if (!#pE.hasTypeInfo())
	            throw new ExpressionSemanticException("Unknown type for array access");
            Class cl = #pE.getTypeInfoClass();

            if (!cl.isArray())
                throw new ExpressionSemanticException("Cannot perform array indexing on non-array type");

            #arrayIndex.setTypeInfo(cl.getComponentType());
	    }
	;

constant
    :   NUM_INT        { #constant.setTypeInfo(Integer.TYPE); }
    |   CHAR_LITERAL   { #constant.setTypeInfo(Character.TYPE); }
    |   STRING_LITERAL { #constant.setTypeInfo(String.class); }
    |   NUM_FLOAT      { #constant.setTypeInfo(Float.TYPE); }
    |   NUM_DOUBLE     { #constant.setTypeInfo(Double.TYPE); }
    |   NUM_LONG       { #constant.setTypeInfo(Long.TYPE); }
    ;

newExpression
	:	#("new" t:type
	        { #newExpression.setTypeInfo(#t); }
			(
				newArrayDeclarator (arrayInitializer)?
			 |
			    elist /*(objBlock)? */
			)
		)
			
	;

newArrayDeclarator
	:	#( ARRAY_DECLARATOR (newArrayDeclarator)? (expression)? )
	;

typeSpec
	:	#(TYPE tSA:typeSpecArray)
	    { #typeSpec.setTypeInfo(#tSA); }
	;

typeSpecArray
	:	#( ARRAY_DECLARATOR tSA:typeSpecArray )
	    { #typeSpecArray.setTypeInfo(Array.newInstance(#tSA.getTypeInfoClass(),1).getClass()); }
	|	t:type
	    { #typeSpecArray.setTypeInfo(#t); }
	;

// the typeInfo() for this rule ends up the resolved Java class for the type
type:	i:identifier
        { #type.setTypeInfo(symbolLookupType(#i.getTypeText())); }
	|	bIT:builtInType
	    { #type.setTypeInfo(#bIT); }
	;

// the typeText() for this rule ends up the fully qualified Java type name
identifier     
	:	i1:IDENT
	    { #identifier.setTypeText(#i1.getText()); }
	|	#( DOT i2:identifier i3:IDENT )
	    { #identifier.setTypeText(#i2.getTypeText() + "." + #i3.getText()); }
	;

builtInType
    :   "void"      { #builtInType.setTypeInfo(Void.TYPE); }
    |   "boolean"   { #builtInType.setTypeInfo(Boolean.TYPE); }
    |   "byte"      { #builtInType.setTypeInfo(Byte.TYPE); }
    |   "char"      { #builtInType.setTypeInfo(Character.TYPE); }
    |   "short"     { #builtInType.setTypeInfo(Short.TYPE); }
    |   "int"       { #builtInType.setTypeInfo(Integer.TYPE); }
    |   "float"     { #builtInType.setTypeInfo(Float.TYPE); }
    |   "long"      { #builtInType.setTypeInfo(Long.TYPE); }
    |   "double"    { #builtInType.setTypeInfo(Double.TYPE); }
    ;

