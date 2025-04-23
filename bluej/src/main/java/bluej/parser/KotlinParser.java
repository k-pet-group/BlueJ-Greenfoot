/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009, 2012,2014,2022,2024  Michael Kolling and John Rosenberg

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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import bluej.parser.lexer.KotlinLexer;
import bluej.parser.lexer.KotlinTokenFilter;
import bluej.parser.lexer.KotlinTokenTypes;
import bluej.parser.lexer.LocatableToken;

/**
 * A parser for Kotlin source code.
 */
public class KotlinParser extends KotlinParserCallbacks
{
    protected KotlinTokenFilter tokenStream;
    protected LocatableToken lastToken;

    public static TokenStream getLexer(Reader r)
    {
        return new KotlinLexer(r);
    }

    public static TokenStream getLexer(Reader r, boolean handleComments, boolean handleMultilineStrings)
    {
        return new KotlinLexer(r, handleComments, handleMultilineStrings);
    }

    private static TokenStream getLexer(Reader r, int line, int col, int pos)
    {
        return new KotlinLexer(r, line, col, pos);
    }

    public KotlinParser(Reader r)
    {
        TokenStream lexer = getLexer(r);
        tokenStream = new KotlinTokenFilter(lexer, this);
    }

    public KotlinParser(Reader r, boolean handleComments)
    {
        TokenStream lexer = getLexer(r, handleComments, true);
        tokenStream = new KotlinTokenFilter(lexer, this);
    }

    public KotlinParser(Reader r, int line, int col, int pos)
    {
        TokenStream lexer = getLexer(r, line, col, pos);
        tokenStream = new KotlinTokenFilter(lexer, this);
    }

    public final KotlinTokenFilter getTokenStream()
    {
        return tokenStream;
    }

    /**
     * Get the last token seen during the previous parse.
     */
    public final LocatableToken getLastToken()
    {
        return lastToken;
    }

    /**
     * Called when a comment token is encountered.
     */
    public void gotComment(LocatableToken token)
    {
        // Default implementation does nothing
    }

    /**
     * Parse a compilation unit (a source file).
     */
    public final void parseCU()
    {
        try {
            LocatableToken token;
            while ((token = tokenStream.nextToken()).getType() != KotlinTokenTypes.EOF) {
                // Process tokens and call appropriate callbacks
                if (token.getType() == KotlinTokenTypes.LITERAL_package) {
                    beginPackageStatement(token);
                    processPackage();
                } else if (token.getType() == KotlinTokenTypes.LITERAL_class) {
                    processClass(token);
                } else if (token.getType() == KotlinTokenTypes.LITERAL_interface) {
                    processInterface(token);
                } else if (token.getType() == KotlinTokenTypes.LITERAL_fun) {
                    processFunction(token);
                } else if (token.getType() == KotlinTokenTypes.LITERAL_val || 
                           token.getType() == KotlinTokenTypes.LITERAL_var) {
                    processProperty(token);
                }
                // Store the last token seen
                lastToken = token;
            }
        }
        catch (Exception e) {
            // Catch and log exceptions
            System.err.println("Error parsing Kotlin file: " + e.getMessage());
            e.printStackTrace();
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
            token = tokenStream.nextToken();

            // Check for end of package declaration
            if (token.getType() == KotlinTokenTypes.SEMI || 
                token.getType() == KotlinTokenTypes.LITERAL_class || 
                token.getType() == KotlinTokenTypes.LITERAL_interface || 
                token.getType() == KotlinTokenTypes.LITERAL_fun || 
                token.getType() == KotlinTokenTypes.LITERAL_val || 
                token.getType() == KotlinTokenTypes.LITERAL_var || 
                token.getType() == KotlinTokenTypes.EOF) {

                foundPackageEnd = true;

                // If we found a semicolon, remember it
                if (token.getType() == KotlinTokenTypes.SEMI) {
                    foundSemicolon = true;
                }
                // If we found a class/interface/function/property, push it back so it can be processed later
                else if (token.getType() != KotlinTokenTypes.EOF) {
                    tokenStream.pushBack(token);
                }
            } else if (token.getType() == KotlinTokenTypes.IDENT) {
                pkgTokens.add(token);
            }
        }

        gotPackage(pkgTokens);
        // Only call gotPackageSemi if we actually found a semicolon
        if (foundSemicolon && token != null) {
            gotPackageSemi(token);
        }
    }

    private void processClass(LocatableToken classToken)
    {
        gotModifier(classToken);
        modifiersConsumed();

        // Get class name
        LocatableToken nameToken = tokenStream.nextToken();
        if (nameToken.getType() == KotlinTokenTypes.IDENT) {
            gotTypeDef(classToken, 0); // 0 for class
            gotTypeDefName(nameToken);

            // Check for extends or implements
            LocatableToken token;
            while ((token = tokenStream.nextToken()).getType() != KotlinTokenTypes.LCURLY) {
                if (token.getType() == KotlinTokenTypes.COLON) {
                    // Process inheritance
                    processInheritance();
                }
                if (token.getType() == KotlinTokenTypes.EOF) {
                    return;
                }
            }

            // Process class body
            beginTypeBody(token);
            processBody();
            endTypeBody(tokenStream.LA(1), true);
        }
    }

