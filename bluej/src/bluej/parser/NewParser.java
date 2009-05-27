package bluej.parser;

import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import antlr.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaTokenTypes;

/**
 * Base class for Java parsers.
 * 
 * @author Davin McCall
 */
public class NewParser
{
	protected JavaTokenFilter tokenStream;
	protected JavaLexer lexer;
	
	public NewParser(Reader r)
	{
		EscapedUnicodeReader euReader = new EscapedUnicodeReader(r);
		lexer = new JavaLexer(euReader);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        euReader.setAttachedScanner(lexer);
		tokenStream = new JavaTokenFilter(lexer);
	}
	
	/**
	 * An error occurred during parsing. Override this method to control error behaviour.
	 * @param msg A message describing the error
	 */
	protected void error(String msg)
	{
		throw new RuntimeException("Parse error: (" + lexer.getLine() + ":" + lexer.getColumn() + ") " + msg);
	}
	
	/**
	 * Parse a compilation unit (from the beginning).
	 */
	public void parseCU()
	{
		parseCU(0);
	}
	
	public void parseCU(int state)
	{
		try {
			switch(state) {
			case 0:
				// optional: package statement
				if (tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_package) {
					tokenStream.nextToken();
					LocatableToken token = tokenStream.nextToken();
					parseDottedIdent(token);
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.SEMI) {
						error("Expecting ';' at end of package declaration");
						tokenStream.pushBack(token);
					}
				}
				reachedCUstate(1);

			case 1:
				// optional: imports
				while (tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_import) {
					tokenStream.nextToken(); // "import"
					LocatableToken token = tokenStream.nextToken();
					parseDottedIdent(token);
					if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
						tokenStream.nextToken();
						token = tokenStream.nextToken();
						if (token.getType() == JavaTokenTypes.SEMI) {
							error("Trailing '.' in import statement");
						}
						else if (token.getType() == JavaTokenTypes.STAR) {
							token = tokenStream.nextToken();
							if (token.getType() != JavaTokenTypes.SEMI) {
								error("Expected ';' following import statement");
								tokenStream.pushBack(token);
							}
						}
						else {
							error("Expected package/class identifier, or '*', in import statement.");
							if (tokenStream.LA(1).getType() == JavaTokenTypes.SEMI) {
								tokenStream.nextToken();
							}
						}
					}
					else {
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.SEMI) {
							error("Expected ';' following import statement");
							tokenStream.pushBack(token);
						}
					}
				}
				reachedCUstate(2);

