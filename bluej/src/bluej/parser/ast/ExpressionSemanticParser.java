// $ANTLR 2.7.1: "javaexpr.tree.g" -> "ExpressionSemanticParser.java"$

package bluej.parser.ast;

import java.lang.reflect.*;
import antlr.collections.AST;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.SemanticException;
import antlr.ASTPair;


/**
 * Perform semantic checks on a Java expression
 */
public class ExpressionSemanticParser extends antlr.TreeParser
       implements ExpressionSemanticParserTokenTypes
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
public ExpressionSemanticParser() {
	tokenNames = _tokenNames;
}

	public final void initializer(AST _t) throws RecognitionException {
		
		TypeInfoAST initializer_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST initializer_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case EXPR:
			{
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				initializer_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case ARRAY_INIT:
			{
				arrayInitializer(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				initializer_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = initializer_AST;
		_retTree = _t;
	}
	
	public final void expression(AST _t) throws RecognitionException {
		
		TypeInfoAST expression_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST expression_AST = null;
		TypeInfoAST e_AST = null;
		TypeInfoAST e = null;
		
		try {      // for error handling
			AST __t11 = _t;
			TypeInfoAST tmp1_AST = null;
			TypeInfoAST tmp1_AST_in = null;
			tmp1_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp1_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp1_AST);
			ASTPair __currentAST11 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,EXPR);
			_t = _t.getFirstChild();
			e = _t==ASTNULL ? null : (TypeInfoAST)_t;
			expr(_t);
			_t = _retTree;
			e_AST = (TypeInfoAST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST11;
			_t = __t11;
			_t = _t.getNextSibling();
			expression_AST = (TypeInfoAST)currentAST.root;
			expression_AST.setTypeInfo(e_AST);
			expression_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = expression_AST;
		_retTree = _t;
	}
	
	public final void arrayInitializer(AST _t) throws RecognitionException {
		
		TypeInfoAST arrayInitializer_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST arrayInitializer_AST = null;
		
		try {      // for error handling
			AST __t3 = _t;
			TypeInfoAST tmp2_AST = null;
			TypeInfoAST tmp2_AST_in = null;
			tmp2_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp2_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp2_AST);
			ASTPair __currentAST3 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,ARRAY_INIT);
			_t = _t.getFirstChild();
			{
			_loop5:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==EXPR||_t.getType()==ARRAY_INIT)) {
					initializer(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop5;
				}
				
			} while (true);
			}
			currentAST = __currentAST3;
			_t = __t3;
			_t = _t.getNextSibling();
			arrayInitializer_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = arrayInitializer_AST;
		_retTree = _t;
	}
	
	public final void elist(AST _t) throws RecognitionException {
		
		TypeInfoAST elist_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST elist_AST = null;
		TypeInfoAST e_AST = null;
		TypeInfoAST e = null;
		
		java.util.List el = new java.util.ArrayList();
		
		
		try {      // for error handling
			AST __t7 = _t;
			TypeInfoAST tmp3_AST = null;
			TypeInfoAST tmp3_AST_in = null;
			tmp3_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp3_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp3_AST);
			ASTPair __currentAST7 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,ELIST);
			_t = _t.getFirstChild();
			{
			_loop9:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==EXPR)) {
					e = _t==ASTNULL ? null : (TypeInfoAST)_t;
					expression(_t);
					_t = _retTree;
					e_AST = (TypeInfoAST)returnAST;
					astFactory.addASTChild(currentAST, returnAST);
					el.add(e_AST.getTypeInfoClass());
				}
				else {
					break _loop9;
				}
				
			} while (true);
			}
			currentAST = __currentAST7;
			_t = __t7;
			_t = _t.getNextSibling();
			elist_AST = (TypeInfoAST)currentAST.root;
			
			elist_AST.setTypeInfo(el);
			
			elist_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = elist_AST;
		_retTree = _t;
	}
	
	public final void expr(AST _t) throws RecognitionException {
		
		TypeInfoAST expr_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST expr_AST = null;
		TypeInfoAST ass_l_AST = null;
		TypeInfoAST ass_l = null;
		TypeInfoAST ass_r_AST = null;
		TypeInfoAST ass_r = null;
		TypeInfoAST plusass_l_AST = null;
		TypeInfoAST plusass_l = null;
		TypeInfoAST minusass_l_AST = null;
		TypeInfoAST minusass_l = null;
		TypeInfoAST starass_l_AST = null;
		TypeInfoAST starass_l = null;
		TypeInfoAST divass_l_AST = null;
		TypeInfoAST divass_l = null;
		TypeInfoAST modass_l_AST = null;
		TypeInfoAST modass_l = null;
		TypeInfoAST srass_l_AST = null;
		TypeInfoAST srass_l = null;
		TypeInfoAST bsrass_l_AST = null;
		TypeInfoAST bsrass_l = null;
		TypeInfoAST lor_l_AST = null;
		TypeInfoAST lor_l = null;
		TypeInfoAST lor_r_AST = null;
		TypeInfoAST lor_r = null;
		TypeInfoAST land_l_AST = null;
		TypeInfoAST land_l = null;
		TypeInfoAST land_r_AST = null;
		TypeInfoAST land_r = null;
		TypeInfoAST ne_l_AST = null;
		TypeInfoAST ne_l = null;
		TypeInfoAST ne_r_AST = null;
		TypeInfoAST ne_r = null;
		TypeInfoAST eq_l_AST = null;
		TypeInfoAST eq_l = null;
		TypeInfoAST eq_r_AST = null;
		TypeInfoAST eq_r = null;
		TypeInfoAST lt_l_AST = null;
		TypeInfoAST lt_l = null;
		TypeInfoAST lt_r_AST = null;
		TypeInfoAST lt_r = null;
		TypeInfoAST gt_l_AST = null;
		TypeInfoAST gt_l = null;
		TypeInfoAST gt_r_AST = null;
		TypeInfoAST gt_r = null;
		TypeInfoAST le_l_AST = null;
		TypeInfoAST le_l = null;
		TypeInfoAST le_r_AST = null;
		TypeInfoAST le_r = null;
		TypeInfoAST ge_l_AST = null;
		TypeInfoAST ge_l = null;
		TypeInfoAST ge_r_AST = null;
		TypeInfoAST ge_r = null;
		TypeInfoAST sl_l_AST = null;
		TypeInfoAST sl_l = null;
		TypeInfoAST sl_r_AST = null;
		TypeInfoAST sl_r = null;
		TypeInfoAST sr_l_AST = null;
		TypeInfoAST sr_l = null;
		TypeInfoAST sr_r_AST = null;
		TypeInfoAST sr_r = null;
		TypeInfoAST bsr_l_AST = null;
		TypeInfoAST bsr_l = null;
		TypeInfoAST bsr_r_AST = null;
		TypeInfoAST bsr_r = null;
		TypeInfoAST bor_l_AST = null;
		TypeInfoAST bor_l = null;
		TypeInfoAST bor_r_AST = null;
		TypeInfoAST bor_r = null;
		TypeInfoAST bxor_l_AST = null;
		TypeInfoAST bxor_l = null;
		TypeInfoAST bxor_r_AST = null;
		TypeInfoAST bxor_r = null;
		TypeInfoAST band_l_AST = null;
		TypeInfoAST band_l = null;
		TypeInfoAST band_r_AST = null;
		TypeInfoAST band_r = null;
		TypeInfoAST plus_l_AST = null;
		TypeInfoAST plus_l = null;
		TypeInfoAST plus_r_AST = null;
		TypeInfoAST plus_r = null;
		TypeInfoAST minus_l_AST = null;
		TypeInfoAST minus_l = null;
		TypeInfoAST minus_r_AST = null;
		TypeInfoAST minus_r = null;
		TypeInfoAST div_l_AST = null;
		TypeInfoAST div_l = null;
		TypeInfoAST div_r_AST = null;
		TypeInfoAST div_r = null;
		TypeInfoAST mod_l_AST = null;
		TypeInfoAST mod_l = null;
		TypeInfoAST mod_r_AST = null;
		TypeInfoAST mod_r = null;
		TypeInfoAST star_l_AST = null;
		TypeInfoAST star_l = null;
		TypeInfoAST star_r_AST = null;
		TypeInfoAST star_r = null;
		TypeInfoAST pE_AST = null;
		TypeInfoAST pE = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case QUESTION:
			{
				AST __t13 = _t;
				TypeInfoAST tmp4_AST = null;
				TypeInfoAST tmp4_AST_in = null;
				tmp4_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp4_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp4_AST);
				ASTPair __currentAST13 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,QUESTION);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST13;
				_t = __t13;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case ASSIGN:
			{
				AST __t14 = _t;
				TypeInfoAST tmp5_AST = null;
				TypeInfoAST tmp5_AST_in = null;
				tmp5_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp5_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp5_AST);
				ASTPair __currentAST14 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,ASSIGN);
				_t = _t.getFirstChild();
				ass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				ass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				ass_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				ass_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST14;
				_t = __t14;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.setTypeInfo(ass_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case PLUS_ASSIGN:
			{
				AST __t15 = _t;
				TypeInfoAST tmp6_AST = null;
				TypeInfoAST tmp6_AST_in = null;
				tmp6_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp6_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp6_AST);
				ASTPair __currentAST15 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,PLUS_ASSIGN);
				_t = _t.getFirstChild();
				plusass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				plusass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST15;
				_t = __t15;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case MINUS_ASSIGN:
			{
				AST __t16 = _t;
				TypeInfoAST tmp7_AST = null;
				TypeInfoAST tmp7_AST_in = null;
				tmp7_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp7_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp7_AST);
				ASTPair __currentAST16 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MINUS_ASSIGN);
				_t = _t.getFirstChild();
				minusass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				minusass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST16;
				_t = __t16;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case STAR_ASSIGN:
			{
				AST __t17 = _t;
				TypeInfoAST tmp8_AST = null;
				TypeInfoAST tmp8_AST_in = null;
				tmp8_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp8_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp8_AST);
				ASTPair __currentAST17 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,STAR_ASSIGN);
				_t = _t.getFirstChild();
				starass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				starass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST17;
				_t = __t17;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case DIV_ASSIGN:
			{
				AST __t18 = _t;
				TypeInfoAST tmp9_AST = null;
				TypeInfoAST tmp9_AST_in = null;
				tmp9_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp9_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp9_AST);
				ASTPair __currentAST18 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DIV_ASSIGN);
				_t = _t.getFirstChild();
				divass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				divass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST18;
				_t = __t18;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case MOD_ASSIGN:
			{
				AST __t19 = _t;
				TypeInfoAST tmp10_AST = null;
				TypeInfoAST tmp10_AST_in = null;
				tmp10_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp10_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp10_AST);
				ASTPair __currentAST19 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MOD_ASSIGN);
				_t = _t.getFirstChild();
				modass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				modass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST19;
				_t = __t19;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case SR_ASSIGN:
			{
				AST __t20 = _t;
				TypeInfoAST tmp11_AST = null;
				TypeInfoAST tmp11_AST_in = null;
				tmp11_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp11_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp11_AST);
				ASTPair __currentAST20 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SR_ASSIGN);
				_t = _t.getFirstChild();
				srass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				srass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST20;
				_t = __t20;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BSR_ASSIGN:
			{
				AST __t21 = _t;
				TypeInfoAST tmp12_AST = null;
				TypeInfoAST tmp12_AST_in = null;
				tmp12_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp12_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp12_AST);
				ASTPair __currentAST21 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BSR_ASSIGN);
				_t = _t.getFirstChild();
				bsrass_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bsrass_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST21;
				_t = __t21;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case SL_ASSIGN:
			{
				AST __t22 = _t;
				TypeInfoAST tmp13_AST = null;
				TypeInfoAST tmp13_AST_in = null;
				tmp13_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp13_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp13_AST);
				ASTPair __currentAST22 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SL_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST22;
				_t = __t22;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BAND_ASSIGN:
			{
				AST __t23 = _t;
				TypeInfoAST tmp14_AST = null;
				TypeInfoAST tmp14_AST_in = null;
				tmp14_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp14_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp14_AST);
				ASTPair __currentAST23 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BAND_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST23;
				_t = __t23;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BXOR_ASSIGN:
			{
				AST __t24 = _t;
				TypeInfoAST tmp15_AST = null;
				TypeInfoAST tmp15_AST_in = null;
				tmp15_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp15_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp15_AST);
				ASTPair __currentAST24 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BXOR_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST24;
				_t = __t24;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BOR_ASSIGN:
			{
				AST __t25 = _t;
				TypeInfoAST tmp16_AST = null;
				TypeInfoAST tmp16_AST_in = null;
				tmp16_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp16_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp16_AST);
				ASTPair __currentAST25 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BOR_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST25;
				_t = __t25;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LOR:
			{
				AST __t26 = _t;
				TypeInfoAST tmp17_AST = null;
				TypeInfoAST tmp17_AST_in = null;
				tmp17_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp17_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp17_AST);
				ASTPair __currentAST26 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LOR);
				_t = _t.getFirstChild();
				lor_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				lor_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				lor_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				lor_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST26;
				_t = __t26;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(lor_l_AST, lor_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LAND:
			{
				AST __t27 = _t;
				TypeInfoAST tmp18_AST = null;
				TypeInfoAST tmp18_AST_in = null;
				tmp18_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp18_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp18_AST);
				ASTPair __currentAST27 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LAND);
				_t = _t.getFirstChild();
				land_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				land_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				land_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				land_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST27;
				_t = __t27;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(land_l_AST, land_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case NOT_EQUAL:
			{
				AST __t28 = _t;
				TypeInfoAST tmp19_AST = null;
				TypeInfoAST tmp19_AST_in = null;
				tmp19_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp19_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp19_AST);
				ASTPair __currentAST28 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,NOT_EQUAL);
				_t = _t.getFirstChild();
				ne_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				ne_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				ne_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				ne_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST28;
				_t = __t28;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(ne_l_AST, ne_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case EQUAL:
			{
				AST __t29 = _t;
				TypeInfoAST tmp20_AST = null;
				TypeInfoAST tmp20_AST_in = null;
				tmp20_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp20_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp20_AST);
				ASTPair __currentAST29 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,EQUAL);
				_t = _t.getFirstChild();
				eq_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				eq_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				eq_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				eq_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST29;
				_t = __t29;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(eq_l_AST, eq_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LT:
			{
				AST __t30 = _t;
				TypeInfoAST tmp21_AST = null;
				TypeInfoAST tmp21_AST_in = null;
				tmp21_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp21_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp21_AST);
				ASTPair __currentAST30 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LT);
				_t = _t.getFirstChild();
				lt_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				lt_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				lt_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				lt_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST30;
				_t = __t30;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(lt_l_AST, lt_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case GT:
			{
				AST __t31 = _t;
				TypeInfoAST tmp22_AST = null;
				TypeInfoAST tmp22_AST_in = null;
				tmp22_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp22_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp22_AST);
				ASTPair __currentAST31 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,GT);
				_t = _t.getFirstChild();
				gt_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				gt_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				gt_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				gt_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST31;
				_t = __t31;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(gt_l_AST, gt_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LE:
			{
				AST __t32 = _t;
				TypeInfoAST tmp23_AST = null;
				TypeInfoAST tmp23_AST_in = null;
				tmp23_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp23_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp23_AST);
				ASTPair __currentAST32 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LE);
				_t = _t.getFirstChild();
				le_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				le_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				le_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				le_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST32;
				_t = __t32;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(le_l_AST, le_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case GE:
			{
				AST __t33 = _t;
				TypeInfoAST tmp24_AST = null;
				TypeInfoAST tmp24_AST_in = null;
				tmp24_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp24_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp24_AST);
				ASTPair __currentAST33 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,GE);
				_t = _t.getFirstChild();
				ge_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				ge_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				ge_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				ge_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST33;
				_t = __t33;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryBoolOp(ge_l_AST, ge_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case SL:
			{
				AST __t34 = _t;
				TypeInfoAST tmp25_AST = null;
				TypeInfoAST tmp25_AST_in = null;
				tmp25_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp25_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp25_AST);
				ASTPair __currentAST34 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SL);
				_t = _t.getFirstChild();
				sl_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				sl_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				sl_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				sl_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST34;
				_t = __t34;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case SR:
			{
				AST __t35 = _t;
				TypeInfoAST tmp26_AST = null;
				TypeInfoAST tmp26_AST_in = null;
				tmp26_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp26_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp26_AST);
				ASTPair __currentAST35 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SR);
				_t = _t.getFirstChild();
				sr_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				sr_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				sr_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				sr_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST35;
				_t = __t35;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BSR:
			{
				AST __t36 = _t;
				TypeInfoAST tmp27_AST = null;
				TypeInfoAST tmp27_AST_in = null;
				tmp27_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp27_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp27_AST);
				ASTPair __currentAST36 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BSR);
				_t = _t.getFirstChild();
				bsr_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bsr_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				bsr_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bsr_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST36;
				_t = __t36;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BOR:
			{
				AST __t37 = _t;
				TypeInfoAST tmp28_AST = null;
				TypeInfoAST tmp28_AST_in = null;
				tmp28_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp28_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp28_AST);
				ASTPair __currentAST37 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BOR);
				_t = _t.getFirstChild();
				bor_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bor_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				bor_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bor_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST37;
				_t = __t37;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(bor_l_AST, bor_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BXOR:
			{
				AST __t38 = _t;
				TypeInfoAST tmp29_AST = null;
				TypeInfoAST tmp29_AST_in = null;
				tmp29_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp29_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp29_AST);
				ASTPair __currentAST38 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BXOR);
				_t = _t.getFirstChild();
				bxor_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bxor_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				bxor_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				bxor_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST38;
				_t = __t38;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(bxor_l_AST, bxor_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BAND:
			{
				AST __t39 = _t;
				TypeInfoAST tmp30_AST = null;
				TypeInfoAST tmp30_AST_in = null;
				tmp30_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp30_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp30_AST);
				ASTPair __currentAST39 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BAND);
				_t = _t.getFirstChild();
				band_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				band_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				band_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				band_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST39;
				_t = __t39;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(band_l_AST, band_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case PLUS:
			{
				AST __t40 = _t;
				TypeInfoAST tmp31_AST = null;
				TypeInfoAST tmp31_AST_in = null;
				tmp31_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp31_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp31_AST);
				ASTPair __currentAST40 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,PLUS);
				_t = _t.getFirstChild();
				plus_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				plus_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				plus_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				plus_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST40;
				_t = __t40;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(plus_l_AST, plus_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case MINUS:
			{
				AST __t41 = _t;
				TypeInfoAST tmp32_AST = null;
				TypeInfoAST tmp32_AST_in = null;
				tmp32_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp32_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp32_AST);
				ASTPair __currentAST41 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MINUS);
				_t = _t.getFirstChild();
				minus_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				minus_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				minus_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				minus_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST41;
				_t = __t41;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(minus_l_AST, minus_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case DIV:
			{
				AST __t42 = _t;
				TypeInfoAST tmp33_AST = null;
				TypeInfoAST tmp33_AST_in = null;
				tmp33_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp33_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp33_AST);
				ASTPair __currentAST42 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DIV);
				_t = _t.getFirstChild();
				div_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				div_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				div_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				div_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST42;
				_t = __t42;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(div_l_AST, div_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case MOD:
			{
				AST __t43 = _t;
				TypeInfoAST tmp34_AST = null;
				TypeInfoAST tmp34_AST_in = null;
				tmp34_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp34_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp34_AST);
				ASTPair __currentAST43 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MOD);
				_t = _t.getFirstChild();
				mod_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				mod_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				mod_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				mod_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST43;
				_t = __t43;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(mod_l_AST, mod_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case STAR:
			{
				AST __t44 = _t;
				TypeInfoAST tmp35_AST = null;
				TypeInfoAST tmp35_AST_in = null;
				tmp35_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp35_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp35_AST);
				ASTPair __currentAST44 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,STAR);
				_t = _t.getFirstChild();
				star_l = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				star_l_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				star_r = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				star_r_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST44;
				_t = __t44;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.performBinaryNumOp(star_l_AST, star_r_AST);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case INC:
			{
				AST __t45 = _t;
				TypeInfoAST tmp36_AST = null;
				TypeInfoAST tmp36_AST_in = null;
				tmp36_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp36_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp36_AST);
				ASTPair __currentAST45 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,INC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST45;
				_t = __t45;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case DEC:
			{
				AST __t46 = _t;
				TypeInfoAST tmp37_AST = null;
				TypeInfoAST tmp37_AST_in = null;
				tmp37_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp37_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp37_AST);
				ASTPair __currentAST46 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DEC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST46;
				_t = __t46;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case POST_INC:
			{
				AST __t47 = _t;
				TypeInfoAST tmp38_AST = null;
				TypeInfoAST tmp38_AST_in = null;
				tmp38_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp38_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp38_AST);
				ASTPair __currentAST47 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,POST_INC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST47;
				_t = __t47;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case POST_DEC:
			{
				AST __t48 = _t;
				TypeInfoAST tmp39_AST = null;
				TypeInfoAST tmp39_AST_in = null;
				tmp39_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp39_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp39_AST);
				ASTPair __currentAST48 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,POST_DEC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST48;
				_t = __t48;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case BNOT:
			{
				AST __t49 = _t;
				TypeInfoAST tmp40_AST = null;
				TypeInfoAST tmp40_AST_in = null;
				tmp40_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp40_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp40_AST);
				ASTPair __currentAST49 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BNOT);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST49;
				_t = __t49;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LNOT:
			{
				AST __t50 = _t;
				TypeInfoAST tmp41_AST = null;
				TypeInfoAST tmp41_AST_in = null;
				tmp41_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp41_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp41_AST);
				ASTPair __currentAST50 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LNOT);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST50;
				_t = __t50;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.setTypeInfo(Boolean.TYPE);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_instanceof:
			{
				AST __t51 = _t;
				TypeInfoAST tmp42_AST = null;
				TypeInfoAST tmp42_AST_in = null;
				tmp42_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp42_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp42_AST);
				ASTPair __currentAST51 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_instanceof);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST51;
				_t = __t51;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.setTypeInfo(Boolean.TYPE);
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case UNARY_MINUS:
			{
				AST __t52 = _t;
				TypeInfoAST tmp43_AST = null;
				TypeInfoAST tmp43_AST_in = null;
				tmp43_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp43_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp43_AST);
				ASTPair __currentAST52 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,UNARY_MINUS);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST52;
				_t = __t52;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case UNARY_PLUS:
			{
				AST __t53 = _t;
				TypeInfoAST tmp44_AST = null;
				TypeInfoAST tmp44_AST_in = null;
				tmp44_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp44_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp44_AST);
				ASTPair __currentAST53 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,UNARY_PLUS);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST53;
				_t = __t53;
				_t = _t.getNextSibling();
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case TYPE:
			case TYPECAST:
			case INDEX_OP:
			case METHOD_CALL:
			case IDENT:
			case DOT:
			case LITERAL_this:
			case LITERAL_super:
			case LITERAL_true:
			case LITERAL_false:
			case LITERAL_null:
			case LITERAL_new:
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				pE = _t==ASTNULL ? null : (TypeInfoAST)_t;
				primaryExpression(_t);
				_t = _retTree;
				pE_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				expr_AST = (TypeInfoAST)currentAST.root;
				expr_AST.setTypeInfo(pE_AST); if(!pE_AST.hasTypeInfo()) System.out.println("woops");
				expr_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = expr_AST;
		_retTree = _t;
	}
	
	public final void primaryExpression(AST _t) throws RecognitionException {
		
		TypeInfoAST primaryExpression_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST primaryExpression_AST = null;
		TypeInfoAST ident = null;
		TypeInfoAST ident_AST = null;
		TypeInfoAST dE_AST = null;
		TypeInfoAST dE = null;
		TypeInfoAST i2 = null;
		TypeInfoAST i2_AST = null;
		TypeInfoAST newId = null;
		TypeInfoAST newId_AST = null;
		TypeInfoAST bIT_AST = null;
		TypeInfoAST bIT = null;
		TypeInfoAST aI_AST = null;
		TypeInfoAST aI = null;
		TypeInfoAST mc_pE_AST = null;
		TypeInfoAST mc_pE = null;
		TypeInfoAST mc_e_AST = null;
		TypeInfoAST mc_e = null;
		TypeInfoAST tc_tS_AST = null;
		TypeInfoAST tc_tS = null;
		TypeInfoAST tc_e_AST = null;
		TypeInfoAST tc_e = null;
		TypeInfoAST nE_AST = null;
		TypeInfoAST nE = null;
		TypeInfoAST c_AST = null;
		TypeInfoAST c = null;
		TypeInfoAST tS_AST = null;
		TypeInfoAST tS = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			{
				ident = (TypeInfoAST)_t;
				ident_AST = (TypeInfoAST)astFactory.create(ident);
				astFactory.addASTChild(currentAST, ident_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				
				// potentially a variable in our scope ie. str.trim()
				//                                         ^^^
				// or else a type in our scope ie. System.out
				//                                 ^^^^^^
				// or else the start of a longer . expression ie. java.lang.System
				//                                                ^^^^
				
				Class cl = symbolLookupVariable(ident_AST.getText());
				
				if (cl != null)
				primaryExpression_AST.setTypeInfo(cl);                     // resolve variable
				else {
				cl = symbolLookupType(ident_AST.getText());
				System.out.println(ident_AST.getText() + " " + cl);
				
				if (cl != null)           
				primaryExpression_AST.setTypeInfo(cl);                 // resolve type
				else {
				primaryExpression_AST.setTypeText(ident_AST.getText());   // defer doing type resolution
				}
				}
				
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case DOT:
			{
				AST __t55 = _t;
				TypeInfoAST tmp45_AST = null;
				TypeInfoAST tmp45_AST_in = null;
				tmp45_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp45_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp45_AST);
				ASTPair __currentAST55 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DOT);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case TYPE:
				case TYPECAST:
				case INDEX_OP:
				case POST_INC:
				case POST_DEC:
				case METHOD_CALL:
				case UNARY_MINUS:
				case UNARY_PLUS:
				case IDENT:
				case DOT:
				case STAR:
				case LITERAL_this:
				case LITERAL_super:
				case ASSIGN:
				case PLUS_ASSIGN:
				case MINUS_ASSIGN:
				case STAR_ASSIGN:
				case DIV_ASSIGN:
				case MOD_ASSIGN:
				case SR_ASSIGN:
				case BSR_ASSIGN:
				case SL_ASSIGN:
				case BAND_ASSIGN:
				case BXOR_ASSIGN:
				case BOR_ASSIGN:
				case QUESTION:
				case LOR:
				case LAND:
				case BOR:
				case BXOR:
				case BAND:
				case NOT_EQUAL:
				case EQUAL:
				case LT:
				case GT:
				case LE:
				case GE:
				case LITERAL_instanceof:
				case SL:
				case SR:
				case BSR:
				case PLUS:
				case MINUS:
				case DIV:
				case MOD:
				case INC:
				case DEC:
				case BNOT:
				case LNOT:
				case LITERAL_true:
				case LITERAL_false:
				case LITERAL_null:
				case LITERAL_new:
				case NUM_INT:
				case CHAR_LITERAL:
				case STRING_LITERAL:
				case NUM_FLOAT:
				case NUM_LONG:
				case NUM_DOUBLE:
				{
					dE = _t==ASTNULL ? null : (TypeInfoAST)_t;
					expr(_t);
					_t = _retTree;
					dE_AST = (TypeInfoAST)returnAST;
					astFactory.addASTChild(currentAST, returnAST);
					{
					if (_t==null) _t=ASTNULL;
					switch ( _t.getType()) {
					case IDENT:
					{
						i2 = (TypeInfoAST)_t;
						i2_AST = (TypeInfoAST)astFactory.create(i2);
						astFactory.addASTChild(currentAST, i2_AST);
						match(_t,IDENT);
						_t = _t.getNextSibling();
						primaryExpression_AST = (TypeInfoAST)currentAST.root;
						
						// if the expression on the left has type information then
						// we need to look up the right hand side as a field or
						// type
						// unless we are doing a method in which case the right hand
						// side is the name of the method and should be ignored
						if (dE_AST.hasTypeInfo()) {
						Class cl, newcl;
						String name = i2_AST.getText();
						
						cl = dE_AST.getTypeInfoClass();
						
						newcl = symbolLookupLocalField(cl, name);
						if (newcl == null) {
						newcl = symbolLookupLocalType(cl, name);
						}
						
						if (inMethodCall > 0) {
						System.out.println("in method call " + inMethodCall + " with " + cl + " " + name);
						primaryExpression_AST.setTypeInfo(cl);
						primaryExpression_AST.setTypeText(name);
						}
						else {
						if (newcl == null)
										                throw new SemanticException("Unknown symbol '" + name + "' in the type " + cl);
						
						primaryExpression_AST.setTypeInfo(newcl);
						}
						}
						else {
						// try to resolve the . expression we have so far
						String fullName = dE_AST.getTypeText() + "." + i2_AST.getText();
						Class cl;
						
						cl = symbolLookupType(fullName);
						
						if (cl != null)
						primaryExpression_AST.setTypeInfo(cl);         // resolve type
						else
						primaryExpression_AST.setTypeText(fullName);   // defer resolution
						}
						
						break;
					}
					case INDEX_OP:
					{
						arrayIndex(_t);
						_t = _retTree;
						astFactory.addASTChild(currentAST, returnAST);
						
						
						
						break;
					}
					case LITERAL_class:
					{
						TypeInfoAST tmp46_AST = null;
						TypeInfoAST tmp46_AST_in = null;
						tmp46_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
						tmp46_AST_in = (TypeInfoAST)_t;
						astFactory.addASTChild(currentAST, tmp46_AST);
						match(_t,LITERAL_class);
						_t = _t.getNextSibling();
						primaryExpression_AST = (TypeInfoAST)currentAST.root;
						
						// dE must be a class, interface, array or primitive type JLS 15.8.2
						primaryExpression_AST.setTypeInfo(java.lang.Class.class);
						
						break;
					}
					case LITERAL_new:
					{
						AST __t58 = _t;
						TypeInfoAST tmp47_AST = null;
						TypeInfoAST tmp47_AST_in = null;
						tmp47_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
						tmp47_AST_in = (TypeInfoAST)_t;
						astFactory.addASTChild(currentAST, tmp47_AST);
						ASTPair __currentAST58 = currentAST.copy();
						currentAST.root = currentAST.child;
						currentAST.child = null;
						match(_t,LITERAL_new);
						_t = _t.getFirstChild();
						newId = (TypeInfoAST)_t;
						newId_AST = (TypeInfoAST)astFactory.create(newId);
						astFactory.addASTChild(currentAST, newId_AST);
						match(_t,IDENT);
						_t = _t.getNextSibling();
						elist(_t);
						_t = _retTree;
						astFactory.addASTChild(currentAST, returnAST);
						currentAST = __currentAST58;
						_t = __t58;
						_t = _t.getNextSibling();
						primaryExpression_AST = (TypeInfoAST)currentAST.root;
						
										    // dE must be a typeName
										    Class cl;
						if (dE_AST.hasTypeInfo()) {
						cl = dE_AST.getTypeInfoClass();
						}
						else {
						cl = symbolLookupType(dE_AST.getTypeText());
						}
						if (cl != null) {
						primaryExpression_AST.setTypeInfo(symbolLookupLocalType(cl, newId_AST.getText()));
						}
										
						break;
					}
					case LITERAL_this:
					{
						TypeInfoAST tmp48_AST = null;
						TypeInfoAST tmp48_AST_in = null;
						tmp48_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
						tmp48_AST_in = (TypeInfoAST)_t;
						astFactory.addASTChild(currentAST, tmp48_AST);
						match(_t,LITERAL_this);
						_t = _t.getNextSibling();
						if (exceptionHack)
										      throw new ExpressionSemanticException("The 'this' keyword is not allowed in object bench expressions");
						break;
					}
					case LITERAL_super:
					{
						TypeInfoAST tmp49_AST = null;
						TypeInfoAST tmp49_AST_in = null;
						tmp49_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
						tmp49_AST_in = (TypeInfoAST)_t;
						astFactory.addASTChild(currentAST, tmp49_AST);
						match(_t,LITERAL_super);
						_t = _t.getNextSibling();
						if (exceptionHack)
										      throw new ExpressionSemanticException("The 'super' keyword is not allowed in object bench expressions");
						break;
					}
					default:
					{
						throw new NoViableAltException(_t);
					}
					}
					}
					break;
				}
				case ARRAY_DECLARATOR:
				{
					AST __t59 = _t;
					TypeInfoAST tmp50_AST = null;
					TypeInfoAST tmp50_AST_in = null;
					tmp50_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
					tmp50_AST_in = (TypeInfoAST)_t;
					astFactory.addASTChild(currentAST, tmp50_AST);
					ASTPair __currentAST59 = currentAST.copy();
					currentAST.root = currentAST.child;
					currentAST.child = null;
					match(_t,ARRAY_DECLARATOR);
					_t = _t.getFirstChild();
					typeSpecArray(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					currentAST = __currentAST59;
					_t = __t59;
					_t = _t.getNextSibling();
					break;
				}
				case LITERAL_void:
				case LITERAL_boolean:
				case LITERAL_byte:
				case LITERAL_char:
				case LITERAL_short:
				case LITERAL_int:
				case LITERAL_float:
				case LITERAL_long:
				case LITERAL_double:
				{
					bIT = _t==ASTNULL ? null : (TypeInfoAST)_t;
					builtInType(_t);
					_t = _retTree;
					bIT_AST = (TypeInfoAST)returnAST;
					astFactory.addASTChild(currentAST, returnAST);
					{
					if (_t==null) _t=ASTNULL;
					switch ( _t.getType()) {
					case LITERAL_class:
					{
						TypeInfoAST tmp51_AST = null;
						TypeInfoAST tmp51_AST_in = null;
						tmp51_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
						tmp51_AST_in = (TypeInfoAST)_t;
						astFactory.addASTChild(currentAST, tmp51_AST);
						match(_t,LITERAL_class);
						_t = _t.getNextSibling();
						primaryExpression_AST = (TypeInfoAST)currentAST.root;
						primaryExpression_AST.setTypeInfo(bIT_AST);
						break;
					}
					case 3:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(_t);
					}
					}
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				currentAST = __currentAST55;
				_t = __t55;
				_t = _t.getNextSibling();
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case INDEX_OP:
			{
				aI = _t==ASTNULL ? null : (TypeInfoAST)_t;
				arrayIndex(_t);
				_t = _retTree;
				aI_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				
				primaryExpression_AST.setTypeInfo(aI_AST);    
				
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case METHOD_CALL:
			{
				AST __t61 = _t;
				TypeInfoAST tmp52_AST = null;
				TypeInfoAST tmp52_AST_in = null;
				tmp52_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp52_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp52_AST);
				ASTPair __currentAST61 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,METHOD_CALL);
				_t = _t.getFirstChild();
				inMethodCall++;
				mc_pE = _t==ASTNULL ? null : (TypeInfoAST)_t;
				primaryExpression(_t);
				_t = _retTree;
				mc_pE_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				inMethodCall--;
				mc_e = _t==ASTNULL ? null : (TypeInfoAST)_t;
				elist(_t);
				_t = _retTree;
				mc_e_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST61;
				_t = __t61;
				_t = _t.getNextSibling();
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				
					        if (!mc_pE_AST.hasTypeInfo()) { 
				//                    if (inMethodCall > 0)
				//                        throw new ExpressionSemanticException("An object to perform the method call on must be specified");
				//                    else
				throw new SemanticException("Unknown type or variable " + mc_pE_AST.getTypeText());
				}	        
				
					        Class cl;
					        String methodName;
				
				cl = mc_pE_AST.getTypeInfoClass();
				methodName = mc_pE_AST.getTypeText();
				
				primaryExpression_AST.setTypeInfo(symbolLookupLocalMethod(cl,methodName));
					
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case TYPECAST:
			{
				AST __t62 = _t;
				TypeInfoAST tmp53_AST = null;
				TypeInfoAST tmp53_AST_in = null;
				tmp53_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp53_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp53_AST);
				ASTPair __currentAST62 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,TYPECAST);
				_t = _t.getFirstChild();
				tc_tS = _t==ASTNULL ? null : (TypeInfoAST)_t;
				typeSpec(_t);
				_t = _retTree;
				tc_tS_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				tc_e = _t==ASTNULL ? null : (TypeInfoAST)_t;
				expr(_t);
				_t = _retTree;
				tc_e_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST62;
				_t = __t62;
				_t = _t.getNextSibling();
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				
					        primaryExpression_AST.setTypeInfo(tc_tS_AST);
				// must turn it into value (not variable)	        
					
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_new:
			{
				nE = _t==ASTNULL ? null : (TypeInfoAST)_t;
				newExpression(_t);
				_t = _retTree;
				nE_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				primaryExpression_AST.setTypeInfo(nE_AST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				c = _t==ASTNULL ? null : (TypeInfoAST)_t;
				constant(_t);
				_t = _retTree;
				c_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				primaryExpression_AST.setTypeInfo(c_AST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_true:
			{
				TypeInfoAST tmp54_AST = null;
				TypeInfoAST tmp54_AST_in = null;
				tmp54_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp54_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp54_AST);
				match(_t,LITERAL_true);
				_t = _t.getNextSibling();
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				primaryExpression_AST.setTypeInfo(Boolean.TYPE);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_false:
			{
				TypeInfoAST tmp55_AST = null;
				TypeInfoAST tmp55_AST_in = null;
				tmp55_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp55_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp55_AST);
				match(_t,LITERAL_false);
				_t = _t.getNextSibling();
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				primaryExpression_AST.setTypeInfo(Boolean.TYPE);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_null:
			{
				TypeInfoAST tmp56_AST = null;
				TypeInfoAST tmp56_AST_in = null;
				tmp56_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp56_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp56_AST);
				match(_t,LITERAL_null);
				_t = _t.getNextSibling();
				
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case TYPE:
			{
				tS = _t==ASTNULL ? null : (TypeInfoAST)_t;
				typeSpec(_t);
				_t = _retTree;
				tS_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				primaryExpression_AST.setTypeInfo(tS_AST);
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_super:
			{
				TypeInfoAST tmp57_AST = null;
				TypeInfoAST tmp57_AST_in = null;
				tmp57_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp57_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp57_AST);
				match(_t,LITERAL_super);
				_t = _t.getNextSibling();
				if (exceptionHack) throw new ExpressionSemanticException("The 'super' keyword is not allowed in object bench expressions");
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_this:
			{
				TypeInfoAST tmp58_AST = null;
				TypeInfoAST tmp58_AST_in = null;
				tmp58_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp58_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp58_AST);
				match(_t,LITERAL_this);
				_t = _t.getNextSibling();
				if (exceptionHack) throw new ExpressionSemanticException("The 'this' keyword is not allowed in object bench expressions");
				primaryExpression_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = primaryExpression_AST;
		_retTree = _t;
	}
	
	public final void arrayIndex(AST _t) throws RecognitionException {
		
		TypeInfoAST arrayIndex_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST arrayIndex_AST = null;
		TypeInfoAST pE_AST = null;
		TypeInfoAST pE = null;
		TypeInfoAST e_AST = null;
		TypeInfoAST e = null;
		
		try {      // for error handling
			AST __t64 = _t;
			TypeInfoAST tmp59_AST = null;
			TypeInfoAST tmp59_AST_in = null;
			tmp59_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp59_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp59_AST);
			ASTPair __currentAST64 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,INDEX_OP);
			_t = _t.getFirstChild();
			pE = _t==ASTNULL ? null : (TypeInfoAST)_t;
			primaryExpression(_t);
			_t = _retTree;
			pE_AST = (TypeInfoAST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			e = _t==ASTNULL ? null : (TypeInfoAST)_t;
			expression(_t);
			_t = _retTree;
			e_AST = (TypeInfoAST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST64;
			_t = __t64;
			_t = _t.getNextSibling();
			arrayIndex_AST = (TypeInfoAST)currentAST.root;
			
				        if (!pE_AST.hasTypeInfo())
				            throw new ExpressionSemanticException("Unknown type for array access");
			Class cl = pE_AST.getTypeInfoClass();
			
			if (!cl.isArray())
			throw new ExpressionSemanticException("Cannot perform array indexing on non-array type");
			
			arrayIndex_AST.setTypeInfo(cl.getComponentType());
				
			arrayIndex_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = arrayIndex_AST;
		_retTree = _t;
	}
	
	public final void typeSpecArray(AST _t) throws RecognitionException {
		
		TypeInfoAST typeSpecArray_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST typeSpecArray_AST = null;
		TypeInfoAST tSA_AST = null;
		TypeInfoAST tSA = null;
		TypeInfoAST t_AST = null;
		TypeInfoAST t = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ARRAY_DECLARATOR:
			{
				AST __t77 = _t;
				TypeInfoAST tmp60_AST = null;
				TypeInfoAST tmp60_AST_in = null;
				tmp60_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp60_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp60_AST);
				ASTPair __currentAST77 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,ARRAY_DECLARATOR);
				_t = _t.getFirstChild();
				tSA = _t==ASTNULL ? null : (TypeInfoAST)_t;
				typeSpecArray(_t);
				_t = _retTree;
				tSA_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST77;
				_t = __t77;
				_t = _t.getNextSibling();
				typeSpecArray_AST = (TypeInfoAST)currentAST.root;
				typeSpecArray_AST.setTypeInfo(Array.newInstance(tSA_AST.getTypeInfoClass(),1).getClass());
				typeSpecArray_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			case IDENT:
			case DOT:
			{
				t = _t==ASTNULL ? null : (TypeInfoAST)_t;
				type(_t);
				_t = _retTree;
				t_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				typeSpecArray_AST = (TypeInfoAST)currentAST.root;
				typeSpecArray_AST.setTypeInfo(t_AST);
				typeSpecArray_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = typeSpecArray_AST;
		_retTree = _t;
	}
	
	public final void builtInType(AST _t) throws RecognitionException {
		
		TypeInfoAST builtInType_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST builtInType_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case LITERAL_void:
			{
				TypeInfoAST tmp61_AST = null;
				TypeInfoAST tmp61_AST_in = null;
				tmp61_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp61_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp61_AST);
				match(_t,LITERAL_void);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Void.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_boolean:
			{
				TypeInfoAST tmp62_AST = null;
				TypeInfoAST tmp62_AST_in = null;
				tmp62_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp62_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp62_AST);
				match(_t,LITERAL_boolean);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Boolean.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_byte:
			{
				TypeInfoAST tmp63_AST = null;
				TypeInfoAST tmp63_AST_in = null;
				tmp63_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp63_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp63_AST);
				match(_t,LITERAL_byte);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Byte.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_char:
			{
				TypeInfoAST tmp64_AST = null;
				TypeInfoAST tmp64_AST_in = null;
				tmp64_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp64_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp64_AST);
				match(_t,LITERAL_char);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Character.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_short:
			{
				TypeInfoAST tmp65_AST = null;
				TypeInfoAST tmp65_AST_in = null;
				tmp65_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp65_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp65_AST);
				match(_t,LITERAL_short);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Short.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_int:
			{
				TypeInfoAST tmp66_AST = null;
				TypeInfoAST tmp66_AST_in = null;
				tmp66_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp66_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp66_AST);
				match(_t,LITERAL_int);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Integer.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_float:
			{
				TypeInfoAST tmp67_AST = null;
				TypeInfoAST tmp67_AST_in = null;
				tmp67_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp67_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp67_AST);
				match(_t,LITERAL_float);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Float.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_long:
			{
				TypeInfoAST tmp68_AST = null;
				TypeInfoAST tmp68_AST_in = null;
				tmp68_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp68_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp68_AST);
				match(_t,LITERAL_long);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Long.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_double:
			{
				TypeInfoAST tmp69_AST = null;
				TypeInfoAST tmp69_AST_in = null;
				tmp69_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp69_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp69_AST);
				match(_t,LITERAL_double);
				_t = _t.getNextSibling();
				builtInType_AST = (TypeInfoAST)currentAST.root;
				builtInType_AST.setTypeInfo(Double.TYPE);
				builtInType_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = builtInType_AST;
		_retTree = _t;
	}
	
	public final void typeSpec(AST _t) throws RecognitionException {
		
		TypeInfoAST typeSpec_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST typeSpec_AST = null;
		TypeInfoAST tSA_AST = null;
		TypeInfoAST tSA = null;
		
		try {      // for error handling
			AST __t75 = _t;
			TypeInfoAST tmp70_AST = null;
			TypeInfoAST tmp70_AST_in = null;
			tmp70_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp70_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp70_AST);
			ASTPair __currentAST75 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,TYPE);
			_t = _t.getFirstChild();
			tSA = _t==ASTNULL ? null : (TypeInfoAST)_t;
			typeSpecArray(_t);
			_t = _retTree;
			tSA_AST = (TypeInfoAST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST75;
			_t = __t75;
			_t = _t.getNextSibling();
			typeSpec_AST = (TypeInfoAST)currentAST.root;
			typeSpec_AST.setTypeInfo(tSA_AST);
			typeSpec_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = typeSpec_AST;
		_retTree = _t;
	}
	
	public final void newExpression(AST _t) throws RecognitionException {
		
		TypeInfoAST newExpression_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST newExpression_AST = null;
		TypeInfoAST t_AST = null;
		TypeInfoAST t = null;
		
		try {      // for error handling
			AST __t67 = _t;
			TypeInfoAST tmp71_AST = null;
			TypeInfoAST tmp71_AST_in = null;
			tmp71_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp71_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp71_AST);
			ASTPair __currentAST67 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,LITERAL_new);
			_t = _t.getFirstChild();
			t = _t==ASTNULL ? null : (TypeInfoAST)_t;
			type(_t);
			_t = _retTree;
			t_AST = (TypeInfoAST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			newExpression_AST = (TypeInfoAST)currentAST.root;
			newExpression_AST.setTypeInfo(t_AST);
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ARRAY_DECLARATOR:
			{
				newArrayDeclarator(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case ARRAY_INIT:
				{
					arrayInitializer(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case 3:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				break;
			}
			case ELIST:
			{
				elist(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			currentAST = __currentAST67;
			_t = __t67;
			_t = _t.getNextSibling();
			newExpression_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = newExpression_AST;
		_retTree = _t;
	}
	
	public final void constant(AST _t) throws RecognitionException {
		
		TypeInfoAST constant_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST constant_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case NUM_INT:
			{
				TypeInfoAST tmp72_AST = null;
				TypeInfoAST tmp72_AST_in = null;
				tmp72_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp72_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp72_AST);
				match(_t,NUM_INT);
				_t = _t.getNextSibling();
				constant_AST = (TypeInfoAST)currentAST.root;
				constant_AST.setTypeInfo(Integer.TYPE);
				constant_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case CHAR_LITERAL:
			{
				TypeInfoAST tmp73_AST = null;
				TypeInfoAST tmp73_AST_in = null;
				tmp73_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp73_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp73_AST);
				match(_t,CHAR_LITERAL);
				_t = _t.getNextSibling();
				constant_AST = (TypeInfoAST)currentAST.root;
				constant_AST.setTypeInfo(Character.TYPE);
				constant_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case STRING_LITERAL:
			{
				TypeInfoAST tmp74_AST = null;
				TypeInfoAST tmp74_AST_in = null;
				tmp74_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp74_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp74_AST);
				match(_t,STRING_LITERAL);
				_t = _t.getNextSibling();
				constant_AST = (TypeInfoAST)currentAST.root;
				constant_AST.setTypeInfo(String.class);
				constant_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case NUM_FLOAT:
			{
				TypeInfoAST tmp75_AST = null;
				TypeInfoAST tmp75_AST_in = null;
				tmp75_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp75_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp75_AST);
				match(_t,NUM_FLOAT);
				_t = _t.getNextSibling();
				constant_AST = (TypeInfoAST)currentAST.root;
				constant_AST.setTypeInfo(Float.TYPE);
				constant_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case NUM_DOUBLE:
			{
				TypeInfoAST tmp76_AST = null;
				TypeInfoAST tmp76_AST_in = null;
				tmp76_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp76_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp76_AST);
				match(_t,NUM_DOUBLE);
				_t = _t.getNextSibling();
				constant_AST = (TypeInfoAST)currentAST.root;
				constant_AST.setTypeInfo(Double.TYPE);
				constant_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case NUM_LONG:
			{
				TypeInfoAST tmp77_AST = null;
				TypeInfoAST tmp77_AST_in = null;
				tmp77_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp77_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp77_AST);
				match(_t,NUM_LONG);
				_t = _t.getNextSibling();
				constant_AST = (TypeInfoAST)currentAST.root;
				constant_AST.setTypeInfo(Long.TYPE);
				constant_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = constant_AST;
		_retTree = _t;
	}
	
	public final void type(AST _t) throws RecognitionException {
		
		TypeInfoAST type_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST type_AST = null;
		TypeInfoAST i_AST = null;
		TypeInfoAST i = null;
		TypeInfoAST bIT_AST = null;
		TypeInfoAST bIT = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			case DOT:
			{
				i = _t==ASTNULL ? null : (TypeInfoAST)_t;
				identifier(_t);
				_t = _retTree;
				i_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				type_AST = (TypeInfoAST)currentAST.root;
				type_AST.setTypeInfo(symbolLookupType(i_AST.getTypeText()));
				type_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			{
				bIT = _t==ASTNULL ? null : (TypeInfoAST)_t;
				builtInType(_t);
				_t = _retTree;
				bIT_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				type_AST = (TypeInfoAST)currentAST.root;
				type_AST.setTypeInfo(bIT_AST);
				type_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = type_AST;
		_retTree = _t;
	}
	
	public final void newArrayDeclarator(AST _t) throws RecognitionException {
		
		TypeInfoAST newArrayDeclarator_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST newArrayDeclarator_AST = null;
		
		try {      // for error handling
			AST __t71 = _t;
			TypeInfoAST tmp78_AST = null;
			TypeInfoAST tmp78_AST_in = null;
			tmp78_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
			tmp78_AST_in = (TypeInfoAST)_t;
			astFactory.addASTChild(currentAST, tmp78_AST);
			ASTPair __currentAST71 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,ARRAY_DECLARATOR);
			_t = _t.getFirstChild();
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ARRAY_DECLARATOR:
			{
				newArrayDeclarator(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case 3:
			case EXPR:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case EXPR:
			{
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case 3:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			currentAST = __currentAST71;
			_t = __t71;
			_t = _t.getNextSibling();
			newArrayDeclarator_AST = (TypeInfoAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = newArrayDeclarator_AST;
		_retTree = _t;
	}
	
	public final void identifier(AST _t) throws RecognitionException {
		
		TypeInfoAST identifier_AST_in = (TypeInfoAST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		TypeInfoAST identifier_AST = null;
		TypeInfoAST i1 = null;
		TypeInfoAST i1_AST = null;
		TypeInfoAST i2_AST = null;
		TypeInfoAST i2 = null;
		TypeInfoAST i3 = null;
		TypeInfoAST i3_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			{
				i1 = (TypeInfoAST)_t;
				i1_AST = (TypeInfoAST)astFactory.create(i1);
				astFactory.addASTChild(currentAST, i1_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				identifier_AST = (TypeInfoAST)currentAST.root;
				identifier_AST.setTypeText(i1_AST.getText());
				identifier_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			case DOT:
			{
				AST __t80 = _t;
				TypeInfoAST tmp79_AST = null;
				TypeInfoAST tmp79_AST_in = null;
				tmp79_AST = (TypeInfoAST)astFactory.create((TypeInfoAST)_t);
				tmp79_AST_in = (TypeInfoAST)_t;
				astFactory.addASTChild(currentAST, tmp79_AST);
				ASTPair __currentAST80 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DOT);
				_t = _t.getFirstChild();
				i2 = _t==ASTNULL ? null : (TypeInfoAST)_t;
				identifier(_t);
				_t = _retTree;
				i2_AST = (TypeInfoAST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				i3 = (TypeInfoAST)_t;
				i3_AST = (TypeInfoAST)astFactory.create(i3);
				astFactory.addASTChild(currentAST, i3_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				currentAST = __currentAST80;
				_t = __t80;
				_t = _t.getNextSibling();
				identifier_AST = (TypeInfoAST)currentAST.root;
				identifier_AST.setTypeText(i2_AST.getTypeText() + "." + i3_AST.getText());
				identifier_AST = (TypeInfoAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = identifier_AST;
		_retTree = _t;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"BLOCK",
		"MODIFIERS",
		"OBJBLOCK",
		"SLIST",
		"CTOR_DEF",
		"METHOD_DEF",
		"VARIABLE_DEF",
		"INSTANCE_INIT",
		"STATIC_INIT",
		"TYPE",
		"CLASS_DEF",
		"INTERFACE_DEF",
		"PACKAGE_DEF",
		"ARRAY_DECLARATOR",
		"EXTENDS_CLAUSE",
		"IMPLEMENTS_CLAUSE",
		"PARAMETERS",
		"PARAMETER_DEF",
		"LABELED_STAT",
		"TYPECAST",
		"INDEX_OP",
		"POST_INC",
		"POST_DEC",
		"METHOD_CALL",
		"EXPR",
		"ARRAY_INIT",
		"IMPORT",
		"UNARY_MINUS",
		"UNARY_PLUS",
		"CASE_GROUP",
		"ELIST",
		"FOR_INIT",
		"FOR_CONDITION",
		"FOR_ITERATOR",
		"EMPTY_STAT",
		"\"final\"",
		"\"abstract\"",
		"\"strictfp\"",
		"SUPER_CTOR_CALL",
		"CTOR_CALL",
		"COMMENT_DEF",
		"\"package\"",
		"SEMI",
		"\"import\"",
		"LBRACK",
		"RBRACK",
		"\"void\"",
		"\"boolean\"",
		"\"byte\"",
		"\"char\"",
		"\"short\"",
		"\"int\"",
		"\"float\"",
		"\"long\"",
		"\"double\"",
		"an identifier",
		"DOT",
		"STAR",
		"\"private\"",
		"\"public\"",
		"\"protected\"",
		"\"static\"",
		"\"transient\"",
		"\"native\"",
		"\"threadsafe\"",
		"\"synchronized\"",
		"\"volatile\"",
		"\"class\"",
		"\"extends\"",
		"\"interface\"",
		"LCURLY",
		"RCURLY",
		"COMMA",
		"\"implements\"",
		"LPAREN",
		"RPAREN",
		"\"this\"",
		"\"super\"",
		"ASSIGN",
		"\"throws\"",
		"COLON",
		"\"if\"",
		"\"else\"",
		"\"for\"",
		"\"while\"",
		"\"do\"",
		"\"break\"",
		"\"continue\"",
		"\"return\"",
		"\"switch\"",
		"\"throw\"",
		"\"case\"",
		"\"default\"",
		"\"try\"",
		"\"finally\"",
		"\"catch\"",
		"PLUS_ASSIGN",
		"MINUS_ASSIGN",
		"STAR_ASSIGN",
		"DIV_ASSIGN",
		"MOD_ASSIGN",
		"SR_ASSIGN",
		"BSR_ASSIGN",
		"SL_ASSIGN",
		"BAND_ASSIGN",
		"BXOR_ASSIGN",
		"BOR_ASSIGN",
		"QUESTION",
		"LOR",
		"LAND",
		"BOR",
		"BXOR",
		"BAND",
		"NOT_EQUAL",
		"EQUAL",
		"LT",
		"GT",
		"LE",
		"GE",
		"\"instanceof\"",
		"SL",
		"SR",
		"BSR",
		"PLUS",
		"MINUS",
		"DIV",
		"MOD",
		"INC",
		"DEC",
		"BNOT",
		"LNOT",
		"\"true\"",
		"\"false\"",
		"\"null\"",
		"\"new\"",
		"a number",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"NUM_FLOAT",
		"NUM_LONG",
		"NUM_DOUBLE",
		"WS",
		"SL_COMMENT",
		"ML_COMMENT",
		"ESC",
		"HEX_DIGIT",
		"VOCAB",
		"EXPONENT",
		"FLOAT_SUFFIX"
	};
	
	}
	
