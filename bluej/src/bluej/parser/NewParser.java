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
			if (isTypeDeclarator(token)) {
				String typeDesc;
				if (token.getType() == JavaTokenTypes.LITERAL_class) {
					typeDesc = "class";
				}
				else if (token.getType() == JavaTokenTypes.LITERAL_interface) {
					typeDesc = "interface";
				}
				else {
					typeDesc = "enum";
				}
				
				gotClassTypeDef();
				
				// Class name
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.IDENT) {
					error("Expected identifier (in " + typeDesc + " definition)");
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
				|| tokType == JavaTokenTypes.LITERAL_native
				|| tokType == JavaTokenTypes.STRICTFP);
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
			if (token.getType() == JavaTokenTypes.LITERAL_throws) {
				do {
					parseTypeSpec(false);
					token = tokenStream.nextToken();
				} while (token.getType() == JavaTokenTypes.COMMA);
			}
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
			if (token.getType() == JavaTokenTypes.SEMI) {
				return; // empty statement
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_return) {
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.SEMI) {
					tokenStream.pushBack(token);
					parseExpression();
					token = tokenStream.nextToken();
				}
				if (token.getType() != JavaTokenTypes.SEMI) {
					error("Expecting ';' after 'return' statement");
					tokenStream.pushBack(token);
				}
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_for) {
				parseForStatement(token);
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_while) {
				parseWhileStatement(token);
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_if) {
				parseIfStatement(token);
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_do) {
				parseDoWhileStatement(token);
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_assert) {
				error("XXX"); // TODO
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_switch) {
				parseSwitchStatement(token);
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_case) {
				parseExpression();
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.COLON) {
					error("Expecting ':' at end of case expression");
					tokenStream.pushBack(token);
					return;
				}
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_default) {
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.COLON) {
					error("Expecting ':' at end of case expression");
					tokenStream.pushBack(token);
					return;
				}
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_continue
					|| token.getType() == JavaTokenTypes.LITERAL_break) {
				// There might be a label afterwards
				token = tokenStream.nextToken();
				if (token.getType() == JavaTokenTypes.IDENT) {
					token = tokenStream.nextToken();
				}
				if (token.getType() != JavaTokenTypes.SEMI) {
					error("Expecting ';' at end of " + token.getText() + " statement");
					tokenStream.pushBack(token);
				}
			}
			else if (token.getType() == JavaTokenTypes.LITERAL_throw) {
				parseExpression();
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.SEMI) {
					error("Expecting ';' at end of 'throw' statement");
					tokenStream.pushBack(token);
				}
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
					return;
				}
				tokenStream.pushBack(ctoken);
				tokenStream.pushBack(token);

				// A declaration of a variable?
				List<LocatableToken> tlist = new LinkedList<LocatableToken>();
				boolean isTypeSpec = parseTypeSpec(true, tlist);
				token = tokenStream.nextToken();
				tokenStream.pushBack(token);
				pushBackAll(tlist);
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
			else if (isTypeDeclarator(token)) {
				tokenStream.pushBack(token);
				parseTypeDef();
			}
			else if (isPrimitiveType(token)) {
				tokenStream.pushBack(token);
				List<LocatableToken> tlist = new LinkedList<LocatableToken>();
				parseTypeSpec(false, tlist);
				
				if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
					// int.class, or int[].class are possible
					pushBackAll(tlist);
					parseExpression();
				}
				else {
					pushBackAll(tlist);
					parseVariableDeclaration();
				}
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
	
	public void parseSwitchStatement(LocatableToken token)
	{
		try {
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.LPAREN) {
				error("Expected '(' after 'switch'");
				tokenStream.pushBack(token);
				return;
			}
			parseExpression();
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("Expected ')' at end of expression (in 'switch(...)')");
				tokenStream.pushBack(token);
				return;
			}
			token = tokenStream.nextToken();
			parseStatement(token);
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	public void parseDoWhileStatement(LocatableToken token)
	{
		try {
			token = tokenStream.nextToken();
			parseStatement(token);
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.LITERAL_while) {
				error("Expecting 'while' after statement block (in 'do ... while')");
				tokenStream.pushBack(token);
				return;
			}
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.LPAREN) {
				error("Expecting '(' after 'while'");
				tokenStream.pushBack(token);
				return;
			}
			parseExpression();
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("Expecting ')' after conditional expression (in 'while' statement)");
				tokenStream.pushBack(token);
			}
			token = tokenStream.nextToken();
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	public void parseWhileStatement(LocatableToken token)
	{
		try {
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.LPAREN) {
				error("Expecting '(' after 'while'");
				tokenStream.pushBack(token);
				return;
			}
			parseExpression();
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("Expecting ')' after conditional expression (in 'while' statement)");
				tokenStream.pushBack(token);
			}
			token = tokenStream.nextToken();
			parseStatement(token);
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
				List<LocatableToken> tlist = new LinkedList<LocatableToken>();
				boolean isTypeSpec = parseTypeSpec(true, tlist);
				if (isTypeSpec && tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
					token = tokenStream.nextToken(); // identifier
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
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.SEMI) {
							error("Expecting ';' after initialiser (in for statement)");
							tokenStream.pushBack(token);
							return;
						}
					}
				}
				else {
					// Not a type spec, so, we might have a general statement
					pushBackAll(tlist);
					token = tokenStream.nextToken();
					parseStatement(token);
				}
			}
			else {
				token = tokenStream.nextToken(); // SEMI
			}
			
			// We're expecting a regular (old-style) statement at this point
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
	 * Parse an "if" statement.
	 * @param token  The token corresponding to the "if" literal.
	 */
	public void parseIfStatement(LocatableToken token)
	{
		try {
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.LPAREN) {
				error("Expecting '(' after 'if'");
				tokenStream.pushBack(token);
				return;
			}
			parseExpression();
			token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("Expecting ')' after conditional expression (in 'if' statement)");
				tokenStream.pushBack(token);
			}
			token = tokenStream.nextToken();
			parseStatement(token);
			while (tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_else) {
				tokenStream.nextToken(); // else
				parseStatement(tokenStream.nextToken());
			}
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
		List<LocatableToken> tokens = new LinkedList<LocatableToken>();
		boolean rval = parseTypeSpec(speculative, tokens);
		if (rval == false) {
			pushBackAll(tokens);
		}
		return rval;
	}
	
	/**
	 * TODO fix comment, ttokens parameter
	 * Parse a type specification. This could be a primitive type (including void),
	 * or a class type (qualified or not, possibly with type parameters). This can
	 * do a speculative parse if the following tokens might either be a type specification
	 * or a statement-expression. (In particular, if 'identifier <' is a valid initial
	 * expression sequence, don't use this method).
	 * 
	 * @param speculative  Whether this is a speculative parse, i.e. we might not actually
	 *                     have a type specification. If this is set some parse errors will
	 *                     simply return false.
	 * 
	 * @return true if we saw what might be a type specification (even if it
	 * 		               contains errors), or false if it does not appear to be
	 *                     a type specification. (only meaningful if speculative == true).
	 */
	public boolean parseTypeSpec(boolean speculative, List<LocatableToken> ttokens)
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
						boolean rval = parseTypeSpec(speculative, ttokens);
						if (rval == false) {
							return false;
						}
						token = tokenStream.nextToken();
						ttokens.add(token);
						if (token.getType() == JavaTokenTypes.GT) {
							break;
						}
						if (token.getType() != JavaTokenTypes.COMMA) {
							if (!speculative) {
								error("Expecting ',' in type parameter list");
							}
							return false;
						}
					}
					token = tokenStream.nextToken();
				}
			}
			
			// check for array declarators
			while (token.getType() == JavaTokenTypes.LBRACK
					&& tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
				ttokens.add(token);
				token = tokenStream.nextToken(); // RBRACK
				ttokens.add(token);
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
	 * Check whether a token is an operator. Note that the LPAREN token can be an operator
	 * (method call) or value (parenthesized expression).
	 * 
	 * "new" is not classified as an operator here (an operator operates on a value).
	 */
	public boolean isOperator(LocatableToken token)
	{
		int ttype = token.getType();
		return ttype == JavaTokenTypes.PLUS
			|| ttype == JavaTokenTypes.MINUS
			|| ttype == JavaTokenTypes.STAR
			|| ttype == JavaTokenTypes.DIV
			|| ttype == JavaTokenTypes.LBRACK
			|| ttype == JavaTokenTypes.LPAREN
			|| ttype == JavaTokenTypes.PLUS_ASSIGN
			|| ttype == JavaTokenTypes.STAR_ASSIGN
			|| ttype == JavaTokenTypes.MINUS_ASSIGN
			|| ttype == JavaTokenTypes.DIV_ASSIGN
			|| ttype == JavaTokenTypes.DOT
			|| ttype == JavaTokenTypes.EQUAL
			|| ttype == JavaTokenTypes.NOT_EQUAL
			|| ttype == JavaTokenTypes.ASSIGN
			|| ttype == JavaTokenTypes.BNOT
			|| ttype == JavaTokenTypes.LNOT
			|| ttype == JavaTokenTypes.INC
			|| ttype == JavaTokenTypes.DEC
			|| ttype == JavaTokenTypes.BOR
			|| ttype == JavaTokenTypes.BOR_ASSIGN
			|| ttype == JavaTokenTypes.BAND
			|| ttype == JavaTokenTypes.BAND_ASSIGN
			|| ttype == JavaTokenTypes.BXOR
			|| ttype == JavaTokenTypes.BXOR_ASSIGN
			|| ttype == JavaTokenTypes.SL
			|| ttype == JavaTokenTypes.SL_ASSIGN
			|| ttype == JavaTokenTypes.SR
			|| ttype == JavaTokenTypes.SR_ASSIGN
			|| ttype == JavaTokenTypes.BSR
			|| ttype == JavaTokenTypes.BSR_ASSIGN
			|| ttype == JavaTokenTypes.MOD
			|| ttype == JavaTokenTypes.MOD_ASSIGN
			|| ttype == JavaTokenTypes.LITERAL_instanceof;
	}
	
	/**
	 * Check whether an operator is a binary operator.
	 * 
	 * "instanceof" is not considered to be a binary operator (operates on only one value).
	 */
	public boolean isBinaryOperator(LocatableToken token)
	{
		int ttype = token.getType();
		return ttype == JavaTokenTypes.PLUS
			|| ttype == JavaTokenTypes.MINUS
			|| ttype == JavaTokenTypes.STAR
			|| ttype == JavaTokenTypes.DIV
			|| ttype == JavaTokenTypes.MOD
			|| ttype == JavaTokenTypes.BOR
			|| ttype == JavaTokenTypes.BXOR
			|| ttype == JavaTokenTypes.BAND
			|| ttype == JavaTokenTypes.SL
			|| ttype == JavaTokenTypes.SR
			|| ttype == JavaTokenTypes.BSR
			|| ttype == JavaTokenTypes.BSR_ASSIGN
			|| ttype == JavaTokenTypes.SR_ASSIGN
			|| ttype == JavaTokenTypes.SL_ASSIGN
			|| ttype == JavaTokenTypes.BAND_ASSIGN
			|| ttype == JavaTokenTypes.BXOR_ASSIGN
			|| ttype == JavaTokenTypes.BOR_ASSIGN
			|| ttype == JavaTokenTypes.MOD_ASSIGN
			|| ttype == JavaTokenTypes.DIV_ASSIGN
			|| ttype == JavaTokenTypes.STAR_ASSIGN
			|| ttype == JavaTokenTypes.MINUS_ASSIGN
			|| ttype == JavaTokenTypes.PLUS_ASSIGN
			|| ttype == JavaTokenTypes.ASSIGN
			|| ttype == JavaTokenTypes.DOT
			|| ttype == JavaTokenTypes.EQUAL
			|| ttype == JavaTokenTypes.NOT_EQUAL
			|| ttype == JavaTokenTypes.LT
			|| ttype == JavaTokenTypes.LE
			|| ttype == JavaTokenTypes.GT
			|| ttype == JavaTokenTypes.GE
			|| ttype == JavaTokenTypes.LAND
			|| ttype == JavaTokenTypes.LOR;
	}
	
	public boolean isUnaryOperator(LocatableToken token)
	{
		int ttype = token.getType();
		return ttype == JavaTokenTypes.PLUS
			|| ttype == JavaTokenTypes.MINUS
			|| ttype == JavaTokenTypes.LNOT
			|| ttype == JavaTokenTypes.BNOT
			|| ttype == JavaTokenTypes.INC
			|| ttype == JavaTokenTypes.DEC;
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
					if (token.getType() != JavaTokenTypes.IDENT && !isPrimitiveType(token)) {
						error("Expected type identifier after \"new\" (in expression)");
						return;
					}
					tokenStream.pushBack(token);
					parseTypeSpec(false);
					token = tokenStream.nextToken();
					boolean gotArrayDimension = false;
					while (token.getType() == JavaTokenTypes.LBRACK) {
						// array dimensions
						gotArrayDimension = true;
						if (tokenStream.LA(1).getType() != JavaTokenTypes.RBRACK) {
							parseExpression();
						}
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RBRACK) {
							error("Expecting ']' after array dimension (in new ... expression)");
							tokenStream.pushBack(token);
						}
						token = tokenStream.nextToken();
					}
					if (gotArrayDimension) {
						tokenStream.pushBack(token);
					}
					else {
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
					
					if (tokenStream.LA(1).getType() == JavaTokenTypes.LCURLY) {
						// Either an initialiser list, or a class body (anonymous inner class)
						if (gotArrayDimension) {
							parseExpression();
						}
						else {
							tokenStream.nextToken(); // LCURLY
							parseClassBody();
							token = tokenStream.nextToken();
							if (token.getType() != JavaTokenTypes.RCURLY) {
								error("Expected '}' at end of inner class body");
								tokenStream.pushBack(token);
								return;
							}
						}
					}
				}
				else if (token.getType() == JavaTokenTypes.LCURLY) {
					// an initialiser list for an array
					do {
						if (tokenStream.LA(1).getType() == JavaTokenTypes.RCURLY) {
							token = tokenStream.nextToken(); // RCURLY
							break;
						}
						parseExpression();
						token = tokenStream.nextToken();
					} while (token.getType() == JavaTokenTypes.COMMA);
					if (token.getType() != JavaTokenTypes.RCURLY) {
						error("Expected '}' at end of initialiser list expression");
						tokenStream.pushBack(token);
					}
				}
				else if (token.getType() == JavaTokenTypes.IDENT) {
					// tokenStream.pushBack(token);
					parseDottedIdent(token);
					//parseTypeSpec(false); // call it a type, it might actually be a value
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
				else if (isPrimitiveType(token)) {
					// Not really part of an expression, but may be followed by
					// .class or [].class  (eg int.class, int[][].class)
				}
				else if (isUnaryOperator(token)) {
					// Unary operator
					token = tokenStream.nextToken();
					continue;
				}
				else if (token.getType() == JavaTokenTypes.LPAREN) {
					// Either a parenthesised expression, or a type cast
					List<LocatableToken> tlist = new LinkedList<LocatableToken>();
					boolean isTypeSpec = parseTypeSpec(true, tlist);
					if (isTypeSpec && tokenStream.LA(1).getType() == JavaTokenTypes.RPAREN
							&& (!isOperator(tokenStream.LA(2))
									|| tokenStream.LA(2).getType() == JavaTokenTypes.LPAREN)
									|| isUnaryOperator(tokenStream.LA(2))) {
						// This surely must be type cast
						token = tokenStream.nextToken(); // RPAREN
						token = tokenStream.nextToken();
						continue;
					}
					else {
						pushBackAll(tlist);
						parseExpression();
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RPAREN) {
							error("Unmatched '(' in expression; expecting ')'");
							tokenStream.pushBack(token);
							return;
						}
					}
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
							|| token.getType() == JavaTokenTypes.COMMA
							|| token.getType() == JavaTokenTypes.COLON
							|| token.getType() == JavaTokenTypes.EOF
							|| token.getType() == JavaTokenTypes.RCURLY)
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
						// Arrary subscript?
						if (tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
							// No subscript means that this is a type - must be followed by
							// ".class" normally. Eg Object[].class
							token = tokenStream.nextToken(); // RBRACK
							continue;
						}
						parseExpression();
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.RBRACK) {
							error("Expected ']' after array subscript expression");
							tokenStream.pushBack(token);
						}
					}
					else if (token.getType() == JavaTokenTypes.LITERAL_instanceof) {
						parseTypeSpec(false);
					}
					else if (token.getType() == JavaTokenTypes.DOT) {
						// Handle dot operator specially, as there are some special cases
						token = tokenStream.nextToken();
						if (token.getType() != JavaTokenTypes.LITERAL_class) {
							break;
						}
					}
					else if (isBinaryOperator(token)) {
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
				while (token.getType() == JavaTokenTypes.LBRACK) {
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.RBRACK) {
						error("Expected ']' (after '[') in parameter declaration");
					}
					token = tokenStream.nextToken();
				}
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
