/*
 This file is part of the BlueJ program. 
 Copyright (C) 2025  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.util.ArrayList;
import java.util.List;

import bluej.parser.lexer.*;

/**
 * Base class for Kotlin parsers.
 * 
 * <p>We parse the source, and when we see certain constructs we call a corresponding method
 * from our JavaParserCallbacks parent class, which subclasses can override (for instance,
 * beginForLoop, beginForLoopBody, endForLoop).
 * 
 * Almost all of the methods defined by this class are final, to avoid accidentally overriding them
 * in subclasses and accidentally changing the parser behaviour.
 * 
 * In general it is arranged so that a call to beginXYZ() is always followed by a call to
 * endXYZ(). 
 */
public class KotlinParser implements ParserBehavior
{
    private SourceParser parser;

    public static final int TYPEDEF_CLASS = 0;
    public static final int TYPEDEF_INTERFACE = 1;
    public static final int TYPEDEF_ENUM = 2;
    public static final int TYPEDEF_ANNOTATION = 3;
    /** looks like a type definition, but has an error */
    public static final int TYPEDEF_ERROR = 4;
    /** doesn't parse as a type definition at all */
    public static final int TYPEDEF_EPIC_FAIL = 5;

    public KotlinParser(SourceParser parser)
    {
        this.parser = parser;
    }

    public final JavaTokenFilter getTokenStream()
    {
        return parser.getTokenStream();
    }

    /**
     * Get the last token seen during the previous parse.
     * Many parser methods return after having read a complete structure (such as a class definition). This
     * method allows the retrieval of the last token which was actually part of the structure.
     */
    public final LocatableToken getLastToken()
    {
        return parser.getLastToken();
    }

    /**
     * Signal a parse error, occurring because the next token in the token stream is
     * not valid in the current context.
     * 
     * @param msg A message/code describing the error
     */
    private void error(String msg)
    {
        errorBehind(msg, getLastToken());
    }

    /**
     * Signal a parser error, occurring because the given token in the token stream is
     * not valid in the current context, but for which a useful error diagnosis can be
     * provided. The entire token will be highlighted as erroneous.
     * 
     * @param msg    A message/code describing the error
     * @paran token  The invalid token
     */
    private void error(String msg, LocatableToken token)
    {
        parser.error(msg, token.getLine(), token.getColumn(), token.getEndLine(), token.getEndColumn());
    }

    private void errorBefore(String msg, LocatableToken token)
    {
        parser.error(msg, token.getLine(), token.getColumn(), token.getLine(), token.getColumn());
    }

    private void errorBehind(String msg, LocatableToken token)
    {
        parser.error(msg, token.getEndLine(), token.getEndColumn(), token.getEndLine(), token.getEndColumn());
    }

    /**
     * Called when a comment token is encountered.
     */
    public void gotComment(LocatableToken token)
    {
        // Default implementation does nothing
    }

    /**
     * Parse a compilation unit (from the beginning).
     */
    public void parseCU()
    {
        int state = 0;
        while (getTokenStream().LA(1).getType() != JavaTokenTypes.EOF) {
            LocatableToken nextToken = getTokenStream().LA(1);

            if (nextToken.getType() == JavaTokenTypes.SEMI) {
                nextToken();
                continue;
            }
            state = parseCUpart(state);
        }
        parser.finishedCU(state);
    }

    /**
     * Get the next token from the token stream.
     */
    protected final LocatableToken nextToken()
    {
        return parser.setLastToken(getTokenStream().nextToken());
    }

    /**
     * Check whether a particular token is a type declaration initiator, i.e "class", "interface"
     * or "enum"
     */
    public final boolean isTypeDeclarator(LocatableToken token)
    {
        return token.getType() == JavaTokenTypes.LITERAL_class
        || token.getType() == JavaTokenTypes.LITERAL_enum
        || token.getType() == JavaTokenTypes.LITERAL_interface;
    }