			case 2:
				while (true) {
					// optional: class/interface/enum
					LocatableToken token = tokenStream.nextToken();
					if (token.getType() == JavaTokenTypes.EOF) {
						break;
					}
					if (isModifier(token) || isTypeDeclarator(token)) {
						tokenStream.pushBack(token);
						parseTypeDef();
					}
					else {
						error("Unexpected token"); // TODO improve
					}
				}
			}
		}
		catch (TokenStreamException tse) {
			// TODO
			tse.printStackTrace();
		}
	}
	
	/** reached a compilation unit state */
	protected void reachedCUstate(int i) { }
	
	/**
	 * Check whether a particular token is a type declaration initiator, i.e "class", "interface"
	 * or "enum"
	 */
	public boolean isTypeDeclarator(LocatableToken token)
	{
		return token.getType() == JavaTokenTypes.LITERAL_class
			|| token.getType() == JavaTokenTypes.LITERAL_enum
			|| token.getType() == JavaTokenTypes.LITERAL_interface;
	}
	
	/**
	 * Check whether a token is a primitive type - "int" "float" etc
	 */
	public boolean isPrimitiveType(LocatableToken token)
	{
		return token.getType() == JavaTokenTypes.LITERAL_void
			|| token.getType() == JavaTokenTypes.LITERAL_boolean
			|| token.getType() == JavaTokenTypes.LITERAL_byte
			|| token.getType() == JavaTokenTypes.LITERAL_char
			|| token.getType() == JavaTokenTypes.LITERAL_short
			|| token.getType() == JavaTokenTypes.LITERAL_int
			|| token.getType() == JavaTokenTypes.LITERAL_long
			|| token.getType() == JavaTokenTypes.LITERAL_float
			|| token.getType() == JavaTokenTypes.LITERAL_double;
	}
	
	/**
	 * Parse a type definition (class, interface, enum).
	 */
	public void parseTypeDef()
	{
		try {
			// possibly, modifiers: [public|private|protected] [static]
			parseModifiers();
			
			// [class|interface|enum]
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() == JavaTokenTypes.LITERAL_class) {
				gotClassTypeDef();
				
				// Class name
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.IDENT) {
					error("Expected identifier (in class definition)");
					tokenStream.pushBack(token);
					return;
				}
				gotTypeDefName(token);
				
				// TODO: template arguments
				
				// extends...
				token = tokenStream.nextToken();
				if (token.getType() == JavaTokenTypes.LITERAL_extends) {
					gotTypeDefExtends(token);
					parseTypeSpec(false);
					token = tokenStream.nextToken();
				}
				
				// implements...
				if (token.getType() == JavaTokenTypes.LITERAL_implements) {
					gotTypeDefImplements(token);
					parseTypeSpec(false);
					token = tokenStream.nextToken();
					while (token.getType() == JavaTokenTypes.COMMA) {
						parseTypeSpec(false);
						token = tokenStream.nextToken();
					}
				}
				
				// Body!
				if (token.getType() != JavaTokenTypes.LCURLY) {
					error("Expected '{' (in class definition)");
					tokenStream.pushBack(token);
					return;
				}
				
				parseClassBody();
				
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.RCURLY) {
					error("Expected '}' (in class definition)");
					return;
				}
			}
			else {
				// TODO: interface, enum
				error("Unexpected token, type = " + token.getType() + ", text=\"" + token.getText() + "\""); // TODO improve
			}
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	/** Called when the type definition is a class definition */
	protected void gotClassTypeDef() { }
	
	/** Called when we have the identifier token for a class/interface/enum definition */
	protected void gotTypeDefName(LocatableToken nameToken) { }
	
	protected void gotTypeDefExtends(LocatableToken extendsToken) { }
	protected void gotTypeDefImplements(LocatableToken implementsToken) { }
	
	/**
	 * Check whether a token represents a modifier.
	 */
	protected boolean isModifier(LocatableToken token)
	{
		int tokType = token.getType();
		return (tokType == JavaTokenTypes.LITERAL_public
				|| tokType == JavaTokenTypes.LITERAL_private
				|| tokType == JavaTokenTypes.LITERAL_protected
				|| tokType == JavaTokenTypes.ABSTRACT
				|| tokType == JavaTokenTypes.FINAL
				|| tokType == JavaTokenTypes.LITERAL_static
				|| tokType == JavaTokenTypes.LITERAL_volatile
				|| tokType == JavaTokenTypes.LITERAL_native);
	}
	
	/**
	 * Parse a modifier list (and return all modifier tokens in a list)
	 */
	public List<LocatableToken> parseModifiers()
	{
		List<LocatableToken> rval = new LinkedList<LocatableToken>();
		try {
			LocatableToken token = tokenStream.nextToken();
			while (isModifier(token)) {
				rval.add(token);
				token = tokenStream.nextToken();
			}
			tokenStream.pushBack(token);
		} catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
		return rval;
	}
	
	public void parseClassBody()
	{
		try {
			LocatableToken token = tokenStream.nextToken();
			while (token.getType() != JavaTokenTypes.RCURLY) {
				tokenStream.pushBack(token);
				// field declaration, method declaration, inner class
				List<LocatableToken> modifiers = parseModifiers();
				token = tokenStream.nextToken();
				if (token.getType() == JavaTokenTypes.LITERAL_class
						|| token.getType() == JavaTokenTypes.LITERAL_interface
						|| token.getType() == JavaTokenTypes.LITERAL_enum) {
					tokenStream.pushBack(token);
					pushBackAll(modifiers);
					parseTypeDef();
				}
				else {
					// Not an inner type: should be a method/constructor or field,
					// or (possibly static) a initialisation block
					if (token.getType() == JavaTokenTypes.LCURLY) {
						// initialisation block
						parseStmtBlock();
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RCURLY) {
							error("Expecting '}' (at end of initialisation block)");
							tokenStream.pushBack(token);
						}
					}
					else if (tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN) {
						// constructor
						tokenStream.nextToken();
						parseMethodParamsBody();
					}
					else {
						// method, field
						tokenStream.pushBack(token);
						parseTypeSpec(false);
						token = tokenStream.nextToken(); // identifier
						if (token.getType() != JavaTokenTypes.IDENT) {
							error("Expected identifier (method or field name), got token type " + token.getType());
							return;
						}
						token = tokenStream.nextToken();
						if (token.getType() == JavaTokenTypes.SEMI) {
							// field declaration: done
							token = tokenStream.nextToken();
							continue;
						}
						else if (token.getType() == JavaTokenTypes.ASSIGN) {
							// field declaration
							parseExpression();
							token = tokenStream.nextToken();
							if (token.getType() != JavaTokenTypes.SEMI) { 
								error("Expected ';' at end of declaration");
							}
							token = tokenStream.nextToken();
							continue;
						}
						else if (token.getType() == JavaTokenTypes.LPAREN) {
							// method declaration
							parseMethodParamsBody();
						}
						else {
							error("Expected ';' or '=' or '(' (in field or method declaration), got token type: " + token.getType());
							tokenStream.pushBack(token);
						}
					}
				}
				token = tokenStream.nextToken();
			}
			tokenStream.pushBack(token);
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace();
			// TODO
		}
	}
	
	/**
	 * We've got the return type, name, and opening parenthesis of a method/constructor
	 * declaration. Parse the rest.
	 */
	public void parseMethodParamsBody()
	{
		try {
			parseParameterList();
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("Expected ')' at end of parameter list (in method declaration)");
				tokenStream.pushBack(token);
				return;
			}
			token = tokenStream.nextToken();
			if (token.getType() == JavaTokenTypes.LCURLY) {
				// method body
				parseStmtBlock();
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.RCURLY) {
					error("Expected '}' at end of method body");
					tokenStream.pushBack(token);
				}
			}
			else if (token.getType() != JavaTokenTypes.SEMI) {
				error("Expected ';' or '{' following parameter list in method declaration");
				tokenStream.pushBack(token);
			}
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	/**
	 * Parse a statement block - such as a method body
	 */
	public void parseStmtBlock()
	{
		try {
			while(true) {
				LocatableToken token = tokenStream.nextToken();
				if (token.getType() == JavaTokenTypes.EOF
						|| token.getType() == JavaTokenTypes.RCURLY
						|| token.getType() == JavaTokenTypes.RPAREN) {
					tokenStream.pushBack(token);
					return;
				}
				parseStatement(token);
			}
		} catch (TokenStreamException e) {
			e.printStackTrace(); // TODO
		}
	}
	
	public void parseStatement(LocatableToken token)
	{
		try {
			if (token.getType() == JavaTokenTypes.LITERAL_for) {
				parseForStatement(token);
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_assert) {
				error("XXX"); // TODO
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_break) {
				error("XXX"); // TODO
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_switch) {
				error("XXX"); // TODO
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_case) {
				error("XXX"); // TODO
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_try) {
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.LCURLY) {
					error ("Expecting '{' after 'try'");
					tokenStream.pushBack(token);
					return;
				}
				parseStatement(token);
				int laType = tokenStream.LA(1).getType();
				while (laType == JavaTokenTypes.LITERAL_catch
						|| laType == JavaTokenTypes.LITERAL_finally) {
					token = tokenStream.nextToken();
					if (laType == JavaTokenTypes.LITERAL_catch) {
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.LPAREN) {
							error("Expecting '(' after 'catch'");
							tokenStream.pushBack(token);
							return;
						}
						parseTypeSpec(false);
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.IDENT) {
							error("Expecting identifier after type (in 'catch' expression)");
							tokenStream.pushBack(token);
							return;
						}
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RPAREN) {
							error("Expecting ')' after identifier (in 'catch' expression)");
							tokenStream.pushBack(token);
							return;
						}
					}
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.LCURLY) {
						error("Expecting '{' after 'catch'/'finally'");
						tokenStream.pushBack(token);
						return;
					}
					parseStatement(token); // parse as a statement block
					laType = tokenStream.LA(1).getType();
				}
			}
			else if (token.getType() == JavaTokenTypes.IDENT) {
				// A label?
				LocatableToken ctoken = tokenStream.nextToken();
				if (ctoken.getType() == JavaTokenTypes.COLON) {
					token = tokenStream.nextToken();
					return;
				}
				tokenStream.pushBack(ctoken);

				// A declaration of a variable?
				List<LocatableToken> identTokens = parseDottedIdent(token);
				// TODO above is wrong; it's a type spec, not a dotted ident
				token = tokenStream.nextToken();
				tokenStream.pushBack(token);
				pushBackAll(identTokens);
				if (token.getType() == JavaTokenTypes.IDENT) {
					parseVariableDeclaration();
				}
				else {
					parseExpression();						
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.SEMI) {
						error("Expected ';' at end of previous statement");
						tokenStream.pushBack(token);
					}
				}
			}
			else if (isModifier(token)) {
				// Variable declaration or inner class
				parseModifiers();
				if (isTypeDeclarator(tokenStream.LA(1))) {
					parseTypeDef();
				}
				else {
					parseVariableDeclaration();
				}
			}
			else if (isPrimitiveType(token)) {
				tokenStream.pushBack(token);
				parseVariableDeclaration();
			}
			else if (token.getType() == JavaTokenTypes.LCURLY) {
				parseStmtBlock();
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.RCURLY) {
					error("Expecting '}' at end of statement block");
					if (token.getType() != JavaTokenTypes.RPAREN) {
						tokenStream.pushBack(token);
					}
				}
			}
			else {
				tokenStream.pushBack(token);
				parseExpression();
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.SEMI) {
					error("Expected ';' at end of previous statement");
					tokenStream.pushBack(token);
				}
			}
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	public void parseForStatement(LocatableToken forToken)
	{
		// TODO: if we get an unexpected token in the part between '(' and ')' check
		// if it is ')'. If so we might still expect a loop body to follow.
		try {
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.LPAREN) {
				error("Expecting '(' after 'for'");
				tokenStream.pushBack(token);
				return;
			}
			if (tokenStream.LA(1).getType() != JavaTokenTypes.SEMI) {
				// Could be an old or new style for-loop.
				// for ( System.out.println("yu"); ;);
				boolean isTypeSpec = parseTypeSpec(true);
				if (isTypeSpec) {
					token = tokenStream.nextToken(); // identifier
					if (token.getType() != JavaTokenTypes.IDENT) {
						error("Expecting identifier after type (in loop initialiser)");
						if (tokenStream.LA(1).getType() != JavaTokenTypes.ASSIGN
								&& tokenStream.LA(1).getType() != JavaTokenTypes.COLON) {
							tokenStream.pushBack(token);
							if (token.getType() != JavaTokenTypes.ASSIGN
									&& token.getType() != JavaTokenTypes.COLON) {
								return;
							}
						}
					}
					
					token = tokenStream.nextToken();
					if (token.getType() == JavaTokenTypes.COLON) {
						// This is a "new" for loop (Java 5)
						parseExpression();
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RPAREN) {
							error("Expecting ')' (in for statement)");
							tokenStream.pushBack(token);
							return;
						}
						token = tokenStream.nextToken();
						parseStatement(token); // loop body
						return;
					}
					else {
						// Old style loop with initialiser
						if (token.getType() != JavaTokenTypes.ASSIGN) {
							error("Expecting '=' to complete initializer (in for loop)");
							tokenStream.pushBack(token);
							return;
						}
						parseExpression();
					}
				}
				else {
					// Not a type spec, so, we might have a general statement
					parseStatement(token);
				}
			}
			
			// We're expecting a regular (old-style) statement at this point
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.SEMI) {
				error("Expecting ';' after initialiser (in for statement)");
				tokenStream.pushBack(token);
				return;
			}
			if (tokenStream.LA(1).getType() != JavaTokenTypes.SEMI) {
				// test expression
				parseExpression();
			}
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.SEMI) {
				error("Expecting ';' after test expression (in for statement)");
				tokenStream.pushBack(token);
				return;
			}
			if (tokenStream.LA(1).getType() != JavaTokenTypes.RPAREN) {
				// loop increment expression
				parseExpression();
			}
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("Expecting ')' at end of 'for(...'");
				tokenStream.pushBack(token);
				return;
			}
			token = tokenStream.nextToken();
			if (token.getType() == JavaTokenTypes.RCURLY
					|| token.getType() == JavaTokenTypes.EOF) {
				error("Expecting statement after 'for(...)'");
				return;
			}
			parseStatement(token);
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	/**
	 * Parse a variable declaration, possibly with an initialiser, always followed by ';'
	 */
	public void parseVariableDeclaration()
	{
		parseModifiers();
		parseTypeSpec(false);
		try {
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.IDENT) {
				error("Expecting identifier (in variable/field declaration)");
				tokenStream.pushBack(token);
				return;
			}
			token = tokenStream.nextToken();
			if (token.getType() == JavaTokenTypes.ASSIGN) {
				parseExpression();
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.SEMI) {
					error("Expecting ';' at end of variable declaration");
					tokenStream.pushBack(token);
				}
			}
			else if (token.getType() != JavaTokenTypes.SEMI) {
				error("Expecting ';' or '=' (in variable declaration)");
				tokenStream.pushBack(token);
			}
		} catch (TokenStreamException e) {
			e.printStackTrace(); // TODO
		}
	}
	
	// TODO comment
	public boolean parseTypeSpec(boolean speculative)
	{
		return parseTypeSpec(speculative, true);
	}
	
	// TODO comment
	public boolean parseTypeSpec(boolean speculative, boolean idMayFollow)
	{
		List<LocatableToken> tokens = new LinkedList<LocatableToken>();
		boolean rval = parseTypeSpec(speculative, idMayFollow, tokens);
		if (rval == false) {
			pushBackAll(tokens);
		}
		return rval;
	}
	
	
	/**
	 * Parse a type specification. This could be a primitive type (including void),
	 * or a class type (qualified or not, possibly with type parameters). This can
	 * do a speculative parse if the following tokens might either be a type specification
	 * or a statement-expression. (In particular, if 'identifier <' is a valid initial
	 * expression sequence, don't use this method).
	 * 
	 * @param speculative  Whether this is a speculative parse, i.e. we might not actually
	 *                     have a type specification. If this is set some parse errors will
	 *                     simply return false.
	 * @return true if we saw what appears to be a type specification (even if it
	 * 		               contains errors), or false if it does not appear to be
	 *                     a type specification. (only meaningful if speculative == true).
	 */
	public boolean parseTypeSpec(boolean speculative, boolean idMayFollow, List<LocatableToken> ttokens)
	{
		try {
			ttokens.addAll(parseModifiers());
			LocatableToken token = tokenStream.nextToken();
			if (isPrimitiveType(token)) {
				// Ok, we have a base type
				speculative = false;
				ttokens.add(token);
				token = tokenStream.nextToken();
			}
			else {
				if (token.getType() != JavaTokenTypes.IDENT) {
					if (! speculative) {
						error("Expected type identifier");
					}
					tokenStream.pushBack(token);
					return false;
				}

				ttokens.addAll(parseDottedIdent(token));

				token = tokenStream.nextToken();
				// TODO must handle '>>' tokens, urgh (and '>>>?')
				if (token.getType() == JavaTokenTypes.LT) {
					// Type parameters? (or is it a "less than" comparison?)
					ttokens.add(token);
					while (true) {
						//token = tokenStream.LA(1);
						// TODO wildcards
						boolean rval = parseTypeSpec(speculative, false, ttokens);
						if (rval == false) {
							return false;
						}
						token = tokenStream.nextToken();
						if (token.getType() == JavaTokenTypes.GT) {
							if (speculative && !idMayFollow) {
								if (tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
									return false;
								}
							}
							break;
						}
						if (token.getType() != JavaTokenTypes.COMMA) {
							if (!speculative) {
								error("Expecting ',' in type parameter list");
							}
							tokenStream.pushBack(token);
							return false;
						}
					}
					token = tokenStream.nextToken();
				}
			}
			
			// check for array declarators
			while (token.getType() == JavaTokenTypes.LBRACK) {
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.RBRACK) {
					if (! speculative) {
						error("Expected ']' after '[' (in array type specification)");
						break;
					}
					else {
						tokenStream.pushBack(token);
						pushBackAll(ttokens);
						return false;
					}
				}
				token = tokenStream.nextToken();
			}
			
			tokenStream.pushBack(token);
			return true;

		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
			return false;
		}
	}
	
	/**
	 * Parse a dotted identifier. This could be a variable, method or type name.
	 * @param first The first token in the dotted identifier (should be an IDENT)
	 * @return A list of tokens making up the dotted identifier
	 */
	public List<LocatableToken> parseDottedIdent(LocatableToken first)
	{
		List<LocatableToken> rval = new LinkedList<LocatableToken>();
		rval.add(first);
		try {
			LocatableToken token = tokenStream.nextToken();
			while (token.getType() == JavaTokenTypes.DOT) {
				LocatableToken ntoken = tokenStream.nextToken();
				if (ntoken.getType() != JavaTokenTypes.IDENT) {
					// This could be for example "xyz.class"
					tokenStream.pushBack(ntoken);
					break;
				}
				rval.add(token);
				rval.add(ntoken);
				token = tokenStream.nextToken();
			}
			tokenStream.pushBack(token);
		} catch (TokenStreamException e) {
			e.printStackTrace(); // TODO
		}
		return rval;
	}
	
	/**
	 * Parse an expression
	 */
	public void parseExpression()
	{
		try {
			LocatableToken token = tokenStream.nextToken();
			
			while (true) {
				if (token.getType() == JavaTokenTypes.LITERAL_new) {
					// new XYZ(...)
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.IDENT) {
						error("Expected type identifier after \"new\" (in expression)");
						return;
					}
					tokenStream.pushBack(token);
					parseTypeSpec(false);
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.LPAREN) {
						error("Expected '(' after type name (in 'new ...' expression)");
						return;
					}
					parseArgumentList();
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.RPAREN) {
						error("Expected ')' at end of argument list (in 'new ...' expression)");
						return;
					}
				}
				else if (token.getType() == JavaTokenTypes.IDENT) {
					parseDottedIdent(token);
				}
				else if (token.getType() == JavaTokenTypes.STRING_LITERAL
						|| token.getType() == JavaTokenTypes.CHAR_LITERAL
						|| token.getType() == JavaTokenTypes.NUM_INT
						|| token.getType() == JavaTokenTypes.NUM_LONG
						|| token.getType() == JavaTokenTypes.NUM_DOUBLE
						|| token.getType() == JavaTokenTypes.NUM_FLOAT
						|| token.getType() == JavaTokenTypes.LITERAL_null
						|| token.getType() == JavaTokenTypes.LITERAL_this
						|| token.getType() == JavaTokenTypes.LITERAL_super
						|| token.getType() == JavaTokenTypes.LITERAL_true
						|| token.getType() == JavaTokenTypes.LITERAL_false) {
					// Literals need no further processing
				}
				else if (token.getType() == JavaTokenTypes.MINUS
						|| token.getType() == JavaTokenTypes.PLUS) {
					// Unary operator
					token = tokenStream.nextToken();
					continue;
				}
				else if (token.getType() == JavaTokenTypes.LPAREN) {
					// Either a parenthesized expression, or a type cast
					boolean isTypeSpec = parseTypeSpec(true, false);
					if (isTypeSpec) {
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RPAREN) {
							error("Expecting ')' after type specification (in type cast expression)");
							tokenStream.pushBack(token);
						}
					}
					else {
						parseExpression();
						token = tokenStream.nextToken();
						if (token.getType() != ')') {
							error("Unmatched '(' in expression; expecting ')'");
							tokenStream.pushBack(token);
							return;
						}
					}
					token = tokenStream.nextToken();
					continue;
				}
				else {
					// TODO
					error("Unexpected token in expression, type=" + token.getType() + ", text=\"" + token.getText() + "\"");
					return;
				}
				
				// Now we get an operator, or end of expression
				while (true) {
					token = tokenStream.nextToken();
					if (token.getType() == JavaTokenTypes.RPAREN
							|| token.getType() == JavaTokenTypes.SEMI
							|| token.getType() == JavaTokenTypes.RBRACK
							|| token.getType() == JavaTokenTypes.COMMA)
					{
						// These are all legitimate expression endings
						tokenStream.pushBack(token);
						return;
					}
					else if (token.getType() == JavaTokenTypes.LPAREN) {
						// Method call
						int nextType = tokenStream.LA(1).getType();
						if (nextType == JavaTokenTypes.RPAREN) {
							tokenStream.nextToken();
						}
						else {
							parseExpression();
							token = tokenStream.nextToken();
							while (token.getType() == JavaTokenTypes.COMMA) {
								parseExpression();
								token = tokenStream.nextToken();
							}
							if (token.getType() != JavaTokenTypes.RPAREN) {
								error("Expected ')' to terminate method call parameter list");
							}
						}
					}
					else if (token.getType() == JavaTokenTypes.LBRACK) {
						// Arrary subscript
						parseExpression();
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RBRACK) {
							error("Expected ']' after array subscript expression");
							tokenStream.pushBack(token);
						}
					}
					else if (token.getType() == JavaTokenTypes.PLUS
							|| token.getType() == JavaTokenTypes.MINUS
							|| token.getType() == JavaTokenTypes.STAR
							|| token.getType() == JavaTokenTypes.DIV
							|| token.getType() == JavaTokenTypes.ASSIGN
							|| token.getType() == JavaTokenTypes.LT
							|| token.getType() == JavaTokenTypes.GT
							|| token.getType() == JavaTokenTypes.DOT
							|| token.getType() == JavaTokenTypes.PLUS_ASSIGN
							|| token.getType() == JavaTokenTypes.MINUS_ASSIGN
							|| token.getType() == JavaTokenTypes.STAR_ASSIGN
							|| token.getType() == JavaTokenTypes.DIV_ASSIGN) {
						// Binary operators - need another operand
						token = tokenStream.nextToken();
						break;
					}
					else if (token.getType() == JavaTokenTypes.INC
							|| token.getType() == JavaTokenTypes.DEC) {
						// post operators (unary)
						continue;
					}
					else {
						// TODO
						error("Unexpected token in expression, type=" + token.getType() + ", text=\"" + token.getText() + "\"");
						return;
					}
				}
			}
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	/**
	 * Parse a comma-separated, possibly empty list of arguments to a method/constructor
	 */
	public void parseArgumentList()
	{
		try {
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				tokenStream.pushBack(token);
				do  {
					parseExpression();
					token = tokenStream.nextToken();
				} while (token.getType() == JavaTokenTypes.COMMA);
				if (token.getType() != JavaTokenTypes.RPAREN) {
					error("Expecting ',' or ')' (in argument list)");
				}
			}
			tokenStream.pushBack(token); // push back the ')' or erroneous token
			return;
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	/**
	 * Parse a list of formal parameters (possibly empty)
	 */
	public void parseParameterList()
	{
		try {
			LocatableToken token = tokenStream.nextToken();
			while (token.getType() != JavaTokenTypes.RPAREN
					&& token.getType() != JavaTokenTypes.RCURLY) {
				tokenStream.pushBack(token);
				parseTypeSpec(false);
				token = tokenStream.nextToken(); // identifier
				if (token.getType() != JavaTokenTypes.IDENT) {
					error("Expected parameter identifier (in method parameter)");
					// skip to next ',', ')' or '}' TODO
					return;
				}
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.COMMA) {
					break;
				}
				token = tokenStream.nextToken();
			}
			tokenStream.pushBack(token);
		} catch (TokenStreamException e) {
			e.printStackTrace(); // TODO
		}
	}
	
	private void pushBackAll(List<LocatableToken> tokens)
	{
		ListIterator<LocatableToken> i = tokens.listIterator(tokens.size());
		while (i.hasPrevious()) {
			tokenStream.pushBack(i.previous());
		}
	}
}
