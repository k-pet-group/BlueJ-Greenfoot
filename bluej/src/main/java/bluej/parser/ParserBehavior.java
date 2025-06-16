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

import bluej.parser.lexer.LocatableToken;
import java.util.List;

/**
 * Interface defining the customizable parsing behavior for different languages.
 * This interface represents the Strategy in the Strategy pattern, allowing
 * different parsing behaviors to be injected into the SourceParser.
 * 
 * @author Vitaly Bragilevsky
 */
public interface ParserBehavior {
    /**
     * Parse a compilation unit (from the beginning).
     */
    void parseCU();
    
    /**
     * Parse a part of a compilation unit, starting from the given state.
     * 
     * @param state The state to start parsing from
     * @return The new state after parsing
     */
    int parseCUpart(int state);
    
    /**
     * Parse a package statement.
     * 
     * @param token The "package" token
     * @return The last token seen during parsing
     */
    LocatableToken parsePackageStmt(LocatableToken token);
    
    /**
     * Parse an import statement.
     */
    void parseImportStatement();
    
    /**
     * Parse an import statement starting with the given token.
     * 
     * @param importToken The "import" token
     */
    void parseImportStatement(LocatableToken importToken);
    
    /**
     * Parse a type definition (class, interface, enum).
     */
    void parseTypeDef();
    
    /**
     * Parse a type definition (class, interface, enum) starting with the given token.
     * 
     * @param firstToken The first token of the type definition
     */
    void parseTypeDef(LocatableToken firstToken);
    
    /**
     * Parse a type body.
     * 
     * @param tdType The type definition type
     * @param token The token that starts the type body
     * @return The last token seen during parsing
     */
    LocatableToken parseTypeBody(int tdType, LocatableToken token);

    int parseTypeDefBegin();

    LocatableToken parseTypeDefPart2(boolean b);

    void parseClassElement(LocatableToken nextToken);

    LocatableToken parseStatement(LocatableToken last, boolean b);

    boolean parseTypeSpec(boolean b, boolean b1, List<LocatableToken> ll);

    void parseClassBody();

    void parseExpression();

    LocatableToken parseVariableDeclarations();

    boolean parseTypeSpec(boolean processArray);

    void parseMethodParamsBody();
}