    /**
     * Check whether a token is a primitive type - "int" "float" etc
     */
    public static boolean isPrimitiveType(LocatableToken token)
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
     * Parse a part of a compilation unit, starting from the given state.
     * 
     * @param state The state to start parsing from
     * @return The new state after parsing
     */
    public final int parseCUpart(int state)
    {
        LocatableToken token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_package) {
            if (state != 0) {
                error("Only one 'package' statement is allowed", token);
            }
            token = parsePackageStmt(token);
            parser.reachedCUstate(1); state = 1;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_import) {
            parseImportStatement(token);
            parser.reachedCUstate(1); state = 1;
        }
        else if (isModifier(token) || isTypeDeclarator(token)) {
            // optional: class/interface/enum
            parser.gotTopLevelDecl(token);
            parser.gotDeclBegin(token);
            getTokenStream().pushBack(token);
            parseModifiers();
            parseTypeDef(token);
            parser.reachedCUstate(2); state = 2;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_fun) {
            parser.gotTopLevelDecl(token);
            parser.gotDeclBegin(token);
            processFunction(token);
            parser.reachedCUstate(2); state = 2;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_val ||
                 token.getType() == JavaTokenTypes.LITERAL_var) {
            parser.gotTopLevelDecl(token);
            parser.gotDeclBegin(token);
            processProperty(token);
            parser.reachedCUstate(2); state = 2;
        }
        else if (token.getType() == JavaTokenTypes.EOF) {
            return state;
        }
        else {
            // TODO give different diagnostic depending on state
            error("Expected: Type definition (class, interface or enum)", token);
        }
        return state;
    }

    /**
     * Check whether a token represents a modifier (or an "at" symbol,
     * denoting an annotation).
     */
    public static boolean isModifier(LocatableToken token)
    {
        int tokType = token.getType();
        return (tokType == JavaTokenTypes.LITERAL_public
                || tokType == JavaTokenTypes.LITERAL_private
                || tokType == JavaTokenTypes.LITERAL_protected
                || tokType == JavaTokenTypes.LITERAL_internal
                || tokType == JavaTokenTypes.ABSTRACT
                || tokType == JavaTokenTypes.FINAL
                || tokType == JavaTokenTypes.LITERAL_static
                || tokType == JavaTokenTypes.LITERAL_volatile
                || tokType == JavaTokenTypes.LITERAL_native
                || tokType == JavaTokenTypes.STRICTFP
                || tokType == JavaTokenTypes.LITERAL_transient
                || tokType == JavaTokenTypes.LITERAL_synchronized
                || tokType == JavaTokenTypes.AT
                || tokType == JavaTokenTypes.LITERAL_default
                || tokType == JavaTokenTypes.LITERAL_sealed
                || tokType == JavaTokenTypes.LITERAL_non_sealed
                || tokType == JavaTokenTypes.LITERAL_open
                || tokType == JavaTokenTypes.LITERAL_data
                || tokType == JavaTokenTypes.LITERAL_actual
                || tokType == JavaTokenTypes.LITERAL_expect
                || tokType == JavaTokenTypes.LITERAL_const
                || tokType == JavaTokenTypes.LITERAL_lateinit
                || tokType == JavaTokenTypes.LITERAL_override
                || tokType == JavaTokenTypes.LITERAL_suspend
                || tokType == JavaTokenTypes.LITERAL_tailrec
                || tokType == JavaTokenTypes.LITERAL_vararg
                || tokType == JavaTokenTypes.LITERAL_infix
                || tokType == JavaTokenTypes.LITERAL_inline
                || tokType == JavaTokenTypes.LITERAL_external
                || tokType == JavaTokenTypes.LITERAL_operator
                || tokType == JavaTokenTypes.LITERAL_inner);
    }

    /**
     * Parse a modifier list (and return all modifier tokens in a list)
     */
    public final List<LocatableToken> parseModifiers()
    {
        List<LocatableToken> rval = new ArrayList<>();

        LocatableToken token = getTokenStream().nextToken();
        while (isModifier(token)) {
            if (token.getType() == JavaTokenTypes.AT) {
                if (getTokenStream().LA(1).getType() == JavaTokenTypes.IDENT) {
                    parser.setLastToken(token);
                    parseAnnotation();
                }
                else {
                    getTokenStream().pushBack(token);
                    return rval;
                }
            }
            else {
                parser.gotModifier(token);
            }
            parser.setLastToken(token);
            rval.add(token);
            token = nextToken();
        }                       
        getTokenStream().pushBack(token);

        return rval;
    }

    /**
     * Parse an annotation.
     */
    private void parseAnnotation()
    {
        // TODO: Implement annotation parsing for Kotlin
        // For now, just skip the annotation
        LocatableToken token = nextToken();
        while (token.getType() != JavaTokenTypes.SEMI && 
               token.getType() != JavaTokenTypes.LCURLY &&
               token.getType() != JavaTokenTypes.RCURLY &&
               token.getType() != JavaTokenTypes.EOF) {
            token = nextToken();
        }
        getTokenStream().pushBack(token);
    }

    @Override
    public LocatableToken parsePackageStmt(LocatableToken token) {
        parser.beginPackageStatement(token);
        processPackage();
        return getLastToken();
    }

    /**
     * Parse a dotted identifier. This could be a variable, method or type name.
     * @param first The first token in the dotted identifier (should be an IDENT)
     * @return A list of tokens making up the dotted identifier
     */
    public final List<LocatableToken> parseDottedIdent(LocatableToken first)
    {
        List<LocatableToken> rval = new ArrayList<>();
        rval.add(first);
        LocatableToken token = nextToken();
        while (token.getType() == JavaTokenTypes.DOT) {
            LocatableToken ntoken = nextToken();
            if (ntoken.getType() != JavaTokenTypes.IDENT) {
                // This could be for example "xyz.class"
                getTokenStream().pushBack(ntoken);
                break;
            }
            rval.add(token);
            rval.add(ntoken);
            token = nextToken();
        }
        getTokenStream().pushBack(token);
        return rval;
    }

    /**
     * Parse an import statement.
     */
    public final void parseImportStatement()
    {
        LocatableToken token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_import) {
            parseImportStatement(token);
        }
        else {
            error("Import statements must start with \"import\".");
        }
    }

    /**
     * Parse an import statement starting with the given token.
     * 
     * @param importToken The "import" token
     */
    public final void parseImportStatement(final LocatableToken importToken)
    {
        LocatableToken token = importToken;
        parser.beginElement(token);
        boolean isStatic = false;
        token = getTokenStream().nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_static) {
            isStatic = true;
            token = getTokenStream().nextToken();
        }
        if (token.getType() != JavaTokenTypes.IDENT) {
            getTokenStream().pushBack(token);
            error("Expecting identifier (package containing element to be imported)");
            parser.endElement(token, false);
            return;
        }

        List<LocatableToken> tokens = parseDottedIdent(token);
        LocatableToken lastIdentToken = parser.getLastToken();
        if (getTokenStream().LA(1).getType() == JavaTokenTypes.DOT) {
            LocatableToken lastToken = nextToken(); // DOT
            token = nextToken();
            if (token.getType() == JavaTokenTypes.SEMI) {
                parser.error("Trailing '.' in import statement", lastToken.getLine(), lastToken.getColumn(),
                        lastToken.getEndLine(), lastToken.getEndColumn());
            }
            else if (token.getType() == JavaTokenTypes.STAR) {
                lastToken = token;
                token = nextToken();
                if (token.getType() != JavaTokenTypes.SEMI) {
                    getTokenStream().pushBack(token);
                    parser.error("Expected ';' following import statement", lastToken.getEndLine(), lastToken.getEndColumn(),
                            lastToken.getEndLine(), lastToken.getEndColumn());
                }
                else {
                    parser.gotWildcardImport(tokens, isStatic, importToken, token);
                    parser.gotImportStmtSemi(token);
                }
            }
            else {
                error("Expected package/class identifier, or '*', in import statement.");
                if (getTokenStream().LA(1).getType() == JavaTokenTypes.SEMI) {
                    nextToken();
                }
            }
        }
        else {
            token = nextToken();
            if (token.getType() != JavaTokenTypes.SEMI) {
                getTokenStream().pushBack(token);
                parser.error("Expected ';' following import statement", lastIdentToken.getEndLine(), lastIdentToken.getEndColumn(),
                        lastIdentToken.getEndLine(), lastIdentToken.getEndColumn());
            }
            else {
                parser.gotImport(tokens, isStatic, importToken, token);
                parser.gotImportStmtSemi(token);
            }
        }
    }

    /**
     * Parse a type definition (class, interface, enum).
     * Returns with {@code lastToken} set to the last token seen as part of the definition.
     */
    public final void parseTypeDef()
    {
        parseModifiers();
        parseTypeDef(getTokenStream().LA(1));
    }

    /**
     * Parse a type definition (class, interface, enum).
     * Returns with {@code lastToken} set to the last token seen as part of the definition.
     * 
     * @param firstToken  the first token of the type definition, which might still be in the token
     *                    stream, or which might be a modifier already read.
     */
    public final void parseTypeDef(LocatableToken firstToken)
    {
        int tdType = parseTypeDefBegin();
        if (tdType != TYPEDEF_EPIC_FAIL) {
            parser.gotTypeDef(firstToken, tdType);
        }
        parser.modifiersConsumed();
        if (tdType == TYPEDEF_EPIC_FAIL) {
            parser.endDecl(getTokenStream().LA(1));
            return;
        }

        // Class name
        LocatableToken token = getTokenStream().nextToken();
        if (token.getType() != JavaTokenTypes.IDENT) {
            getTokenStream().pushBack(token);
            parser.gotTypeDefEnd(token, false);
            error("Expected identifier (in type definition)");
            return;
        }
        parser.gotTypeDefName(token);

        token = parseTypeDefPart2(false);

        if (token == null) {
            // If parseTypeDefPart2 returns null, it means we've encountered the start of a new declaration
            // Consider the class definition complete
            parser.gotTypeDefEnd(getLastToken(), true);
            return;
        }

        if (token.getType() != JavaTokenTypes.LCURLY) {
            // In Kotlin, classes can be defined without curly braces
            // If we don't find a curly brace, consider the class definition complete
            getTokenStream().pushBack(token);
            parser.gotTypeDefEnd(token, false);
//            error("Expected '{' (in type definition)");
            return;
        }

        token = parseTypeBody(tdType, token);
        parser.gotTypeDefEnd(token, token.getType() == JavaTokenTypes.RCURLY);
    }

    /**
     * Parse a type body.
     * 
     * @param tdType The type definition type
     * @param token The token that starts the type body
     * @return The last token seen during parsing
     */
    public final LocatableToken parseTypeBody(int tdType, LocatableToken token)
    {
        parser.beginTypeBody(token);

        if (tdType == TYPEDEF_ENUM) {
            // For enum classes, we need to handle enum constants first
            parseEnumConstants();
        }

        // Check if the next token is a closing brace (empty body)
        LocatableToken nextToken = getTokenStream().LA(1);
        if (nextToken.getType() == JavaTokenTypes.RCURLY) {
            // Empty body, consume the closing brace
            token = nextToken();
        } else {
            // Non-empty body, parse it
            parseClassBody();
            // Check if the next token is a closing brace
            token = getTokenStream().LA(1);
            if (token.getType() == JavaTokenTypes.RCURLY) {
                // Consume the closing brace
                token = nextToken();
            } else {
                // If it's not a closing brace, try to recover
                // This is especially important for sealed classes with inner classes
                // that inherit from the outer class
                int braceCount = 1; // We've already seen one opening brace
                while (braceCount > 0 && token.getType() != JavaTokenTypes.EOF) {
                    token = nextToken();
                    if (token.getType() == JavaTokenTypes.LCURLY) {
                        braceCount++;
                    } else if (token.getType() == JavaTokenTypes.RCURLY) {
                        braceCount--;
                    }
                }

                if (braceCount == 0) {
                    // Found the matching closing brace, consume it
                    // It's already consumed in the loop above
                } else {
                    // If we still can't find a closing brace, report an error
                    error("Expected '}' (in class definition)");
                }
            }
        }
        parser.endTypeBody(token, token.getType() == JavaTokenTypes.RCURLY);
        return token;
    }

    /**
     * Parse enum constants in an enum class.
     * Enum constants are identifiers separated by commas, optionally followed by a semicolon.
     */
    private void parseEnumConstants()
    {
        LocatableToken token = getTokenStream().LA(1);

        // If the first token is a closing brace, there are no enum constants
        if (token.getType() == JavaTokenTypes.RCURLY) {
            return;
        }

        // Begin field declarations for enum constants
        parser.beginFieldDeclarations(token);

        boolean foundSemicolon = false;

        while (token.getType() != JavaTokenTypes.RCURLY && token.getType() != JavaTokenTypes.EOF) {
            if (token.getType() == JavaTokenTypes.SEMI) {
                // Found a semicolon, which marks the end of enum constants
                foundSemicolon = true;
                nextToken(); // consume the semicolon
                break;
            }

            if (token.getType() == JavaTokenTypes.IDENT) {
                // Found an enum constant
                LocatableToken identToken = token;
                parser.gotDeclBegin(identToken);
                token = nextToken(); // consume the identifier

                // Notify the parser about the enum constant (treating it as a field)
                parser.gotField(identToken, identToken, false);

                boolean hasConstructorArgs = false;
                boolean hasClassBody = false;

                // Check for constructor arguments
                if (token.getType() == JavaTokenTypes.LPAREN) {
                    hasConstructorArgs = true;
                    // Begin argument list
                    parser.beginArgumentList(token);

                    // Skip constructor arguments
                    int parenCount = 1;
                    while (parenCount > 0 && token.getType() != JavaTokenTypes.EOF) {
                        token = nextToken();
                        if (token.getType() == JavaTokenTypes.LPAREN) {
                            parenCount++;
                        } else if (token.getType() == JavaTokenTypes.RPAREN) {
                            parenCount--;
                        }
                    }

                    // End argument list
                    parser.endArgumentList(token);
                    token = nextToken(); // consume the closing parenthesis
                }

                // Check for enum constant class body
                if (token.getType() == JavaTokenTypes.LCURLY) {
                    hasClassBody = true;
                    // Begin anonymous class body for enum constant
                    parser.beginAnonClassBody(token, true);

                    // Skip enum constant class body
                    int braceCount = 1;
                    while (braceCount > 0 && token.getType() != JavaTokenTypes.EOF) {
                        token = nextToken();
                        if (token.getType() == JavaTokenTypes.LCURLY) {
                            braceCount++;
                        } else if (token.getType() == JavaTokenTypes.RCURLY) {
                            braceCount--;
                        }
                    }

                    // End anonymous class body
                    parser.endAnonClassBody(token, true);
                    token = nextToken(); // consume the closing brace
                }

                // End the field (enum constant)
                parser.endField(token, true);

                // Check for comma or semicolon
                if (token.getType() == JavaTokenTypes.COMMA) {
                    token = nextToken(); // consume the comma
                } else if (token.getType() == JavaTokenTypes.SEMI) {
                    // Found a semicolon, which marks the end of enum constants
                    foundSemicolon = true;
                    nextToken(); // consume the semicolon
                    break;
                } else if (token.getType() == JavaTokenTypes.RCURLY) {
                    // End of enum class body
                    getTokenStream().pushBack(token);
                    break;
                }
            } else {
                // Not an identifier, skip to the next token
                token = nextToken();
            }

            token = getTokenStream().LA(1);
        }

        // End field declarations for enum constants
        parser.endFieldDeclarations(token, foundSemicolon);

        // If we found a semicolon, we've already consumed it
        // If we didn't find a semicolon, we're at the end of the enum constants
        // and the next token is either a closing brace or the start of a class member
    }

    /**
     * Parse the beginning of a type definition.
     * 
     * @return The type definition type
     */
    public final int parseTypeDefBegin()
    {
        LocatableToken token = getTokenStream().nextToken();

        if (token.getType() == JavaTokenTypes.LITERAL_class) {
            return TYPEDEF_CLASS;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_interface) {
            return TYPEDEF_INTERFACE;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_enum) {
            return TYPEDEF_ENUM;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_sealed) {
            // For sealed classes, we need to check if the next token is "class"
            LocatableToken nextToken = getTokenStream().nextToken();
            if (nextToken.getType() == JavaTokenTypes.LITERAL_class) {
                // Push back the "class" token so it can be consumed by the parser
                getTokenStream().pushBack(nextToken);
                // Push back the "sealed" token so it can be treated as a modifier
                getTokenStream().pushBack(token);
                return TYPEDEF_CLASS;
            }
            // If it's not followed by "class", push back both tokens
            getTokenStream().pushBack(nextToken);
            getTokenStream().pushBack(token);
            return TYPEDEF_EPIC_FAIL;
        }

        getTokenStream().pushBack(token);
        return TYPEDEF_EPIC_FAIL;
    }

    /**
     * Parse the second part of a type definition.
     * 
     * @param isRecord Whether this is a record definition
     * @return The last token seen during parsing
     */
    public final LocatableToken parseTypeDefPart2(boolean isRecord)
    {
        // Check for extends or implements
        LocatableToken token = getTokenStream().nextToken();
        while (token.getType() != JavaTokenTypes.LCURLY && token.getType() != JavaTokenTypes.EOF) {
            // Check if we've reached the start of a new class definition
            if (token.getType() == JavaTokenTypes.LITERAL_class ||
                token.getType() == JavaTokenTypes.LITERAL_interface ||
                token.getType() == JavaTokenTypes.LITERAL_enum ||
                token.getType() == JavaTokenTypes.LITERAL_fun ||
                token.getType() == JavaTokenTypes.LITERAL_val ||
                token.getType() == JavaTokenTypes.LITERAL_var ||
                token.getType() == JavaTokenTypes.LITERAL_open) {
                // Found the start of a new declaration
                // Push it back so it can be consumed by the parser
                getTokenStream().pushBack(token);
                return null;
            }

            if (token.getType() == JavaTokenTypes.COLON) {
                // Process inheritance (Kotlin uses ':' instead of 'extends'/'implements')
                processInheritance();

                // After processing inheritance, check if the next token is the start of a new declaration
                token = getTokenStream().LA(1);
                if (token.getType() == JavaTokenTypes.LITERAL_class ||
                    token.getType() == JavaTokenTypes.LITERAL_interface ||
                    token.getType() == JavaTokenTypes.LITERAL_enum ||
                    token.getType() == JavaTokenTypes.LITERAL_fun ||
                    token.getType() == JavaTokenTypes.LITERAL_val ||
                    token.getType() == JavaTokenTypes.LITERAL_var ||
                    token.getType() == JavaTokenTypes.LITERAL_open) {
                    // Found the start of a new declaration
                    // This means the current class definition has ended
                    return null;
                }
            }
            token = getTokenStream().nextToken();
        }

        // In Kotlin, classes can be defined without curly braces
        // If we reach EOF, it's a valid end of class definition
        return token;
    }

    /**
     * Having seen '{', parse the rest of a class body.
     */
    public final void parseClassBody()
    {
        LocatableToken token = getTokenStream().nextToken();
        while (token.getType() != JavaTokenTypes.RCURLY) {
            if (token.getType() == JavaTokenTypes.EOF) {
                error("Unexpected end-of-file in type body; missing '}'", token);
                return;
            }
            parseClassElement(token);
            token = getTokenStream().LA(1);
            if (token.getType() == JavaTokenTypes.RCURLY) {
                // Found the closing brace, but don't consume it
                // Let parseTypeBody consume it
                break;
            }
            token = nextToken();
        }
        // Only push back the token if it's not a closing brace
        if (token.getType() != JavaTokenTypes.RCURLY) {
            getTokenStream().pushBack(token);
        }
    }

    /**
     * Parse a class element (field, method, inner class, etc.)
     * 
     * @param token The first token of the class element
     */
    public final void parseClassElement(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.SEMI) {
            // A spurious semicolon.
            return;
        }

        parser.gotDeclBegin(token);
        getTokenStream().pushBack(token);
        LocatableToken hiddenToken = token.getHiddenBefore();

        // field declaration, method declaration, inner class
        List<LocatableToken> modifiers = parseModifiers();
        LocatableToken firstMod = null;
        if (!modifiers.isEmpty()) {
            firstMod = modifiers.get(0);
        }

        token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_class
                || token.getType() == JavaTokenTypes.LITERAL_interface
                || token.getType() == JavaTokenTypes.LITERAL_enum
                || token.getType() == JavaTokenTypes.AT
                || token.getType() == JavaTokenTypes.LITERAL_sealed) {
            parser.gotInnerType(token);
            getTokenStream().pushBack(token);
            parseTypeDef(firstMod != null ? firstMod : token);
            parser.endElement(getLastToken(), true);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_companion) {
            // Process companion object
            LocatableToken companionToken = token;
            token = nextToken();
            if (token.getType() == JavaTokenTypes.LITERAL_object) {
                processCompanionObject(companionToken, token);
            } else {
                // Not a companion object, push back both tokens
                getTokenStream().pushBack(token);
                getTokenStream().pushBack(companionToken);
                error("Expected 'object' after 'companion'", token);
            }
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_object) {
            // Process object declaration
            processObjectDeclaration(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_fun) {
            // Process function
            processFunction(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_val ||
                 token.getType() == JavaTokenTypes.LITERAL_var ||
                 token.getType() == JavaTokenTypes.LITERAL_const) {
            // Process property
            processProperty(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_init) {
            // Kotlin init block
            LocatableToken initToken = token;
            token = nextToken();
            if (token.getType() != JavaTokenTypes.LCURLY) {
                error("Expected '{' after 'init'", token);
                getTokenStream().pushBack(token);
                getTokenStream().pushBack(initToken);
                return;
            }

            LocatableToken firstToken = firstMod == null ? initToken : firstMod;
            parser.beginInitBlock(firstToken, token);
            parser.modifiersConsumed();
            parser.getTokenStream().pushBack(token);
            parseStmtBlock();
            token = nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expecting '}' (at end of init block)");
                getTokenStream().pushBack(token);
                parser.endInitBlock(token, false);
                parser.endElement(token, false);
                return;
            }
            parser.endInitBlock(token, true);
            parser.endElement(token, true);
        }
        else if (token.getType() == JavaTokenTypes.LCURLY) {
            // initialisation block
            LocatableToken firstToken = firstMod == null ? token : firstMod;
            parser.beginInitBlock(firstToken, token);
            parser.modifiersConsumed();
            parseStmtBlock();
            token = nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expecting '}' (at end of initialisation block)");
                getTokenStream().pushBack(token);
                parser.endInitBlock(token, false);
                parser.endElement(token, false);
                return;
            }
            parser.endInitBlock(token, true);
            parser.endElement(token, true);
        }
        else if (token.getType() == JavaTokenTypes.IDENT) {
            // This could be an enum constant in an enum class
            // Process it as a field
            LocatableToken identToken = token;
            parser.gotField(identToken, identToken, false);

            token = nextToken();

            // Check for comma or semicolon
            if (token.getType() == JavaTokenTypes.COMMA) {
                // Another enum constant follows
                parser.endField(token, true);
            } else if (token.getType() == JavaTokenTypes.SEMI) {
                // End of enum constants
                parser.endField(token, true);
            } else if (token.getType() == JavaTokenTypes.RCURLY) {
                // End of enum class body
                getTokenStream().pushBack(token);
                parser.endField(token, true);
            } else {
                // Not a valid token after enum constant
                getTokenStream().pushBack(token);
                parser.endField(token, false);
            }
        }
        else {
            // Not recognized, skip to next semicolon or closing brace
            error("Unexpected token in class body: " + token.getText(), token);
            while (token.getType() != JavaTokenTypes.SEMI && 
                   token.getType() != JavaTokenTypes.RCURLY &&
                   token.getType() != JavaTokenTypes.EOF) {
                token = nextToken();
            }
            if (token.getType() == JavaTokenTypes.RCURLY) {
                getTokenStream().pushBack(token);
            }
            parser.endElement(token, token.getType() == JavaTokenTypes.SEMI);
        }
    }

    /**
     * Parse a statement block.
     */
    public final void parseStmtBlock()
    {
        LocatableToken token = getTokenStream().LA(1);
        if (token.getType() != JavaTokenTypes.LCURLY) {
            error("Expected '{' (at beginning of statement block)");
            return;
        }

        token = nextToken(); // consume the '{'
        parser.beginStmtblockBody(token);

        token = getTokenStream().LA(1);
        while (token.getType() != JavaTokenTypes.RCURLY && token.getType() != JavaTokenTypes.EOF) {
            parseStatement();
            token = getTokenStream().LA(1);
        }

        if (token.getType() == JavaTokenTypes.EOF) {
            error("Unexpected end-of-file in statement block; missing '}'");
            parser.endStmtblockBody(token, false);
            return;
        }

        // Don't consume the '}'
        parser.endStmtblockBody(token, true);
    }

    /**
     * Parse a statement.
     * 
     * @return The last token seen during parsing
     */
    public final LocatableToken parseStatement()
    {
        return parseStatement(nextToken(), false);
    }

    /**
     * Parse a statement.
     * 
     * @param token The first token of the statement
     * @param allowComma Whether to allow commas in the statement
     * @return The last token seen during parsing
     */
    public final LocatableToken parseStatement(LocatableToken token, boolean allowComma)
    {
        // For now, just skip to the next semicolon or closing brace
        while (token.getType() != JavaTokenTypes.SEMI && 
               token.getType() != JavaTokenTypes.RCURLY &&
               token.getType() != JavaTokenTypes.EOF) {
            token = nextToken();
        }

        if (token.getType() == JavaTokenTypes.RCURLY) {
            getTokenStream().pushBack(token);
        }

        return token;
    }

    /**
     * Parse a type specification.
     * 
     * @param processArray Whether to process array declarators
     * @return Whether the parsing was successful
     */
    public final boolean parseTypeSpec(boolean processArray)
    {
        return parseTypeSpec(false, processArray, new ArrayList<>());
    }

    /**
     * Parse a type specification.
     * 
     * @param speculative Whether this is a speculative parse
     * @param processArray Whether to process array declarators
     * @param ttokens List to store tokens
     * @return Whether the parsing was successful
     */
    public final boolean parseTypeSpec(boolean speculative, boolean processArray, List<LocatableToken> ttokens)
    {
        // For now, just return false
        return false;
    }

    /**
     * Parse an expression.
     */
    public final void parseExpression()
    {
        parseExpression(false, true);
    }

    /**
     * Parse an expression.
     * 
     * @param isLambdaBody Whether this is a lambda body
     * @param lambdaAllowed Whether lambdas are allowed in this expression
     */
    public final void parseExpression(boolean isLambdaBody, boolean lambdaAllowed)
    {
        // For now, just skip to the next semicolon or closing brace
        LocatableToken token = nextToken();
        while (token.getType() != JavaTokenTypes.SEMI && 
               token.getType() != JavaTokenTypes.RCURLY &&
               token.getType() != JavaTokenTypes.EOF) {
            token = nextToken();
        }

        if (token.getType() == JavaTokenTypes.RCURLY) {
            getTokenStream().pushBack(token);
        }
    }

    /**
     * Parse variable declarations.
     * 
     * @return The last token seen during parsing
     */
    public final LocatableToken parseVariableDeclarations()
    {
        return parseVariableDeclarations(nextToken(), true);
    }

    /**
     * Parse variable declarations.
     * 
     * @param first The first token of the variable declarations
     * @param mustEndWithSemi Whether the declarations must end with a semicolon
     * @return The last token seen during parsing
     */
    public final LocatableToken parseVariableDeclarations(LocatableToken first, boolean mustEndWithSemi)
    {
        // For now, just skip to the next semicolon or closing brace
        LocatableToken token = first;
        while (token.getType() != JavaTokenTypes.SEMI && 
               token.getType() != JavaTokenTypes.RCURLY &&
               token.getType() != JavaTokenTypes.EOF) {
            token = nextToken();
        }

        if (token.getType() == JavaTokenTypes.RCURLY) {
            getTokenStream().pushBack(token);
        }

        return token;
    }

    /**
     * Parse method parameters and body.
     */
    public final void parseMethodParamsBody()
    {
        // For now, just skip to the next closing brace
        LocatableToken token = nextToken();
        int braceCount = 0;

        while (token.getType() != JavaTokenTypes.EOF) {
            if (token.getType() == JavaTokenTypes.LCURLY) {
                braceCount++;
            }
            else if (token.getType() == JavaTokenTypes.RCURLY) {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
            token = nextToken();
        }
    }

    private void processPackage()
    {
        List<LocatableToken> pkgTokens = new ArrayList<>();
        LocatableToken token = null;
        boolean foundPackageEnd = false;
        boolean foundSemicolon = false;

        // Collect package name tokens
        while (!foundPackageEnd) {
            token = getTokenStream().nextToken();

            // Check for end of package declaration
            if (token.getType() == JavaTokenTypes.SEMI ||
                token.getType() == JavaTokenTypes.LITERAL_class ||
                token.getType() == JavaTokenTypes.LITERAL_interface ||
                token.getType() == JavaTokenTypes.LITERAL_fun ||
                token.getType() == JavaTokenTypes.LITERAL_val ||
                token.getType() == JavaTokenTypes.LITERAL_var ||
                token.getType() == JavaTokenTypes.EOF) {

                foundPackageEnd = true;

                // If we found a semicolon, remember it
                if (token.getType() == JavaTokenTypes.SEMI) {
                    foundSemicolon = true;
                }
                // If we found a class/interface/function/property, push it back so it can be processed later
                else if (token.getType() != JavaTokenTypes.EOF) {
                    getTokenStream().pushBack(token);
                }
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                pkgTokens.add(token);
            }
        }

        parser.gotPackage(pkgTokens);
        // Only call gotPackageSemi if we actually found a semicolon
        if (foundSemicolon) {
            parser.gotPackageSemi(token);
        }
    }

    private void processClass(LocatableToken classToken)
    {
        parser.gotModifier(classToken);
        parseTypeDef(classToken);
    }

    private void processInterface(LocatableToken interfaceToken)
    {
        parser.gotModifier(interfaceToken);
        parseTypeDef(interfaceToken);
    }

    private void processInheritance()
    {
        // Skip whitespace
        LocatableToken token = getTokenStream().nextToken();

        // Process superclass or interfaces
        List<LocatableToken> typeTokens = new ArrayList<>();
        while (token.getType() != JavaTokenTypes.LCURLY &&
               token.getType() != JavaTokenTypes.EOF) {
            // Check if we've reached the start of a new class definition
            if (token.getType() == JavaTokenTypes.LITERAL_class ||
                token.getType() == JavaTokenTypes.LITERAL_interface ||
                token.getType() == JavaTokenTypes.LITERAL_enum ||
                token.getType() == JavaTokenTypes.LITERAL_fun ||
                token.getType() == JavaTokenTypes.LITERAL_val ||
                token.getType() == JavaTokenTypes.LITERAL_var) {
                // Found the start of a new declaration
                // Push it back so it can be consumed by the parser
                getTokenStream().pushBack(token);
                return;
            }

            if (token.getType() == JavaTokenTypes.IDENT) {
                typeTokens.add(token);
                parser.beginTypeDefExtends(token);
                parser.gotTypeSpec(typeTokens);
                parser.endTypeDefExtends();
                typeTokens.clear();

                // Check for constructor arguments after the type
                token = getTokenStream().nextToken();
                if (token.getType() == JavaTokenTypes.LPAREN) {
                    // Skip constructor arguments
                    int parenCount = 1;
                    while (parenCount > 0 && token.getType() != JavaTokenTypes.EOF) {
                        token = getTokenStream().nextToken();
                        if (token.getType() == JavaTokenTypes.LPAREN) {
                            parenCount++;
                        } else if (token.getType() == JavaTokenTypes.RPAREN) {
                            parenCount--;
                        }
                    }
                    // Continue to the next token after the closing parenthesis
                    if (token.getType() != JavaTokenTypes.EOF) {
                        token = getTokenStream().nextToken();
                    }
                    continue;
                } else if (token.getType() == JavaTokenTypes.COMMA) {
                    // Multiple inheritance, continue with the next type
                    token = getTokenStream().nextToken();
                    continue;
                } else if (token.getType() == JavaTokenTypes.LCURLY) {
                    // Found the opening brace of the class body
                    // Push it back so it can be consumed by the parser
                    getTokenStream().pushBack(token);
                    return;
                } else if (token.getType() == JavaTokenTypes.LITERAL_class ||
                           token.getType() == JavaTokenTypes.LITERAL_interface ||
                           token.getType() == JavaTokenTypes.LITERAL_enum ||
                           token.getType() == JavaTokenTypes.LITERAL_fun ||
                           token.getType() == JavaTokenTypes.LITERAL_val ||
                           token.getType() == JavaTokenTypes.LITERAL_var) {
                    // Found the start of a new declaration
                    // Push it back so it can be consumed by the parser
                    getTokenStream().pushBack(token);
                    return;
                } else {
                    // No constructor arguments, continue with the current token
                    continue;
                }
            } else if (token.getType() == JavaTokenTypes.COMMA) {
                // Multiple inheritance, continue with the next type
                token = getTokenStream().nextToken();
                continue;
            } else if (token.getType() == JavaTokenTypes.LCURLY) {
                // Found the opening brace of the class body
                // Push it back so it can be consumed by the parser
                getTokenStream().pushBack(token);
                return;
            }
            token = getTokenStream().nextToken();
        }

        // Put back the last token (LCURLY or EOF)
        getTokenStream().pushBack(token);
    }

    private void processFunction(LocatableToken funToken)
    {
//        parser.gotDeclBegin(funToken);

        // Get function name
        LocatableToken nameToken = getTokenStream().nextToken();
        if (nameToken.getType() != JavaTokenTypes.IDENT) {
            parser.endDecl(nameToken);
            return;
        }

        // Process return type if present
        List<LocatableToken> typeTokens = new ArrayList<>();
        boolean hasReturnType = false;
        List<LocatableToken> paramNameTokens = new ArrayList<>();
        List<List<LocatableToken>> paramTypeTokensList = new ArrayList<>();

        // Process parameters and return type
        LocatableToken token = getTokenStream().nextToken();

        // Check for parameters
        if (token.getType() == JavaTokenTypes.LPAREN) {
            // Parse parameter list
            token = getTokenStream().nextToken();
            while (token.getType() != JavaTokenTypes.RPAREN && token.getType() != JavaTokenTypes.EOF) {
                // Parse parameter
                if (token.getType() == JavaTokenTypes.IDENT) {
                    LocatableToken paramNameToken = token;
                    paramNameTokens.add(paramNameToken);

                    // Check for parameter type
                    token = getTokenStream().nextToken();
                    if (token.getType() == JavaTokenTypes.COLON) {
                        // Parameter has a type
                        token = getTokenStream().nextToken();
                        if (token.getType() == JavaTokenTypes.IDENT) {
                            List<LocatableToken> paramTypeTokens = new ArrayList<>();
                            paramTypeTokens.add(token);
                            paramTypeTokensList.add(paramTypeTokens);
                        } else {
                            paramTypeTokensList.add(null);
                        }
                    } else {
                        paramTypeTokensList.add(null);
                        continue;
                    }
                }

                // Move to next parameter or closing parenthesis
                token = getTokenStream().nextToken();
                if (token.getType() == JavaTokenTypes.COMMA) {
                    token = getTokenStream().nextToken();
                }
            }

            // After parameters, look for return type
            token = getTokenStream().nextToken();
            if (token.getType() == JavaTokenTypes.COLON) {
                // Process return type
                token = getTokenStream().nextToken();
                if (token.getType() == JavaTokenTypes.IDENT || isPrimitiveType(token)) {
                    typeTokens.add(token);
                    hasReturnType = true;
                }
            }

            // Find the opening curly brace
            while (token.getType() != JavaTokenTypes.LCURLY && token.getType() != JavaTokenTypes.EOF) {
                token = getTokenStream().nextToken();
            }
        } else if (token.getType() == JavaTokenTypes.COLON) {
            // Process return type without parameters
            token = getTokenStream().nextToken();
            if (token.getType() == JavaTokenTypes.IDENT) {
                typeTokens.add(token);
                hasReturnType = true;
            }

            // Find the opening curly brace
            while (token.getType() != JavaTokenTypes.LCURLY && token.getType() != JavaTokenTypes.EOF) {
                token = getTokenStream().nextToken();
            }
        }

        if (token.getType() == JavaTokenTypes.EOF) {
            parser.endDecl(token);
            return;
        }

        if (hasReturnType) {
            parser.gotTypeSpec(typeTokens);
        }
        parser.gotMethodDeclaration(nameToken, funToken.getHiddenBefore());

            parser.modifiersConsumed();

        // Now process the parameters after the method context is set up
        for (int i = 0; i < paramNameTokens.size(); i++) {
            List<LocatableToken> paramTypeTokens = paramTypeTokensList.get(i);
            if (paramTypeTokens != null) {
                parser.gotTypeSpec(paramTypeTokens);
            }
            parser.gotMethodParameter(paramNameTokens.get(i), null);
        }

        parser.gotAllMethodParameters();

        // Process function body
        parser.beginMethodBody(token);
        processBody();
        LocatableToken lastToken = getLastToken();
        parser.endMethodBody(lastToken, true);

        // For top-level functions, we need to be careful not to call endMethodDecl
        // if we're not inside a class, as it will cause a ClassCastException
        try {
            parser.endMethodDecl(lastToken, lastToken.getType() == JavaTokenTypes.RCURLY);
        } catch (ClassCastException e) {
            // This is a top-level function, not inside a class
            // Just end the element instead
            parser.endElement(lastToken, lastToken.getType() == JavaTokenTypes.RCURLY);
//
//            // Mark that this file has top-level functions
//            if (parser instanceof InfoParser) {
//                ((InfoParser) parser).setHasTopLevelFunctions(true);
//            }
        }
    }

    private void processProperty(LocatableToken propertyToken)
    {
//        parser.gotDeclBegin(propertyToken);
        boolean isVal = propertyToken.getType() == JavaTokenTypes.LITERAL_val;

        // Get property name
        LocatableToken nameToken = getTokenStream().nextToken();
        if (nameToken.getType() != JavaTokenTypes.IDENT) {
            parser.endDecl(nameToken);
            return;
        }

        // Begin field declarations
        parser.beginFieldDeclarations(propertyToken);

        // Process type and initializer
        LocatableToken token;
        List<LocatableToken> typeTokens = new ArrayList<>();
        boolean hasType = false;

        while ((token = getTokenStream().nextToken()).getType() != JavaTokenTypes.SEMI &&
               token.getType() != JavaTokenTypes.EOF) {
            if (token.getType() == JavaTokenTypes.COLON) {
                // Process type
                token = getTokenStream().nextToken();
                if (token.getType() == JavaTokenTypes.IDENT || isPrimitiveType(token)) {
                    typeTokens.add(token);
                    hasType = true;
                }
            } else if (token.getType() == JavaTokenTypes.ASSIGN) {
                break; // We'll process the initializer after setting up the field
            }
        }

        // If we found a type, tell the parser about it
        if (hasType) {
            parser.gotTypeSpec(typeTokens);
        }

        // Now that we've processed the type, we can create the field
        parser.gotField(propertyToken, nameToken, true);

        // If we found an assignment, process the initializer
        if (token.getType() == JavaTokenTypes.ASSIGN) {
            parser.beginExpression(token, false);
            skipToSemicolon();
            parser.endExpression(token, false);
        }

        // End field declaration
        parser.endField(token, true);
        parser.endFieldDeclarations(token, true);
    }

    private void processBody()
    {
        int braceCount = 1;

        while (braceCount > 0) {
            LocatableToken token = getTokenStream().nextToken();
            if (token.getType() == JavaTokenTypes.EOF) {
                return;
            } else if (token.getType() == JavaTokenTypes.LCURLY) {
                // Begin a statement block
                parser.beginStmtblockBody(token);
                braceCount++;
            } else if (token.getType() == JavaTokenTypes.RCURLY) {
                braceCount--;
                if (braceCount > 0) {
                    // End a statement block (but not the outer block)
                    parser.endStmtblockBody(token, true);
                }
                parser.setLastToken(token);
            } else if (token.getType() == JavaTokenTypes.LITERAL_class) {
                // Process nested class
                processClass(token);
            } else if (token.getType() == JavaTokenTypes.LITERAL_interface) {
                // Process nested interface
                processInterface(token);
            } else if (token.getType() == JavaTokenTypes.LITERAL_fun) {
                // Process function
                processFunction(token);
            } else if (token.getType() == JavaTokenTypes.LITERAL_val ||
                       token.getType() == JavaTokenTypes.LITERAL_var) {
                // Process property
                processProperty(token);
            } else {
                // For other tokens, we might need to handle them as expressions or statements
                // For simplicity, we'll just treat them as part of the current block
            }
        }
    }

    private void skipToSemicolon()
    {
        LocatableToken token;
        while ((token = getTokenStream().nextToken()).getType() != JavaTokenTypes.SEMI) {
            if (token.getType() == JavaTokenTypes.EOF) {
                return;
            }
            // In Kotlin, semicolons are optional, and a property declaration can end with a newline
            // So we should stop at a newline or the start of a new declaration
            if (token.getType() == JavaTokenTypes.LITERAL_var ||
                token.getType() == JavaTokenTypes.LITERAL_val ||
                token.getType() == JavaTokenTypes.LITERAL_fun ||
                token.getType() == JavaTokenTypes.LITERAL_class ||
                token.getType() == JavaTokenTypes.LITERAL_interface ||
                token.getType() == JavaTokenTypes.LITERAL_init) {
                getTokenStream().pushBack(token);
                return;
            }
        }
    }

    /**
     * Process a companion object declaration.
     * 
     * @param companionToken The 'companion' token
     * @param objectToken The 'object' token
     */
    private void processCompanionObject(LocatableToken companionToken, LocatableToken objectToken)
    {
        // Notify the parser that we've found an inner type
        parser.gotInnerType(companionToken);

        // Begin type definition
        parser.gotTypeDef(companionToken, TYPEDEF_CLASS);
        parser.modifiersConsumed();

        // Check for a name (optional for companion objects)
        LocatableToken token = getTokenStream().LA(1);
        if (token.getType() == JavaTokenTypes.IDENT) {
            // Named companion object
            token = getTokenStream().nextToken();
            parser.gotTypeDefName(token);
            token = getTokenStream().nextToken();
        } else {
            // Anonymous companion object, use "Companion" as the name
            parser.gotTypeDefName(objectToken);
            token = getTokenStream().nextToken();
        }

        // Process inheritance if present
        while (token.getType() != JavaTokenTypes.LCURLY) {
            if (token.getType() == JavaTokenTypes.COLON) {
                // Process inheritance (Kotlin uses ':' instead of 'extends'/'implements')
                processInheritance();
            }
            else if (token.getType() == JavaTokenTypes.EOF) {
                error("Unexpected end-of-file in companion object definition");
                return;
            }
            token = getTokenStream().nextToken();
        }

        // Process the body
        parser.beginTypeBody(token);
        parseClassBody();
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RCURLY) {
            error("Expected '}' (in companion object definition)");
        }
        parser.endTypeBody(token, token.getType() == JavaTokenTypes.RCURLY);
        parser.gotTypeDefEnd(token, token.getType() == JavaTokenTypes.RCURLY);
    }

    /**
     * Process an object declaration.
     * 
     * @param objectToken The 'object' token
     */
    private void processObjectDeclaration(LocatableToken objectToken)
    {
        // Notify the parser that we've found an inner type
        parser.gotInnerType(objectToken);

        // Begin type definition
        parser.gotTypeDef(objectToken, TYPEDEF_CLASS);
        parser.modifiersConsumed();

        // Get object name
        LocatableToken token = getTokenStream().nextToken();
        if (token.getType() != JavaTokenTypes.IDENT) {
            getTokenStream().pushBack(token);
            parser.gotTypeDefEnd(token, false);
            error("Expected identifier (in object declaration)");
            return;
        }
        parser.gotTypeDefName(token);

        // Process inheritance if present
        token = getTokenStream().nextToken();
        while (token.getType() != JavaTokenTypes.LCURLY) {
            if (token.getType() == JavaTokenTypes.COLON) {
                // Process inheritance (Kotlin uses ':' instead of 'extends'/'implements')
                processInheritance();
            }
            else if (token.getType() == JavaTokenTypes.EOF) {
                error("Unexpected end-of-file in object declaration");
                return;
            }
            token = getTokenStream().nextToken();
        }

        // Process the body
        parser.beginTypeBody(token);
        parseClassBody();
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RCURLY) {
            error("Expected '}' (in object declaration)");
        }
        parser.endTypeBody(token, token.getType() == JavaTokenTypes.RCURLY);
        parser.gotTypeDefEnd(token, token.getType() == JavaTokenTypes.RCURLY);
    }
}
