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
		switch(state) {
		case 0:
			// optional: package statement
			// TODO
			reachedCUstate(1);
			
		case 1:
			// optional: imports
			// TODO
			reachedCUstate(2);
			
		case 2:
			while (true) {
				// optional: class/interface/enum
				try {
					LocatableToken token = tokenStream.nextToken();
					if (token.getType() == JavaTokenTypes.EOF) {
						break;
					}
					if (token.getType() == JavaTokenTypes.LITERAL_public ||
							token.getType() == JavaTokenTypes.LITERAL_private ||
							token.getType() == JavaTokenTypes.LITERAL_protected ||
							token.getType() == JavaTokenTypes.LITERAL_class ||
							token.getType() == JavaTokenTypes.LITERAL_enum ||
							token.getType() == JavaTokenTypes.LITERAL_interface) {
						tokenStream.pushBack(token);
						parseTypeDef();
					}
					else {
						error("Unexpected token"); // TODO improve
					}
				}
				catch (TokenStreamException tse) {
					// TODO
					tse.printStackTrace();
				}
			}
		}
	}
	
	/** reached a compilation unit state */
	protected void reachedCUstate(int i) { }
	
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
				
				// Body!
				token = tokenStream.nextToken();
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

	/**
	 * Parse a modifier list (and return all modifier tokens in a list)
	 */
	public List<LocatableToken> parseModifiers()
	{
		List<LocatableToken> rval = new LinkedList<LocatableToken>();
		try {
			LocatableToken token = tokenStream.nextToken();
			while (token.getType() == JavaTokenTypes.LITERAL_public
					|| token.getType() == JavaTokenTypes.LITERAL_private
					|| token.getType() == JavaTokenTypes.LITERAL_protected
					|| token.getType() == JavaTokenTypes.LITERAL_static
					|| token.getType() == JavaTokenTypes.LITERAL_volatile) {
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
					// Not an inner type: should be a method or field
					tokenStream.pushBack(token);
					parseTypeSpec();
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
						return;
					}
					else if (token.getType() == JavaTokenTypes.LPAREN) {
						// method declaration
						parseParameterList();
						token = tokenStream.nextToken();
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
					else {
						error("Expected ';' or '=' or '(' (in field or method declaration), got token type: " + token.getType());
						tokenStream.pushBack(token);
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
	 * Parse a statement block - such as a method body
	 */
	public void parseStmtBlock()
	{
		try {
			LocatableToken token = tokenStream.nextToken();
			while(true) {
				if (token.getType() == JavaTokenTypes.EOF
						|| token.getType() == JavaTokenTypes.RCURLY
						|| token.getType() == JavaTokenTypes.RPAREN) {
					tokenStream.pushBack(token);
					return;
				}
				
				if (token.getType() == JavaTokenTypes.LITERAL_for) {
					error("XXX"); // TODO
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
				else if (token.getType() == JavaTokenTypes.IDENT) {
					// A label?
					LocatableToken ctoken = tokenStream.nextToken();
					if (ctoken.getType() == JavaTokenTypes.COLON) {
						token = tokenStream.nextToken();
						continue;
					}
					tokenStream.pushBack(ctoken);
					
					// A declaration of a variable?
					List<LocatableToken> identTokens = parseDottedIdent(token);
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
						}
						else {
							token = tokenStream.nextToken();
						}
					}
				}
				else {
					parseExpression();
					token = tokenStream.nextToken();
					if (token.getType() != JavaTokenTypes.SEMI) {
						error("Expected ';' at end of previous statement");
					}
					else {
						token = tokenStream.nextToken();
					}
				}
				token = tokenStream.nextToken();
			}
		} catch (TokenStreamException e) {
			e.printStackTrace(); // TODO
		}
	}
	
	/**
	 * Parse a variable declaration, possibly with an initialiser, always followed by ';'
	 */
	public void parseVariableDeclaration()
	{
		parseModifiers();
		parseTypeSpec();
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
	
	// Parse a type specification. This could be a primitive type (including void),
	// or a class type (qualified or not, possibly with type parameters)
	public void parseTypeSpec()
	{
		try {
			parseModifiers();
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() == JavaTokenTypes.LITERAL_void
					|| token.getType() == JavaTokenTypes.LITERAL_boolean
					|| token.getType() == JavaTokenTypes.LITERAL_byte
					|| token.getType() == JavaTokenTypes.LITERAL_char
					|| token.getType() == JavaTokenTypes.LITERAL_short
					|| token.getType() == JavaTokenTypes.LITERAL_int
					|| token.getType() == JavaTokenTypes.LITERAL_long
					|| token.getType() == JavaTokenTypes.LITERAL_float
					|| token.getType() == JavaTokenTypes.LITERAL_double) {
				return;
			}
			
			if (token.getType() != JavaTokenTypes.IDENT) {
				error("Expected type identifier");
				return;
			}
			
			parseDottedIdent(token);
			
			token = tokenStream.nextToken();
			// TODO must handle '>>' tokens, urgh (and '>>>?')
			if (token.getType() == JavaTokenTypes.LT) {
				// Type parameters
				while (true) {
					token = tokenStream.nextToken();
					// TODO wildcards
					tokenStream.pushBack(token);
					parseTypeSpec();
					token = tokenStream.nextToken();
					if (token.getType() == JavaTokenTypes.GT) {
						return;
					}
					if (token.getType() != JavaTokenTypes.COMMA) {
						error("Expecting ',' in type parameter list");
					}
					else {
						token = tokenStream.nextToken();
					}
				}
			}
			tokenStream.pushBack(token);
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
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
			if (token.getType() == JavaTokenTypes.LITERAL_new) {
				token = tokenStream.nextToken();
				if (token.getType() != JavaTokenTypes.IDENT) {
					error("Expected type identifier after \"new\" (in expression)");
					return;
				}
				tokenStream.pushBack(token);
				parseTypeSpec();
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
			else {
				error("Unexpected token in expression, type=" + token.getType() + ", text=\"" + token.getText() + "\"");
				return;
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
			// TODO
			LocatableToken token = tokenStream.nextToken();
			if (token.getType() != JavaTokenTypes.RPAREN) {
				error("FIXME"); // TODO
			}
			tokenStream.pushBack(token);
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
				parseTypeSpec();
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
