// $ANTLR 2.7.2: "unittest.tree.g" -> "UnitTestParser.java"$

    package bluej.parser.ast.gen;
    
    import bluej.parser.SourceSpan;
    import bluej.parser.SourceLocation;
	import bluej.parser.ast.LocatableAST;
	    
    import java.util.*;
    import antlr.BaseAST;

import antlr.TreeParser;
import antlr.Token;
import antlr.collections.AST;
import antlr.RecognitionException;
import antlr.ANTLRException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.collections.impl.BitSet;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;


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
public class UnitTestParser extends antlr.TreeParser       implements UnitTestParserTokenTypes
 {

    /**
     * Locate the comment token associated with a method/class.
     * We do this by looking at the hidden tokens associated with
     * both the modifiers (public, static etc) of the method/class
     * and then keyword associated (ie 'class' for classes, or the method
     * name for methods).
     *
     * ie.
     * \** asdasd *\ public void methodName(int x)
     *               ^mods       ^keyword
     *
     * \** asdasd *\ class MyClass {
     *               ^keyword
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
public UnitTestParser() {
	tokenNames = _tokenNames;
}

	public final void compilationUnit(AST _t) throws RecognitionException {
		
		AST compilationUnit_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST compilationUnit_AST = null;
		
		try {      // for error handling
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case PACKAGE_DEF:
			{
				packageDefinition(_t);
				_t = _retTree;
				break;
			}
			case 3:
			case CLASS_DEF:
			case INTERFACE_DEF:
			case IMPORT:
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
			_loop4:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==IMPORT)) {
					importDefinition(_t);
					_t = _retTree;
				}
				else {
					break _loop4;
				}
				
			} while (true);
			}
			{
			_loop6:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==CLASS_DEF||_t.getType()==INTERFACE_DEF)) {
					typeDefinition(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop6;
				}
				
			} while (true);
			}
			compilationUnit_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = compilationUnit_AST;
		_retTree = _t;
	}
	
	public final void packageDefinition(AST _t) throws RecognitionException {
		
		AST packageDefinition_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST packageDefinition_AST = null;
		
		try {      // for error handling
			AST __t8 = _t;
			AST tmp1_AST = null;
			AST tmp1_AST_in = null;
			tmp1_AST = astFactory.create((AST)_t);
			tmp1_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp1_AST);
			ASTPair __currentAST8 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,PACKAGE_DEF);
			_t = _t.getFirstChild();
			identifier(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST8;
			_t = __t8;
			_t = _t.getNextSibling();
			packageDefinition_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = packageDefinition_AST;
		_retTree = _t;
	}
	
	public final void importDefinition(AST _t) throws RecognitionException {
		
		AST importDefinition_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST importDefinition_AST = null;
		
		try {      // for error handling
			AST __t10 = _t;
			AST tmp2_AST = null;
			AST tmp2_AST_in = null;
			tmp2_AST = astFactory.create((AST)_t);
			tmp2_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp2_AST);
			ASTPair __currentAST10 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,IMPORT);
			_t = _t.getFirstChild();
			identifierStar(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST10;
			_t = __t10;
			_t = _t.getNextSibling();
			importDefinition_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = importDefinition_AST;
		_retTree = _t;
	}
	
	public final void typeDefinition(AST _t) throws RecognitionException {
		
		AST typeDefinition_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeDefinition_AST = null;
		AST m_AST = null;
		AST m = null;
		AST i = null;
		AST i_AST = null;
		AST ec_AST = null;
		AST ec = null;
		AST ob_AST = null;
		AST ob = null;
		AST im_AST = null;
		AST im = null;
		AST ii = null;
		AST ii_AST = null;
		AST iec_AST = null;
		AST iec = null;
		AST ib_AST = null;
		AST ib = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case CLASS_DEF:
			{
				AST __t12 = _t;
				AST tmp3_AST = null;
				AST tmp3_AST_in = null;
				tmp3_AST = astFactory.create((AST)_t);
				tmp3_AST_in = (AST)_t;
				ASTPair __currentAST12 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,CLASS_DEF);
				_t = _t.getFirstChild();
				m = _t==ASTNULL ? null : (AST)_t;
				modifiers(_t);
				_t = _retTree;
				m_AST = (AST)returnAST;
				i = (AST)_t;
				AST i_AST_in = null;
				i_AST = astFactory.create(i);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				ec = _t==ASTNULL ? null : (AST)_t;
				extendsClause(_t);
				_t = _retTree;
				ec_AST = (AST)returnAST;
				implementsClause(_t);
				_t = _retTree;
				ob = _t==ASTNULL ? null : (AST)_t;
				objBlock(_t);
				_t = _retTree;
				ob_AST = (AST)returnAST;
				currentAST = __currentAST12;
				_t = __t12;
				_t = _t.getNextSibling();
				typeDefinition_AST = (AST)currentAST.root;
				
				Token commentToken = helpFindComment(m, i);
				AST comment;
				
				if (commentToken != null)
				comment = new LocatableAST(commentToken);
				else
				comment = null;
				
							// lc and rc and the left and right curly brackets associated with this
							// class definition. We have stored them in the LocatableAST as
							// 'important' tokens (done inside java.g)
				LocatableAST lc = new LocatableAST(((LocatableAST) ob).getImportantToken(0));
				LocatableAST rc = new LocatableAST(((LocatableAST) ob).getImportantToken(1));
				
				typeDefinition_AST = (AST)astFactory.make( (new ASTArray(7)).add(i_AST).add(lc).add(rc).add(m_AST).add(ec_AST).add(ob_AST).add((AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(COMMENT_DEF,"COMMENT_DEF")).add(comment)))); 
					
				currentAST.root = typeDefinition_AST;
				currentAST.child = typeDefinition_AST!=null &&typeDefinition_AST.getFirstChild()!=null ?
					typeDefinition_AST.getFirstChild() : typeDefinition_AST;
				currentAST.advanceChildToEnd();
				break;
			}
			case INTERFACE_DEF:
			{
				AST __t13 = _t;
				AST tmp4_AST = null;
				AST tmp4_AST_in = null;
				tmp4_AST = astFactory.create((AST)_t);
				tmp4_AST_in = (AST)_t;
				ASTPair __currentAST13 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,INTERFACE_DEF);
				_t = _t.getFirstChild();
				im = _t==ASTNULL ? null : (AST)_t;
				modifiers(_t);
				_t = _retTree;
				im_AST = (AST)returnAST;
				ii = (AST)_t;
				AST ii_AST_in = null;
				ii_AST = astFactory.create(ii);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				iec = _t==ASTNULL ? null : (AST)_t;
				extendsClause(_t);
				_t = _retTree;
				iec_AST = (AST)returnAST;
				ib = _t==ASTNULL ? null : (AST)_t;
				interfaceBlock(_t);
				_t = _retTree;
				ib_AST = (AST)returnAST;
				currentAST = __currentAST13;
				_t = __t13;
				_t = _t.getNextSibling();
				typeDefinition_AST = (AST)currentAST.root;
				
				Token commentToken = helpFindComment(im, ii);
				AST comment;
				
				if (commentToken != null)
				comment = new LocatableAST(commentToken);
				else
				comment = null;
				
							// lc and rc and the left and right curly brackets associated with this
							// class definition. We have stored them in the LocatableAST as
							// 'important' tokens (done inside java.g)
				LocatableAST lc = new LocatableAST(((LocatableAST) ib).getImportantToken(0));
				LocatableAST rc = new LocatableAST(((LocatableAST) ib).getImportantToken(1));
				
				typeDefinition_AST = (AST)astFactory.make( (new ASTArray(7)).add(ii_AST).add(lc).add(rc).add(im_AST).add(iec_AST).add(ib_AST).add((AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(COMMENT_DEF,"COMMENT_DEF")).add(comment)))); 
					
				currentAST.root = typeDefinition_AST;
				currentAST.child = typeDefinition_AST!=null &&typeDefinition_AST.getFirstChild()!=null ?
					typeDefinition_AST.getFirstChild() : typeDefinition_AST;
				currentAST.advanceChildToEnd();
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
		returnAST = typeDefinition_AST;
		_retTree = _t;
	}
	
	public final void identifier(AST _t) throws RecognitionException {
		
		AST identifier_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST identifier_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			{
				AST tmp5_AST = null;
				AST tmp5_AST_in = null;
				tmp5_AST = astFactory.create((AST)_t);
				tmp5_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp5_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				identifier_AST = (AST)currentAST.root;
				break;
			}
			case DOT:
			{
				AST __t75 = _t;
				AST tmp6_AST = null;
				AST tmp6_AST_in = null;
				tmp6_AST = astFactory.create((AST)_t);
				tmp6_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp6_AST);
				ASTPair __currentAST75 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DOT);
				_t = _t.getFirstChild();
				identifier(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp7_AST = null;
				AST tmp7_AST_in = null;
				tmp7_AST = astFactory.create((AST)_t);
				tmp7_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp7_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				currentAST = __currentAST75;
				_t = __t75;
				_t = _t.getNextSibling();
				identifier_AST = (AST)currentAST.root;
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
	
	public final void identifierStar(AST _t) throws RecognitionException {
		
		AST identifierStar_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST identifierStar_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			{
				AST tmp8_AST = null;
				AST tmp8_AST_in = null;
				tmp8_AST = astFactory.create((AST)_t);
				tmp8_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp8_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				identifierStar_AST = (AST)currentAST.root;
				break;
			}
			case DOT:
			{
				AST __t77 = _t;
				AST tmp9_AST = null;
				AST tmp9_AST_in = null;
				tmp9_AST = astFactory.create((AST)_t);
				tmp9_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp9_AST);
				ASTPair __currentAST77 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DOT);
				_t = _t.getFirstChild();
				identifier(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case STAR:
				{
					AST tmp10_AST = null;
					AST tmp10_AST_in = null;
					tmp10_AST = astFactory.create((AST)_t);
					tmp10_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp10_AST);
					match(_t,STAR);
					_t = _t.getNextSibling();
					break;
				}
				case IDENT:
				{
					AST tmp11_AST = null;
					AST tmp11_AST_in = null;
					tmp11_AST = astFactory.create((AST)_t);
					tmp11_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp11_AST);
					match(_t,IDENT);
					_t = _t.getNextSibling();
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				currentAST = __currentAST77;
				_t = __t77;
				_t = _t.getNextSibling();
				identifierStar_AST = (AST)currentAST.root;
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
		returnAST = identifierStar_AST;
		_retTree = _t;
	}
	
	public final void modifiers(AST _t) throws RecognitionException {
		
		AST modifiers_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST modifiers_AST = null;
		
		try {      // for error handling
			AST __t21 = _t;
			AST tmp12_AST = null;
			AST tmp12_AST_in = null;
			tmp12_AST = astFactory.create((AST)_t);
			tmp12_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp12_AST);
			ASTPair __currentAST21 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,MODIFIERS);
			_t = _t.getFirstChild();
			{
			_loop23:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_tokenSet_0.member(_t.getType()))) {
					modifier(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop23;
				}
				
			} while (true);
			}
			currentAST = __currentAST21;
			_t = __t21;
			_t = _t.getNextSibling();
			modifiers_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = modifiers_AST;
		_retTree = _t;
	}
	
	public final void extendsClause(AST _t) throws RecognitionException {
		
		AST extendsClause_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST extendsClause_AST = null;
		AST id_AST = null;
		AST id = null;
		
		try {      // for error handling
			AST __t26 = _t;
			AST tmp13_AST = null;
			AST tmp13_AST_in = null;
			tmp13_AST = astFactory.create((AST)_t);
			tmp13_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp13_AST);
			ASTPair __currentAST26 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,EXTENDS_CLAUSE);
			_t = _t.getFirstChild();
			{
			_loop28:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==IDENT||_t.getType()==DOT)) {
					id = _t==ASTNULL ? null : (AST)_t;
					identifier(_t);
					_t = _retTree;
					id_AST = (AST)returnAST;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop28;
				}
				
			} while (true);
			}
			currentAST = __currentAST26;
			_t = __t26;
			_t = _t.getNextSibling();
			extendsClause_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = extendsClause_AST;
		_retTree = _t;
	}
	
	public final void implementsClause(AST _t) throws RecognitionException {
		
		AST implementsClause_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST implementsClause_AST = null;
		
		try {      // for error handling
			AST __t30 = _t;
			AST tmp14_AST = null;
			AST tmp14_AST_in = null;
			tmp14_AST = astFactory.create((AST)_t);
			tmp14_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp14_AST);
			ASTPair __currentAST30 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,IMPLEMENTS_CLAUSE);
			_t = _t.getFirstChild();
			{
			_loop32:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==IDENT||_t.getType()==DOT)) {
					identifier(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop32;
				}
				
			} while (true);
			}
			currentAST = __currentAST30;
			_t = __t30;
			_t = _t.getNextSibling();
			implementsClause_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = implementsClause_AST;
		_retTree = _t;
	}
	
	public final void objBlock(AST _t) throws RecognitionException {
		
		AST objBlock_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST objBlock_AST = null;
		
		try {      // for error handling
			AST __t38 = _t;
			AST tmp15_AST = null;
			AST tmp15_AST_in = null;
			tmp15_AST = astFactory.create((AST)_t);
			tmp15_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp15_AST);
			ASTPair __currentAST38 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,OBJBLOCK);
			_t = _t.getFirstChild();
			{
			_loop42:
			do {
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case CTOR_DEF:
				{
					ctorDef(_t);
					_t = _retTree;
					break;
				}
				case METHOD_DEF:
				{
					methodDef(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case VARIABLE_DEF:
				{
					variableDef(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case CLASS_DEF:
				case INTERFACE_DEF:
				{
					typeDefinition(_t);
					_t = _retTree;
					break;
				}
				case STATIC_INIT:
				{
					AST __t40 = _t;
					AST tmp16_AST = null;
					AST tmp16_AST_in = null;
					tmp16_AST = astFactory.create((AST)_t);
					tmp16_AST_in = (AST)_t;
					ASTPair __currentAST40 = currentAST.copy();
					currentAST.root = currentAST.child;
					currentAST.child = null;
					match(_t,STATIC_INIT);
					_t = _t.getFirstChild();
					slist(_t);
					_t = _retTree;
					currentAST = __currentAST40;
					_t = __t40;
					_t = _t.getNextSibling();
					break;
				}
				case INSTANCE_INIT:
				{
					AST __t41 = _t;
					AST tmp17_AST = null;
					AST tmp17_AST_in = null;
					tmp17_AST = astFactory.create((AST)_t);
					tmp17_AST_in = (AST)_t;
					ASTPair __currentAST41 = currentAST.copy();
					currentAST.root = currentAST.child;
					currentAST.child = null;
					match(_t,INSTANCE_INIT);
					_t = _t.getFirstChild();
					slist(_t);
					_t = _retTree;
					currentAST = __currentAST41;
					_t = __t41;
					_t = _t.getNextSibling();
					break;
				}
				default:
				{
					break _loop42;
				}
				}
			} while (true);
			}
			currentAST = __currentAST38;
			_t = __t38;
			_t = _t.getNextSibling();
			objBlock_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = objBlock_AST;
		_retTree = _t;
	}
	
	public final void interfaceBlock(AST _t) throws RecognitionException {
		
		AST interfaceBlock_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interfaceBlock_AST = null;
		
		try {      // for error handling
			AST __t34 = _t;
			AST tmp18_AST = null;
			AST tmp18_AST_in = null;
			tmp18_AST = astFactory.create((AST)_t);
			tmp18_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp18_AST);
			ASTPair __currentAST34 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,OBJBLOCK);
			_t = _t.getFirstChild();
			{
			_loop36:
			do {
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case METHOD_DEF:
				{
					methodDecl(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case VARIABLE_DEF:
				{
					variableDef(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				default:
				{
					break _loop36;
				}
				}
			} while (true);
			}
			currentAST = __currentAST34;
			_t = __t34;
			_t = _t.getNextSibling();
			interfaceBlock_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = interfaceBlock_AST;
		_retTree = _t;
	}
	
	public final void typeSpec(AST _t) throws RecognitionException {
		
		AST typeSpec_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeSpec_AST = null;
		
		try {      // for error handling
			AST __t15 = _t;
			AST tmp19_AST = null;
			AST tmp19_AST_in = null;
			tmp19_AST = astFactory.create((AST)_t);
			tmp19_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp19_AST);
			ASTPair __currentAST15 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,TYPE);
			_t = _t.getFirstChild();
			typeSpecArray(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST15;
			_t = __t15;
			_t = _t.getNextSibling();
			typeSpec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = typeSpec_AST;
		_retTree = _t;
	}
	
	public final void typeSpecArray(AST _t) throws RecognitionException {
		
		AST typeSpecArray_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeSpecArray_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ARRAY_DECLARATOR:
			{
				AST __t17 = _t;
				AST tmp20_AST = null;
				AST tmp20_AST_in = null;
				tmp20_AST = astFactory.create((AST)_t);
				tmp20_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp20_AST);
				ASTPair __currentAST17 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,ARRAY_DECLARATOR);
				_t = _t.getFirstChild();
				typeSpecArray(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST17;
				_t = __t17;
				_t = _t.getNextSibling();
				typeSpecArray_AST = (AST)currentAST.root;
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
				type(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				typeSpecArray_AST = (AST)currentAST.root;
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
	
	public final void type(AST _t) throws RecognitionException {
		
		AST type_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			case DOT:
			{
				identifier(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				type_AST = (AST)currentAST.root;
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
				builtInType(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				type_AST = (AST)currentAST.root;
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
	
	public final void builtInType(AST _t) throws RecognitionException {
		
		AST builtInType_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST builtInType_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case LITERAL_void:
			{
				AST tmp21_AST = null;
				AST tmp21_AST_in = null;
				tmp21_AST = astFactory.create((AST)_t);
				tmp21_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp21_AST);
				match(_t,LITERAL_void);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_boolean:
			{
				AST tmp22_AST = null;
				AST tmp22_AST_in = null;
				tmp22_AST = astFactory.create((AST)_t);
				tmp22_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp22_AST);
				match(_t,LITERAL_boolean);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_byte:
			{
				AST tmp23_AST = null;
				AST tmp23_AST_in = null;
				tmp23_AST = astFactory.create((AST)_t);
				tmp23_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp23_AST);
				match(_t,LITERAL_byte);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_char:
			{
				AST tmp24_AST = null;
				AST tmp24_AST_in = null;
				tmp24_AST = astFactory.create((AST)_t);
				tmp24_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp24_AST);
				match(_t,LITERAL_char);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_short:
			{
				AST tmp25_AST = null;
				AST tmp25_AST_in = null;
				tmp25_AST = astFactory.create((AST)_t);
				tmp25_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp25_AST);
				match(_t,LITERAL_short);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_int:
			{
				AST tmp26_AST = null;
				AST tmp26_AST_in = null;
				tmp26_AST = astFactory.create((AST)_t);
				tmp26_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp26_AST);
				match(_t,LITERAL_int);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_float:
			{
				AST tmp27_AST = null;
				AST tmp27_AST_in = null;
				tmp27_AST = astFactory.create((AST)_t);
				tmp27_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp27_AST);
				match(_t,LITERAL_float);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_long:
			{
				AST tmp28_AST = null;
				AST tmp28_AST_in = null;
				tmp28_AST = astFactory.create((AST)_t);
				tmp28_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp28_AST);
				match(_t,LITERAL_long);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_double:
			{
				AST tmp29_AST = null;
				AST tmp29_AST_in = null;
				tmp29_AST = astFactory.create((AST)_t);
				tmp29_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp29_AST);
				match(_t,LITERAL_double);
				_t = _t.getNextSibling();
				builtInType_AST = (AST)currentAST.root;
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
	
	public final void modifier(AST _t) throws RecognitionException {
		
		AST modifier_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST modifier_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case LITERAL_private:
			{
				AST tmp30_AST = null;
				AST tmp30_AST_in = null;
				tmp30_AST = astFactory.create((AST)_t);
				tmp30_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp30_AST);
				match(_t,LITERAL_private);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_public:
			{
				AST tmp31_AST = null;
				AST tmp31_AST_in = null;
				tmp31_AST = astFactory.create((AST)_t);
				tmp31_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp31_AST);
				match(_t,LITERAL_public);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_protected:
			{
				AST tmp32_AST = null;
				AST tmp32_AST_in = null;
				tmp32_AST = astFactory.create((AST)_t);
				tmp32_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp32_AST);
				match(_t,LITERAL_protected);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_static:
			{
				AST tmp33_AST = null;
				AST tmp33_AST_in = null;
				tmp33_AST = astFactory.create((AST)_t);
				tmp33_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp33_AST);
				match(_t,LITERAL_static);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_transient:
			{
				AST tmp34_AST = null;
				AST tmp34_AST_in = null;
				tmp34_AST = astFactory.create((AST)_t);
				tmp34_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp34_AST);
				match(_t,LITERAL_transient);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case FINAL:
			{
				AST tmp35_AST = null;
				AST tmp35_AST_in = null;
				tmp35_AST = astFactory.create((AST)_t);
				tmp35_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp35_AST);
				match(_t,FINAL);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case ABSTRACT:
			{
				AST tmp36_AST = null;
				AST tmp36_AST_in = null;
				tmp36_AST = astFactory.create((AST)_t);
				tmp36_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp36_AST);
				match(_t,ABSTRACT);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_native:
			{
				AST tmp37_AST = null;
				AST tmp37_AST_in = null;
				tmp37_AST = astFactory.create((AST)_t);
				tmp37_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp37_AST);
				match(_t,LITERAL_native);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_threadsafe:
			{
				AST tmp38_AST = null;
				AST tmp38_AST_in = null;
				tmp38_AST = astFactory.create((AST)_t);
				tmp38_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp38_AST);
				match(_t,LITERAL_threadsafe);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_synchronized:
			{
				AST tmp39_AST = null;
				AST tmp39_AST_in = null;
				tmp39_AST = astFactory.create((AST)_t);
				tmp39_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp39_AST);
				match(_t,LITERAL_synchronized);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_const:
			{
				AST tmp40_AST = null;
				AST tmp40_AST_in = null;
				tmp40_AST = astFactory.create((AST)_t);
				tmp40_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp40_AST);
				match(_t,LITERAL_const);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_volatile:
			{
				AST tmp41_AST = null;
				AST tmp41_AST_in = null;
				tmp41_AST = astFactory.create((AST)_t);
				tmp41_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp41_AST);
				match(_t,LITERAL_volatile);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
				break;
			}
			case STRICTFP:
			{
				AST tmp42_AST = null;
				AST tmp42_AST_in = null;
				tmp42_AST = astFactory.create((AST)_t);
				tmp42_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp42_AST);
				match(_t,STRICTFP);
				_t = _t.getNextSibling();
				modifier_AST = (AST)currentAST.root;
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
		returnAST = modifier_AST;
		_retTree = _t;
	}
	
	public final void methodDecl(AST _t) throws RecognitionException {
		
		AST methodDecl_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST methodDecl_AST = null;
		
		try {      // for error handling
			AST __t47 = _t;
			AST tmp43_AST = null;
			AST tmp43_AST_in = null;
			tmp43_AST = astFactory.create((AST)_t);
			tmp43_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp43_AST);
			ASTPair __currentAST47 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,METHOD_DEF);
			_t = _t.getFirstChild();
			modifiers(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			typeSpec(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			methodHead(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST47;
			_t = __t47;
			_t = _t.getNextSibling();
			methodDecl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = methodDecl_AST;
		_retTree = _t;
	}
	
	public final void variableDef(AST _t) throws RecognitionException {
		
		AST variableDef_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST variableDef_AST = null;
		AST m_AST = null;
		AST m = null;
		AST t_AST = null;
		AST t = null;
		
		try {      // for error handling
			AST __t52 = _t;
			AST tmp44_AST = null;
			AST tmp44_AST_in = null;
			tmp44_AST = astFactory.create((AST)_t);
			tmp44_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp44_AST);
			ASTPair __currentAST52 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,VARIABLE_DEF);
			_t = _t.getFirstChild();
			m = _t==ASTNULL ? null : (AST)_t;
			modifiers(_t);
			_t = _retTree;
			m_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			t = _t==ASTNULL ? null : (AST)_t;
			typeSpec(_t);
			_t = _retTree;
			t_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			variableDeclarator(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			varInitializer(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST52;
			_t = __t52;
			_t = _t.getNextSibling();
			variableDef_AST = (AST)currentAST.root;
			
			LocatableAST semiholder = (LocatableAST) variableDef_AST;
			
			if (semiholder.getImportantTokenCount() > 0) {
				            AST semi = new LocatableAST(semiholder.getImportantToken(0));
			
			variableDef_AST = (AST)astFactory.make( (new ASTArray(3)).add(tmp44_AST).add(findFirstChild(m_AST,t_AST)).add(semi)); 
			}
			else {
			// note: we get here when there are multiple declarations of a
			// variable on a line ie int x=2,y=3;
			// will come here for the y=3 declaration. As this is encompassed
			// in the int x=2...; declaration we can throw this away.
			variableDef_AST = null; 
			}
				
			currentAST.root = variableDef_AST;
			currentAST.child = variableDef_AST!=null &&variableDef_AST.getFirstChild()!=null ?
				variableDef_AST.getFirstChild() : variableDef_AST;
			currentAST.advanceChildToEnd();
			variableDef_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = variableDef_AST;
		_retTree = _t;
	}
	
	public final void ctorDef(AST _t) throws RecognitionException {
		
		AST ctorDef_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST ctorDef_AST = null;
		
		try {      // for error handling
			AST __t44 = _t;
			AST tmp45_AST = null;
			AST tmp45_AST_in = null;
			tmp45_AST = astFactory.create((AST)_t);
			tmp45_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp45_AST);
			ASTPair __currentAST44 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,CTOR_DEF);
			_t = _t.getFirstChild();
			modifiers(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			methodHead(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case SLIST:
			{
				slist(_t);
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
			currentAST = __currentAST44;
			_t = __t44;
			_t = _t.getNextSibling();
			ctorDef_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = ctorDef_AST;
		_retTree = _t;
	}
	
	public final void methodDef(AST _t) throws RecognitionException {
		
		AST methodDef_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST methodDef_AST = null;
		AST m_AST = null;
		AST m = null;
		AST t_AST = null;
		AST t = null;
		AST mh_AST = null;
		AST mh = null;
		AST sl_AST = null;
		AST sl = null;
		
		try {      // for error handling
			AST __t49 = _t;
			AST tmp46_AST = null;
			AST tmp46_AST_in = null;
			tmp46_AST = astFactory.create((AST)_t);
			tmp46_AST_in = (AST)_t;
			ASTPair __currentAST49 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,METHOD_DEF);
			_t = _t.getFirstChild();
			m = _t==ASTNULL ? null : (AST)_t;
			modifiers(_t);
			_t = _retTree;
			m_AST = (AST)returnAST;
			t = _t==ASTNULL ? null : (AST)_t;
			typeSpec(_t);
			_t = _retTree;
			t_AST = (AST)returnAST;
			mh = _t==ASTNULL ? null : (AST)_t;
			methodHead(_t);
			_t = _retTree;
			mh_AST = (AST)returnAST;
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case SLIST:
			{
				sl = _t==ASTNULL ? null : (AST)_t;
				slist(_t);
				_t = _retTree;
				sl_AST = (AST)returnAST;
				methodDef_AST = (AST)currentAST.root;
				
				Token commentToken = helpFindComment(m, t);
				AST comment;
				
				if (commentToken != null)
				comment = new LocatableAST(commentToken);
				else
				comment = null;
				
				LocatableAST bracketholder = (LocatableAST) sl;
				
				if (bracketholder.getImportantTokenCount() > 0) {
				AST bracket = new LocatableAST(bracketholder.getImportantToken(0));
				methodDef_AST = (AST)astFactory.make( (new ASTArray(5)).add(astFactory.create(METHOD_DEF,"METHOD_DEF")).add(mh_AST).add(sl_AST).add(bracket).add((AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(COMMENT_DEF,"COMMENT_DEF")).add(comment))));
				}
					
				currentAST.root = methodDef_AST;
				currentAST.child = methodDef_AST!=null &&methodDef_AST.getFirstChild()!=null ?
					methodDef_AST.getFirstChild() : methodDef_AST;
				currentAST.advanceChildToEnd();
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
			currentAST = __currentAST49;
			_t = __t49;
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = methodDef_AST;
		_retTree = _t;
	}
	
	public final void slist(AST _t) throws RecognitionException {
		
		AST slist_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST slist_AST = null;
		
		try {      // for error handling
			AST __t80 = _t;
			AST tmp47_AST = null;
			AST tmp47_AST_in = null;
			tmp47_AST = astFactory.create((AST)_t);
			tmp47_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp47_AST);
			ASTPair __currentAST80 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,SLIST);
			_t = _t.getFirstChild();
			{
			_loop82:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_tokenSet_1.member(_t.getType()))) {
					stat(_t);
					_t = _retTree;
				}
				else {
					break _loop82;
				}
				
			} while (true);
			}
			currentAST = __currentAST80;
			_t = __t80;
			_t = _t.getNextSibling();
			slist_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = slist_AST;
		_retTree = _t;
	}
	
	public final void methodHead(AST _t) throws RecognitionException {
		
		AST methodHead_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST methodHead_AST = null;
		AST i = null;
		AST i_AST = null;
		AST pd_AST = null;
		AST pd = null;
		
		try {      // for error handling
			i = (AST)_t;
			AST i_AST_in = null;
			i_AST = astFactory.create(i);
			astFactory.makeASTRoot(currentAST, i_AST);
			match(_t,IDENT);
			_t = _t.getNextSibling();
			AST __t66 = _t;
			AST tmp48_AST = null;
			AST tmp48_AST_in = null;
			tmp48_AST = astFactory.create((AST)_t);
			tmp48_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp48_AST);
			ASTPair __currentAST66 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,PARAMETERS);
			_t = _t.getFirstChild();
			{
			_loop68:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==PARAMETER_DEF)) {
					pd = _t==ASTNULL ? null : (AST)_t;
					parameterDef(_t);
					_t = _retTree;
					pd_AST = (AST)returnAST;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop68;
				}
				
			} while (true);
			}
			currentAST = __currentAST66;
			_t = __t66;
			_t = _t.getNextSibling();
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case LITERAL_throws:
			{
				throwsClause(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case 3:
			case SLIST:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			methodHead_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = methodHead_AST;
		_retTree = _t;
	}
	
	public final void variableDeclarator(AST _t) throws RecognitionException {
		
		AST variableDeclarator_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST variableDeclarator_AST = null;
		AST i = null;
		AST i_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			{
				i = (AST)_t;
				AST i_AST_in = null;
				i_AST = astFactory.create(i);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				break;
			}
			case LBRACK:
			{
				AST tmp49_AST = null;
				AST tmp49_AST_in = null;
				tmp49_AST = astFactory.create((AST)_t);
				tmp49_AST_in = (AST)_t;
				match(_t,LBRACK);
				_t = _t.getNextSibling();
				variableDeclarator(_t);
				_t = _retTree;
				variableDeclarator_AST = (AST)currentAST.root;
				
					    variableDeclarator_AST = i_AST;
					
				currentAST.root = variableDeclarator_AST;
				currentAST.child = variableDeclarator_AST!=null &&variableDeclarator_AST.getFirstChild()!=null ?
					variableDeclarator_AST.getFirstChild() : variableDeclarator_AST;
				currentAST.advanceChildToEnd();
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
		returnAST = variableDeclarator_AST;
		_retTree = _t;
	}
	
	public final void varInitializer(AST _t) throws RecognitionException {
		
		AST varInitializer_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST varInitializer_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ASSIGN:
			{
				AST __t59 = _t;
				AST tmp50_AST = null;
				AST tmp50_AST_in = null;
				tmp50_AST = astFactory.create((AST)_t);
				tmp50_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp50_AST);
				ASTPair __currentAST59 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,ASSIGN);
				_t = _t.getFirstChild();
				initializer(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST59;
				_t = __t59;
				_t = _t.getNextSibling();
				varInitializer_AST = (AST)currentAST.root;
				break;
			}
			case 3:
			{
				varInitializer_AST = (AST)currentAST.root;
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
		returnAST = varInitializer_AST;
		_retTree = _t;
	}
	
	public final void parameterDef(AST _t) throws RecognitionException {
		
		AST parameterDef_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parameterDef_AST = null;
		
		try {      // for error handling
			AST __t54 = _t;
			AST tmp51_AST = null;
			AST tmp51_AST_in = null;
			tmp51_AST = astFactory.create((AST)_t);
			tmp51_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp51_AST);
			ASTPair __currentAST54 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,PARAMETER_DEF);
			_t = _t.getFirstChild();
			modifiers(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			typeSpec(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp52_AST = null;
			AST tmp52_AST_in = null;
			tmp52_AST = astFactory.create((AST)_t);
			tmp52_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp52_AST);
			match(_t,IDENT);
			_t = _t.getNextSibling();
			currentAST = __currentAST54;
			_t = __t54;
			_t = _t.getNextSibling();
			parameterDef_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = parameterDef_AST;
		_retTree = _t;
	}
	
	public final void objectinitializer(AST _t) throws RecognitionException {
		
		AST objectinitializer_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST objectinitializer_AST = null;
		
		try {      // for error handling
			AST __t56 = _t;
			AST tmp53_AST = null;
			AST tmp53_AST_in = null;
			tmp53_AST = astFactory.create((AST)_t);
			tmp53_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp53_AST);
			ASTPair __currentAST56 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,INSTANCE_INIT);
			_t = _t.getFirstChild();
			slist(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST56;
			_t = __t56;
			_t = _t.getNextSibling();
			objectinitializer_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = objectinitializer_AST;
		_retTree = _t;
	}
	
	public final void initializer(AST _t) throws RecognitionException {
		
		AST initializer_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST initializer_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case EXPR:
			{
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				initializer_AST = (AST)currentAST.root;
				break;
			}
			case ARRAY_INIT:
			{
				arrayInitializer(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				initializer_AST = (AST)currentAST.root;
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
		
		AST expression_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expression_AST = null;
		
		try {      // for error handling
			AST __t125 = _t;
			AST tmp54_AST = null;
			AST tmp54_AST_in = null;
			tmp54_AST = astFactory.create((AST)_t);
			tmp54_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp54_AST);
			ASTPair __currentAST125 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,EXPR);
			_t = _t.getFirstChild();
			expr(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST125;
			_t = __t125;
			_t = _t.getNextSibling();
			expression_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = expression_AST;
		_retTree = _t;
	}
	
	public final void arrayInitializer(AST _t) throws RecognitionException {
		
		AST arrayInitializer_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST arrayInitializer_AST = null;
		
		try {      // for error handling
			AST __t62 = _t;
			AST tmp55_AST = null;
			AST tmp55_AST_in = null;
			tmp55_AST = astFactory.create((AST)_t);
			tmp55_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp55_AST);
			ASTPair __currentAST62 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,ARRAY_INIT);
			_t = _t.getFirstChild();
			{
			_loop64:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==EXPR||_t.getType()==ARRAY_INIT)) {
					initializer(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop64;
				}
				
			} while (true);
			}
			currentAST = __currentAST62;
			_t = __t62;
			_t = _t.getNextSibling();
			arrayInitializer_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = arrayInitializer_AST;
		_retTree = _t;
	}
	
	public final void throwsClause(AST _t) throws RecognitionException {
		
		AST throwsClause_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST throwsClause_AST = null;
		
		try {      // for error handling
			AST __t71 = _t;
			AST tmp56_AST = null;
			AST tmp56_AST_in = null;
			tmp56_AST = astFactory.create((AST)_t);
			tmp56_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp56_AST);
			ASTPair __currentAST71 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,LITERAL_throws);
			_t = _t.getFirstChild();
			{
			_loop73:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==IDENT||_t.getType()==DOT)) {
					identifier(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop73;
				}
				
			} while (true);
			}
			currentAST = __currentAST71;
			_t = __t71;
			_t = _t.getNextSibling();
			throwsClause_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = throwsClause_AST;
		_retTree = _t;
	}
	
	public final void stat(AST _t) throws RecognitionException {
		
		AST stat_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST stat_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case CLASS_DEF:
			case INTERFACE_DEF:
			{
				typeDefinition(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat_AST = (AST)currentAST.root;
				break;
			}
			case VARIABLE_DEF:
			{
				variableDef(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat_AST = (AST)currentAST.root;
				break;
			}
			case EXPR:
			{
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LABELED_STAT:
			{
				AST __t84 = _t;
				AST tmp57_AST = null;
				AST tmp57_AST_in = null;
				tmp57_AST = astFactory.create((AST)_t);
				tmp57_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp57_AST);
				ASTPair __currentAST84 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LABELED_STAT);
				_t = _t.getFirstChild();
				AST tmp58_AST = null;
				AST tmp58_AST_in = null;
				tmp58_AST = astFactory.create((AST)_t);
				tmp58_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp58_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				stat(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST84;
				_t = __t84;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_if:
			{
				AST __t85 = _t;
				AST tmp59_AST = null;
				AST tmp59_AST_in = null;
				tmp59_AST = astFactory.create((AST)_t);
				tmp59_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp59_AST);
				ASTPair __currentAST85 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_if);
				_t = _t.getFirstChild();
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case SLIST:
				case VARIABLE_DEF:
				case CLASS_DEF:
				case INTERFACE_DEF:
				case LABELED_STAT:
				case EXPR:
				case EMPTY_STAT:
				case LITERAL_synchronized:
				case LITERAL_if:
				case LITERAL_for:
				case LITERAL_while:
				case LITERAL_do:
				case LITERAL_break:
				case LITERAL_continue:
				case LITERAL_return:
				case LITERAL_switch:
				case LITERAL_throw:
				case LITERAL_try:
				{
					stat(_t);
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
				currentAST = __currentAST85;
				_t = __t85;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_for:
			{
				AST __t87 = _t;
				AST tmp60_AST = null;
				AST tmp60_AST_in = null;
				tmp60_AST = astFactory.create((AST)_t);
				tmp60_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp60_AST);
				ASTPair __currentAST87 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_for);
				_t = _t.getFirstChild();
				AST __t88 = _t;
				AST tmp61_AST = null;
				AST tmp61_AST_in = null;
				tmp61_AST = astFactory.create((AST)_t);
				tmp61_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp61_AST);
				ASTPair __currentAST88 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,FOR_INIT);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case VARIABLE_DEF:
				{
					variableDef(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case ELIST:
				{
					elist(_t);
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
				currentAST = __currentAST88;
				_t = __t88;
				_t = _t.getNextSibling();
				AST __t90 = _t;
				AST tmp62_AST = null;
				AST tmp62_AST_in = null;
				tmp62_AST = astFactory.create((AST)_t);
				tmp62_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp62_AST);
				ASTPair __currentAST90 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,FOR_CONDITION);
				_t = _t.getFirstChild();
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
				currentAST = __currentAST90;
				_t = __t90;
				_t = _t.getNextSibling();
				AST __t92 = _t;
				AST tmp63_AST = null;
				AST tmp63_AST_in = null;
				tmp63_AST = astFactory.create((AST)_t);
				tmp63_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp63_AST);
				ASTPair __currentAST92 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,FOR_ITERATOR);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case ELIST:
				{
					elist(_t);
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
				currentAST = __currentAST92;
				_t = __t92;
				_t = _t.getNextSibling();
				stat(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST87;
				_t = __t87;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_while:
			{
				AST __t94 = _t;
				AST tmp64_AST = null;
				AST tmp64_AST_in = null;
				tmp64_AST = astFactory.create((AST)_t);
				tmp64_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp64_AST);
				ASTPair __currentAST94 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_while);
				_t = _t.getFirstChild();
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST94;
				_t = __t94;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_do:
			{
				AST __t95 = _t;
				AST tmp65_AST = null;
				AST tmp65_AST_in = null;
				tmp65_AST = astFactory.create((AST)_t);
				tmp65_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp65_AST);
				ASTPair __currentAST95 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_do);
				_t = _t.getFirstChild();
				stat(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST95;
				_t = __t95;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_break:
			{
				AST __t96 = _t;
				AST tmp66_AST = null;
				AST tmp66_AST_in = null;
				tmp66_AST = astFactory.create((AST)_t);
				tmp66_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp66_AST);
				ASTPair __currentAST96 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_break);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case IDENT:
				{
					AST tmp67_AST = null;
					AST tmp67_AST_in = null;
					tmp67_AST = astFactory.create((AST)_t);
					tmp67_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp67_AST);
					match(_t,IDENT);
					_t = _t.getNextSibling();
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
				currentAST = __currentAST96;
				_t = __t96;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_continue:
			{
				AST __t98 = _t;
				AST tmp68_AST = null;
				AST tmp68_AST_in = null;
				tmp68_AST = astFactory.create((AST)_t);
				tmp68_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp68_AST);
				ASTPair __currentAST98 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_continue);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case IDENT:
				{
					AST tmp69_AST = null;
					AST tmp69_AST_in = null;
					tmp69_AST = astFactory.create((AST)_t);
					tmp69_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp69_AST);
					match(_t,IDENT);
					_t = _t.getNextSibling();
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
				currentAST = __currentAST98;
				_t = __t98;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_return:
			{
				AST __t100 = _t;
				AST tmp70_AST = null;
				AST tmp70_AST_in = null;
				tmp70_AST = astFactory.create((AST)_t);
				tmp70_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp70_AST);
				ASTPair __currentAST100 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_return);
				_t = _t.getFirstChild();
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
				currentAST = __currentAST100;
				_t = __t100;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_switch:
			{
				AST __t102 = _t;
				AST tmp71_AST = null;
				AST tmp71_AST_in = null;
				tmp71_AST = astFactory.create((AST)_t);
				tmp71_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp71_AST);
				ASTPair __currentAST102 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_switch);
				_t = _t.getFirstChild();
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				{
				_loop104:
				do {
					if (_t==null) _t=ASTNULL;
					if ((_t.getType()==CASE_GROUP)) {
						caseGroup(_t);
						_t = _retTree;
						astFactory.addASTChild(currentAST, returnAST);
					}
					else {
						break _loop104;
					}
					
				} while (true);
				}
				currentAST = __currentAST102;
				_t = __t102;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_throw:
			{
				AST __t105 = _t;
				AST tmp72_AST = null;
				AST tmp72_AST_in = null;
				tmp72_AST = astFactory.create((AST)_t);
				tmp72_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp72_AST);
				ASTPair __currentAST105 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_throw);
				_t = _t.getFirstChild();
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST105;
				_t = __t105;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_synchronized:
			{
				AST __t106 = _t;
				AST tmp73_AST = null;
				AST tmp73_AST_in = null;
				tmp73_AST = astFactory.create((AST)_t);
				tmp73_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp73_AST);
				ASTPair __currentAST106 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_synchronized);
				_t = _t.getFirstChild();
				expression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST106;
				_t = __t106;
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_try:
			{
				tryBlock(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat_AST = (AST)currentAST.root;
				break;
			}
			case SLIST:
			{
				slist(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				stat_AST = (AST)currentAST.root;
				break;
			}
			case EMPTY_STAT:
			{
				AST tmp74_AST = null;
				AST tmp74_AST_in = null;
				tmp74_AST = astFactory.create((AST)_t);
				tmp74_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp74_AST);
				match(_t,EMPTY_STAT);
				_t = _t.getNextSibling();
				stat_AST = (AST)currentAST.root;
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
		returnAST = stat_AST;
		_retTree = _t;
	}
	
	public final void elist(AST _t) throws RecognitionException {
		
		AST elist_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST elist_AST = null;
		
		try {      // for error handling
			AST __t121 = _t;
			AST tmp75_AST = null;
			AST tmp75_AST_in = null;
			tmp75_AST = astFactory.create((AST)_t);
			tmp75_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp75_AST);
			ASTPair __currentAST121 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,ELIST);
			_t = _t.getFirstChild();
			{
			_loop123:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==EXPR)) {
					expression(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop123;
				}
				
			} while (true);
			}
			currentAST = __currentAST121;
			_t = __t121;
			_t = _t.getNextSibling();
			elist_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = elist_AST;
		_retTree = _t;
	}
	
	public final void caseGroup(AST _t) throws RecognitionException {
		
		AST caseGroup_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST caseGroup_AST = null;
		
		try {      // for error handling
			AST __t108 = _t;
			AST tmp76_AST = null;
			AST tmp76_AST_in = null;
			tmp76_AST = astFactory.create((AST)_t);
			tmp76_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp76_AST);
			ASTPair __currentAST108 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,CASE_GROUP);
			_t = _t.getFirstChild();
			{
			int _cnt111=0;
			_loop111:
			do {
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case LITERAL_case:
				{
					AST __t110 = _t;
					AST tmp77_AST = null;
					AST tmp77_AST_in = null;
					tmp77_AST = astFactory.create((AST)_t);
					tmp77_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp77_AST);
					ASTPair __currentAST110 = currentAST.copy();
					currentAST.root = currentAST.child;
					currentAST.child = null;
					match(_t,LITERAL_case);
					_t = _t.getFirstChild();
					expression(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					currentAST = __currentAST110;
					_t = __t110;
					_t = _t.getNextSibling();
					break;
				}
				case LITERAL_default:
				{
					AST tmp78_AST = null;
					AST tmp78_AST_in = null;
					tmp78_AST = astFactory.create((AST)_t);
					tmp78_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp78_AST);
					match(_t,LITERAL_default);
					_t = _t.getNextSibling();
					break;
				}
				default:
				{
					if ( _cnt111>=1 ) { break _loop111; } else {throw new NoViableAltException(_t);}
				}
				}
				_cnt111++;
			} while (true);
			}
			slist(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST108;
			_t = __t108;
			_t = _t.getNextSibling();
			caseGroup_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = caseGroup_AST;
		_retTree = _t;
	}
	
	public final void tryBlock(AST _t) throws RecognitionException {
		
		AST tryBlock_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST tryBlock_AST = null;
		
		try {      // for error handling
			AST __t113 = _t;
			AST tmp79_AST = null;
			AST tmp79_AST_in = null;
			tmp79_AST = astFactory.create((AST)_t);
			tmp79_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp79_AST);
			ASTPair __currentAST113 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,LITERAL_try);
			_t = _t.getFirstChild();
			slist(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop115:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==LITERAL_catch)) {
					handler(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop115;
				}
				
			} while (true);
			}
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case LITERAL_finally:
			{
				AST __t117 = _t;
				AST tmp80_AST = null;
				AST tmp80_AST_in = null;
				tmp80_AST = astFactory.create((AST)_t);
				tmp80_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp80_AST);
				ASTPair __currentAST117 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LITERAL_finally);
				_t = _t.getFirstChild();
				slist(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST117;
				_t = __t117;
				_t = _t.getNextSibling();
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
			currentAST = __currentAST113;
			_t = __t113;
			_t = _t.getNextSibling();
			tryBlock_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = tryBlock_AST;
		_retTree = _t;
	}
	
	public final void handler(AST _t) throws RecognitionException {
		
		AST handler_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST handler_AST = null;
		
		try {      // for error handling
			AST __t119 = _t;
			AST tmp81_AST = null;
			AST tmp81_AST_in = null;
			tmp81_AST = astFactory.create((AST)_t);
			tmp81_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp81_AST);
			ASTPair __currentAST119 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,LITERAL_catch);
			_t = _t.getFirstChild();
			parameterDef(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			slist(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST119;
			_t = __t119;
			_t = _t.getNextSibling();
			handler_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = handler_AST;
		_retTree = _t;
	}
	
	public final void expr(AST _t) throws RecognitionException {
		
		AST expr_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expr_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case QUESTION:
			{
				AST __t127 = _t;
				AST tmp82_AST = null;
				AST tmp82_AST_in = null;
				tmp82_AST = astFactory.create((AST)_t);
				tmp82_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp82_AST);
				ASTPair __currentAST127 = currentAST.copy();
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
				currentAST = __currentAST127;
				_t = __t127;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case ASSIGN:
			{
				AST __t128 = _t;
				AST tmp83_AST = null;
				AST tmp83_AST_in = null;
				tmp83_AST = astFactory.create((AST)_t);
				tmp83_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp83_AST);
				ASTPair __currentAST128 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST128;
				_t = __t128;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case PLUS_ASSIGN:
			{
				AST __t129 = _t;
				AST tmp84_AST = null;
				AST tmp84_AST_in = null;
				tmp84_AST = astFactory.create((AST)_t);
				tmp84_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp84_AST);
				ASTPair __currentAST129 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,PLUS_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST129;
				_t = __t129;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case MINUS_ASSIGN:
			{
				AST __t130 = _t;
				AST tmp85_AST = null;
				AST tmp85_AST_in = null;
				tmp85_AST = astFactory.create((AST)_t);
				tmp85_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp85_AST);
				ASTPair __currentAST130 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MINUS_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST130;
				_t = __t130;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case STAR_ASSIGN:
			{
				AST __t131 = _t;
				AST tmp86_AST = null;
				AST tmp86_AST_in = null;
				tmp86_AST = astFactory.create((AST)_t);
				tmp86_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp86_AST);
				ASTPair __currentAST131 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,STAR_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST131;
				_t = __t131;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case DIV_ASSIGN:
			{
				AST __t132 = _t;
				AST tmp87_AST = null;
				AST tmp87_AST_in = null;
				tmp87_AST = astFactory.create((AST)_t);
				tmp87_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp87_AST);
				ASTPair __currentAST132 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DIV_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST132;
				_t = __t132;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case MOD_ASSIGN:
			{
				AST __t133 = _t;
				AST tmp88_AST = null;
				AST tmp88_AST_in = null;
				tmp88_AST = astFactory.create((AST)_t);
				tmp88_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp88_AST);
				ASTPair __currentAST133 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MOD_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST133;
				_t = __t133;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case SR_ASSIGN:
			{
				AST __t134 = _t;
				AST tmp89_AST = null;
				AST tmp89_AST_in = null;
				tmp89_AST = astFactory.create((AST)_t);
				tmp89_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp89_AST);
				ASTPair __currentAST134 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SR_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST134;
				_t = __t134;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BSR_ASSIGN:
			{
				AST __t135 = _t;
				AST tmp90_AST = null;
				AST tmp90_AST_in = null;
				tmp90_AST = astFactory.create((AST)_t);
				tmp90_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp90_AST);
				ASTPair __currentAST135 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BSR_ASSIGN);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST135;
				_t = __t135;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case SL_ASSIGN:
			{
				AST __t136 = _t;
				AST tmp91_AST = null;
				AST tmp91_AST_in = null;
				tmp91_AST = astFactory.create((AST)_t);
				tmp91_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp91_AST);
				ASTPair __currentAST136 = currentAST.copy();
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
				currentAST = __currentAST136;
				_t = __t136;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BAND_ASSIGN:
			{
				AST __t137 = _t;
				AST tmp92_AST = null;
				AST tmp92_AST_in = null;
				tmp92_AST = astFactory.create((AST)_t);
				tmp92_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp92_AST);
				ASTPair __currentAST137 = currentAST.copy();
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
				currentAST = __currentAST137;
				_t = __t137;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BXOR_ASSIGN:
			{
				AST __t138 = _t;
				AST tmp93_AST = null;
				AST tmp93_AST_in = null;
				tmp93_AST = astFactory.create((AST)_t);
				tmp93_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp93_AST);
				ASTPair __currentAST138 = currentAST.copy();
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
				currentAST = __currentAST138;
				_t = __t138;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BOR_ASSIGN:
			{
				AST __t139 = _t;
				AST tmp94_AST = null;
				AST tmp94_AST_in = null;
				tmp94_AST = astFactory.create((AST)_t);
				tmp94_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp94_AST);
				ASTPair __currentAST139 = currentAST.copy();
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
				currentAST = __currentAST139;
				_t = __t139;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case LOR:
			{
				AST __t140 = _t;
				AST tmp95_AST = null;
				AST tmp95_AST_in = null;
				tmp95_AST = astFactory.create((AST)_t);
				tmp95_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp95_AST);
				ASTPair __currentAST140 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LOR);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST140;
				_t = __t140;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case LAND:
			{
				AST __t141 = _t;
				AST tmp96_AST = null;
				AST tmp96_AST_in = null;
				tmp96_AST = astFactory.create((AST)_t);
				tmp96_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp96_AST);
				ASTPair __currentAST141 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LAND);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST141;
				_t = __t141;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BOR:
			{
				AST __t142 = _t;
				AST tmp97_AST = null;
				AST tmp97_AST_in = null;
				tmp97_AST = astFactory.create((AST)_t);
				tmp97_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp97_AST);
				ASTPair __currentAST142 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BOR);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST142;
				_t = __t142;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BXOR:
			{
				AST __t143 = _t;
				AST tmp98_AST = null;
				AST tmp98_AST_in = null;
				tmp98_AST = astFactory.create((AST)_t);
				tmp98_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp98_AST);
				ASTPair __currentAST143 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BXOR);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST143;
				_t = __t143;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BAND:
			{
				AST __t144 = _t;
				AST tmp99_AST = null;
				AST tmp99_AST_in = null;
				tmp99_AST = astFactory.create((AST)_t);
				tmp99_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp99_AST);
				ASTPair __currentAST144 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BAND);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST144;
				_t = __t144;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case NOT_EQUAL:
			{
				AST __t145 = _t;
				AST tmp100_AST = null;
				AST tmp100_AST_in = null;
				tmp100_AST = astFactory.create((AST)_t);
				tmp100_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp100_AST);
				ASTPair __currentAST145 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,NOT_EQUAL);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST145;
				_t = __t145;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case EQUAL:
			{
				AST __t146 = _t;
				AST tmp101_AST = null;
				AST tmp101_AST_in = null;
				tmp101_AST = astFactory.create((AST)_t);
				tmp101_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp101_AST);
				ASTPair __currentAST146 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,EQUAL);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST146;
				_t = __t146;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case LT:
			{
				AST __t147 = _t;
				AST tmp102_AST = null;
				AST tmp102_AST_in = null;
				tmp102_AST = astFactory.create((AST)_t);
				tmp102_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp102_AST);
				ASTPair __currentAST147 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LT);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST147;
				_t = __t147;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case GT:
			{
				AST __t148 = _t;
				AST tmp103_AST = null;
				AST tmp103_AST_in = null;
				tmp103_AST = astFactory.create((AST)_t);
				tmp103_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp103_AST);
				ASTPair __currentAST148 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,GT);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST148;
				_t = __t148;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case LE:
			{
				AST __t149 = _t;
				AST tmp104_AST = null;
				AST tmp104_AST_in = null;
				tmp104_AST = astFactory.create((AST)_t);
				tmp104_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp104_AST);
				ASTPair __currentAST149 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LE);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST149;
				_t = __t149;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case GE:
			{
				AST __t150 = _t;
				AST tmp105_AST = null;
				AST tmp105_AST_in = null;
				tmp105_AST = astFactory.create((AST)_t);
				tmp105_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp105_AST);
				ASTPair __currentAST150 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,GE);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST150;
				_t = __t150;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case SL:
			{
				AST __t151 = _t;
				AST tmp106_AST = null;
				AST tmp106_AST_in = null;
				tmp106_AST = astFactory.create((AST)_t);
				tmp106_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp106_AST);
				ASTPair __currentAST151 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SL);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST151;
				_t = __t151;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case SR:
			{
				AST __t152 = _t;
				AST tmp107_AST = null;
				AST tmp107_AST_in = null;
				tmp107_AST = astFactory.create((AST)_t);
				tmp107_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp107_AST);
				ASTPair __currentAST152 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SR);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST152;
				_t = __t152;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BSR:
			{
				AST __t153 = _t;
				AST tmp108_AST = null;
				AST tmp108_AST_in = null;
				tmp108_AST = astFactory.create((AST)_t);
				tmp108_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp108_AST);
				ASTPair __currentAST153 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BSR);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST153;
				_t = __t153;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case PLUS:
			{
				AST __t154 = _t;
				AST tmp109_AST = null;
				AST tmp109_AST_in = null;
				tmp109_AST = astFactory.create((AST)_t);
				tmp109_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp109_AST);
				ASTPair __currentAST154 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,PLUS);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST154;
				_t = __t154;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case MINUS:
			{
				AST __t155 = _t;
				AST tmp110_AST = null;
				AST tmp110_AST_in = null;
				tmp110_AST = astFactory.create((AST)_t);
				tmp110_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp110_AST);
				ASTPair __currentAST155 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MINUS);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST155;
				_t = __t155;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case DIV:
			{
				AST __t156 = _t;
				AST tmp111_AST = null;
				AST tmp111_AST_in = null;
				tmp111_AST = astFactory.create((AST)_t);
				tmp111_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp111_AST);
				ASTPair __currentAST156 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DIV);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST156;
				_t = __t156;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case MOD:
			{
				AST __t157 = _t;
				AST tmp112_AST = null;
				AST tmp112_AST_in = null;
				tmp112_AST = astFactory.create((AST)_t);
				tmp112_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp112_AST);
				ASTPair __currentAST157 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,MOD);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST157;
				_t = __t157;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case STAR:
			{
				AST __t158 = _t;
				AST tmp113_AST = null;
				AST tmp113_AST_in = null;
				tmp113_AST = astFactory.create((AST)_t);
				tmp113_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp113_AST);
				ASTPair __currentAST158 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,STAR);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST158;
				_t = __t158;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case INC:
			{
				AST __t159 = _t;
				AST tmp114_AST = null;
				AST tmp114_AST_in = null;
				tmp114_AST = astFactory.create((AST)_t);
				tmp114_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp114_AST);
				ASTPair __currentAST159 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,INC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST159;
				_t = __t159;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case DEC:
			{
				AST __t160 = _t;
				AST tmp115_AST = null;
				AST tmp115_AST_in = null;
				tmp115_AST = astFactory.create((AST)_t);
				tmp115_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp115_AST);
				ASTPair __currentAST160 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,DEC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST160;
				_t = __t160;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case POST_INC:
			{
				AST __t161 = _t;
				AST tmp116_AST = null;
				AST tmp116_AST_in = null;
				tmp116_AST = astFactory.create((AST)_t);
				tmp116_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp116_AST);
				ASTPair __currentAST161 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,POST_INC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST161;
				_t = __t161;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case POST_DEC:
			{
				AST __t162 = _t;
				AST tmp117_AST = null;
				AST tmp117_AST_in = null;
				tmp117_AST = astFactory.create((AST)_t);
				tmp117_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp117_AST);
				ASTPair __currentAST162 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,POST_DEC);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST162;
				_t = __t162;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case BNOT:
			{
				AST __t163 = _t;
				AST tmp118_AST = null;
				AST tmp118_AST_in = null;
				tmp118_AST = astFactory.create((AST)_t);
				tmp118_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp118_AST);
				ASTPair __currentAST163 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,BNOT);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST163;
				_t = __t163;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case LNOT:
			{
				AST __t164 = _t;
				AST tmp119_AST = null;
				AST tmp119_AST_in = null;
				tmp119_AST = astFactory.create((AST)_t);
				tmp119_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp119_AST);
				ASTPair __currentAST164 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,LNOT);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST164;
				_t = __t164;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_instanceof:
			{
				AST __t165 = _t;
				AST tmp120_AST = null;
				AST tmp120_AST_in = null;
				tmp120_AST = astFactory.create((AST)_t);
				tmp120_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp120_AST);
				ASTPair __currentAST165 = currentAST.copy();
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
				currentAST = __currentAST165;
				_t = __t165;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case UNARY_MINUS:
			{
				AST __t166 = _t;
				AST tmp121_AST = null;
				AST tmp121_AST_in = null;
				tmp121_AST = astFactory.create((AST)_t);
				tmp121_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp121_AST);
				ASTPair __currentAST166 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,UNARY_MINUS);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST166;
				_t = __t166;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case UNARY_PLUS:
			{
				AST __t167 = _t;
				AST tmp122_AST = null;
				AST tmp122_AST_in = null;
				tmp122_AST = astFactory.create((AST)_t);
				tmp122_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp122_AST);
				ASTPair __currentAST167 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,UNARY_PLUS);
				_t = _t.getFirstChild();
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST167;
				_t = __t167;
				_t = _t.getNextSibling();
				expr_AST = (AST)currentAST.root;
				break;
			}
			case TYPE:
			case TYPECAST:
			case INDEX_OP:
			case METHOD_CALL:
			case SUPER_CTOR_CALL:
			case CTOR_CALL:
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
				primaryExpression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr_AST = (AST)currentAST.root;
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
		
		AST primaryExpression_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST primaryExpression_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case IDENT:
			{
				AST tmp123_AST = null;
				AST tmp123_AST_in = null;
				tmp123_AST = astFactory.create((AST)_t);
				tmp123_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp123_AST);
				match(_t,IDENT);
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case DOT:
			{
				AST __t169 = _t;
				AST tmp124_AST = null;
				AST tmp124_AST_in = null;
				tmp124_AST = astFactory.create((AST)_t);
				tmp124_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp124_AST);
				ASTPair __currentAST169 = currentAST.copy();
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
				case SUPER_CTOR_CALL:
				case CTOR_CALL:
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
					expr(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					{
					if (_t==null) _t=ASTNULL;
					switch ( _t.getType()) {
					case IDENT:
					{
						AST tmp125_AST = null;
						AST tmp125_AST_in = null;
						tmp125_AST = astFactory.create((AST)_t);
						tmp125_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp125_AST);
						match(_t,IDENT);
						_t = _t.getNextSibling();
						break;
					}
					case INDEX_OP:
					{
						arrayIndex(_t);
						_t = _retTree;
						astFactory.addASTChild(currentAST, returnAST);
						break;
					}
					case LITERAL_this:
					{
						AST tmp126_AST = null;
						AST tmp126_AST_in = null;
						tmp126_AST = astFactory.create((AST)_t);
						tmp126_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp126_AST);
						match(_t,LITERAL_this);
						_t = _t.getNextSibling();
						break;
					}
					case LITERAL_class:
					{
						AST tmp127_AST = null;
						AST tmp127_AST_in = null;
						tmp127_AST = astFactory.create((AST)_t);
						tmp127_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp127_AST);
						match(_t,LITERAL_class);
						_t = _t.getNextSibling();
						break;
					}
					case LITERAL_new:
					{
						AST __t172 = _t;
						AST tmp128_AST = null;
						AST tmp128_AST_in = null;
						tmp128_AST = astFactory.create((AST)_t);
						tmp128_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp128_AST);
						ASTPair __currentAST172 = currentAST.copy();
						currentAST.root = currentAST.child;
						currentAST.child = null;
						match(_t,LITERAL_new);
						_t = _t.getFirstChild();
						AST tmp129_AST = null;
						AST tmp129_AST_in = null;
						tmp129_AST = astFactory.create((AST)_t);
						tmp129_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp129_AST);
						match(_t,IDENT);
						_t = _t.getNextSibling();
						elist(_t);
						_t = _retTree;
						astFactory.addASTChild(currentAST, returnAST);
						currentAST = __currentAST172;
						_t = __t172;
						_t = _t.getNextSibling();
						break;
					}
					case LITERAL_super:
					{
						AST tmp130_AST = null;
						AST tmp130_AST_in = null;
						tmp130_AST = astFactory.create((AST)_t);
						tmp130_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp130_AST);
						match(_t,LITERAL_super);
						_t = _t.getNextSibling();
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
					AST __t173 = _t;
					AST tmp131_AST = null;
					AST tmp131_AST_in = null;
					tmp131_AST = astFactory.create((AST)_t);
					tmp131_AST_in = (AST)_t;
					astFactory.addASTChild(currentAST, tmp131_AST);
					ASTPair __currentAST173 = currentAST.copy();
					currentAST.root = currentAST.child;
					currentAST.child = null;
					match(_t,ARRAY_DECLARATOR);
					_t = _t.getFirstChild();
					typeSpecArray(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					currentAST = __currentAST173;
					_t = __t173;
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
					builtInType(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					{
					if (_t==null) _t=ASTNULL;
					switch ( _t.getType()) {
					case LITERAL_class:
					{
						AST tmp132_AST = null;
						AST tmp132_AST_in = null;
						tmp132_AST = astFactory.create((AST)_t);
						tmp132_AST_in = (AST)_t;
						astFactory.addASTChild(currentAST, tmp132_AST);
						match(_t,LITERAL_class);
						_t = _t.getNextSibling();
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
				currentAST = __currentAST169;
				_t = __t169;
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case INDEX_OP:
			{
				arrayIndex(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case METHOD_CALL:
			{
				AST __t175 = _t;
				AST tmp133_AST = null;
				AST tmp133_AST_in = null;
				tmp133_AST = astFactory.create((AST)_t);
				tmp133_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp133_AST);
				ASTPair __currentAST175 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,METHOD_CALL);
				_t = _t.getFirstChild();
				primaryExpression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				elist(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST175;
				_t = __t175;
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case SUPER_CTOR_CALL:
			case CTOR_CALL:
			{
				ctorCall(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case TYPECAST:
			{
				AST __t176 = _t;
				AST tmp134_AST = null;
				AST tmp134_AST_in = null;
				tmp134_AST = astFactory.create((AST)_t);
				tmp134_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp134_AST);
				ASTPair __currentAST176 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,TYPECAST);
				_t = _t.getFirstChild();
				typeSpec(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				expr(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST176;
				_t = __t176;
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_new:
			{
				newExpression(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				constant(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_super:
			{
				AST tmp135_AST = null;
				AST tmp135_AST_in = null;
				tmp135_AST = astFactory.create((AST)_t);
				tmp135_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp135_AST);
				match(_t,LITERAL_super);
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_true:
			{
				AST tmp136_AST = null;
				AST tmp136_AST_in = null;
				tmp136_AST = astFactory.create((AST)_t);
				tmp136_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp136_AST);
				match(_t,LITERAL_true);
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_false:
			{
				AST tmp137_AST = null;
				AST tmp137_AST_in = null;
				tmp137_AST = astFactory.create((AST)_t);
				tmp137_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp137_AST);
				match(_t,LITERAL_false);
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_this:
			{
				AST tmp138_AST = null;
				AST tmp138_AST_in = null;
				tmp138_AST = astFactory.create((AST)_t);
				tmp138_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp138_AST);
				match(_t,LITERAL_this);
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_null:
			{
				AST tmp139_AST = null;
				AST tmp139_AST_in = null;
				tmp139_AST = astFactory.create((AST)_t);
				tmp139_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp139_AST);
				match(_t,LITERAL_null);
				_t = _t.getNextSibling();
				primaryExpression_AST = (AST)currentAST.root;
				break;
			}
			case TYPE:
			{
				typeSpec(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				primaryExpression_AST = (AST)currentAST.root;
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
		
		AST arrayIndex_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST arrayIndex_AST = null;
		
		try {      // for error handling
			AST __t182 = _t;
			AST tmp140_AST = null;
			AST tmp140_AST_in = null;
			tmp140_AST = astFactory.create((AST)_t);
			tmp140_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp140_AST);
			ASTPair __currentAST182 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,INDEX_OP);
			_t = _t.getFirstChild();
			expr(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			expression(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
			currentAST = __currentAST182;
			_t = __t182;
			_t = _t.getNextSibling();
			arrayIndex_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = arrayIndex_AST;
		_retTree = _t;
	}
	
	public final void ctorCall(AST _t) throws RecognitionException {
		
		AST ctorCall_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST ctorCall_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case CTOR_CALL:
			{
				AST __t178 = _t;
				AST tmp141_AST = null;
				AST tmp141_AST_in = null;
				tmp141_AST = astFactory.create((AST)_t);
				tmp141_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp141_AST);
				ASTPair __currentAST178 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,CTOR_CALL);
				_t = _t.getFirstChild();
				elist(_t);
				_t = _retTree;
				astFactory.addASTChild(currentAST, returnAST);
				currentAST = __currentAST178;
				_t = __t178;
				_t = _t.getNextSibling();
				ctorCall_AST = (AST)currentAST.root;
				break;
			}
			case SUPER_CTOR_CALL:
			{
				AST __t179 = _t;
				AST tmp142_AST = null;
				AST tmp142_AST_in = null;
				tmp142_AST = astFactory.create((AST)_t);
				tmp142_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp142_AST);
				ASTPair __currentAST179 = currentAST.copy();
				currentAST.root = currentAST.child;
				currentAST.child = null;
				match(_t,SUPER_CTOR_CALL);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case ELIST:
				{
					elist(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case TYPE:
				case TYPECAST:
				case INDEX_OP:
				case METHOD_CALL:
				case SUPER_CTOR_CALL:
				case CTOR_CALL:
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
					primaryExpression(_t);
					_t = _retTree;
					astFactory.addASTChild(currentAST, returnAST);
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
				currentAST = __currentAST179;
				_t = __t179;
				_t = _t.getNextSibling();
				ctorCall_AST = (AST)currentAST.root;
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
		returnAST = ctorCall_AST;
		_retTree = _t;
	}
	
	public final void newExpression(AST _t) throws RecognitionException {
		
		AST newExpression_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST newExpression_AST = null;
		
		try {      // for error handling
			AST __t185 = _t;
			AST tmp143_AST = null;
			AST tmp143_AST_in = null;
			tmp143_AST = astFactory.create((AST)_t);
			tmp143_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp143_AST);
			ASTPair __currentAST185 = currentAST.copy();
			currentAST.root = currentAST.child;
			currentAST.child = null;
			match(_t,LITERAL_new);
			_t = _t.getFirstChild();
			type(_t);
			_t = _retTree;
			astFactory.addASTChild(currentAST, returnAST);
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
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case OBJBLOCK:
				{
					objBlock(_t);
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
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			currentAST = __currentAST185;
			_t = __t185;
			_t = _t.getNextSibling();
			newExpression_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = newExpression_AST;
		_retTree = _t;
	}
	
	public final void constant(AST _t) throws RecognitionException {
		
		AST constant_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST constant_AST = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case NUM_INT:
			{
				AST tmp144_AST = null;
				AST tmp144_AST_in = null;
				tmp144_AST = astFactory.create((AST)_t);
				tmp144_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp144_AST);
				match(_t,NUM_INT);
				_t = _t.getNextSibling();
				constant_AST = (AST)currentAST.root;
				break;
			}
			case CHAR_LITERAL:
			{
				AST tmp145_AST = null;
				AST tmp145_AST_in = null;
				tmp145_AST = astFactory.create((AST)_t);
				tmp145_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp145_AST);
				match(_t,CHAR_LITERAL);
				_t = _t.getNextSibling();
				constant_AST = (AST)currentAST.root;
				break;
			}
			case STRING_LITERAL:
			{
				AST tmp146_AST = null;
				AST tmp146_AST_in = null;
				tmp146_AST = astFactory.create((AST)_t);
				tmp146_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp146_AST);
				match(_t,STRING_LITERAL);
				_t = _t.getNextSibling();
				constant_AST = (AST)currentAST.root;
				break;
			}
			case NUM_FLOAT:
			{
				AST tmp147_AST = null;
				AST tmp147_AST_in = null;
				tmp147_AST = astFactory.create((AST)_t);
				tmp147_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp147_AST);
				match(_t,NUM_FLOAT);
				_t = _t.getNextSibling();
				constant_AST = (AST)currentAST.root;
				break;
			}
			case NUM_DOUBLE:
			{
				AST tmp148_AST = null;
				AST tmp148_AST_in = null;
				tmp148_AST = astFactory.create((AST)_t);
				tmp148_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp148_AST);
				match(_t,NUM_DOUBLE);
				_t = _t.getNextSibling();
				constant_AST = (AST)currentAST.root;
				break;
			}
			case NUM_LONG:
			{
				AST tmp149_AST = null;
				AST tmp149_AST_in = null;
				tmp149_AST = astFactory.create((AST)_t);
				tmp149_AST_in = (AST)_t;
				astFactory.addASTChild(currentAST, tmp149_AST);
				match(_t,NUM_LONG);
				_t = _t.getNextSibling();
				constant_AST = (AST)currentAST.root;
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
	
	public final void newArrayDeclarator(AST _t) throws RecognitionException {
		
		AST newArrayDeclarator_AST_in = (AST)_t;
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST newArrayDeclarator_AST = null;
		
		try {      // for error handling
			AST __t190 = _t;
			AST tmp150_AST = null;
			AST tmp150_AST_in = null;
			tmp150_AST = astFactory.create((AST)_t);
			tmp150_AST_in = (AST)_t;
			astFactory.addASTChild(currentAST, tmp150_AST);
			ASTPair __currentAST190 = currentAST.copy();
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
			currentAST = __currentAST190;
			_t = __t190;
			_t = _t.getNextSibling();
			newArrayDeclarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		returnAST = newArrayDeclarator_AST;
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
		"\"assert\"",
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
		"EXPONENT",
		"FLOAT_SUFFIX",
		"\"const\""
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { -4611682170136690688L, 127L, 33554432L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 275150587008L, 19321061408L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	}
	
