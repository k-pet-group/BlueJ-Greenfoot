// $ANTLR 2.7.0: "src/net/sourceforge/transmogrify/symtab/parser/java.g" -> "JavaRecognizer.java"$

package net.sourceforge.transmogrify.symtab.parser;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;
/** Java 1.2 Recognizer
 *
 * Run 'java Main <directory full of java files>'
 *
 * Contributing authors:
 *    John Mitchell   johnm@non.net
 *    Terence Parr    parrt@magelang.com
 *    John Lilley     jlilley@empathy.com
 *    Scott Stanchfield thetick@magelang.com
 *    Markus Mohnen       mohnen@informatik.rwth-aachen.de
 *    Peter Williams    pwilliams@netdynamics.com
 *
 * Version 1.00 December 9, 1997 -- initial release
 * Version 1.01 December 10, 1997
 *    fixed bug in octal def (0..7 not 0..8)
 * Version 1.10 August 1998 (parrt)
 *    added tree construction
 *    fixed definition of WS,comments for mac,pc,unix newlines
 *    added unary plus
 * Version 1.11 (Nov 20, 1998)
 *    Added "shutup" option to turn off last ambig warning.
 *    Fixed inner class def to allow named class defs as statements
 *    synchronized requires compound not simple statement
 *    add [] after builtInType DOT class in primaryExpression
 *    "const" is reserved but not valid..removed from modifiers
 * Version 1.12 (Feb 2, 1999)
 *    Changed LITERAL_xxx to xxx in tree grammar.
 *    Updated java.g to use tokens {...} now for 2.6.0 (new feature).
 *
 * Version 1.13 (Apr 23, 1999)
 *    Didn't have (stat)? for else clause in tree parser.
 *    Didn't gen ASTs for interface extends.  Updated tree parser too.
 *    Updated to 2.6.0.
 * Version 1.14 (Jun 20, 1999)
 *    Allowed final/abstract on local classes.
 *    Removed local interfaces from methods
 *    Put instanceof precedence where it belongs...in relationalExpr
 *      It also had expr not type as arg; fixed it.
 *    Missing ! on SEMI in classBlock
 *    fixed: (expr) + "string" was parsed incorrectly (+ as unary plus).
 *    fixed: didn't like Object[].class in parser or tree parser
 * Version 1.15 (Jun 26, 1999)
 *    Screwed up rule with instanceof in it. :(  Fixed.
 *    Tree parser didn't like (expr).something; fixed.
 *    Allowed multiple inheritance in tree grammar. oops.
 * Version 1.16 (August 22, 1999)
 *    Extending an interface built a wacky tree: had extra EXTENDS.
 *    Tree grammar didn't allow multiple superinterfaces.
 *    Tree grammar didn't allow empty var initializer: {}
 * Version 1.17 (October 12, 1999)
 *    ESC lexer rule allowed 399 max not 377 max.
 *    java.tree.g didn't handle the expression of synchronized
 *      statements.
 *
 * Version tracking now done with following ID:
 *
 * $Id: JavaRecognizer.java 1011 2001-11-22 10:36:26Z ajp $
 *
 * BUG:
 *    Doesn't like boolean.class!
 *
 * class Test {
 *   public static void main( String args[] ) {
 *     if (boolean.class.equals(boolean.class)) {
 *       System.out.println("works");
 *     }
 *   }
 * }
 *
 * This grammar is in the PUBLIC DOMAIN
 */
