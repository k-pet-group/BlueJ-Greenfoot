/*
 * ANTLR-generated file resulting from grammar antlr.g
 * 
 * Terence Parr, MageLang Institute
 * with John Lilley, Empathy Software
 * ANTLR Version 2.5.0; 1996,1997
 */

package antlr;

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

import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ANTLRParser extends antlr.LLkParser
       implements ANTLRTokenTypes
 {

	public static final String version = "2.5.0";
	private static final boolean DEBUG_PARSER = false;

	ANTLRGrammarParseBehavior behavior;
	Tool tool;
	protected int blockNesting= -1;

	public ANTLRParser(
		TokenBuffer tokenBuf, 
		ANTLRGrammarParseBehavior behavior_,
		Tool tool_
	) {
		super(tokenBuf, 1);
		tokenNames = _tokenNames;
		behavior = behavior_;
		tool = tool_;
	}

	private boolean lastInRule() throws IOException {
		if ( blockNesting==0 && (LA(1)==SEMI || LA(1)==LITERAL_exception || LA(1)==OR) ) {
			return true;
		}
		return false;
	}

protected ANTLRParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public ANTLRParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected ANTLRParser(Tokenizer lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public ANTLRParser(Tokenizer lexer) {
  this(lexer,2);
}

	public final void grammar() throws ParserException, IOException {
		
		Token  h = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_header:
			{
				match(LITERAL_header);
				h = LT(1);
				match(ACTION);
				if ( guessing==0 ) {
					behavior.refHeaderAction(h);
				}
				break;
			}
			case EOF:
			case ACTION:
			case DOC_COMMENT:
			case LITERAL_lexclass:
			case LITERAL_class:
			case OPTIONS:
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
			case OPTIONS:
			{
				fileOptionsSpec();
				break;
			}
			case EOF:
			case ACTION:
			case DOC_COMMENT:
			case LITERAL_lexclass:
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
				if (((LA(1) >= ACTION && LA(1) <= LITERAL_class))) {
					classDef();
				}
				else {
					break _loop5;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
		}
		catch (ParserException ex) {
			if (guessing==0) {
				
						reportError("rule grammar trapped: "+ex.toString());
						consumeUntil(EOF);
					
			} else {
				throw ex;
			}
		}
	}
	
	public final void fileOptionsSpec() throws ParserException, IOException {
		
		Token idTok; Token value;
		
		match(OPTIONS);
		{
		_loop16:
		do {
			if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF)) {
				idTok=id();
				match(ASSIGN);
				value=optionValue();
				if ( guessing==0 ) {
					behavior.setFileOption(idTok, value);
				}
				match(SEMI);
			}
			else {
				break _loop16;
			}
			
		} while (true);
		}
		match(RCURLY);
	}
	
	public final void classDef() throws ParserException, IOException {
		
		Token  a = null;
		Token  d = null;
		String doc=null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ACTION:
			{
				a = LT(1);
				match(ACTION);
				if ( guessing==0 ) {
					behavior.refPreambleAction(a);
				}
				break;
			}
			case DOC_COMMENT:
			case LITERAL_lexclass:
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
			switch ( LA(1)) {
			case DOC_COMMENT:
			{
				d = LT(1);
				match(DOC_COMMENT);
				if ( guessing==0 ) {
					doc=d.getText();
				}
				break;
			}
			case LITERAL_lexclass:
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
			boolean synPredMatched11 = false;
			if (((LA(1)==LITERAL_lexclass||LA(1)==LITERAL_class) && (LA(2)==TOKEN_REF||LA(2)==RULE_REF))) {
				int _m11 = mark();
				synPredMatched11 = true;
				guessing++;
				try {
					{
					switch ( LA(1)) {
					case LITERAL_lexclass:
					{
						match(LITERAL_lexclass);
						break;
					}
					case LITERAL_class:
					{
						match(LITERAL_class);
						id();
						match(LITERAL_extends);
						match(LITERAL_Lexer);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1));
					}
					}
					}
				}
				catch (ParserException pe) {
					synPredMatched11 = false;
				}
				rewind(_m11);
				guessing--;
			}
			if ( synPredMatched11 ) {
				lexerSpec(doc);
			}
			else {
				boolean synPredMatched13 = false;
				if (((LA(1)==LITERAL_class) && (LA(2)==TOKEN_REF||LA(2)==RULE_REF))) {
					int _m13 = mark();
					synPredMatched13 = true;
					guessing++;
					try {
						{
						match(LITERAL_class);
						id();
						match(LITERAL_extends);
						match(LITERAL_TreeParser);
						}
					}
					catch (ParserException pe) {
						synPredMatched13 = false;
					}
					rewind(_m13);
					guessing--;
				}
				if ( synPredMatched13 ) {
					treeParserSpec(doc);
				}
				else if ((LA(1)==LITERAL_class) && (LA(2)==TOKEN_REF||LA(2)==RULE_REF)) {
					parserSpec(doc);
				}
				else {
					throw new NoViableAltException(LT(1));
				}
				}
				}
				rules();
				if ( guessing==0 ) {
					behavior.endGrammar();
				}
			}
			catch (ParserException ex) {
				if (guessing==0) {
					
							if ( ex instanceof NoViableAltException ) {
								NoViableAltException e = (NoViableAltException)ex;
								if ( e.token.getType()==DOC_COMMENT ) {
									reportError("line "+ex.line+": JAVADOC comments may only prefix rules and grammars");
								}
								else {
									reportError("rule classDef trapped: "+ex.toString());
								}
							}
							else {
								reportError("rule classDef trapped: "+ex.toString());
							}
							behavior.abortGrammar();
							boolean consuming = true;
							// consume everything until the next class definition or EOF
							while (consuming) {
								consume();
								switch(LA(1)) {
								case LITERAL_class:
								case LITERAL_lexclass:
								case EOF:
									consuming = false;
									break;
								}
							}
						
				} else {
					throw ex;
				}
			}
		}
		
	public final  Token  id() throws ParserException, IOException {
		 Token idTok ;
		
		Token  a = null;
		Token  b = null;
		idTok = null;
		
		switch ( LA(1)) {
		case TOKEN_REF:
		{
			a = LT(1);
			match(TOKEN_REF);
			if ( guessing==0 ) {
				idTok = a;
			}
			break;
		}
		case RULE_REF:
		{
			b = LT(1);
			match(RULE_REF);
			if ( guessing==0 ) {
				idTok = b;
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		return idTok ;
	}
	
	public final void lexerSpec(
		String doc
	) throws ParserException, IOException {
		
		Token  lc = null;
		Token  a = null;
		
			Token idTok;
			String sup=null;
		
		
		{
		switch ( LA(1)) {
		case LITERAL_lexclass:
		{
			lc = LT(1);
			match(LITERAL_lexclass);
			idTok=id();
			if ( guessing==0 ) {
				System.out.println("warning: line " + lc.getLine() + ": 'lexclass' is deprecated; use 'class X extends Lexer'");
			}
			break;
		}
		case LITERAL_class:
		{
			match(LITERAL_class);
			idTok=id();
			match(LITERAL_extends);
			match(LITERAL_Lexer);
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				sup=superClass();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.startLexer(idTok,sup,doc);
		}
		match(SEMI);
		{
		switch ( LA(1)) {
		case OPTIONS:
		{
			lexerOptionsSpec();
			break;
		}
		case ACTION:
		case DOC_COMMENT:
		case TOKENS:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
		case TOKENS:
		{
			tokensSpec();
			break;
		}
		case ACTION:
		case DOC_COMMENT:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.endOptions();
		}
		{
		switch ( LA(1)) {
		case ACTION:
		{
			a = LT(1);
			match(ACTION);
			if ( guessing==0 ) {
				behavior.refMemberAction(a);
			}
			break;
		}
		case DOC_COMMENT:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
	
	public final void treeParserSpec(
		String doc
	) throws ParserException, IOException {
		
		Token  a = null;
		
			Token idTok;
			String sup=null;
		
		
		match(LITERAL_class);
		idTok=id();
		match(LITERAL_extends);
		match(LITERAL_TreeParser);
		{
		switch ( LA(1)) {
		case LPAREN:
		{
			sup=superClass();
			break;
		}
		case SEMI:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.startTreeWalker(idTok,sup,doc);
		}
		match(SEMI);
		{
		switch ( LA(1)) {
		case OPTIONS:
		{
			treeParserOptionsSpec();
			break;
		}
		case ACTION:
		case DOC_COMMENT:
		case TOKENS:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
		case TOKENS:
		{
			tokensSpec();
			break;
		}
		case ACTION:
		case DOC_COMMENT:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.endOptions();
		}
		{
		switch ( LA(1)) {
		case ACTION:
		{
			a = LT(1);
			match(ACTION);
			if ( guessing==0 ) {
				behavior.refMemberAction(a);
			}
			break;
		}
		case DOC_COMMENT:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
	
	public final void parserSpec(
		String doc
	) throws ParserException, IOException {
		
		Token  a = null;
		
			Token idTok;
			String sup=null;
		
		
		match(LITERAL_class);
		idTok=id();
		{
		switch ( LA(1)) {
		case LITERAL_extends:
		{
			match(LITERAL_extends);
			match(LITERAL_Parser);
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				sup=superClass();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			break;
		}
		case SEMI:
		{
			if ( guessing==0 ) {
				
							System.out.println("warning: line " +
								idTok.getLine() + ": use 'class X extends Parser'");
							
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.startParser(idTok, sup, doc);
		}
		match(SEMI);
		{
		switch ( LA(1)) {
		case OPTIONS:
		{
			parserOptionsSpec();
			break;
		}
		case ACTION:
		case DOC_COMMENT:
		case TOKENS:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
		case TOKENS:
		{
			tokensSpec();
			break;
		}
		case ACTION:
		case DOC_COMMENT:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.endOptions();
		}
		{
		switch ( LA(1)) {
		case ACTION:
		{
			a = LT(1);
			match(ACTION);
			if ( guessing==0 ) {
				behavior.refMemberAction(a);
			}
			break;
		}
		case DOC_COMMENT:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
	
	public final void rules() throws ParserException, IOException {
		
		
		{
		int _cnt62=0;
		_loop62:
		do {
			if ((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2)))) {
				rule();
			}
			else {
				if ( _cnt62>=1 ) { break _loop62; } else {throw new NoViableAltException(LT(1));}
			}
			
			_cnt62++;
		} while (true);
		}
	}
	
	public final  Token  optionValue() throws ParserException, IOException {
		 Token retval ;
		
		Token  sl = null;
		Token  cl = null;
		Token  il = null;
		retval = null;
		
		switch ( LA(1)) {
		case SEMI:
		{
			break;
		}
		case TOKEN_REF:
		case RULE_REF:
		{
			retval=id();
			break;
		}
		case STRING_LITERAL:
		{
			sl = LT(1);
			match(STRING_LITERAL);
			if ( guessing==0 ) {
				retval = sl;
			}
			break;
		}
		case CHAR_LITERAL:
		{
			cl = LT(1);
			match(CHAR_LITERAL);
			if ( guessing==0 ) {
				retval = cl;
			}
			break;
		}
		case INT:
		{
			il = LT(1);
			match(INT);
			if ( guessing==0 ) {
				retval = il;
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		return retval ;
	}
	
	public final void parserOptionsSpec() throws ParserException, IOException {
		
		Token idTok; Token value;
		
		match(OPTIONS);
		{
		_loop19:
		do {
			if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF)) {
				idTok=id();
				match(ASSIGN);
				value=optionValue();
				if ( guessing==0 ) {
					behavior.setGrammarOption(idTok, value);
				}
				match(SEMI);
			}
			else {
				break _loop19;
			}
			
		} while (true);
		}
		match(RCURLY);
	}
	
	public final void treeParserOptionsSpec() throws ParserException, IOException {
		
		Token idTok; Token value;
		
		match(OPTIONS);
		{
		_loop22:
		do {
			if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF)) {
				idTok=id();
				match(ASSIGN);
				value=optionValue();
				if ( guessing==0 ) {
					behavior.setGrammarOption(idTok, value);
				}
				match(SEMI);
			}
			else {
				break _loop22;
			}
			
		} while (true);
		}
		match(RCURLY);
	}
	
	public final void lexerOptionsSpec() throws ParserException, IOException {
		
		Token idTok; Token value; BitSet b;
		
		match(OPTIONS);
		{
		_loop25:
		do {
			switch ( LA(1)) {
			case LITERAL_charVocabulary:
			{
				match(LITERAL_charVocabulary);
				match(ASSIGN);
				b=charSet();
				match(SEMI);
				if ( guessing==0 ) {
					behavior.setCharVocabulary(b);
				}
				break;
			}
			case TOKEN_REF:
			case RULE_REF:
			{
				idTok=id();
				match(ASSIGN);
				value=optionValue();
				if ( guessing==0 ) {
					behavior.setGrammarOption(idTok, value);
				}
				match(SEMI);
				break;
			}
			default:
			{
				break _loop25;
			}
			}
		} while (true);
		}
		match(RCURLY);
	}
	
	public final  BitSet  charSet() throws ParserException, IOException {
		 BitSet b ;
		
		
			b = null; 
			BitSet tmpSet = null;
		
		
		b=setBlockElement();
		{
		_loop32:
		do {
			if ((LA(1)==OR)) {
				match(OR);
				tmpSet=setBlockElement();
				if ( guessing==0 ) {
					b.orInPlace(tmpSet);
				}
			}
			else {
				break _loop32;
			}
			
		} while (true);
		}
		return b ;
	}
	
	public final void subruleOptionsSpec() throws ParserException, IOException {
		
		Token idTok; Token value;
		
		match(OPTIONS);
		{
		_loop28:
		do {
			if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF)) {
				idTok=id();
				match(ASSIGN);
				value=optionValue();
				if ( guessing==0 ) {
					behavior.setSubruleOption(idTok, value);
				}
				match(SEMI);
			}
			else {
				break _loop28;
			}
			
		} while (true);
		}
		match(RCURLY);
	}
	
	public final  BitSet  setBlockElement() throws ParserException, IOException {
		 BitSet b ;
		
		Token  c1 = null;
		Token  c2 = null;
		
			b = null;
			int rangeMin = 0; 
		
		
		c1 = LT(1);
		match(CHAR_LITERAL);
		if ( guessing==0 ) {
			
					rangeMin = ANTLRLexer.tokenTypeForCharLiteral(c1.getText()); 
					b = BitSet.of(rangeMin);
				
		}
		{
		switch ( LA(1)) {
		case RANGE:
		{
			match(RANGE);
			c2 = LT(1);
			match(CHAR_LITERAL);
			if ( guessing==0 ) {
				
							int rangeMax = ANTLRLexer.tokenTypeForCharLiteral(c2.getText()); 
							if (rangeMax < rangeMin) {
								tool.error("Malformed range", c1.getLine());
							}
							for (int i = rangeMin+1; i <= rangeMax; i++) {
								b.add(i);
							}
						
			}
			break;
		}
		case SEMI:
		case OR:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		return b ;
	}
	
	public final void tokensSpec() throws ParserException, IOException {
		
		
		match(TOKENS);
		match(TOKEN_REF);
		{
		switch ( LA(1)) {
		case ASSIGN:
		{
			match(ASSIGN);
			match(STRING_LITERAL);
			break;
		}
		case RCURLY:
		case COMMA:
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
		_loop39:
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
				case RCURLY:
				case COMMA:
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
				break _loop39;
			}
			
		} while (true);
		}
		match(RCURLY);
	}
	
	public final void dummy() throws ParserException, IOException {
		
		
		match(LITERAL_tokens);
	}
	
	public final String  superClass() throws ParserException, IOException {
		String sup;
		
		sup=null;
		
		match(LPAREN);
		if ( guessing==0 ) {
			sup = LT(1).getText();
		}
		{
		switch ( LA(1)) {
		case TOKEN_REF:
		{
			match(TOKEN_REF);
			break;
		}
		case RULE_REF:
		{
			match(RULE_REF);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		match(RPAREN);
		return sup;
	}
	
	public final void rule() throws ParserException, IOException {
		
		Token  d = null;
		Token  p1 = null;
		Token  p2 = null;
		Token  p3 = null;
		Token  aa = null;
		Token  rt = null;
		Token  a = null;
		
			String access="public"; 
			Token idTok;
			String doc=null;
			boolean ruleAutoGen = true;
			blockNesting = -1;	// block increments, so -1 to make rule at level 0
		
		
		{
		switch ( LA(1)) {
		case DOC_COMMENT:
		{
			d = LT(1);
			match(DOC_COMMENT);
			if ( guessing==0 ) {
				doc=d.getText();
			}
			break;
		}
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
		case LITERAL_protected:
		{
			p1 = LT(1);
			match(LITERAL_protected);
			if ( guessing==0 ) {
				access=p1.getText();
			}
			break;
		}
		case LITERAL_public:
		{
			p2 = LT(1);
			match(LITERAL_public);
			if ( guessing==0 ) {
				access=p2.getText();
			}
			break;
		}
		case LITERAL_private:
		{
			p3 = LT(1);
			match(LITERAL_private);
			if ( guessing==0 ) {
				access=p3.getText();
			}
			break;
		}
		case TOKEN_REF:
		case RULE_REF:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		idTok=id();
		{
		switch ( LA(1)) {
		case BANG:
		{
			match(BANG);
			if ( guessing==0 ) {
				ruleAutoGen = false;
			}
			break;
		}
		case ACTION:
		case OPTIONS:
		case ARG_ACTION:
		case LITERAL_returns:
		case COLON:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			
					behavior.defineRuleName(idTok, access, ruleAutoGen, doc);
				
		}
		{
		switch ( LA(1)) {
		case ARG_ACTION:
		{
			aa = LT(1);
			match(ARG_ACTION);
			if ( guessing==0 ) {
				behavior.refArgAction(aa);
			}
			break;
		}
		case ACTION:
		case OPTIONS:
		case LITERAL_returns:
		case COLON:
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
			rt = LT(1);
			match(ARG_ACTION);
			if ( guessing==0 ) {
				behavior.refReturnAction(rt);
			}
			break;
		}
		case ACTION:
		case OPTIONS:
		case COLON:
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
		case OPTIONS:
		{
			ruleOptionsSpec();
			break;
		}
		case ACTION:
		case COLON:
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
			a = LT(1);
			match(ACTION);
			if ( guessing==0 ) {
				behavior.refInitAction(a);
			}
			break;
		}
		case COLON:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		match(COLON);
		block();
		match(SEMI);
		{
		switch ( LA(1)) {
		case LITERAL_exception:
		{
			exceptionGroup();
			break;
		}
		case EOF:
		case ACTION:
		case DOC_COMMENT:
		case LITERAL_lexclass:
		case LITERAL_class:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.endRule(idTok.getText());
		}
	}
	
	public final void ruleOptionsSpec() throws ParserException, IOException {
		
		Token idTok; Token value;
		
		match(OPTIONS);
		{
		_loop74:
		do {
			if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF)) {
				idTok=id();
				match(ASSIGN);
				value=optionValue();
				if ( guessing==0 ) {
					behavior.setRuleOption(idTok, value);
				}
				match(SEMI);
			}
			else {
				break _loop74;
			}
			
		} while (true);
		}
		match(RCURLY);
	}
	
	public final void block() throws ParserException, IOException {
		
		
		if ( guessing==0 ) {
			blockNesting++;
		}
		alternative();
		{
		_loop77:
		do {
			if ((LA(1)==OR)) {
				match(OR);
				alternative();
			}
			else {
				break _loop77;
			}
			
		} while (true);
		}
		if ( guessing==0 ) {
			blockNesting--;
		}
	}
	
	public final void exceptionGroup() throws ParserException, IOException {
		
		
		if ( guessing==0 ) {
			behavior.beginExceptionGroup();
		}
		{
		int _cnt85=0;
		_loop85:
		do {
			if ((LA(1)==LITERAL_exception)) {
				exceptionSpec();
			}
			else {
				if ( _cnt85>=1 ) { break _loop85; } else {throw new NoViableAltException(LT(1));}
			}
			
			_cnt85++;
		} while (true);
		}
		if ( guessing==0 ) {
			behavior.endExceptionGroup();
		}
	}
	
	public final void alternative() throws ParserException, IOException {
		
		boolean altAutoGen = true;
		
		{
		switch ( LA(1)) {
		case BANG:
		{
			match(BANG);
			if ( guessing==0 ) {
				altAutoGen=false;
			}
			break;
		}
		case ACTION:
		case SEMI:
		case STRING_LITERAL:
		case CHAR_LITERAL:
		case OR:
		case TOKEN_REF:
		case LPAREN:
		case RULE_REF:
		case RPAREN:
		case LITERAL_exception:
		case NOT_OP:
		case SEMPRED:
		case TREE_BEGIN:
		case WILDCARD:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.beginAlt(altAutoGen);
		}
		{
		_loop81:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				element();
			}
			else {
				break _loop81;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case LITERAL_exception:
		{
			exceptionSpecNoLabel();
			break;
		}
		case SEMI:
		case OR:
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.endAlt();
		}
	}
	
	public final void element() throws ParserException, IOException {
		
		Token  rr = null;
		Token  aa = null;
		Token  tr = null;
		Token  aa2 = null;
		Token  r2 = null;
		Token  aa3 = null;
		Token  a = null;
		Token  p = null;
		
			Token label = null; 
			Token assignId = null; 
			Token args = null; 
			int autoGen = GrammarElement.AUTO_GEN_NONE;
		
		
		switch ( LA(1)) {
		case ACTION:
		{
			a = LT(1);
			match(ACTION);
			if ( guessing==0 ) {
				behavior.refAction(a);
			}
			break;
		}
		case SEMPRED:
		{
			p = LT(1);
			match(SEMPRED);
			if ( guessing==0 ) {
				behavior.refSemPred(p);
			}
			break;
		}
		case TREE_BEGIN:
		{
			tree();
			break;
		}
		default:
			if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF) && (LA(2)==ASSIGN)) {
				assignId=id();
				match(ASSIGN);
				{
				if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF) && (LA(2)==COLON)) {
					label=id();
					match(COLON);
				}
				else if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF) && (_tokenSet_3.member(LA(2)))) {
				}
				else {
					throw new NoViableAltException(LT(1));
				}
				
				}
				{
				switch ( LA(1)) {
				case RULE_REF:
				{
					rr = LT(1);
					match(RULE_REF);
					{
					switch ( LA(1)) {
					case ARG_ACTION:
					{
						aa = LT(1);
						match(ARG_ACTION);
						if ( guessing==0 ) {
							args=aa;
						}
						break;
					}
					case ACTION:
					case SEMI:
					case STRING_LITERAL:
					case CHAR_LITERAL:
					case OR:
					case TOKEN_REF:
					case LPAREN:
					case RULE_REF:
					case RPAREN:
					case BANG:
					case LITERAL_exception:
					case NOT_OP:
					case SEMPRED:
					case TREE_BEGIN:
					case WILDCARD:
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
					case BANG:
					{
						match(BANG);
						if ( guessing==0 ) {
							autoGen = GrammarElement.AUTO_GEN_BANG;
						}
						break;
					}
					case ACTION:
					case SEMI:
					case STRING_LITERAL:
					case CHAR_LITERAL:
					case OR:
					case TOKEN_REF:
					case LPAREN:
					case RULE_REF:
					case RPAREN:
					case LITERAL_exception:
					case NOT_OP:
					case SEMPRED:
					case TREE_BEGIN:
					case WILDCARD:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1));
					}
					}
					}
					if ( guessing==0 ) {
						behavior.refRule(assignId, rr, label, args, autoGen);
					}
					break;
				}
				case TOKEN_REF:
				{
					tr = LT(1);
					match(TOKEN_REF);
					{
					switch ( LA(1)) {
					case ARG_ACTION:
					{
						aa2 = LT(1);
						match(ARG_ACTION);
						if ( guessing==0 ) {
							args=aa2;
						}
						break;
					}
					case ACTION:
					case SEMI:
					case STRING_LITERAL:
					case CHAR_LITERAL:
					case OR:
					case TOKEN_REF:
					case LPAREN:
					case RULE_REF:
					case RPAREN:
					case LITERAL_exception:
					case NOT_OP:
					case SEMPRED:
					case TREE_BEGIN:
					case WILDCARD:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1));
					}
					}
					}
					if ( guessing==0 ) {
						behavior.refToken(assignId, tr, label, args, false, autoGen, lastInRule());
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1));
				}
				}
				}
			}
			else if ((_tokenSet_4.member(LA(1))) && (_tokenSet_5.member(LA(2)))) {
				{
				if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF) && (LA(2)==COLON)) {
					label=id();
					match(COLON);
				}
				else if ((_tokenSet_4.member(LA(1))) && (_tokenSet_6.member(LA(2)))) {
				}
				else {
					throw new NoViableAltException(LT(1));
				}
				
				}
				{
				switch ( LA(1)) {
				case RULE_REF:
				{
					r2 = LT(1);
					match(RULE_REF);
					{
					switch ( LA(1)) {
					case ARG_ACTION:
					{
						aa3 = LT(1);
						match(ARG_ACTION);
						if ( guessing==0 ) {
							args=aa3;
						}
						break;
					}
					case ACTION:
					case SEMI:
					case STRING_LITERAL:
					case CHAR_LITERAL:
					case OR:
					case TOKEN_REF:
					case LPAREN:
					case RULE_REF:
					case RPAREN:
					case BANG:
					case LITERAL_exception:
					case NOT_OP:
					case SEMPRED:
					case TREE_BEGIN:
					case WILDCARD:
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
					case BANG:
					{
						match(BANG);
						if ( guessing==0 ) {
							autoGen = GrammarElement.AUTO_GEN_BANG;
						}
						break;
					}
					case ACTION:
					case SEMI:
					case STRING_LITERAL:
					case CHAR_LITERAL:
					case OR:
					case TOKEN_REF:
					case LPAREN:
					case RULE_REF:
					case RPAREN:
					case LITERAL_exception:
					case NOT_OP:
					case SEMPRED:
					case TREE_BEGIN:
					case WILDCARD:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1));
					}
					}
					}
					if ( guessing==0 ) {
						behavior.refRule(assignId, r2, label, args, autoGen);
					}
					break;
				}
				case NOT_OP:
				{
					match(NOT_OP);
					{
					switch ( LA(1)) {
					case CHAR_LITERAL:
					case TOKEN_REF:
					{
						notTerminal(label);
						break;
					}
					case LPAREN:
					{
						ebnf(label,true);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1));
					}
					}
					}
					break;
				}
				case LPAREN:
				{
					ebnf(label,false);
					break;
				}
				default:
					if ((LA(1)==STRING_LITERAL||LA(1)==CHAR_LITERAL||LA(1)==TOKEN_REF) && (LA(2)==RANGE)) {
						range(label);
					}
					else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_8.member(LA(2)))) {
						terminal(label);
					}
				else {
					throw new NoViableAltException(LT(1));
				}
				}
				}
			}
		else {
			throw new NoViableAltException(LT(1));
		}
		}
	}
	
	public final void exceptionSpecNoLabel() throws ParserException, IOException {
		
		
		match(LITERAL_exception);
		if ( guessing==0 ) {
			behavior.beginExceptionSpec(null);
		}
		{
		_loop92:
		do {
			if ((LA(1)==LITERAL_catch)) {
				exceptionHandler();
			}
			else {
				break _loop92;
			}
			
		} while (true);
		}
		if ( guessing==0 ) {
			behavior.endExceptionSpec();
		}
	}
	
	public final void exceptionSpec() throws ParserException, IOException {
		
		Token  aa = null;
		Token labelAction = null;
		
		match(LITERAL_exception);
		{
		switch ( LA(1)) {
		case ARG_ACTION:
		{
			aa = LT(1);
			match(ARG_ACTION);
			if ( guessing==0 ) {
				labelAction = aa;
			}
			break;
		}
		case EOF:
		case ACTION:
		case DOC_COMMENT:
		case LITERAL_lexclass:
		case LITERAL_class:
		case TOKEN_REF:
		case RULE_REF:
		case LITERAL_protected:
		case LITERAL_public:
		case LITERAL_private:
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
		if ( guessing==0 ) {
			behavior.beginExceptionSpec(labelAction);
		}
		{
		_loop89:
		do {
			if ((LA(1)==LITERAL_catch)) {
				exceptionHandler();
			}
			else {
				break _loop89;
			}
			
		} while (true);
		}
		if ( guessing==0 ) {
			behavior.endExceptionSpec();
		}
	}
	
	public final void exceptionHandler() throws ParserException, IOException {
		
		Token  a1 = null;
		Token  a2 = null;
		Token exType; Token exName;
		
		match(LITERAL_catch);
		a1 = LT(1);
		match(ARG_ACTION);
		a2 = LT(1);
		match(ACTION);
		if ( guessing==0 ) {
			behavior.refExceptionHandler(a1, a2.getText());
		}
	}
	
	public final void range(
		 Token label 
	) throws ParserException, IOException {
		
		Token  crLeft = null;
		Token  crRight = null;
		Token  t = null;
		Token  u = null;
		Token  v = null;
		Token  w = null;
		
			Token trLeft=null;
			Token trRight=null;
			int autoGen=GrammarElement.AUTO_GEN_NONE;
		
		
		switch ( LA(1)) {
		case CHAR_LITERAL:
		{
			crLeft = LT(1);
			match(CHAR_LITERAL);
			match(RANGE);
			crRight = LT(1);
			match(CHAR_LITERAL);
			{
			switch ( LA(1)) {
			case BANG:
			{
				match(BANG);
				if ( guessing==0 ) {
					autoGen = GrammarElement.AUTO_GEN_BANG;
				}
				break;
			}
			case ACTION:
			case SEMI:
			case STRING_LITERAL:
			case CHAR_LITERAL:
			case OR:
			case TOKEN_REF:
			case LPAREN:
			case RULE_REF:
			case RPAREN:
			case LITERAL_exception:
			case NOT_OP:
			case SEMPRED:
			case TREE_BEGIN:
			case WILDCARD:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			if ( guessing==0 ) {
				behavior.refCharRange(crLeft, crRight, label, autoGen, lastInRule());
			}
			break;
		}
		case STRING_LITERAL:
		case TOKEN_REF:
		{
			{
			switch ( LA(1)) {
			case TOKEN_REF:
			{
				t = LT(1);
				match(TOKEN_REF);
				if ( guessing==0 ) {
					trLeft=t;
				}
				break;
			}
			case STRING_LITERAL:
			{
				u = LT(1);
				match(STRING_LITERAL);
				if ( guessing==0 ) {
					trLeft=u;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			match(RANGE);
			{
			switch ( LA(1)) {
			case TOKEN_REF:
			{
				v = LT(1);
				match(TOKEN_REF);
				if ( guessing==0 ) {
					trRight=v;
				}
				break;
			}
			case STRING_LITERAL:
			{
				w = LT(1);
				match(STRING_LITERAL);
				if ( guessing==0 ) {
					trRight=w;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			autoGen=ast_type_spec();
			if ( guessing==0 ) {
				behavior.refTokenRange(trLeft, trRight, label, autoGen, lastInRule());
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
	}
	
	public final void terminal(
		 Token label 
	) throws ParserException, IOException {
		
		Token  cl = null;
		Token  tr = null;
		Token  aa = null;
		Token  sl = null;
		Token  wi = null;
		
			int autoGen=GrammarElement.AUTO_GEN_NONE;
			Token args=null;
		
		
		switch ( LA(1)) {
		case CHAR_LITERAL:
		{
			cl = LT(1);
			match(CHAR_LITERAL);
			{
			switch ( LA(1)) {
			case BANG:
			{
				match(BANG);
				if ( guessing==0 ) {
					autoGen = GrammarElement.AUTO_GEN_BANG;
				}
				break;
			}
			case ACTION:
			case SEMI:
			case STRING_LITERAL:
			case CHAR_LITERAL:
			case OR:
			case TOKEN_REF:
			case LPAREN:
			case RULE_REF:
			case RPAREN:
			case LITERAL_exception:
			case NOT_OP:
			case SEMPRED:
			case TREE_BEGIN:
			case WILDCARD:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			if ( guessing==0 ) {
				behavior.refCharLiteral(cl, label, false, autoGen, lastInRule());
			}
			break;
		}
		case TOKEN_REF:
		{
			tr = LT(1);
			match(TOKEN_REF);
			autoGen=ast_type_spec();
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				aa = LT(1);
				match(ARG_ACTION);
				if ( guessing==0 ) {
					args=aa;
				}
				break;
			}
			case ACTION:
			case SEMI:
			case STRING_LITERAL:
			case CHAR_LITERAL:
			case OR:
			case TOKEN_REF:
			case LPAREN:
			case RULE_REF:
			case RPAREN:
			case LITERAL_exception:
			case NOT_OP:
			case SEMPRED:
			case TREE_BEGIN:
			case WILDCARD:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			if ( guessing==0 ) {
				behavior.refToken(null, tr, label, args, false, autoGen, lastInRule());
			}
			break;
		}
		case STRING_LITERAL:
		{
			sl = LT(1);
			match(STRING_LITERAL);
			autoGen=ast_type_spec();
			if ( guessing==0 ) {
				behavior.refStringLiteral(sl, label, autoGen, lastInRule());
			}
			break;
		}
		case WILDCARD:
		{
			wi = LT(1);
			match(WILDCARD);
			autoGen=ast_type_spec();
			if ( guessing==0 ) {
				behavior.refWildcard(wi, label, autoGen);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
	}
	
	public final void notTerminal(
		 Token label 
	) throws ParserException, IOException {
		
		Token  cl = null;
		Token  tr = null;
		int autoGen=GrammarElement.AUTO_GEN_NONE;
		
		switch ( LA(1)) {
		case CHAR_LITERAL:
		{
			cl = LT(1);
			match(CHAR_LITERAL);
			{
			switch ( LA(1)) {
			case BANG:
			{
				match(BANG);
				if ( guessing==0 ) {
					autoGen = GrammarElement.AUTO_GEN_BANG;
				}
				break;
			}
			case ACTION:
			case SEMI:
			case STRING_LITERAL:
			case CHAR_LITERAL:
			case OR:
			case TOKEN_REF:
			case LPAREN:
			case RULE_REF:
			case RPAREN:
			case LITERAL_exception:
			case NOT_OP:
			case SEMPRED:
			case TREE_BEGIN:
			case WILDCARD:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			if ( guessing==0 ) {
				behavior.refCharLiteral(cl, label, true, autoGen, lastInRule());
			}
			break;
		}
		case TOKEN_REF:
		{
			tr = LT(1);
			match(TOKEN_REF);
			autoGen=ast_type_spec();
			if ( guessing==0 ) {
				behavior.refToken(null, tr, label, null, true, autoGen, lastInRule());
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
	}
	
	public final void ebnf(
		 Token label, boolean not 
	) throws ParserException, IOException {
		
		Token  lp = null;
		Token  aa = null;
		Token  ab = null;
		
		lp = LT(1);
		match(LPAREN);
		if ( guessing==0 ) {
			behavior.beginSubRule(label, lp.getLine(), not);
		}
		{
		if ((LA(1)==OPTIONS)) {
			subruleOptionsSpec();
			{
			switch ( LA(1)) {
			case ACTION:
			{
				aa = LT(1);
				match(ACTION);
				if ( guessing==0 ) {
					behavior.refInitAction(aa);
				}
				break;
			}
			case COLON:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			match(COLON);
		}
		else if ((LA(1)==ACTION) && (LA(2)==COLON)) {
			ab = LT(1);
			match(ACTION);
			if ( guessing==0 ) {
				behavior.refInitAction(ab);
			}
			match(COLON);
		}
		else if ((_tokenSet_9.member(LA(1))) && (_tokenSet_10.member(LA(2)))) {
		}
		else {
			throw new NoViableAltException(LT(1));
		}
		
		}
		block();
		match(RPAREN);
		{
		switch ( LA(1)) {
		case ACTION:
		case SEMI:
		case STRING_LITERAL:
		case CHAR_LITERAL:
		case OR:
		case TOKEN_REF:
		case LPAREN:
		case RULE_REF:
		case RPAREN:
		case BANG:
		case LITERAL_exception:
		case NOT_OP:
		case SEMPRED:
		case TREE_BEGIN:
		case QUESTION:
		case STAR:
		case PLUS:
		case WILDCARD:
		{
			{
			switch ( LA(1)) {
			case QUESTION:
			{
				match(QUESTION);
				if ( guessing==0 ) {
					behavior.optionalSubRule();
				}
				break;
			}
			case STAR:
			{
				match(STAR);
				if ( guessing==0 ) {
					behavior.zeroOrMoreSubRule();;
				}
				break;
			}
			case PLUS:
			{
				match(PLUS);
				if ( guessing==0 ) {
					behavior.oneOrMoreSubRule();
				}
				break;
			}
			case ACTION:
			case SEMI:
			case STRING_LITERAL:
			case CHAR_LITERAL:
			case OR:
			case TOKEN_REF:
			case LPAREN:
			case RULE_REF:
			case RPAREN:
			case BANG:
			case LITERAL_exception:
			case NOT_OP:
			case SEMPRED:
			case TREE_BEGIN:
			case WILDCARD:
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
			case BANG:
			{
				match(BANG);
				if ( guessing==0 ) {
					behavior.noASTSubRule();
				}
				break;
			}
			case ACTION:
			case SEMI:
			case STRING_LITERAL:
			case CHAR_LITERAL:
			case OR:
			case TOKEN_REF:
			case LPAREN:
			case RULE_REF:
			case RPAREN:
			case LITERAL_exception:
			case NOT_OP:
			case SEMPRED:
			case TREE_BEGIN:
			case WILDCARD:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1));
			}
			}
			}
			break;
		}
		case IMPLIES:
		{
			match(IMPLIES);
			if ( guessing==0 ) {
				behavior.synPred();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		if ( guessing==0 ) {
			behavior.endSubRule();
		}
	}
	
	public final void tree() throws ParserException, IOException {
		
		Token  lp = null;
		
		lp = LT(1);
		match(TREE_BEGIN);
		if ( guessing==0 ) {
			behavior.beginTree(lp.getLine());
		}
		rootNode();
		if ( guessing==0 ) {
			behavior.beginChildList();
		}
		{
		int _cnt107=0;
		_loop107:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				element();
			}
			else {
				if ( _cnt107>=1 ) { break _loop107; } else {throw new NoViableAltException(LT(1));}
			}
			
			_cnt107++;
		} while (true);
		}
		if ( guessing==0 ) {
			behavior.endChildList();
		}
		match(RPAREN);
		if ( guessing==0 ) {
			behavior.endTree();
		}
	}
	
	public final void rootNode() throws ParserException, IOException {
		
		Token label = null;
		
		{
		if ((LA(1)==TOKEN_REF||LA(1)==RULE_REF) && (LA(2)==COLON)) {
			label=id();
			match(COLON);
		}
		else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_11.member(LA(2)))) {
		}
		else {
			throw new NoViableAltException(LT(1));
		}
		
		}
		terminal(label);
	}
	
	public final  int  ast_type_spec() throws ParserException, IOException {
		 int autoGen ;
		
		autoGen = GrammarElement.AUTO_GEN_NONE;
		
		{
		switch ( LA(1)) {
		case CARET:
		{
			match(CARET);
			if ( guessing==0 ) {
				autoGen = GrammarElement.AUTO_GEN_CARET;
			}
			break;
		}
		case BANG:
		{
			match(BANG);
			if ( guessing==0 ) {
				autoGen = GrammarElement.AUTO_GEN_BANG;
			}
			break;
		}
		case ACTION:
		case SEMI:
		case STRING_LITERAL:
		case CHAR_LITERAL:
		case OR:
		case TOKEN_REF:
		case LPAREN:
		case RULE_REF:
		case RPAREN:
		case ARG_ACTION:
		case LITERAL_exception:
		case NOT_OP:
		case SEMPRED:
		case TREE_BEGIN:
		case WILDCARD:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1));
		}
		}
		}
		return autoGen ;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"header\"",
		"ACTION",
		"DOC_COMMENT",
		"\"lexclass\"",
		"\"class\"",
		"\"extends\"",
		"\"Lexer\"",
		"\"TreeParser\"",
		"OPTIONS",
		"ASSIGN",
		"SEMI",
		"RCURLY",
		"\"charVocabulary\"",
		"STRING_LITERAL",
		"CHAR_LITERAL",
		"INT",
		"OR",
		"RANGE",
		"TOKENS",
		"TOKEN_REF",
		"COMMA",
		"\"tokens\"",
		"LPAREN",
		"RULE_REF",
		"RPAREN",
		"\"Parser\"",
		"\"protected\"",
		"\"public\"",
		"\"private\"",
		"BANG",
		"ARG_ACTION",
		"\"returns\"",
		"COLON",
		"\"exception\"",
		"\"catch\"",
		"NOT_OP",
		"SEMPRED",
		"TREE_BEGIN",
		"QUESTION",
		"STAR",
		"PLUS",
		"IMPLIES",
		"CARET",
		"WILDCARD",
		"\"options\"",
		"WS",
		"COMMENT",
		"SL_COMMENT",
		"ML_COMMENT",
		"ESC",
		"DIGIT",
		"XDIGIT",
		"VOCAB",
		"NESTED_ARG_ACTION",
		"NESTED_ACTION",
		"WS_LOOP",
		"INTERNAL_RULE_REF",
		"WS_OPT",
		"NOT_USEFUL"
	};
	
	private static final long _tokenSet_0_data_[] = { 7658799168L, 0L };
	public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
	private static final long _tokenSet_1_data_[] = { 136507822112L, 0L };
	public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
	private static final long _tokenSet_2_data_[] = { 144585989160992L, 0L };
	public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
	private static final long _tokenSet_3_data_[] = { 144749467418656L, 0L };
	public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
	private static final long _tokenSet_4_data_[] = { 141287454277632L, 0L };
	public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
	private static final long _tokenSet_5_data_[] = { 215186933174304L, 0L };
	public static final BitSet _tokenSet_5 = new BitSet(_tokenSet_5_data_);
	private static final long _tokenSet_6_data_[] = { 215118213697568L, 0L };
	public static final BitSet _tokenSet_6 = new BitSet(_tokenSet_6_data_);
	private static final long _tokenSet_7_data_[] = { 140737497137152L, 0L };
	public static final BitSet _tokenSet_7 = new BitSet(_tokenSet_7_data_);
	private static final long _tokenSet_8_data_[] = { 215118211596320L, 0L };
	public static final BitSet _tokenSet_8 = new BitSet(_tokenSet_8_data_);
	private static final long _tokenSet_9_data_[] = { 144732287533088L, 0L };
	public static final BitSet _tokenSet_9 = new BitSet(_tokenSet_9_data_);
	private static final long _tokenSet_10_data_[] = { 281432508756000L, 0L };
	public static final BitSet _tokenSet_10 = new BitSet(_tokenSet_10_data_);
	private static final long _tokenSet_11_data_[] = { 214980503142432L, 0L };
	public static final BitSet _tokenSet_11 = new BitSet(_tokenSet_11_data_);
	
	}
