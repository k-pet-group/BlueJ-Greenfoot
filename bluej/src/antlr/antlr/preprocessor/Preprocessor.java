/*
 * ANTLR-generated file resulting from grammar preproc.g
 * 
 * Terence Parr, MageLang Institute
 * with John Lilley, Empathy Software
 * ANTLR Version 2.5.0; 1996,1997
 */

package antlr.preprocessor;

import java.io.IOException;
import antlr.Tokenizer;
import antlr.TokenBuffer;
import antlr.LLkParser;
import antlr.Token;
import antlr.ParserException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.collections.impl.BitSet;

import antlr.collections.impl.IndexedVector;
import java.util.Hashtable;
import antlr.preprocessor.Grammar;

public class Preprocessor extends antlr.LLkParser
       implements PreprocessorTokenTypes
 {

protected Preprocessor(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public Preprocessor(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected Preprocessor(Tokenizer lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public Preprocessor(Tokenizer lexer) {
  this(lexer,1);
}

	public final void grammarFile(
		Hierarchy hier, String file
	) throws ParserException, IOException {
		
		Token  hdr = null;
		
			Grammar gr;
			IndexedVector opt=null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case HEADER_ACTION:
			{
				hdr = LT(1);
				match(HEADER_ACTION);
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
			case OPTIONS_START:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				opt=optionSpec(null);
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			_loop5:
			do {
				if ((LA(1)==ACTION||LA(1)==LITERAL_class)) {
					gr=class_def(hier);
					
								// System.out.println(gr);
								if ( hdr!=null ) {
									hier.getFile(file).setHeaderAction(hdr.getText());
								}
								if ( opt!=null ) {
									hier.getFile(file).setOptions(opt);
								}
								if ( gr!=null ) {
									gr.setFileName(file);
									hier.addGrammar(gr);
								}
								
				}
				else {
					break _loop5;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_0);
		}
	}
	
	public final IndexedVector  optionSpec(
		Grammar gr
	) throws ParserException, IOException {
		IndexedVector options;
		
		Token  op = null;
		Token  rhs = null;
		
			options = new IndexedVector();
		
		
		try {      // for error handling
			match(OPTIONS_START);
			{
			_loop21:
			do {
				if ((LA(1)==ID)) {
					op = LT(1);
					match(ID);
					rhs = LT(1);
					match(ASSIGN_RHS);
					
									Option newOp = new Option(op.getText(),rhs.getText(),gr);
									options.appendElement(newOp.getName(),newOp);
									if ( rhs.getText().equals("tokdef") ) {
										gr.specifiedVocabulary = true;
									}
									
				}
				else {
					break _loop21;
				}
				
			} while (true);
			}
			match(RCURLY);
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return options;
	}
	
	public final Grammar  class_def(
		Hierarchy hier
	) throws ParserException, IOException {
		Grammar gr;
		
		Token  preamble = null;
		Token  sub = null;
		Token  sup = null;
		Token  memberA = null;
		
			gr=null;
			IndexedVector rules = new IndexedVector(100);
			IndexedVector classOptions = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ACTION:
			{
				preamble = LT(1);
				match(ACTION);
				break;
			}
			case LITERAL_class:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			match(LITERAL_class);
			sub = LT(1);
			match(ID);
			match(LITERAL_extends);
			sup = LT(1);
			match(ID);
			match(SEMI);
			
						gr = (Grammar)hier.getGrammar(sub.getText());
						if ( gr!=null ) {
							antlr.Tool.toolError("redefinition of grammar "+gr.getName()+" ignored");
							gr=null;
						}
						else {
							gr = new Grammar(sub.getText(), sup.getText(), rules);
							if ( preamble!=null ) {
								gr.setPreambleAction(preamble.getText());
							}
						}
					
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				classOptions=optionSpec(gr);
				break;
			}
			case ACTION:
			case ID:
			case TOKENS_START:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			
					if ( gr!=null ) {
						gr.setOptions(classOptions);
					}
					
			{
			switch ( LA(1)) {
			case TOKENS_START:
			{
				tokensSpec();
				break;
			}
			case ACTION:
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			switch ( LA(1)) {
			case ACTION:
			{
				memberA = LT(1);
				match(ACTION);
				gr.setMemberAction(memberA.getText());
				break;
			}
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			int _cnt12=0;
			_loop12:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					rule(gr);
				}
				else {
					if ( _cnt12>=1 ) { break _loop12; } else {throw new NoViableAltException(LT(1));}
				}
				
				_cnt12++;
			} while (true);
			}
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_3);
		}
		return gr;
	}
	
	public final void tokensSpec() throws ParserException, IOException {
		
		
		try {      // for error handling
			match(TOKENS_START);
			match(TOKEN_REF);
			{
			switch ( LA(1)) {
			case ASSIGN:
			{
				match(ASSIGN);
				match(STRING_LITERAL);
				break;
			}
			case COMMA:
			case RCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			_loop17:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					match(TOKEN_REF);
					{
					switch ( LA(1)) {
					case ASSIGN:
					{
						match(ASSIGN);
						match(STRING_LITERAL);
						break;
					}
					case COMMA:
					case RCURLY:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1));
					}
					}
					}
				}
				else {
					break _loop17;
				}
				
			} while (true);
			}
			match(RCURLY);
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_4);
		}
	}
	
	public final void rule(
		Grammar gr
	) throws ParserException, IOException {
		
		Token  r = null;
		Token  arg = null;
		Token  ret = null;
		Token  init = null;
		Token  blk = null;
		
			IndexedVector o = null;	// options for rule
			String vis = null;
			boolean bang=false;
			String eg=null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_protected:
			{
				match(LITERAL_protected);
				vis="protected";
				break;
			}
			case LITERAL_private:
			{
				match(LITERAL_private);
				vis="private";
				break;
			}
			case LITERAL_public:
			{
				match(LITERAL_public);
				vis="public";
				break;
			}
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			r = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case BANG:
			{
				match(BANG);
				bang=true;
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case ARG_ACTION:
			case LITERAL_returns:
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				arg = LT(1);
				match(ARG_ACTION);
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case LITERAL_returns:
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_returns:
			{
				match(LITERAL_returns);
				ret = LT(1);
				match(ARG_ACTION);
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				o=optionSpec(null);
				break;
			}
			case ACTION:
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			switch ( LA(1)) {
			case ACTION:
			{
				init = LT(1);
				match(ACTION);
				break;
			}
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			blk = LT(1);
			match(RULE_BLOCK);
			eg=exceptionGroup();
			
					String rtext = blk.getText()+eg;
					Rule ppr = new Rule(r.getText(),rtext,o,gr);
					if ( arg!=null ) {
						ppr.setArgs(arg.getText());
					}
					if ( ret!=null ) {
						ppr.setReturnValue(ret.getText());
					}
					if ( init!=null ) {
						ppr.setInitAction(init.getText());
					}
					if ( bang ) {
						ppr.setBang();
					}
					ppr.setVisibility(vis);
					if ( gr!=null ) {
						gr.addRule(ppr);
					}
					
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
	}
	
	public final void dummy() throws ParserException, IOException {
		
		
		try {      // for error handling
			match(LITERAL_tokens);
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_0);
		}
	}
	
	public final String  exceptionGroup() throws ParserException, IOException {
		String g;
		
		String e=null; g="";
		
		try {      // for error handling
			{
			_loop31:
			do {
				if ((LA(1)==LITERAL_exception)) {
					e=exceptionSpec();
					g += e;
				}
				else {
					break _loop31;
				}
				
			} while (true);
			}
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
		return g;
	}
	
	public final String  exceptionSpec() throws ParserException, IOException {
		String es;
		
		Token  aa = null;
		String h=null;
		es = System.getProperty("line.separator")+"exception ";
		
		
		try {      // for error handling
			match(LITERAL_exception);
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				aa = LT(1);
				match(ARG_ACTION);
				es += aa.getText();
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_exception:
			case LITERAL_catch:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			{
			_loop35:
			do {
				if ((LA(1)==LITERAL_catch)) {
					h=exceptionHandler();
					es += h;
				}
				else {
					break _loop35;
				}
				
			} while (true);
			}
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
		return es;
	}
	
	public final String  exceptionHandler() throws ParserException, IOException {
		String h;
		
		Token  a1 = null;
		Token  a2 = null;
		h=null;
		
		try {      // for error handling
			match(LITERAL_catch);
			a1 = LT(1);
			match(ARG_ACTION);
			a2 = LT(1);
			match(ACTION);
			h = System.getProperty("line.separator")+
						 "catch "+a1.getText()+" "+a2.getText();
		}
		catch (ParserException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return h;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"HEADER_ACTION",
		"ACTION",
		"\"class\"",
		"ID",
		"\"extends\"",
		"SEMI",
		"TOKENS_START",
		"TOKEN_REF",
		"ASSIGN",
		"STRING_LITERAL",
		"COMMA",
		"RCURLY",
		"\"tokens\"",
		"OPTIONS_START",
		"ASSIGN_RHS",
		"\"protected\"",
		"\"private\"",
		"\"public\"",
		"BANG",
		"ARG_ACTION",
		"\"returns\"",
		"RULE_BLOCK",
		"\"exception\"",
		"\"catch\"",
		"SUBRULE_BLOCK",
		"ALT",
		"ELEMENT",
		"ID_OR_KEYWORD",
		"WS",
		"COMMENT",
		"SL_COMMENT",
		"ML_COMMENT",
		"CHAR_LITERAL",
		"ESC",
		"DIGIT",
		"XDIGIT"
	};
	
	private static final long _tokenSet_0_data_[] = { 2L, 0L };
	public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
	private static final long _tokenSet_1_data_[] = { 37225698L, 0L };
	public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
	private static final long _tokenSet_2_data_[] = { 3670144L, 0L };
	public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
	private static final long _tokenSet_3_data_[] = { 98L, 0L };
	public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
	private static final long _tokenSet_4_data_[] = { 3670176L, 0L };
	public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
	private static final long _tokenSet_5_data_[] = { 3670242L, 0L };
	public static final BitSet _tokenSet_5 = new BitSet(_tokenSet_5_data_);
	private static final long _tokenSet_6_data_[] = { 70779106L, 0L };
	public static final BitSet _tokenSet_6 = new BitSet(_tokenSet_6_data_);
	private static final long _tokenSet_7_data_[] = { 204996834L, 0L };
	public static final BitSet _tokenSet_7 = new BitSet(_tokenSet_7_data_);
	
	}