public class JavaRecognizer extends antlr.LLkParser
       implements JavaTokenTypes
 {

protected JavaRecognizer(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public JavaRecognizer(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected JavaRecognizer(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public JavaRecognizer(TokenStream lexer) {
  this(lexer,2);
}

public JavaRecognizer(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final void compilationUnit() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST compilationUnit_AST = null;
		
		{
		switch ( LA(1)) {
		case LITERAL_package:
		{
			packageDefinition();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case EOF:
		case FINAL:
		case ABSTRACT:
		case SEMI:
		case LITERAL_import:
		case LITERAL_private:
		case LITERAL_public:
		case LITERAL_protected:
		case LITERAL_static:
		case LITERAL_transient:
		case LITERAL_native:
		case LITERAL_threadsafe:
		case LITERAL_synchronized:
		case LITERAL_volatile:
		case LITERAL_class:
		case LITERAL_interface:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop4:
		do {
			if ((LA(1)==LITERAL_import)) {
				importDefinition();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop4;
			}
			
		} while (true);
		}
		{
		_loop6:
		do {
			if ((_tokenSet_0.member(LA(1)))) {
				typeDefinition();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop6;
			}
			
		} while (true);
		}
		AST tmp1_AST = null;
		tmp1_AST = (AST)astFactory.create(LT(1));
		match(Token.EOF_TYPE);
		compilationUnit_AST = (AST)currentAST.root;
		returnAST = compilationUnit_AST;
	}
	
	public final void packageDefinition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST packageDefinition_AST = null;
		Token  p = null;
		AST p_AST = null;
		
		p = LT(1);
		if (inputState.guessing==0) {
			p_AST = (AST)astFactory.create(p);
			astFactory.makeASTRoot(currentAST, p_AST);
		}
		match(LITERAL_package);
		if ( inputState.guessing==0 ) {
			p_AST.setType(PACKAGE_DEF);
		}
		identifier();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		AST tmp2_AST = null;
		tmp2_AST = (AST)astFactory.create(LT(1));
		match(SEMI);
		packageDefinition_AST = (AST)currentAST.root;
		returnAST = packageDefinition_AST;
	}
	
	public final void importDefinition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST importDefinition_AST = null;
		Token  i = null;
		AST i_AST = null;
		
		i = LT(1);
		if (inputState.guessing==0) {
			i_AST = (AST)astFactory.create(i);
			astFactory.makeASTRoot(currentAST, i_AST);
		}
		match(LITERAL_import);
		if ( inputState.guessing==0 ) {
			i_AST.setType(IMPORT);
		}
		identifierStar();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		AST tmp3_AST = null;
		tmp3_AST = (AST)astFactory.create(LT(1));
		match(SEMI);
		importDefinition_AST = (AST)currentAST.root;
		returnAST = importDefinition_AST;
	}
	
	public final void typeDefinition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeDefinition_AST = null;
		AST m_AST = null;
		
		switch ( LA(1)) {
		case FINAL:
		case ABSTRACT:
		case LITERAL_private:
		case LITERAL_public:
		case LITERAL_protected:
		case LITERAL_static:
		case LITERAL_transient:
		case LITERAL_native:
		case LITERAL_threadsafe:
		case LITERAL_synchronized:
		case LITERAL_volatile:
		case LITERAL_class:
		case LITERAL_interface:
		{
			modifiers();
			if (inputState.guessing==0) {
				m_AST = (AST)returnAST;
			}
			{
			switch ( LA(1)) {
			case LITERAL_class:
			{
				classDefinition(m_AST);
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				break;
			}
			case LITERAL_interface:
			{
				interfaceDefinition(m_AST);
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			typeDefinition_AST = (AST)currentAST.root;
			break;
		}
		case SEMI:
		{
			AST tmp4_AST = null;
			tmp4_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			typeDefinition_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = typeDefinition_AST;
	}
	
	public final void identifier() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST identifier_AST = null;
		
		AST tmp5_AST = null;
		if (inputState.guessing==0) {
			tmp5_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp5_AST);
		}
		match(IDENT);
		{
		_loop26:
		do {
			if ((LA(1)==DOT)) {
				AST tmp6_AST = null;
				if (inputState.guessing==0) {
					tmp6_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp6_AST);
				}
				match(DOT);
				AST tmp7_AST = null;
				if (inputState.guessing==0) {
					tmp7_AST = (AST)astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp7_AST);
				}
				match(IDENT);
			}
			else {
				break _loop26;
			}
			
		} while (true);
		}
		identifier_AST = (AST)currentAST.root;
		returnAST = identifier_AST;
	}
	
	public final void identifierStar() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST identifierStar_AST = null;
		
		AST tmp8_AST = null;
		if (inputState.guessing==0) {
			tmp8_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp8_AST);
		}
		match(IDENT);
		{
		_loop29:
		do {
			if ((LA(1)==DOT) && (LA(2)==IDENT)) {
				AST tmp9_AST = null;
				if (inputState.guessing==0) {
					tmp9_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp9_AST);
				}
				match(DOT);
				AST tmp10_AST = null;
				if (inputState.guessing==0) {
					tmp10_AST = (AST)astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp10_AST);
				}
				match(IDENT);
			}
			else {
				break _loop29;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case DOT:
		{
			AST tmp11_AST = null;
			if (inputState.guessing==0) {
				tmp11_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp11_AST);
			}
			match(DOT);
			AST tmp12_AST = null;
			if (inputState.guessing==0) {
				tmp12_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp12_AST);
			}
			match(STAR);
			break;
		}
		case SEMI:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		identifierStar_AST = (AST)currentAST.root;
		returnAST = identifierStar_AST;
	}
	
	public final void modifiers() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST modifiers_AST = null;
		
		{
		_loop14:
		do {
			if ((_tokenSet_1.member(LA(1)))) {
				modifier();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop14;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			modifiers_AST = (AST)currentAST.root;
			modifiers_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(MODIFIERS,"MODIFIERS")).add(modifiers_AST));
			currentAST.root = modifiers_AST;
			currentAST.child = modifiers_AST!=null &&modifiers_AST.getFirstChild()!=null ?
				modifiers_AST.getFirstChild() : modifiers_AST;
			currentAST.advanceChildToEnd();
		}
		modifiers_AST = (AST)currentAST.root;
		returnAST = modifiers_AST;
	}
	
	public final void classDefinition(
		AST modifiers
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST classDefinition_AST = null;
		AST sc_AST = null;
		AST ic_AST = null;
		AST cb_AST = null;
		
		AST tmp13_AST = null;
		if (inputState.guessing==0) {
			tmp13_AST = (AST)astFactory.create(LT(1));
		}
		match(LITERAL_class);
		AST tmp14_AST = null;
		if (inputState.guessing==0) {
			tmp14_AST = (AST)astFactory.create(LT(1));
		}
		match(IDENT);
		superClassClause();
		if (inputState.guessing==0) {
			sc_AST = (AST)returnAST;
		}
		implementsClause();
		if (inputState.guessing==0) {
			ic_AST = (AST)returnAST;
		}
		classBlock();
		if (inputState.guessing==0) {
			cb_AST = (AST)returnAST;
		}
		if ( inputState.guessing==0 ) {
			classDefinition_AST = (AST)currentAST.root;
			classDefinition_AST = (AST)astFactory.make( (new ASTArray(6)).add((AST)astFactory.create(CLASS_DEF,"CLASS_DEF")).add(modifiers).add(tmp14_AST).add(sc_AST).add(ic_AST).add(cb_AST));
			currentAST.root = classDefinition_AST;
			currentAST.child = classDefinition_AST!=null &&classDefinition_AST.getFirstChild()!=null ?
				classDefinition_AST.getFirstChild() : classDefinition_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = classDefinition_AST;
	}
	
	public final void interfaceDefinition(
		AST modifiers
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interfaceDefinition_AST = null;
		AST ie_AST = null;
		AST cb_AST = null;
		
		AST tmp15_AST = null;
		if (inputState.guessing==0) {
			tmp15_AST = (AST)astFactory.create(LT(1));
		}
		match(LITERAL_interface);
		AST tmp16_AST = null;
		if (inputState.guessing==0) {
			tmp16_AST = (AST)astFactory.create(LT(1));
		}
		match(IDENT);
		interfaceExtends();
		if (inputState.guessing==0) {
			ie_AST = (AST)returnAST;
		}
		classBlock();
		if (inputState.guessing==0) {
			cb_AST = (AST)returnAST;
		}
		if ( inputState.guessing==0 ) {
			interfaceDefinition_AST = (AST)currentAST.root;
			interfaceDefinition_AST = (AST)astFactory.make( (new ASTArray(5)).add((AST)astFactory.create(INTERFACE_DEF,"INTERFACE_DEF")).add(modifiers).add(tmp16_AST).add(ie_AST).add(cb_AST));
			currentAST.root = interfaceDefinition_AST;
			currentAST.child = interfaceDefinition_AST!=null &&interfaceDefinition_AST.getFirstChild()!=null ?
				interfaceDefinition_AST.getFirstChild() : interfaceDefinition_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = interfaceDefinition_AST;
	}
	
/** A declaration is the creation of a reference or primitive-type variable
 *  Create a separate Type/Var tree for each var in the var list.
 */
	public final void declaration() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST declaration_AST = null;
		AST m_AST = null;
		AST t_AST = null;
		AST v_AST = null;
		
		modifiers();
		if (inputState.guessing==0) {
			m_AST = (AST)returnAST;
		}
		typeSpec(false);
		if (inputState.guessing==0) {
			t_AST = (AST)returnAST;
		}
		variableDefinitions(m_AST,t_AST);
		if (inputState.guessing==0) {
			v_AST = (AST)returnAST;
		}
		if ( inputState.guessing==0 ) {
			declaration_AST = (AST)currentAST.root;
			declaration_AST = v_AST;
			currentAST.root = declaration_AST;
			currentAST.child = declaration_AST!=null &&declaration_AST.getFirstChild()!=null ?
				declaration_AST.getFirstChild() : declaration_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = declaration_AST;
	}
	
	public final void typeSpec(
		boolean addImagNode
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeSpec_AST = null;
		
		switch ( LA(1)) {
		case IDENT:
		{
			classTypeSpec(addImagNode);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			typeSpec_AST = (AST)currentAST.root;
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
			builtInTypeSpec(addImagNode);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			typeSpec_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = typeSpec_AST;
	}
	
	public final void variableDefinitions(
		AST mods, AST t
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST variableDefinitions_AST = null;
		
		variableDeclarator(getASTFactory().dupTree(mods),
               getASTFactory().dupTree(t));
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop54:
		do {
			if ((LA(1)==COMMA)) {
				AST tmp17_AST = null;
				tmp17_AST = (AST)astFactory.create(LT(1));
				match(COMMA);
				variableDeclarator(getASTFactory().dupTree(mods),
                 getASTFactory().dupTree(t));
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop54;
			}
			
		} while (true);
		}
		variableDefinitions_AST = (AST)currentAST.root;
		returnAST = variableDefinitions_AST;
	}
	
	public final void modifier() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST modifier_AST = null;
		
		switch ( LA(1)) {
		case LITERAL_private:
		{
			AST tmp18_AST = null;
			if (inputState.guessing==0) {
				tmp18_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp18_AST);
			}
			match(LITERAL_private);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_public:
		{
			AST tmp19_AST = null;
			if (inputState.guessing==0) {
				tmp19_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp19_AST);
			}
			match(LITERAL_public);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_protected:
		{
			AST tmp20_AST = null;
			if (inputState.guessing==0) {
				tmp20_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp20_AST);
			}
			match(LITERAL_protected);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_static:
		{
			AST tmp21_AST = null;
			if (inputState.guessing==0) {
				tmp21_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp21_AST);
			}
			match(LITERAL_static);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_transient:
		{
			AST tmp22_AST = null;
			if (inputState.guessing==0) {
				tmp22_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp22_AST);
			}
			match(LITERAL_transient);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case FINAL:
		{
			AST tmp23_AST = null;
			if (inputState.guessing==0) {
				tmp23_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp23_AST);
			}
			match(FINAL);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case ABSTRACT:
		{
			AST tmp24_AST = null;
			if (inputState.guessing==0) {
				tmp24_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp24_AST);
			}
			match(ABSTRACT);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_native:
		{
			AST tmp25_AST = null;
			if (inputState.guessing==0) {
				tmp25_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp25_AST);
			}
			match(LITERAL_native);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_threadsafe:
		{
			AST tmp26_AST = null;
			if (inputState.guessing==0) {
				tmp26_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp26_AST);
			}
			match(LITERAL_threadsafe);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_synchronized:
		{
			AST tmp27_AST = null;
			if (inputState.guessing==0) {
				tmp27_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp27_AST);
			}
			match(LITERAL_synchronized);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_volatile:
		{
			AST tmp28_AST = null;
			if (inputState.guessing==0) {
				tmp28_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp28_AST);
			}
			match(LITERAL_volatile);
			modifier_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = modifier_AST;
	}
	
	public final void classTypeSpec(
		boolean addImagNode
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST classTypeSpec_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		
		identifier();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop18:
		do {
			if ((LA(1)==LBRACK)) {
				lb = LT(1);
				if (inputState.guessing==0) {
					lb_AST = (AST)astFactory.create(lb);
					astFactory.makeASTRoot(currentAST, lb_AST);
				}
				match(LBRACK);
				if ( inputState.guessing==0 ) {
					lb_AST.setType(ARRAY_DECLARATOR);
				}
				AST tmp29_AST = null;
				tmp29_AST = (AST)astFactory.create(LT(1));
				match(RBRACK);
			}
			else {
				break _loop18;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			classTypeSpec_AST = (AST)currentAST.root;
			
			if ( addImagNode ) {
			classTypeSpec_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(TYPE,"TYPE")).add(classTypeSpec_AST));
			}
			
			currentAST.root = classTypeSpec_AST;
			currentAST.child = classTypeSpec_AST!=null &&classTypeSpec_AST.getFirstChild()!=null ?
				classTypeSpec_AST.getFirstChild() : classTypeSpec_AST;
			currentAST.advanceChildToEnd();
		}
		classTypeSpec_AST = (AST)currentAST.root;
		returnAST = classTypeSpec_AST;
	}
	
	public final void builtInTypeSpec(
		boolean addImagNode
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST builtInTypeSpec_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		
		builtInType();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop21:
		do {
			if ((LA(1)==LBRACK)) {
				lb = LT(1);
				if (inputState.guessing==0) {
					lb_AST = (AST)astFactory.create(lb);
					astFactory.makeASTRoot(currentAST, lb_AST);
				}
				match(LBRACK);
				if ( inputState.guessing==0 ) {
					lb_AST.setType(ARRAY_DECLARATOR);
				}
				AST tmp30_AST = null;
				tmp30_AST = (AST)astFactory.create(LT(1));
				match(RBRACK);
			}
			else {
				break _loop21;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			builtInTypeSpec_AST = (AST)currentAST.root;
			
			if ( addImagNode ) {
			builtInTypeSpec_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(TYPE,"TYPE")).add(builtInTypeSpec_AST));
			}
			
			currentAST.root = builtInTypeSpec_AST;
			currentAST.child = builtInTypeSpec_AST!=null &&builtInTypeSpec_AST.getFirstChild()!=null ?
				builtInTypeSpec_AST.getFirstChild() : builtInTypeSpec_AST;
			currentAST.advanceChildToEnd();
		}
		builtInTypeSpec_AST = (AST)currentAST.root;
		returnAST = builtInTypeSpec_AST;
	}
	
	public final void builtInType() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST builtInType_AST = null;
		
		switch ( LA(1)) {
		case LITERAL_void:
		{
			AST tmp31_AST = null;
			if (inputState.guessing==0) {
				tmp31_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp31_AST);
			}
			match(LITERAL_void);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_boolean:
		{
			AST tmp32_AST = null;
			if (inputState.guessing==0) {
				tmp32_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp32_AST);
			}
			match(LITERAL_boolean);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_byte:
		{
			AST tmp33_AST = null;
			if (inputState.guessing==0) {
				tmp33_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp33_AST);
			}
			match(LITERAL_byte);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_char:
		{
			AST tmp34_AST = null;
			if (inputState.guessing==0) {
				tmp34_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp34_AST);
			}
			match(LITERAL_char);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_short:
		{
			AST tmp35_AST = null;
			if (inputState.guessing==0) {
				tmp35_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp35_AST);
			}
			match(LITERAL_short);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_int:
		{
			AST tmp36_AST = null;
			if (inputState.guessing==0) {
				tmp36_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp36_AST);
			}
			match(LITERAL_int);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_float:
		{
			AST tmp37_AST = null;
			if (inputState.guessing==0) {
				tmp37_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp37_AST);
			}
			match(LITERAL_float);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_long:
		{
			AST tmp38_AST = null;
			if (inputState.guessing==0) {
				tmp38_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp38_AST);
			}
			match(LITERAL_long);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_double:
		{
			AST tmp39_AST = null;
			if (inputState.guessing==0) {
				tmp39_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp39_AST);
			}
			match(LITERAL_double);
			builtInType_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = builtInType_AST;
	}
	
	public final void type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_AST = null;
		
		switch ( LA(1)) {
		case IDENT:
		{
			identifier();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
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
			builtInType();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			type_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = type_AST;
	}
	
	public final void superClassClause() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST superClassClause_AST = null;
		AST id_AST = null;
		
		{
		switch ( LA(1)) {
		case LITERAL_extends:
		{
			AST tmp40_AST = null;
			if (inputState.guessing==0) {
				tmp40_AST = (AST)astFactory.create(LT(1));
			}
			match(LITERAL_extends);
			identifier();
			if (inputState.guessing==0) {
				id_AST = (AST)returnAST;
			}
			break;
		}
		case LCURLY:
		case LITERAL_implements:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			superClassClause_AST = (AST)currentAST.root;
			superClassClause_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(EXTENDS_CLAUSE,"EXTENDS_CLAUSE")).add(id_AST));
			currentAST.root = superClassClause_AST;
			currentAST.child = superClassClause_AST!=null &&superClassClause_AST.getFirstChild()!=null ?
				superClassClause_AST.getFirstChild() : superClassClause_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = superClassClause_AST;
	}
	
	public final void implementsClause() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST implementsClause_AST = null;
		Token  i = null;
		AST i_AST = null;
		
		{
		switch ( LA(1)) {
		case LITERAL_implements:
		{
			i = LT(1);
			if (inputState.guessing==0) {
				i_AST = (AST)astFactory.create(i);
			}
			match(LITERAL_implements);
			identifier();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			_loop46:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp41_AST = null;
					tmp41_AST = (AST)astFactory.create(LT(1));
					match(COMMA);
					identifier();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					break _loop46;
				}
				
			} while (true);
			}
			break;
		}
		case LCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			implementsClause_AST = (AST)currentAST.root;
			implementsClause_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(IMPLEMENTS_CLAUSE,"IMPLEMENTS_CLAUSE")).add(implementsClause_AST));
			currentAST.root = implementsClause_AST;
			currentAST.child = implementsClause_AST!=null &&implementsClause_AST.getFirstChild()!=null ?
				implementsClause_AST.getFirstChild() : implementsClause_AST;
			currentAST.advanceChildToEnd();
		}
		implementsClause_AST = (AST)currentAST.root;
		returnAST = implementsClause_AST;
	}
	
	public final void classBlock() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST classBlock_AST = null;
		
		AST tmp42_AST = null;
		tmp42_AST = (AST)astFactory.create(LT(1));
		match(LCURLY);
		{
		_loop38:
		do {
			switch ( LA(1)) {
			case FINAL:
			case ABSTRACT:
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
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_protected:
			case LITERAL_static:
			case LITERAL_transient:
			case LITERAL_native:
			case LITERAL_threadsafe:
			case LITERAL_synchronized:
			case LITERAL_volatile:
			case LITERAL_class:
			case LITERAL_interface:
			case LCURLY:
			{
				field();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				break;
			}
			case SEMI:
			{
				AST tmp43_AST = null;
				tmp43_AST = (AST)astFactory.create(LT(1));
				match(SEMI);
				break;
			}
			default:
			{
				break _loop38;
			}
			}
		} while (true);
		}
		AST tmp44_AST = null;
		if (inputState.guessing==0) {
			tmp44_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp44_AST);
		}
		match(RCURLY);
		if ( inputState.guessing==0 ) {
			classBlock_AST = (AST)currentAST.root;
			classBlock_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(OBJBLOCK,"OBJBLOCK")).add(classBlock_AST));
			currentAST.root = classBlock_AST;
			currentAST.child = classBlock_AST!=null &&classBlock_AST.getFirstChild()!=null ?
				classBlock_AST.getFirstChild() : classBlock_AST;
			currentAST.advanceChildToEnd();
		}
		classBlock_AST = (AST)currentAST.root;
		returnAST = classBlock_AST;
	}
	
	public final void interfaceExtends() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interfaceExtends_AST = null;
		Token  e = null;
		AST e_AST = null;
		
		{
		switch ( LA(1)) {
		case LITERAL_extends:
		{
			e = LT(1);
			if (inputState.guessing==0) {
				e_AST = (AST)astFactory.create(e);
			}
			match(LITERAL_extends);
			identifier();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			_loop42:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp45_AST = null;
					tmp45_AST = (AST)astFactory.create(LT(1));
					match(COMMA);
					identifier();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					break _loop42;
				}
				
			} while (true);
			}
			break;
		}
		case LCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			interfaceExtends_AST = (AST)currentAST.root;
			interfaceExtends_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(EXTENDS_CLAUSE,"EXTENDS_CLAUSE")).add(interfaceExtends_AST));
			currentAST.root = interfaceExtends_AST;
			currentAST.child = interfaceExtends_AST!=null &&interfaceExtends_AST.getFirstChild()!=null ?
				interfaceExtends_AST.getFirstChild() : interfaceExtends_AST;
			currentAST.advanceChildToEnd();
		}
		interfaceExtends_AST = (AST)currentAST.root;
		returnAST = interfaceExtends_AST;
	}
	
	public final void field() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST field_AST = null;
		AST mods_AST = null;
		AST h_AST = null;
		AST s_AST = null;
		AST cd_AST = null;
		AST id_AST = null;
		AST t_AST = null;
		AST param_AST = null;
		AST rt_AST = null;
		AST tc_AST = null;
		AST s2_AST = null;
		AST v_AST = null;
		AST s3_AST = null;
		AST s4_AST = null;
		
		if ((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2)))) {
			modifiers();
			if (inputState.guessing==0) {
				mods_AST = (AST)returnAST;
			}
			{
			switch ( LA(1)) {
			case LITERAL_class:
			{
				classDefinition(mods_AST);
				if (inputState.guessing==0) {
					cd_AST = (AST)returnAST;
				}
				if ( inputState.guessing==0 ) {
					field_AST = (AST)currentAST.root;
					field_AST = cd_AST;
					currentAST.root = field_AST;
					currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
						field_AST.getFirstChild() : field_AST;
					currentAST.advanceChildToEnd();
				}
				break;
			}
			case LITERAL_interface:
			{
				interfaceDefinition(mods_AST);
				if (inputState.guessing==0) {
					id_AST = (AST)returnAST;
				}
				if ( inputState.guessing==0 ) {
					field_AST = (AST)currentAST.root;
					field_AST = id_AST;
					currentAST.root = field_AST;
					currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
						field_AST.getFirstChild() : field_AST;
					currentAST.advanceChildToEnd();
				}
				break;
			}
			default:
				if ((LA(1)==IDENT) && (LA(2)==LPAREN)) {
					ctorHead();
					if (inputState.guessing==0) {
						h_AST = (AST)returnAST;
					}
					compoundStatement();
					if (inputState.guessing==0) {
						s_AST = (AST)returnAST;
					}
					if ( inputState.guessing==0 ) {
						field_AST = (AST)currentAST.root;
						field_AST = (AST)astFactory.make( (new ASTArray(4)).add((AST)astFactory.create(CTOR_DEF,"CTOR_DEF")).add(mods_AST).add(h_AST).add(s_AST));
						currentAST.root = field_AST;
						currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
							field_AST.getFirstChild() : field_AST;
						currentAST.advanceChildToEnd();
					}
				}
				else if (((LA(1) >= LITERAL_void && LA(1) <= IDENT)) && (_tokenSet_4.member(LA(2)))) {
					typeSpec(false);
					if (inputState.guessing==0) {
						t_AST = (AST)returnAST;
					}
					{
					if ((LA(1)==IDENT) && (LA(2)==LPAREN)) {
						AST tmp46_AST = null;
						if (inputState.guessing==0) {
							tmp46_AST = (AST)astFactory.create(LT(1));
						}
						match(IDENT);
						AST tmp47_AST = null;
						tmp47_AST = (AST)astFactory.create(LT(1));
						match(LPAREN);
						parameterDeclarationList();
						if (inputState.guessing==0) {
							param_AST = (AST)returnAST;
						}
						AST tmp48_AST = null;
						tmp48_AST = (AST)astFactory.create(LT(1));
						match(RPAREN);
						returnTypeBrackersOnEndOfMethodHead(t_AST);
						if (inputState.guessing==0) {
							rt_AST = (AST)returnAST;
						}
						{
						switch ( LA(1)) {
						case LITERAL_throws:
						{
							throwsClause();
							if (inputState.guessing==0) {
								tc_AST = (AST)returnAST;
							}
							break;
						}
						case SEMI:
						case LCURLY:
						{
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						{
						switch ( LA(1)) {
						case LCURLY:
						{
							compoundStatement();
							if (inputState.guessing==0) {
								s2_AST = (AST)returnAST;
							}
							break;
						}
						case SEMI:
						{
							AST tmp49_AST = null;
							if (inputState.guessing==0) {
								tmp49_AST = (AST)astFactory.create(LT(1));
							}
							match(SEMI);
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						if ( inputState.guessing==0 ) {
							field_AST = (AST)currentAST.root;
							field_AST = (AST)astFactory.make( (new ASTArray(7)).add((AST)astFactory.create(METHOD_DEF,"METHOD_DEF")).add(mods_AST).add((AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(TYPE,"TYPE")).add(rt_AST))).add(tmp46_AST).add(param_AST).add(tc_AST).add(s2_AST));
							currentAST.root = field_AST;
							currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
								field_AST.getFirstChild() : field_AST;
							currentAST.advanceChildToEnd();
						}
					}
					else if ((LA(1)==IDENT) && (_tokenSet_5.member(LA(2)))) {
						variableDefinitions(mods_AST,t_AST);
						if (inputState.guessing==0) {
							v_AST = (AST)returnAST;
						}
						AST tmp50_AST = null;
						if (inputState.guessing==0) {
							tmp50_AST = (AST)astFactory.create(LT(1));
						}
						match(SEMI);
						if ( inputState.guessing==0 ) {
							field_AST = (AST)currentAST.root;
							field_AST = v_AST;
							currentAST.root = field_AST;
							currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
								field_AST.getFirstChild() : field_AST;
							currentAST.advanceChildToEnd();
						}
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		else if ((LA(1)==LITERAL_static) && (LA(2)==LCURLY)) {
			AST tmp51_AST = null;
			if (inputState.guessing==0) {
				tmp51_AST = (AST)astFactory.create(LT(1));
			}
			match(LITERAL_static);
			compoundStatement();
			if (inputState.guessing==0) {
				s3_AST = (AST)returnAST;
			}
			if ( inputState.guessing==0 ) {
				field_AST = (AST)currentAST.root;
				field_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(STATIC_INIT,"STATIC_INIT")).add(s3_AST));
				currentAST.root = field_AST;
				currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
					field_AST.getFirstChild() : field_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else if ((LA(1)==LCURLY)) {
			compoundStatement();
			if (inputState.guessing==0) {
				s4_AST = (AST)returnAST;
			}
			if ( inputState.guessing==0 ) {
				field_AST = (AST)currentAST.root;
				field_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(INSTANCE_INIT,"INSTANCE_INIT")).add(s4_AST));
				currentAST.root = field_AST;
				currentAST.child = field_AST!=null &&field_AST.getFirstChild()!=null ?
					field_AST.getFirstChild() : field_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		returnAST = field_AST;
	}
	
	public final void ctorHead() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST ctorHead_AST = null;
		
		AST tmp52_AST = null;
		if (inputState.guessing==0) {
			tmp52_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp52_AST);
		}
		match(IDENT);
		AST tmp53_AST = null;
		tmp53_AST = (AST)astFactory.create(LT(1));
		match(LPAREN);
		parameterDeclarationList();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		AST tmp54_AST = null;
		tmp54_AST = (AST)astFactory.create(LT(1));
		match(RPAREN);
		{
		switch ( LA(1)) {
		case LITERAL_throws:
		{
			throwsClause();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case LCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		ctorHead_AST = (AST)currentAST.root;
		returnAST = ctorHead_AST;
	}
	
	public final void compoundStatement() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST compoundStatement_AST = null;
		Token  lc = null;
		AST lc_AST = null;
		
		lc = LT(1);
		if (inputState.guessing==0) {
			lc_AST = (AST)astFactory.create(lc);
			astFactory.makeASTRoot(currentAST, lc_AST);
		}
		match(LCURLY);
		if ( inputState.guessing==0 ) {
			lc_AST.setType(SLIST);
		}
		{
		_loop87:
		do {
			if ((_tokenSet_6.member(LA(1)))) {
				statement();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop87;
			}
			
		} while (true);
		}
		AST tmp55_AST = null;
		if (inputState.guessing==0) {
			tmp55_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp55_AST);
		}
		match(RCURLY);
		compoundStatement_AST = (AST)currentAST.root;
		returnAST = compoundStatement_AST;
	}
	
	public final void parameterDeclarationList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parameterDeclarationList_AST = null;
		
		{
		switch ( LA(1)) {
		case FINAL:
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
		{
			parameterDeclaration();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			_loop78:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp56_AST = null;
					tmp56_AST = (AST)astFactory.create(LT(1));
					match(COMMA);
					parameterDeclaration();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					break _loop78;
				}
				
			} while (true);
			}
			break;
		}
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			parameterDeclarationList_AST = (AST)currentAST.root;
			parameterDeclarationList_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(PARAMETERS,"PARAMETERS")).add(parameterDeclarationList_AST));
			currentAST.root = parameterDeclarationList_AST;
			currentAST.child = parameterDeclarationList_AST!=null &&parameterDeclarationList_AST.getFirstChild()!=null ?
				parameterDeclarationList_AST.getFirstChild() : parameterDeclarationList_AST;
			currentAST.advanceChildToEnd();
		}
		parameterDeclarationList_AST = (AST)currentAST.root;
		returnAST = parameterDeclarationList_AST;
	}
	
	public final void returnTypeBrackersOnEndOfMethodHead(
		AST typ
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST returnTypeBrackersOnEndOfMethodHead_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		
		if ( inputState.guessing==0 ) {
			returnTypeBrackersOnEndOfMethodHead_AST = (AST)currentAST.root;
			returnTypeBrackersOnEndOfMethodHead_AST = typ;
			currentAST.root = returnTypeBrackersOnEndOfMethodHead_AST;
			currentAST.child = returnTypeBrackersOnEndOfMethodHead_AST!=null &&returnTypeBrackersOnEndOfMethodHead_AST.getFirstChild()!=null ?
				returnTypeBrackersOnEndOfMethodHead_AST.getFirstChild() : returnTypeBrackersOnEndOfMethodHead_AST;
			currentAST.advanceChildToEnd();
		}
		{
		_loop74:
		do {
			if ((LA(1)==LBRACK)) {
				lb = LT(1);
				if (inputState.guessing==0) {
					lb_AST = (AST)astFactory.create(lb);
					astFactory.makeASTRoot(currentAST, lb_AST);
				}
				match(LBRACK);
				if ( inputState.guessing==0 ) {
					lb_AST.setType(ARRAY_DECLARATOR);
				}
				AST tmp57_AST = null;
				tmp57_AST = (AST)astFactory.create(LT(1));
				match(RBRACK);
			}
			else {
				break _loop74;
			}
			
		} while (true);
		}
		returnTypeBrackersOnEndOfMethodHead_AST = (AST)currentAST.root;
		returnAST = returnTypeBrackersOnEndOfMethodHead_AST;
	}
	
	public final void throwsClause() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST throwsClause_AST = null;
		
		AST tmp58_AST = null;
		if (inputState.guessing==0) {
			tmp58_AST = (AST)astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp58_AST);
		}
		match(LITERAL_throws);
		identifier();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop71:
		do {
			if ((LA(1)==COMMA)) {
				AST tmp59_AST = null;
				tmp59_AST = (AST)astFactory.create(LT(1));
				match(COMMA);
				identifier();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop71;
			}
			
		} while (true);
		}
		throwsClause_AST = (AST)currentAST.root;
		returnAST = throwsClause_AST;
	}
	
/** Declaration of a variable.  This can be a class/instance variable,
 *   or a local variable in a method
 * It can also include possible initialization.
 */
	public final void variableDeclarator(
		AST mods, AST t
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST variableDeclarator_AST = null;
		Token  id = null;
		AST id_AST = null;
		AST d_AST = null;
		AST v_AST = null;
		
		id = LT(1);
		if (inputState.guessing==0) {
			id_AST = (AST)astFactory.create(id);
		}
		match(IDENT);
		declaratorBrackets(t);
		if (inputState.guessing==0) {
			d_AST = (AST)returnAST;
		}
		varInitializer();
		if (inputState.guessing==0) {
			v_AST = (AST)returnAST;
		}
		if ( inputState.guessing==0 ) {
			variableDeclarator_AST = (AST)currentAST.root;
			variableDeclarator_AST = (AST)astFactory.make( (new ASTArray(5)).add((AST)astFactory.create(VARIABLE_DEF,"VARIABLE_DEF")).add(mods).add((AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(TYPE,"TYPE")).add(d_AST))).add(id_AST).add(v_AST));
			currentAST.root = variableDeclarator_AST;
			currentAST.child = variableDeclarator_AST!=null &&variableDeclarator_AST.getFirstChild()!=null ?
				variableDeclarator_AST.getFirstChild() : variableDeclarator_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = variableDeclarator_AST;
	}
	
	public final void declaratorBrackets(
		AST typ
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST declaratorBrackets_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		
		if ( inputState.guessing==0 ) {
			declaratorBrackets_AST = (AST)currentAST.root;
			declaratorBrackets_AST=typ;
			currentAST.root = declaratorBrackets_AST;
			currentAST.child = declaratorBrackets_AST!=null &&declaratorBrackets_AST.getFirstChild()!=null ?
				declaratorBrackets_AST.getFirstChild() : declaratorBrackets_AST;
			currentAST.advanceChildToEnd();
		}
		{
		_loop58:
		do {
			if ((LA(1)==LBRACK)) {
				lb = LT(1);
				if (inputState.guessing==0) {
					lb_AST = (AST)astFactory.create(lb);
					astFactory.makeASTRoot(currentAST, lb_AST);
				}
				match(LBRACK);
				if ( inputState.guessing==0 ) {
					lb_AST.setType(ARRAY_DECLARATOR);
				}
				AST tmp60_AST = null;
				tmp60_AST = (AST)astFactory.create(LT(1));
				match(RBRACK);
			}
			else {
				break _loop58;
			}
			
		} while (true);
		}
		declaratorBrackets_AST = (AST)currentAST.root;
		returnAST = declaratorBrackets_AST;
	}
	
	public final void varInitializer() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST varInitializer_AST = null;
		
		{
		switch ( LA(1)) {
		case ASSIGN:
		{
			AST tmp61_AST = null;
			if (inputState.guessing==0) {
				tmp61_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp61_AST);
			}
			match(ASSIGN);
			initializer();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case SEMI:
		case COMMA:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		varInitializer_AST = (AST)currentAST.root;
		returnAST = varInitializer_AST;
	}
	
	public final void initializer() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST initializer_AST = null;
		
		switch ( LA(1)) {
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
		case LPAREN:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
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
		{
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			initializer_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		{
			arrayInitializer();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			initializer_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = initializer_AST;
	}
	
	public final void arrayInitializer() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST arrayInitializer_AST = null;
		Token  lc = null;
		AST lc_AST = null;
		
		lc = LT(1);
		if (inputState.guessing==0) {
			lc_AST = (AST)astFactory.create(lc);
			astFactory.makeASTRoot(currentAST, lc_AST);
		}
		match(LCURLY);
		if ( inputState.guessing==0 ) {
			lc_AST.setType(ARRAY_INIT);
		}
		{
		switch ( LA(1)) {
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
		case LCURLY:
		case LPAREN:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
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
		{
			initializer();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			_loop64:
			do {
				if ((LA(1)==COMMA) && (_tokenSet_7.member(LA(2)))) {
					AST tmp62_AST = null;
					tmp62_AST = (AST)astFactory.create(LT(1));
					match(COMMA);
					initializer();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					break _loop64;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case COMMA:
			{
				AST tmp63_AST = null;
				tmp63_AST = (AST)astFactory.create(LT(1));
				match(COMMA);
				break;
			}
			case RCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case RCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		AST tmp64_AST = null;
		if (inputState.guessing==0) {
			tmp64_AST = (AST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp64_AST);
		}
		match(RCURLY);
		arrayInitializer_AST = (AST)currentAST.root;
		returnAST = arrayInitializer_AST;
	}
	
	public final void expression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expression_AST = null;
		
		assignmentExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		if ( inputState.guessing==0 ) {
			expression_AST = (AST)currentAST.root;
			expression_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(EXPR,"EXPR")).add(expression_AST));
			currentAST.root = expression_AST;
			currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
				expression_AST.getFirstChild() : expression_AST;
			currentAST.advanceChildToEnd();
		}
		expression_AST = (AST)currentAST.root;
		returnAST = expression_AST;
	}
	
	public final void parameterDeclaration() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parameterDeclaration_AST = null;
		AST pm_AST = null;
		AST t_AST = null;
		Token  id = null;
		AST id_AST = null;
		AST pd_AST = null;
		
		parameterModifier();
		if (inputState.guessing==0) {
			pm_AST = (AST)returnAST;
		}
		typeSpec(false);
		if (inputState.guessing==0) {
			t_AST = (AST)returnAST;
		}
		id = LT(1);
		if (inputState.guessing==0) {
			id_AST = (AST)astFactory.create(id);
		}
		match(IDENT);
		parameterDeclaratorBrackets(t_AST);
		if (inputState.guessing==0) {
			pd_AST = (AST)returnAST;
		}
		if ( inputState.guessing==0 ) {
			parameterDeclaration_AST = (AST)currentAST.root;
			parameterDeclaration_AST = (AST)astFactory.make( (new ASTArray(4)).add((AST)astFactory.create(PARAMETER_DEF,"PARAMETER_DEF")).add(pm_AST).add((AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(TYPE,"TYPE")).add(pd_AST))).add(id_AST));
			currentAST.root = parameterDeclaration_AST;
			currentAST.child = parameterDeclaration_AST!=null &&parameterDeclaration_AST.getFirstChild()!=null ?
				parameterDeclaration_AST.getFirstChild() : parameterDeclaration_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = parameterDeclaration_AST;
	}
	
	public final void parameterModifier() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parameterModifier_AST = null;
		Token  f = null;
		AST f_AST = null;
		
		{
		switch ( LA(1)) {
		case FINAL:
		{
			f = LT(1);
			if (inputState.guessing==0) {
				f_AST = (AST)astFactory.create(f);
				astFactory.addASTChild(currentAST, f_AST);
			}
			match(FINAL);
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
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			parameterModifier_AST = (AST)currentAST.root;
			parameterModifier_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(MODIFIERS,"MODIFIERS")).add(f_AST));
			currentAST.root = parameterModifier_AST;
			currentAST.child = parameterModifier_AST!=null &&parameterModifier_AST.getFirstChild()!=null ?
				parameterModifier_AST.getFirstChild() : parameterModifier_AST;
			currentAST.advanceChildToEnd();
		}
		parameterModifier_AST = (AST)currentAST.root;
		returnAST = parameterModifier_AST;
	}
	
	public final void parameterDeclaratorBrackets(
		AST t
	) throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parameterDeclaratorBrackets_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		
		if ( inputState.guessing==0 ) {
			parameterDeclaratorBrackets_AST = (AST)currentAST.root;
			parameterDeclaratorBrackets_AST = t;
			currentAST.root = parameterDeclaratorBrackets_AST;
			currentAST.child = parameterDeclaratorBrackets_AST!=null &&parameterDeclaratorBrackets_AST.getFirstChild()!=null ?
				parameterDeclaratorBrackets_AST.getFirstChild() : parameterDeclaratorBrackets_AST;
			currentAST.advanceChildToEnd();
		}
		{
		_loop82:
		do {
			if ((LA(1)==LBRACK)) {
				lb = LT(1);
				if (inputState.guessing==0) {
					lb_AST = (AST)astFactory.create(lb);
					astFactory.makeASTRoot(currentAST, lb_AST);
				}
				match(LBRACK);
				if ( inputState.guessing==0 ) {
					lb_AST.setType(ARRAY_DECLARATOR);
				}
				AST tmp65_AST = null;
				tmp65_AST = (AST)astFactory.create(LT(1));
				match(RBRACK);
			}
			else {
				break _loop82;
			}
			
		} while (true);
		}
		parameterDeclaratorBrackets_AST = (AST)currentAST.root;
		returnAST = parameterDeclaratorBrackets_AST;
	}
	
	public final void statement() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST statement_AST = null;
		Token  c = null;
		AST c_AST = null;
		Token  s = null;
		AST s_AST = null;
		
		switch ( LA(1)) {
		case LCURLY:
		{
			compoundStatement();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_class:
		{
			classDefinition((AST)astFactory.create(MODIFIERS,"MODIFIERS"));
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_if:
		{
			AST tmp66_AST = null;
			if (inputState.guessing==0) {
				tmp66_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp66_AST);
			}
			match(LITERAL_if);
			AST tmp67_AST = null;
			tmp67_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp68_AST = null;
			tmp68_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			statement();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			if ((LA(1)==LITERAL_else) && (_tokenSet_6.member(LA(2)))) {
				AST tmp69_AST = null;
				tmp69_AST = (AST)astFactory.create(LT(1));
				match(LITERAL_else);
				statement();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else if ((_tokenSet_8.member(LA(1))) && (_tokenSet_9.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_for:
		{
			AST tmp70_AST = null;
			if (inputState.guessing==0) {
				tmp70_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp70_AST);
			}
			match(LITERAL_for);
			AST tmp71_AST = null;
			tmp71_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			forInit();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp72_AST = null;
			tmp72_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			forCond();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp73_AST = null;
			tmp73_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			forIter();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp74_AST = null;
			tmp74_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			statement();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_while:
		{
			AST tmp75_AST = null;
			if (inputState.guessing==0) {
				tmp75_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp75_AST);
			}
			match(LITERAL_while);
			AST tmp76_AST = null;
			tmp76_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp77_AST = null;
			tmp77_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			statement();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_do:
		{
			AST tmp78_AST = null;
			if (inputState.guessing==0) {
				tmp78_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp78_AST);
			}
			match(LITERAL_do);
			statement();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp79_AST = null;
			tmp79_AST = (AST)astFactory.create(LT(1));
			match(LITERAL_while);
			AST tmp80_AST = null;
			tmp80_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp81_AST = null;
			tmp81_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			AST tmp82_AST = null;
			tmp82_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_break:
		{
			AST tmp83_AST = null;
			if (inputState.guessing==0) {
				tmp83_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp83_AST);
			}
			match(LITERAL_break);
			{
			switch ( LA(1)) {
			case IDENT:
			{
				AST tmp84_AST = null;
				if (inputState.guessing==0) {
					tmp84_AST = (AST)astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp84_AST);
				}
				match(IDENT);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp85_AST = null;
			tmp85_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_continue:
		{
			AST tmp86_AST = null;
			if (inputState.guessing==0) {
				tmp86_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp86_AST);
			}
			match(LITERAL_continue);
			{
			switch ( LA(1)) {
			case IDENT:
			{
				AST tmp87_AST = null;
				if (inputState.guessing==0) {
					tmp87_AST = (AST)astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp87_AST);
				}
				match(IDENT);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp88_AST = null;
			tmp88_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_return:
		{
			AST tmp89_AST = null;
			if (inputState.guessing==0) {
				tmp89_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp89_AST);
			}
			match(LITERAL_return);
			{
			switch ( LA(1)) {
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
			case LPAREN:
			case PLUS:
			case MINUS:
			case INC:
			case DEC:
			case BNOT:
			case LNOT:
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
			{
				expression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp90_AST = null;
			tmp90_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_switch:
		{
			AST tmp91_AST = null;
			if (inputState.guessing==0) {
				tmp91_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp91_AST);
			}
			match(LITERAL_switch);
			AST tmp92_AST = null;
			tmp92_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp93_AST = null;
			tmp93_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			AST tmp94_AST = null;
			tmp94_AST = (AST)astFactory.create(LT(1));
			match(LCURLY);
			{
			_loop96:
			do {
				if ((LA(1)==LITERAL_case||LA(1)==LITERAL_default)) {
					casesGroup();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					break _loop96;
				}
				
			} while (true);
			}
			AST tmp95_AST = null;
			if (inputState.guessing==0) {
				tmp95_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp95_AST);
			}
			match(RCURLY);
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_try:
		{
			tryBlock();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_throw:
		{
			AST tmp96_AST = null;
			if (inputState.guessing==0) {
				tmp96_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp96_AST);
			}
			match(LITERAL_throw);
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp97_AST = null;
			tmp97_AST = (AST)astFactory.create(LT(1));
			match(SEMI);
			statement_AST = (AST)currentAST.root;
			break;
		}
		case SEMI:
		{
			s = LT(1);
			if (inputState.guessing==0) {
				s_AST = (AST)astFactory.create(s);
				astFactory.addASTChild(currentAST, s_AST);
			}
			match(SEMI);
			if ( inputState.guessing==0 ) {
				s_AST.setType(EMPTY_STAT);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		default:
			if ((LA(1)==FINAL) && (LA(2)==LITERAL_class)) {
				AST tmp98_AST = null;
				tmp98_AST = (AST)astFactory.create(LT(1));
				match(FINAL);
				classDefinition((AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(MODIFIERS,"MODIFIERS")).add((AST)astFactory.create(FINAL,"final"))));
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				statement_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==ABSTRACT) && (LA(2)==LITERAL_class)) {
				AST tmp99_AST = null;
				tmp99_AST = (AST)astFactory.create(LT(1));
				match(ABSTRACT);
				classDefinition((AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(MODIFIERS,"MODIFIERS")).add((AST)astFactory.create(ABSTRACT,"abstract"))));
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				statement_AST = (AST)currentAST.root;
			}
			else {
				boolean synPredMatched90 = false;
				if (((_tokenSet_10.member(LA(1))) && (_tokenSet_11.member(LA(2))))) {
					int _m90 = mark();
					synPredMatched90 = true;
					inputState.guessing++;
					try {
						{
						declaration();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched90 = false;
					}
					rewind(_m90);
					inputState.guessing--;
				}
				if ( synPredMatched90 ) {
					declaration();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					AST tmp100_AST = null;
					tmp100_AST = (AST)astFactory.create(LT(1));
					match(SEMI);
					statement_AST = (AST)currentAST.root;
				}
				else if ((_tokenSet_12.member(LA(1))) && (_tokenSet_13.member(LA(2)))) {
					expression();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					AST tmp101_AST = null;
					tmp101_AST = (AST)astFactory.create(LT(1));
					match(SEMI);
					statement_AST = (AST)currentAST.root;
				}
				else if ((LA(1)==IDENT) && (LA(2)==COLON)) {
					AST tmp102_AST = null;
					if (inputState.guessing==0) {
						tmp102_AST = (AST)astFactory.create(LT(1));
						astFactory.addASTChild(currentAST, tmp102_AST);
					}
					match(IDENT);
					c = LT(1);
					if (inputState.guessing==0) {
						c_AST = (AST)astFactory.create(c);
						astFactory.makeASTRoot(currentAST, c_AST);
					}
					match(COLON);
					if ( inputState.guessing==0 ) {
						c_AST.setType(LABELED_STAT);
					}
					statement();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					statement_AST = (AST)currentAST.root;
				}
				else if ((LA(1)==LITERAL_synchronized) && (LA(2)==LPAREN)) {
					AST tmp103_AST = null;
					if (inputState.guessing==0) {
						tmp103_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp103_AST);
					}
					match(LITERAL_synchronized);
					AST tmp104_AST = null;
					tmp104_AST = (AST)astFactory.create(LT(1));
					match(LPAREN);
					expression();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					AST tmp105_AST = null;
					tmp105_AST = (AST)astFactory.create(LT(1));
					match(RPAREN);
					compoundStatement();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					statement_AST = (AST)currentAST.root;
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}}
			returnAST = statement_AST;
		}
		
	public final void forInit() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST forInit_AST = null;
		
		{
		boolean synPredMatched108 = false;
		if (((_tokenSet_10.member(LA(1))) && (_tokenSet_11.member(LA(2))))) {
			int _m108 = mark();
			synPredMatched108 = true;
			inputState.guessing++;
			try {
				{
				declaration();
				}
			}
			catch (RecognitionException pe) {
				synPredMatched108 = false;
			}
			rewind(_m108);
			inputState.guessing--;
		}
		if ( synPredMatched108 ) {
			declaration();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
		}
		else if ((_tokenSet_12.member(LA(1))) && (_tokenSet_14.member(LA(2)))) {
			expressionList();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
		}
		else if ((LA(1)==SEMI)) {
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		if ( inputState.guessing==0 ) {
			forInit_AST = (AST)currentAST.root;
			forInit_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(FOR_INIT,"FOR_INIT")).add(forInit_AST));
			currentAST.root = forInit_AST;
			currentAST.child = forInit_AST!=null &&forInit_AST.getFirstChild()!=null ?
				forInit_AST.getFirstChild() : forInit_AST;
			currentAST.advanceChildToEnd();
		}
		forInit_AST = (AST)currentAST.root;
		returnAST = forInit_AST;
	}
	
	public final void forCond() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST forCond_AST = null;
		
		{
		switch ( LA(1)) {
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
		case LPAREN:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
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
		{
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case SEMI:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			forCond_AST = (AST)currentAST.root;
			forCond_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(FOR_CONDITION,"FOR_CONDITION")).add(forCond_AST));
			currentAST.root = forCond_AST;
			currentAST.child = forCond_AST!=null &&forCond_AST.getFirstChild()!=null ?
				forCond_AST.getFirstChild() : forCond_AST;
			currentAST.advanceChildToEnd();
		}
		forCond_AST = (AST)currentAST.root;
		returnAST = forCond_AST;
	}
	
	public final void forIter() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST forIter_AST = null;
		
		{
		switch ( LA(1)) {
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
		case LPAREN:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
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
		{
			expressionList();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			forIter_AST = (AST)currentAST.root;
			forIter_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(FOR_ITERATOR,"FOR_ITERATOR")).add(forIter_AST));
			currentAST.root = forIter_AST;
			currentAST.child = forIter_AST!=null &&forIter_AST.getFirstChild()!=null ?
				forIter_AST.getFirstChild() : forIter_AST;
			currentAST.advanceChildToEnd();
		}
		forIter_AST = (AST)currentAST.root;
		returnAST = forIter_AST;
	}
	
	public final void casesGroup() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST casesGroup_AST = null;
		
		{
		int _cnt99=0;
		_loop99:
		do {
			if ((LA(1)==LITERAL_case||LA(1)==LITERAL_default) && (_tokenSet_15.member(LA(2)))) {
				aCase();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				if ( _cnt99>=1 ) { break _loop99; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt99++;
		} while (true);
		}
		caseSList();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		if ( inputState.guessing==0 ) {
			casesGroup_AST = (AST)currentAST.root;
			casesGroup_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(CASE_GROUP,"CASE_GROUP")).add(casesGroup_AST));
			currentAST.root = casesGroup_AST;
			currentAST.child = casesGroup_AST!=null &&casesGroup_AST.getFirstChild()!=null ?
				casesGroup_AST.getFirstChild() : casesGroup_AST;
			currentAST.advanceChildToEnd();
		}
		casesGroup_AST = (AST)currentAST.root;
		returnAST = casesGroup_AST;
	}
	
	public final void tryBlock() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST tryBlock_AST = null;
		
		AST tmp106_AST = null;
		if (inputState.guessing==0) {
			tmp106_AST = (AST)astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp106_AST);
		}
		match(LITERAL_try);
		compoundStatement();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop115:
		do {
			if ((LA(1)==LITERAL_catch)) {
				handler();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop115;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case LITERAL_finally:
		{
			AST tmp107_AST = null;
			if (inputState.guessing==0) {
				tmp107_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp107_AST);
			}
			match(LITERAL_finally);
			compoundStatement();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case FINAL:
		case ABSTRACT:
		case SEMI:
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
		case LITERAL_private:
		case LITERAL_public:
		case LITERAL_protected:
		case LITERAL_static:
		case LITERAL_transient:
		case LITERAL_native:
		case LITERAL_threadsafe:
		case LITERAL_synchronized:
		case LITERAL_volatile:
		case LITERAL_class:
		case LCURLY:
		case RCURLY:
		case LPAREN:
		case LITERAL_if:
		case LITERAL_else:
		case LITERAL_for:
		case LITERAL_while:
		case LITERAL_do:
		case LITERAL_break:
		case LITERAL_continue:
		case LITERAL_return:
		case LITERAL_switch:
		case LITERAL_throw:
		case LITERAL_case:
		case LITERAL_default:
		case LITERAL_try:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
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
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		tryBlock_AST = (AST)currentAST.root;
		returnAST = tryBlock_AST;
	}
	
	public final void aCase() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST aCase_AST = null;
		
		{
		switch ( LA(1)) {
		case LITERAL_case:
		{
			AST tmp108_AST = null;
			if (inputState.guessing==0) {
				tmp108_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp108_AST);
			}
			match(LITERAL_case);
			expression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case LITERAL_default:
		{
			AST tmp109_AST = null;
			if (inputState.guessing==0) {
				tmp109_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp109_AST);
			}
			match(LITERAL_default);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		AST tmp110_AST = null;
		tmp110_AST = (AST)astFactory.create(LT(1));
		match(COLON);
		aCase_AST = (AST)currentAST.root;
		returnAST = aCase_AST;
	}
	
	public final void caseSList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST caseSList_AST = null;
		
		{
		_loop104:
		do {
			if ((_tokenSet_6.member(LA(1)))) {
				statement();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop104;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			caseSList_AST = (AST)currentAST.root;
			caseSList_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(SLIST,"SLIST")).add(caseSList_AST));
			currentAST.root = caseSList_AST;
			currentAST.child = caseSList_AST!=null &&caseSList_AST.getFirstChild()!=null ?
				caseSList_AST.getFirstChild() : caseSList_AST;
			currentAST.advanceChildToEnd();
		}
		caseSList_AST = (AST)currentAST.root;
		returnAST = caseSList_AST;
	}
	
	public final void expressionList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expressionList_AST = null;
		
		expression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop121:
		do {
			if ((LA(1)==COMMA)) {
				AST tmp111_AST = null;
				tmp111_AST = (AST)astFactory.create(LT(1));
				match(COMMA);
				expression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop121;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			expressionList_AST = (AST)currentAST.root;
			expressionList_AST = (AST)astFactory.make( (new ASTArray(2)).add((AST)astFactory.create(ELIST,"ELIST")).add(expressionList_AST));
			currentAST.root = expressionList_AST;
			currentAST.child = expressionList_AST!=null &&expressionList_AST.getFirstChild()!=null ?
				expressionList_AST.getFirstChild() : expressionList_AST;
			currentAST.advanceChildToEnd();
		}
		expressionList_AST = (AST)currentAST.root;
		returnAST = expressionList_AST;
	}
	
	public final void handler() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST handler_AST = null;
		
		AST tmp112_AST = null;
		if (inputState.guessing==0) {
			tmp112_AST = (AST)astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp112_AST);
		}
		match(LITERAL_catch);
		AST tmp113_AST = null;
		tmp113_AST = (AST)astFactory.create(LT(1));
		match(LPAREN);
		parameterDeclaration();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		AST tmp114_AST = null;
		tmp114_AST = (AST)astFactory.create(LT(1));
		match(RPAREN);
		compoundStatement();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		handler_AST = (AST)currentAST.root;
		returnAST = handler_AST;
	}
	
	public final void assignmentExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST assignmentExpression_AST = null;
		
		conditionalExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		switch ( LA(1)) {
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
		{
			{
			switch ( LA(1)) {
			case ASSIGN:
			{
				AST tmp115_AST = null;
				if (inputState.guessing==0) {
					tmp115_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp115_AST);
				}
				match(ASSIGN);
				break;
			}
			case PLUS_ASSIGN:
			{
				AST tmp116_AST = null;
				if (inputState.guessing==0) {
					tmp116_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp116_AST);
				}
				match(PLUS_ASSIGN);
				break;
			}
			case MINUS_ASSIGN:
			{
				AST tmp117_AST = null;
				if (inputState.guessing==0) {
					tmp117_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp117_AST);
				}
				match(MINUS_ASSIGN);
				break;
			}
			case STAR_ASSIGN:
			{
				AST tmp118_AST = null;
				if (inputState.guessing==0) {
					tmp118_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp118_AST);
				}
				match(STAR_ASSIGN);
				break;
			}
			case DIV_ASSIGN:
			{
				AST tmp119_AST = null;
				if (inputState.guessing==0) {
					tmp119_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp119_AST);
				}
				match(DIV_ASSIGN);
				break;
			}
			case MOD_ASSIGN:
			{
				AST tmp120_AST = null;
				if (inputState.guessing==0) {
					tmp120_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp120_AST);
				}
				match(MOD_ASSIGN);
				break;
			}
			case SR_ASSIGN:
			{
				AST tmp121_AST = null;
				if (inputState.guessing==0) {
					tmp121_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp121_AST);
				}
				match(SR_ASSIGN);
				break;
			}
			case BSR_ASSIGN:
			{
				AST tmp122_AST = null;
				if (inputState.guessing==0) {
					tmp122_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp122_AST);
				}
				match(BSR_ASSIGN);
				break;
			}
			case SL_ASSIGN:
			{
				AST tmp123_AST = null;
				if (inputState.guessing==0) {
					tmp123_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp123_AST);
				}
				match(SL_ASSIGN);
				break;
			}
			case BAND_ASSIGN:
			{
				AST tmp124_AST = null;
				if (inputState.guessing==0) {
					tmp124_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp124_AST);
				}
				match(BAND_ASSIGN);
				break;
			}
			case BXOR_ASSIGN:
			{
				AST tmp125_AST = null;
				if (inputState.guessing==0) {
					tmp125_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp125_AST);
				}
				match(BXOR_ASSIGN);
				break;
			}
			case BOR_ASSIGN:
			{
				AST tmp126_AST = null;
				if (inputState.guessing==0) {
					tmp126_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp126_AST);
				}
				match(BOR_ASSIGN);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			assignmentExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case SEMI:
		case RBRACK:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case COLON:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		assignmentExpression_AST = (AST)currentAST.root;
		returnAST = assignmentExpression_AST;
	}
	
	public final void conditionalExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST conditionalExpression_AST = null;
		
		logicalOrExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		switch ( LA(1)) {
		case QUESTION:
		{
			AST tmp127_AST = null;
			if (inputState.guessing==0) {
				tmp127_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp127_AST);
			}
			match(QUESTION);
			assignmentExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp128_AST = null;
			tmp128_AST = (AST)astFactory.create(LT(1));
			match(COLON);
			conditionalExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case SEMI:
		case RBRACK:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case ASSIGN:
		case COLON:
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
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		conditionalExpression_AST = (AST)currentAST.root;
		returnAST = conditionalExpression_AST;
	}
	
	public final void logicalOrExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST logicalOrExpression_AST = null;
		
		logicalAndExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop129:
		do {
			if ((LA(1)==LOR)) {
				AST tmp129_AST = null;
				if (inputState.guessing==0) {
					tmp129_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp129_AST);
				}
				match(LOR);
				logicalAndExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop129;
			}
			
		} while (true);
		}
		logicalOrExpression_AST = (AST)currentAST.root;
		returnAST = logicalOrExpression_AST;
	}
	
	public final void logicalAndExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST logicalAndExpression_AST = null;
		
		inclusiveOrExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop132:
		do {
			if ((LA(1)==LAND)) {
				AST tmp130_AST = null;
				if (inputState.guessing==0) {
					tmp130_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp130_AST);
				}
				match(LAND);
				inclusiveOrExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop132;
			}
			
		} while (true);
		}
		logicalAndExpression_AST = (AST)currentAST.root;
		returnAST = logicalAndExpression_AST;
	}
	
	public final void inclusiveOrExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST inclusiveOrExpression_AST = null;
		
		exclusiveOrExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop135:
		do {
			if ((LA(1)==BOR)) {
				AST tmp131_AST = null;
				if (inputState.guessing==0) {
					tmp131_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp131_AST);
				}
				match(BOR);
				exclusiveOrExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop135;
			}
			
		} while (true);
		}
		inclusiveOrExpression_AST = (AST)currentAST.root;
		returnAST = inclusiveOrExpression_AST;
	}
	
	public final void exclusiveOrExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST exclusiveOrExpression_AST = null;
		
		andExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop138:
		do {
			if ((LA(1)==BXOR)) {
				AST tmp132_AST = null;
				if (inputState.guessing==0) {
					tmp132_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp132_AST);
				}
				match(BXOR);
				andExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop138;
			}
			
		} while (true);
		}
		exclusiveOrExpression_AST = (AST)currentAST.root;
		returnAST = exclusiveOrExpression_AST;
	}
	
	public final void andExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST andExpression_AST = null;
		
		equalityExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop141:
		do {
			if ((LA(1)==BAND)) {
				AST tmp133_AST = null;
				if (inputState.guessing==0) {
					tmp133_AST = (AST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp133_AST);
				}
				match(BAND);
				equalityExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop141;
			}
			
		} while (true);
		}
		andExpression_AST = (AST)currentAST.root;
		returnAST = andExpression_AST;
	}
	
	public final void equalityExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST equalityExpression_AST = null;
		
		relationalExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop145:
		do {
			if ((LA(1)==NOT_EQUAL||LA(1)==EQUAL)) {
				{
				switch ( LA(1)) {
				case NOT_EQUAL:
				{
					AST tmp134_AST = null;
					if (inputState.guessing==0) {
						tmp134_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp134_AST);
					}
					match(NOT_EQUAL);
					break;
				}
				case EQUAL:
				{
					AST tmp135_AST = null;
					if (inputState.guessing==0) {
						tmp135_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp135_AST);
					}
					match(EQUAL);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				relationalExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop145;
			}
			
		} while (true);
		}
		equalityExpression_AST = (AST)currentAST.root;
		returnAST = equalityExpression_AST;
	}
	
	public final void relationalExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST relationalExpression_AST = null;
		
		shiftExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		switch ( LA(1)) {
		case SEMI:
		case RBRACK:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case ASSIGN:
		case COLON:
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
		{
			{
			_loop150:
			do {
				if (((LA(1) >= LT && LA(1) <= GE))) {
					{
					switch ( LA(1)) {
					case LT:
					{
						AST tmp136_AST = null;
						if (inputState.guessing==0) {
							tmp136_AST = (AST)astFactory.create(LT(1));
							astFactory.makeASTRoot(currentAST, tmp136_AST);
						}
						match(LT);
						break;
					}
					case GT:
					{
						AST tmp137_AST = null;
						if (inputState.guessing==0) {
							tmp137_AST = (AST)astFactory.create(LT(1));
							astFactory.makeASTRoot(currentAST, tmp137_AST);
						}
						match(GT);
						break;
					}
					case LE:
					{
						AST tmp138_AST = null;
						if (inputState.guessing==0) {
							tmp138_AST = (AST)astFactory.create(LT(1));
							astFactory.makeASTRoot(currentAST, tmp138_AST);
						}
						match(LE);
						break;
					}
					case GE:
					{
						AST tmp139_AST = null;
						if (inputState.guessing==0) {
							tmp139_AST = (AST)astFactory.create(LT(1));
							astFactory.makeASTRoot(currentAST, tmp139_AST);
						}
						match(GE);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					shiftExpression();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					break _loop150;
				}
				
			} while (true);
			}
			break;
		}
		case LITERAL_instanceof:
		{
			AST tmp140_AST = null;
			if (inputState.guessing==0) {
				tmp140_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp140_AST);
			}
			match(LITERAL_instanceof);
			typeSpec(true);
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		relationalExpression_AST = (AST)currentAST.root;
		returnAST = relationalExpression_AST;
	}
	
	public final void shiftExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST shiftExpression_AST = null;
		
		additiveExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop154:
		do {
			if (((LA(1) >= SL && LA(1) <= BSR))) {
				{
				switch ( LA(1)) {
				case SL:
				{
					AST tmp141_AST = null;
					if (inputState.guessing==0) {
						tmp141_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp141_AST);
					}
					match(SL);
					break;
				}
				case SR:
				{
					AST tmp142_AST = null;
					if (inputState.guessing==0) {
						tmp142_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp142_AST);
					}
					match(SR);
					break;
				}
				case BSR:
				{
					AST tmp143_AST = null;
					if (inputState.guessing==0) {
						tmp143_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp143_AST);
					}
					match(BSR);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				additiveExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop154;
			}
			
		} while (true);
		}
		shiftExpression_AST = (AST)currentAST.root;
		returnAST = shiftExpression_AST;
	}
	
	public final void additiveExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST additiveExpression_AST = null;
		
		multiplicativeExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop158:
		do {
			if ((LA(1)==PLUS||LA(1)==MINUS)) {
				{
				switch ( LA(1)) {
				case PLUS:
				{
					AST tmp144_AST = null;
					if (inputState.guessing==0) {
						tmp144_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp144_AST);
					}
					match(PLUS);
					break;
				}
				case MINUS:
				{
					AST tmp145_AST = null;
					if (inputState.guessing==0) {
						tmp145_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp145_AST);
					}
					match(MINUS);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				multiplicativeExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop158;
			}
			
		} while (true);
		}
		additiveExpression_AST = (AST)currentAST.root;
		returnAST = additiveExpression_AST;
	}
	
	public final void multiplicativeExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST multiplicativeExpression_AST = null;
		
		unaryExpression();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		_loop162:
		do {
			if ((_tokenSet_16.member(LA(1)))) {
				{
				switch ( LA(1)) {
				case STAR:
				{
					AST tmp146_AST = null;
					if (inputState.guessing==0) {
						tmp146_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp146_AST);
					}
					match(STAR);
					break;
				}
				case DIV:
				{
					AST tmp147_AST = null;
					if (inputState.guessing==0) {
						tmp147_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp147_AST);
					}
					match(DIV);
					break;
				}
				case MOD:
				{
					AST tmp148_AST = null;
					if (inputState.guessing==0) {
						tmp148_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp148_AST);
					}
					match(MOD);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				unaryExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				break _loop162;
			}
			
		} while (true);
		}
		multiplicativeExpression_AST = (AST)currentAST.root;
		returnAST = multiplicativeExpression_AST;
	}
	
	public final void unaryExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unaryExpression_AST = null;
		
		switch ( LA(1)) {
		case INC:
		{
			AST tmp149_AST = null;
			if (inputState.guessing==0) {
				tmp149_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp149_AST);
			}
			match(INC);
			unaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case DEC:
		{
			AST tmp150_AST = null;
			if (inputState.guessing==0) {
				tmp150_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp150_AST);
			}
			match(DEC);
			unaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case MINUS:
		{
			AST tmp151_AST = null;
			if (inputState.guessing==0) {
				tmp151_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp151_AST);
			}
			match(MINUS);
			if ( inputState.guessing==0 ) {
				tmp151_AST.setType(UNARY_MINUS);
			}
			unaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case PLUS:
		{
			AST tmp152_AST = null;
			if (inputState.guessing==0) {
				tmp152_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp152_AST);
			}
			match(PLUS);
			if ( inputState.guessing==0 ) {
				tmp152_AST.setType(UNARY_PLUS);
			}
			unaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpression_AST = (AST)currentAST.root;
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
		case LPAREN:
		case BNOT:
		case LNOT:
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
		{
			unaryExpressionNotPlusMinus();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpression_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = unaryExpression_AST;
	}
	
	public final void unaryExpressionNotPlusMinus() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unaryExpressionNotPlusMinus_AST = null;
		Token  lpb = null;
		AST lpb_AST = null;
		Token  lp = null;
		AST lp_AST = null;
		
		switch ( LA(1)) {
		case BNOT:
		{
			AST tmp153_AST = null;
			if (inputState.guessing==0) {
				tmp153_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp153_AST);
			}
			match(BNOT);
			unaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpressionNotPlusMinus_AST = (AST)currentAST.root;
			break;
		}
		case LNOT:
		{
			AST tmp154_AST = null;
			if (inputState.guessing==0) {
				tmp154_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp154_AST);
			}
			match(LNOT);
			unaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			unaryExpressionNotPlusMinus_AST = (AST)currentAST.root;
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
		case LPAREN:
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
		{
			{
			if ((LA(1)==LPAREN) && ((LA(2) >= LITERAL_void && LA(2) <= LITERAL_double))) {
				lpb = LT(1);
				if (inputState.guessing==0) {
					lpb_AST = (AST)astFactory.create(lpb);
					astFactory.makeASTRoot(currentAST, lpb_AST);
				}
				match(LPAREN);
				if ( inputState.guessing==0 ) {
					lpb_AST.setType(TYPECAST);
				}
				builtInTypeSpec(true);
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				AST tmp155_AST = null;
				tmp155_AST = (AST)astFactory.create(LT(1));
				match(RPAREN);
				unaryExpression();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
			}
			else {
				boolean synPredMatched167 = false;
				if (((LA(1)==LPAREN) && (LA(2)==IDENT))) {
					int _m167 = mark();
					synPredMatched167 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						classTypeSpec(true);
						match(RPAREN);
						unaryExpressionNotPlusMinus();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched167 = false;
					}
					rewind(_m167);
					inputState.guessing--;
				}
				if ( synPredMatched167 ) {
					lp = LT(1);
					if (inputState.guessing==0) {
						lp_AST = (AST)astFactory.create(lp);
						astFactory.makeASTRoot(currentAST, lp_AST);
					}
					match(LPAREN);
					if ( inputState.guessing==0 ) {
						lp_AST.setType(TYPECAST);
					}
					classTypeSpec(true);
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					AST tmp156_AST = null;
					tmp156_AST = (AST)astFactory.create(LT(1));
					match(RPAREN);
					unaryExpressionNotPlusMinus();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else if ((_tokenSet_17.member(LA(1))) && (_tokenSet_18.member(LA(2)))) {
					postfixExpression();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				unaryExpressionNotPlusMinus_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			returnAST = unaryExpressionNotPlusMinus_AST;
		}
		
	public final void postfixExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST postfixExpression_AST = null;
		Token  lbc = null;
		AST lbc_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		Token  lp = null;
		AST lp_AST = null;
		Token  in = null;
		AST in_AST = null;
		Token  de = null;
		AST de_AST = null;
		Token  lbt = null;
		AST lbt_AST = null;
		
		switch ( LA(1)) {
		case IDENT:
		case LPAREN:
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
		{
			primaryExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			_loop174:
			do {
				switch ( LA(1)) {
				case DOT:
				{
					AST tmp157_AST = null;
					if (inputState.guessing==0) {
						tmp157_AST = (AST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp157_AST);
					}
					match(DOT);
					{
					switch ( LA(1)) {
					case IDENT:
					{
						AST tmp158_AST = null;
						if (inputState.guessing==0) {
							tmp158_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp158_AST);
						}
						match(IDENT);
						break;
					}
					case LITERAL_this:
					{
						AST tmp159_AST = null;
						if (inputState.guessing==0) {
							tmp159_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp159_AST);
						}
						match(LITERAL_this);
						break;
					}
					case LITERAL_class:
					{
						AST tmp160_AST = null;
						if (inputState.guessing==0) {
							tmp160_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp160_AST);
						}
						match(LITERAL_class);
						break;
					}
					case LITERAL_new:
					{
						newExpression();
						if (inputState.guessing==0) {
							astFactory.addASTChild(currentAST, returnAST);
						}
						break;
					}
					case LITERAL_super:
					{
						AST tmp161_AST = null;
						if (inputState.guessing==0) {
							tmp161_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp161_AST);
						}
						match(LITERAL_super);
						AST tmp162_AST = null;
						if (inputState.guessing==0) {
							tmp162_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp162_AST);
						}
						match(LPAREN);
						{
						switch ( LA(1)) {
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
						case LPAREN:
						case PLUS:
						case MINUS:
						case INC:
						case DEC:
						case BNOT:
						case LNOT:
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
						{
							expressionList();
							if (inputState.guessing==0) {
								astFactory.addASTChild(currentAST, returnAST);
							}
							break;
						}
						case RPAREN:
						{
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						AST tmp163_AST = null;
						if (inputState.guessing==0) {
							tmp163_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp163_AST);
						}
						match(RPAREN);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					break;
				}
				case LPAREN:
				{
					lp = LT(1);
					if (inputState.guessing==0) {
						lp_AST = (AST)astFactory.create(lp);
						astFactory.makeASTRoot(currentAST, lp_AST);
					}
					match(LPAREN);
					if ( inputState.guessing==0 ) {
						lp_AST.setType(METHOD_CALL);
					}
					argList();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					AST tmp164_AST = null;
					tmp164_AST = (AST)astFactory.create(LT(1));
					match(RPAREN);
					break;
				}
				default:
					if ((LA(1)==LBRACK) && (LA(2)==RBRACK)) {
						{
						int _cnt173=0;
						_loop173:
						do {
							if ((LA(1)==LBRACK)) {
								lbc = LT(1);
								if (inputState.guessing==0) {
									lbc_AST = (AST)astFactory.create(lbc);
									astFactory.makeASTRoot(currentAST, lbc_AST);
								}
								match(LBRACK);
								if ( inputState.guessing==0 ) {
									lbc_AST.setType(ARRAY_DECLARATOR);
								}
								AST tmp165_AST = null;
								tmp165_AST = (AST)astFactory.create(LT(1));
								match(RBRACK);
							}
							else {
								if ( _cnt173>=1 ) { break _loop173; } else {throw new NoViableAltException(LT(1), getFilename());}
							}
							
							_cnt173++;
						} while (true);
						}
						AST tmp166_AST = null;
						if (inputState.guessing==0) {
							tmp166_AST = (AST)astFactory.create(LT(1));
							astFactory.makeASTRoot(currentAST, tmp166_AST);
						}
						match(DOT);
						AST tmp167_AST = null;
						if (inputState.guessing==0) {
							tmp167_AST = (AST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp167_AST);
						}
						match(LITERAL_class);
					}
					else if ((LA(1)==LBRACK) && (_tokenSet_12.member(LA(2)))) {
						lb = LT(1);
						if (inputState.guessing==0) {
							lb_AST = (AST)astFactory.create(lb);
							astFactory.makeASTRoot(currentAST, lb_AST);
						}
						match(LBRACK);
						if ( inputState.guessing==0 ) {
							lb_AST.setType(INDEX_OP);
						}
						expression();
						if (inputState.guessing==0) {
							astFactory.addASTChild(currentAST, returnAST);
						}
						AST tmp168_AST = null;
						tmp168_AST = (AST)astFactory.create(LT(1));
						match(RBRACK);
					}
				else {
					break _loop174;
				}
				}
			} while (true);
			}
			{
			switch ( LA(1)) {
			case INC:
			{
				in = LT(1);
				if (inputState.guessing==0) {
					in_AST = (AST)astFactory.create(in);
					astFactory.makeASTRoot(currentAST, in_AST);
				}
				match(INC);
				if ( inputState.guessing==0 ) {
					in_AST.setType(POST_INC);
				}
				break;
			}
			case DEC:
			{
				de = LT(1);
				if (inputState.guessing==0) {
					de_AST = (AST)astFactory.create(de);
					astFactory.makeASTRoot(currentAST, de_AST);
				}
				match(DEC);
				if ( inputState.guessing==0 ) {
					de_AST.setType(POST_DEC);
				}
				break;
			}
			case SEMI:
			case RBRACK:
			case STAR:
			case RCURLY:
			case COMMA:
			case RPAREN:
			case ASSIGN:
			case COLON:
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
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			postfixExpression_AST = (AST)currentAST.root;
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
			builtInType();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			_loop177:
			do {
				if ((LA(1)==LBRACK)) {
					lbt = LT(1);
					if (inputState.guessing==0) {
						lbt_AST = (AST)astFactory.create(lbt);
						astFactory.makeASTRoot(currentAST, lbt_AST);
					}
					match(LBRACK);
					if ( inputState.guessing==0 ) {
						lbt_AST.setType(ARRAY_DECLARATOR);
					}
					AST tmp169_AST = null;
					tmp169_AST = (AST)astFactory.create(LT(1));
					match(RBRACK);
				}
				else {
					break _loop177;
				}
				
			} while (true);
			}
			AST tmp170_AST = null;
			if (inputState.guessing==0) {
				tmp170_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp170_AST);
			}
			match(DOT);
			AST tmp171_AST = null;
			if (inputState.guessing==0) {
				tmp171_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp171_AST);
			}
			match(LITERAL_class);
			postfixExpression_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = postfixExpression_AST;
	}
	
	public final void primaryExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST primaryExpression_AST = null;
		
		switch ( LA(1)) {
		case IDENT:
		{
			AST tmp172_AST = null;
			if (inputState.guessing==0) {
				tmp172_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp172_AST);
			}
			match(IDENT);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_new:
		{
			newExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		{
			constant();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_super:
		{
			AST tmp173_AST = null;
			if (inputState.guessing==0) {
				tmp173_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp173_AST);
			}
			match(LITERAL_super);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_true:
		{
			AST tmp174_AST = null;
			if (inputState.guessing==0) {
				tmp174_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp174_AST);
			}
			match(LITERAL_true);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_false:
		{
			AST tmp175_AST = null;
			if (inputState.guessing==0) {
				tmp175_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp175_AST);
			}
			match(LITERAL_false);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_this:
		{
			AST tmp176_AST = null;
			if (inputState.guessing==0) {
				tmp176_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp176_AST);
			}
			match(LITERAL_this);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LITERAL_null:
		{
			AST tmp177_AST = null;
			if (inputState.guessing==0) {
				tmp177_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp177_AST);
			}
			match(LITERAL_null);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		case LPAREN:
		{
			AST tmp178_AST = null;
			if (inputState.guessing==0) {
				tmp178_AST = (AST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp178_AST);
			}
			match(LPAREN);
			assignmentExpression();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp179_AST = null;
			tmp179_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			primaryExpression_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = primaryExpression_AST;
	}
	
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
	public final void newExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST newExpression_AST = null;
		
		AST tmp180_AST = null;
		if (inputState.guessing==0) {
			tmp180_AST = (AST)astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp180_AST);
		}
		match(LITERAL_new);
		type();
		if (inputState.guessing==0) {
			astFactory.addASTChild(currentAST, returnAST);
		}
		{
		switch ( LA(1)) {
		case LPAREN:
		{
			AST tmp181_AST = null;
			tmp181_AST = (AST)astFactory.create(LT(1));
			match(LPAREN);
			argList();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			AST tmp182_AST = null;
			tmp182_AST = (AST)astFactory.create(LT(1));
			match(RPAREN);
			{
			switch ( LA(1)) {
			case LCURLY:
			{
				classBlock();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				break;
			}
			case SEMI:
			case LBRACK:
			case RBRACK:
			case DOT:
			case STAR:
			case RCURLY:
			case COMMA:
			case LPAREN:
			case RPAREN:
			case ASSIGN:
			case COLON:
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
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case LBRACK:
		{
			newArrayDeclarator();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			{
			switch ( LA(1)) {
			case LCURLY:
			{
				arrayInitializer();
				if (inputState.guessing==0) {
					astFactory.addASTChild(currentAST, returnAST);
				}
				break;
			}
			case SEMI:
			case LBRACK:
			case RBRACK:
			case DOT:
			case STAR:
			case RCURLY:
			case COMMA:
			case LPAREN:
			case RPAREN:
			case ASSIGN:
			case COLON:
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
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		newExpression_AST = (AST)currentAST.root;
		returnAST = newExpression_AST;
	}
	
	public final void argList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST argList_AST = null;
		
		{
		switch ( LA(1)) {
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
		case LPAREN:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
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
		{
			expressionList();
			if (inputState.guessing==0) {
				astFactory.addASTChild(currentAST, returnAST);
			}
			break;
		}
		case RPAREN:
		{
			if ( inputState.guessing==0 ) {
				argList_AST = (AST)currentAST.root;
				argList_AST = (AST)astFactory.create(ELIST,"ELIST");
				currentAST.root = argList_AST;
				currentAST.child = argList_AST!=null &&argList_AST.getFirstChild()!=null ?
					argList_AST.getFirstChild() : argList_AST;
				currentAST.advanceChildToEnd();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		argList_AST = (AST)currentAST.root;
		returnAST = argList_AST;
	}
	
	public final void constant() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST constant_AST = null;
		
		switch ( LA(1)) {
		case NUM_INT:
		{
			AST tmp183_AST = null;
			if (inputState.guessing==0) {
				tmp183_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp183_AST);
			}
			match(NUM_INT);
			constant_AST = (AST)currentAST.root;
			break;
		}
		case CHAR_LITERAL:
		{
			AST tmp184_AST = null;
			if (inputState.guessing==0) {
				tmp184_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp184_AST);
			}
			match(CHAR_LITERAL);
			constant_AST = (AST)currentAST.root;
			break;
		}
		case STRING_LITERAL:
		{
			AST tmp185_AST = null;
			if (inputState.guessing==0) {
				tmp185_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp185_AST);
			}
			match(STRING_LITERAL);
			constant_AST = (AST)currentAST.root;
			break;
		}
		case NUM_FLOAT:
		{
			AST tmp186_AST = null;
			if (inputState.guessing==0) {
				tmp186_AST = (AST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp186_AST);
			}
			match(NUM_FLOAT);
			constant_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = constant_AST;
	}
	
	public final void newArrayDeclarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST newArrayDeclarator_AST = null;
		Token  lb = null;
		AST lb_AST = null;
		
		{
		int _cnt188=0;
		_loop188:
		do {
			if ((LA(1)==LBRACK) && (_tokenSet_19.member(LA(2)))) {
				lb = LT(1);
				if (inputState.guessing==0) {
					lb_AST = (AST)astFactory.create(lb);
					astFactory.makeASTRoot(currentAST, lb_AST);
				}
				match(LBRACK);
				if ( inputState.guessing==0 ) {
					lb_AST.setType(ARRAY_DECLARATOR);
				}
				{
				switch ( LA(1)) {
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
				case LPAREN:
				case PLUS:
				case MINUS:
				case INC:
				case DEC:
				case BNOT:
				case LNOT:
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
				{
					expression();
					if (inputState.guessing==0) {
						astFactory.addASTChild(currentAST, returnAST);
					}
					break;
				}
				case RBRACK:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				AST tmp187_AST = null;
				tmp187_AST = (AST)astFactory.create(LT(1));
				match(RBRACK);
			}
			else {
				if ( _cnt188>=1 ) { break _loop188; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt188++;
		} while (true);
		}
		newArrayDeclarator_AST = (AST)currentAST.root;
		returnAST = newArrayDeclarator_AST;
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
		"IDENT",
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
		"\"this\"",
		"\"super\"",
		"\"true\"",
		"\"false\"",
		"\"null\"",
		"\"new\"",
		"NUM_INT",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"NUM_FLOAT",
		"WS",
		"SL_COMMENT",
		"ML_COMMENT",
		"ESC",
		"HEX_DIGIT",
		"VOCAB",
		"EXPONENT",
		"FLOAT_SUFFIX"
	};
	
	private static final long _tokenSet_0_data_[] = { -288224328837758976L, 47L, 0L, 0L };
	public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
	private static final long _tokenSet_1_data_[] = { -288228726884270080L, 7L, 0L, 0L };
	public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
	private static final long _tokenSet_2_data_[] = { -216241501590519808L, 47L, 0L, 0L };
	public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
	private static final long _tokenSet_3_data_[] = { -144166315366547456L, 1071L, 0L, 0L };
	public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
	private static final long _tokenSet_4_data_[] = { 108103983242936320L, 0L, 0L };
	public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
	private static final long _tokenSet_5_data_[] = { 21990232555520L, 4352L, 0L, 0L };
	public static final BitSet _tokenSet_5 = new BitSet(_tokenSet_5_data_);
	private static final long _tokenSet_6_data_[] = { -216237103544008704L, -1873497444818451377L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_6 = new BitSet(_tokenSet_6_data_);
	private static final long _tokenSet_7_data_[] = { 71987225293750272L, -1873497444986125248L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_7 = new BitSet(_tokenSet_7_data_);
	private static final long _tokenSet_8_data_[] = { -216237103544008704L, -1873497444717722417L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_8 = new BitSet(_tokenSet_8_data_);
	private static final long _tokenSet_9_data_[] = { -46729244180480L, -11025L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_9 = new BitSet(_tokenSet_9_data_);
	private static final long _tokenSet_10_data_[] = { -216241501590519808L, 7L, 0L, 0L };
	public static final BitSet _tokenSet_10 = new BitSet(_tokenSet_10_data_);
	private static final long _tokenSet_11_data_[] = { -144166315366547456L, 7L, 0L, 0L };
	public static final BitSet _tokenSet_11 = new BitSet(_tokenSet_11_data_);
	private static final long _tokenSet_12_data_[] = { 71987225293750272L, -1873497444986125312L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_12 = new BitSet(_tokenSet_12_data_);
	private static final long _tokenSet_13_data_[] = { 288181997640089600L, -1073736704L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_13 = new BitSet(_tokenSet_13_data_);
	private static final long _tokenSet_14_data_[] = { 288181997640089600L, -1073736448L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_14 = new BitSet(_tokenSet_14_data_);
	private static final long _tokenSet_15_data_[] = { 71987225293750272L, -1873497444986108928L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_15 = new BitSet(_tokenSet_15_data_);
	private static final long _tokenSet_16_data_[] = { 144115188075855872L, 1729382256910270464L, 0L, 0L };
	public static final BitSet _tokenSet_16 = new BitSet(_tokenSet_16_data_);
	private static final long _tokenSet_17_data_[] = { 71987225293750272L, 1024L, 2046L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_17 = new BitSet(_tokenSet_17_data_);
	private static final long _tokenSet_18_data_[] = { 288217182012178432L, -1073717888L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_18 = new BitSet(_tokenSet_18_data_);
	private static final long _tokenSet_19_data_[] = { 72022409665839104L, -1873497444986125312L, 2047L, 0L, 0L, 0L };
	public static final BitSet _tokenSet_19 = new BitSet(_tokenSet_19_data_);
	
	}