    private void processInterface(LocatableToken interfaceToken)
    {
        gotModifier(interfaceToken);
        modifiersConsumed();

        // Get interface name
        LocatableToken nameToken = tokenStream.nextToken();
        if (nameToken.getType() == KotlinTokenTypes.IDENT) {
            gotTypeDef(interfaceToken, 1); // 1 for interface
            gotTypeDefName(nameToken);

            // Check for extends
            LocatableToken token;
            while ((token = tokenStream.nextToken()).getType() != KotlinTokenTypes.LCURLY) {
                if (token.getType() == KotlinTokenTypes.COLON) {
                    // Process inheritance
                    processInheritance();
                }
                if (token.getType() == KotlinTokenTypes.EOF) {
                    return;
                }
            }

            // Process interface body
            beginTypeBody(token);
            processBody();
            endTypeBody(tokenStream.LA(1), true);
        }
    }

    private void processInheritance()
    {
        // Skip whitespace
        LocatableToken token = tokenStream.nextToken();

        // Process superclass or interfaces
        List<LocatableToken> typeTokens = new ArrayList<>();
        while (token.getType() != KotlinTokenTypes.LCURLY && 
               token.getType() != KotlinTokenTypes.EOF) {
            if (token.getType() == KotlinTokenTypes.IDENT) {
                typeTokens.add(token);
                beginTypeDefExtends(token);
                gotTypeSpec(typeTokens);
                endTypeDefExtends();
                typeTokens.clear();
            }
            token = tokenStream.nextToken();
        }

        // Put back the last token (LCURLY or EOF)
        tokenStream.pushBack(token);
    }

    private void processFunction(LocatableToken funToken)
    {
        // Get function name
        LocatableToken nameToken = tokenStream.nextToken();
        if (nameToken.getType() == KotlinTokenTypes.IDENT) {
            beginFunctionDeclaration(funToken);
            gotFunctionName(nameToken);

            // Process parameters and return type
            LocatableToken token;
            while ((token = tokenStream.nextToken()).getType() != KotlinTokenTypes.LCURLY) {
                if (token.getType() == KotlinTokenTypes.EOF) {
                    return;
                }
            }

            // Process function body
            beginMethodBody(token);
            processBody();
            endMethodBody(tokenStream.LA(1), true);
            endFunctionDeclaration(tokenStream.LA(1));
        }
    }

    private void processProperty(LocatableToken propertyToken)
    {
        boolean isVal = propertyToken.getType() == KotlinTokenTypes.LITERAL_val;

        // Get property name
        LocatableToken nameToken = tokenStream.nextToken();
        if (nameToken.getType() == KotlinTokenTypes.IDENT) {
            gotPropertyDeclaration(propertyToken, isVal);
            gotPropertyName(nameToken);

            // Process type and initializer
            LocatableToken token;
            List<LocatableToken> typeTokens = new ArrayList<>();
            while ((token = tokenStream.nextToken()).getType() != KotlinTokenTypes.SEMI && 
                   token.getType() != KotlinTokenTypes.EOF) {
                if (token.getType() == KotlinTokenTypes.COLON) {
                    // Process type
                    token = tokenStream.nextToken();
                    if (token.getType() == KotlinTokenTypes.IDENT) {
                        typeTokens.add(token);
                        gotPropertyType(typeTokens);
                    }
                } else if (token.getType() == KotlinTokenTypes.ASSIGN) {
                    // Process initializer
                    gotPropertyInitializer(token);
                    skipToSemicolon();
                }
            }

            endPropertyDeclaration(token);
        }
    }

    private void processBody()
    {
        int braceCount = 1;

        while (braceCount > 0) {
            LocatableToken token = tokenStream.nextToken();
            if (token.getType() == KotlinTokenTypes.EOF) {
                return;
            } else if (token.getType() == KotlinTokenTypes.LCURLY) {
                braceCount++;
            } else if (token.getType() == KotlinTokenTypes.RCURLY) {
                braceCount--;
            } else if (token.getType() == KotlinTokenTypes.LITERAL_class) {
                // Process nested class
                processClass(token);
                // Adjust braceCount to account for the class body that was processed
                braceCount--;
            } else if (token.getType() == KotlinTokenTypes.LITERAL_interface) {
                // Process nested interface
                processInterface(token);
                // Adjust braceCount to account for the interface body that was processed
                braceCount--;
            } else if (token.getType() == KotlinTokenTypes.LITERAL_fun) {
                // Process function
                processFunction(token);
                // Adjust braceCount to account for the function body that was processed
                braceCount--;
            } else if (token.getType() == KotlinTokenTypes.LITERAL_val || 
                       token.getType() == KotlinTokenTypes.LITERAL_var) {
                // Process property
                processProperty(token);
            }
        }
    }

    private void skipToSemicolon()
    {
        LocatableToken token;
        while ((token = tokenStream.nextToken()).getType() != KotlinTokenTypes.SEMI) {
            if (token.getType() == KotlinTokenTypes.EOF) {
                return;
            }
            // In Kotlin, semicolons are optional, and a property declaration can end with a newline
            // So we should stop at a newline or the start of a new declaration
            if (token.getType() == KotlinTokenTypes.LITERAL_var || 
                token.getType() == KotlinTokenTypes.LITERAL_val || 
                token.getType() == KotlinTokenTypes.LITERAL_fun || 
                token.getType() == KotlinTokenTypes.LITERAL_class || 
                token.getType() == KotlinTokenTypes.LITERAL_interface) {
                tokenStream.pushBack(token);
                return;
            }
        }
    }
}
