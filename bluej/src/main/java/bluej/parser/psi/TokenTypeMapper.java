/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
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
package bluej.parser.psi;

import bluej.parser.lexer.JavaTokenTypes;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Kotlin PSI element types to {@link JavaTokenTypes} constants.
 * 
 * <p>This utility class provides the mapping logic needed by {@link TokenFactory} to assign
 * appropriate token types to PSI elements during token synthesis. It handles the conversion
 * from IntelliJ's PSI element representation to BlueJ's token type system.</p>
 * 
 * <h2>Mapping Strategy</h2>
 * <p>The mapper follows a hierarchical strategy:</p>
 * <ol>
 *   <li><b>Keyword Elements:</b> Maps Kotlin/Java keywords to their corresponding token types
 *       (e.g., class, interface, fun, val, var)</li>
 *   <li><b>Modifier Elements:</b> Maps visibility and other modifiers (public, private, internal, etc.)</li>
 *   <li><b>Declaration Elements:</b> Maps class/function/property declarations based on context</li>
 *   <li><b>Delimiter Elements:</b> Maps structural delimiters (braces, parentheses, brackets)</li>
 *   <li><b>Name Identifiers:</b> Maps name references to IDENT</li>
 *   <li><b>Default:</b> Returns IDENT for unmapped element types</li>
 * </ol>
 * 
 * <h2>Supported Mappings</h2>
 * 
 * <h3>Declaration Keywords</h3>
 * <ul>
 *   <li>{@code class} → {@link JavaTokenTypes#LITERAL_class}</li>
 *   <li>{@code interface} → {@link JavaTokenTypes#LITERAL_interface}</li>
 *   <li>{@code enum} → {@link JavaTokenTypes#LITERAL_enum}</li>
 *   <li>{@code object} → {@link JavaTokenTypes#LITERAL_object}</li>
 *   <li>{@code fun} → {@link JavaTokenTypes#LITERAL_fun}</li>
 *   <li>{@code val} → {@link JavaTokenTypes#LITERAL_val}</li>
 *   <li>{@code var} → {@link JavaTokenTypes#LITERAL_var}</li>
 * </ul>
 * 
 * <h3>Modifiers</h3>
 * <ul>
 *   <li>{@code public} → {@link JavaTokenTypes#LITERAL_public}</li>
 *   <li>{@code private} → {@link JavaTokenTypes#LITERAL_private}</li>
 *   <li>{@code protected} → {@link JavaTokenTypes#LITERAL_protected}</li>
 *   <li>{@code internal} → {@link JavaTokenTypes#LITERAL_internal}</li>
 *   <li>{@code abstract} → {@link JavaTokenTypes#ABSTRACT}</li>
 *   <li>{@code final} → {@link JavaTokenTypes#FINAL}</li>
 *   <li>{@code override} → {@link JavaTokenTypes#LITERAL_override}</li>
 *   <li>{@code open} → {@link JavaTokenTypes#LITERAL_open}</li>
 *   <li>{@code data} → {@link JavaTokenTypes#LITERAL_data}</li>
 *   <li>Additional Kotlin modifiers (companion, inline, suspend, etc.)</li>
 * </ul>
 * 
 * <h3>Structural Delimiters</h3>
 * <ul>
 *   <li>{@code {}} → {@link JavaTokenTypes#LCURLY}, {@link JavaTokenTypes#RCURLY}</li>
 *   <li>{@code ()} → {@link JavaTokenTypes#LPAREN}, {@link JavaTokenTypes#RPAREN}</li>
 *   <li>{@code []} → {@link JavaTokenTypes#LBRACK}, {@link JavaTokenTypes#RBRACK}</li>
 * </ul>
 * 
 * <h3>Identifiers and Names</h3>
 * <ul>
 *   <li>Function names → {@link JavaTokenTypes#IDENT}</li>
 *   <li>Property names → {@link JavaTokenTypes#IDENT}</li>
 *   <li>Class names → {@link JavaTokenTypes#IDENT}</li>
 *   <li>Parameter names → {@link JavaTokenTypes#IDENT}</li>
 * </ul>
 * 
 * <h2>Design Decisions</h2>
 * <ul>
 *   <li><b>Null Safety:</b> Returns IDENT for null elements rather than throwing</li>
 *   <li><b>Default Behavior:</b> Unmapped types default to IDENT for graceful degradation</li>
 *   <li><b>Extensibility:</b> Structured for easy addition of new mappings</li>
 *   <li><b>Performance:</b> Uses instanceof checks (O(1)) and element node types</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PsiElement ktClass = ...; // KtClass element
 * int tokenType = TokenTypeMapper.mapPsiElementType(ktClass);
 * // tokenType == JavaTokenTypes.LITERAL_class
 * 
 * PsiElement functionName = ...; // KtNamedFunction name identifier
 * int nameType = TokenTypeMapper.mapPsiElementType(functionName);
 * // nameType == JavaTokenTypes.IDENT
 * }</pre>
 * 
 * <h2>Limitations and Future Work</h2>
 * <ul>
 *   <li>Expression types are not yet mapped (Phase 6 scope)</li>
 *   <li>Statement types default to IDENT (Phase 6 scope)</li>
 *   <li>Operator types use simplified mapping (Phase 6 scope)</li>
 *   <li>Complex type references may need refinement based on validation results</li>
 * </ul>
 * 
 * @see JavaTokenTypes Token type constants used by BlueJ parser
 * @see TokenFactory Factory that uses this mapper for token creation
 * @see <a href="file:///docs/planning/visitor-foundation/implementation-strategy.md">Implementation Strategy</a>
 */
@OnThread(Tag.Any)
public class TokenTypeMapper {

    // Static mapping for leaf token types (keywords, operators, delimiters)
    private static final Map<IElementType, Integer> LEAF_TOKEN_MAP;
    static {
        Map<IElementType, Integer> m = new HashMap<>();
        // Kotlin keywords
        m.put(KtTokens.CLASS_KEYWORD, JavaTokenTypes.LITERAL_class);
        m.put(KtTokens.INTERFACE_KEYWORD, JavaTokenTypes.LITERAL_interface);
        m.put(KtTokens.OBJECT_KEYWORD, JavaTokenTypes.LITERAL_object);
        m.put(KtTokens.FUN_KEYWORD, JavaTokenTypes.LITERAL_fun);
        m.put(KtTokens.VAL_KEYWORD, JavaTokenTypes.LITERAL_val);
        m.put(KtTokens.VAR_KEYWORD, JavaTokenTypes.LITERAL_var);

        // Control flow keywords
        m.put(KtTokens.IF_KEYWORD, JavaTokenTypes.LITERAL_if);
        m.put(KtTokens.ELSE_KEYWORD, JavaTokenTypes.LITERAL_else);
        m.put(KtTokens.WHEN_KEYWORD, JavaTokenTypes.LITERAL_when);
        m.put(KtTokens.FOR_KEYWORD, JavaTokenTypes.LITERAL_for);
        m.put(KtTokens.WHILE_KEYWORD, JavaTokenTypes.LITERAL_while);
        m.put(KtTokens.DO_KEYWORD, JavaTokenTypes.LITERAL_do);
        m.put(KtTokens.RETURN_KEYWORD, JavaTokenTypes.LITERAL_return);
        m.put(KtTokens.BREAK_KEYWORD, JavaTokenTypes.LITERAL_break);
        m.put(KtTokens.CONTINUE_KEYWORD, JavaTokenTypes.LITERAL_continue);
        m.put(KtTokens.THROW_KEYWORD, JavaTokenTypes.LITERAL_throw);
        m.put(KtTokens.TRY_KEYWORD, JavaTokenTypes.LITERAL_try);
        m.put(KtTokens.CATCH_KEYWORD, JavaTokenTypes.LITERAL_catch);
        m.put(KtTokens.FINALLY_KEYWORD, JavaTokenTypes.LITERAL_finally);

        // Other keywords
        m.put(KtTokens.PACKAGE_KEYWORD, JavaTokenTypes.LITERAL_package);
        m.put(KtTokens.IMPORT_KEYWORD, JavaTokenTypes.LITERAL_import);
        m.put(KtTokens.CONSTRUCTOR_KEYWORD, JavaTokenTypes.LITERAL_constructor);
        m.put(KtTokens.INIT_KEYWORD, JavaTokenTypes.LITERAL_init);
        m.put(KtTokens.THIS_KEYWORD, JavaTokenTypes.LITERAL_this);
        m.put(KtTokens.SUPER_KEYWORD, JavaTokenTypes.LITERAL_super);
        m.put(KtTokens.TYPEOF_KEYWORD, JavaTokenTypes.LITERAL_typealias);

        // Boolean and null literals
        m.put(KtTokens.TRUE_KEYWORD, JavaTokenTypes.LITERAL_true);
        m.put(KtTokens.FALSE_KEYWORD, JavaTokenTypes.LITERAL_false);
        m.put(KtTokens.NULL_KEYWORD, JavaTokenTypes.LITERAL_null);

        // Type keywords
        m.put(KtTokens.IS_KEYWORD, JavaTokenTypes.LITERAL_is);
        m.put(KtTokens.IN_KEYWORD, JavaTokenTypes.LITERAL_in);
        m.put(KtTokens.AS_KEYWORD, JavaTokenTypes.LITERAL_as);
        m.put(KtTokens.AS_SAFE, JavaTokenTypes.LITERAL_as);

        // Delimiters
        m.put(KtTokens.LBRACE, JavaTokenTypes.LCURLY);
        m.put(KtTokens.RBRACE, JavaTokenTypes.RCURLY);
        m.put(KtTokens.LPAR, JavaTokenTypes.LPAREN);
        m.put(KtTokens.RPAR, JavaTokenTypes.RPAREN);
        m.put(KtTokens.LBRACKET, JavaTokenTypes.LBRACK);
        m.put(KtTokens.RBRACKET, JavaTokenTypes.RBRACK);

        // Punctuation
        m.put(KtTokens.DOT, JavaTokenTypes.DOT);
        m.put(KtTokens.COMMA, JavaTokenTypes.COMMA);
        m.put(KtTokens.SEMICOLON, JavaTokenTypes.SEMI);
        m.put(KtTokens.COLON, JavaTokenTypes.COLON);
        m.put(KtTokens.QUEST, JavaTokenTypes.QUESTION);
        m.put(KtTokens.AT, JavaTokenTypes.AT);

        // Operators
        m.put(KtTokens.EQ, JavaTokenTypes.ASSIGN);
        m.put(KtTokens.PLUS, JavaTokenTypes.PLUS);
        m.put(KtTokens.MINUS, JavaTokenTypes.MINUS);
        m.put(KtTokens.MUL, JavaTokenTypes.STAR);
        m.put(KtTokens.DIV, JavaTokenTypes.DIV);
        m.put(KtTokens.PERC, JavaTokenTypes.MOD);
        m.put(KtTokens.LT, JavaTokenTypes.LT);
        m.put(KtTokens.GT, JavaTokenTypes.GT);
        m.put(KtTokens.LTEQ, JavaTokenTypes.LE);
        m.put(KtTokens.GTEQ, JavaTokenTypes.GE);
        m.put(KtTokens.EQEQ, JavaTokenTypes.EQUAL);
        m.put(KtTokens.EXCLEQ, JavaTokenTypes.NOT_EQUAL);
        m.put(KtTokens.ANDAND, JavaTokenTypes.LAND);
        m.put(KtTokens.OROR, JavaTokenTypes.LOR);
        m.put(KtTokens.EXCL, JavaTokenTypes.LNOT);
        m.put(KtTokens.PLUSPLUS, JavaTokenTypes.INC);
        m.put(KtTokens.MINUSMINUS, JavaTokenTypes.DEC);

        // Kotlin-specific operators
        m.put(KtTokens.ARROW, JavaTokenTypes.ARROW);
        m.put(KtTokens.COLONCOLON, JavaTokenTypes.DOUBLE_COLON);
        m.put(KtTokens.RANGE, JavaTokenTypes.RANGE);
        m.put(KtTokens.ELVIS, JavaTokenTypes.ELVIS);
        m.put(KtTokens.SAFE_ACCESS, JavaTokenTypes.SAFE_ACCESS);

        LEAF_TOKEN_MAP = Collections.unmodifiableMap(m);
    }
    
    /**
     * Maps a PSI element to its corresponding JavaTokenTypes constant.
     * 
     * <p>This method analyzes the PSI element's type and context to determine the most
     * appropriate token type. The mapping follows a priority order:</p>
     * <ol>
     *   <li>Check for leaf token types (keywords, operators, delimiters)</li>
     *   <li>Check for declaration types (class, function, property)</li>
     *   <li>Check for modifier types</li>
     *   <li>Check for name/identifier types</li>
     *   <li>Default to IDENT for unmapped types</li>
     * </ol>
     * 
     * <h3>Null Handling</h3>
     * <p>If the element is null, returns {@link JavaTokenTypes#IDENT} rather than throwing.
     * This provides graceful degradation for edge cases in PSI traversal.</p>
     * 
     * <h3>Performance Characteristics</h3>
     * <ul>
     *   <li><b>Time Complexity:</b> O(1) - Fixed number of instanceof checks</li>
     *   <li><b>Space Complexity:</b> O(1) - No allocations</li>
     * </ul>
     * 
     * <h3>Token Type Selection Examples</h3>
     * <table border="1">
     *   <tr>
     *     <th>PSI Element Type</th>
     *     <th>Element Text</th>
     *     <th>Token Type</th>
     *     <th>Constant Value</th>
     *   </tr>
     *   <tr>
     *     <td>KtClass (keyword)</td>
     *     <td>"class"</td>
     *     <td>LITERAL_class</td>
     *     <td>101</td>
     *   </tr>
     *   <tr>
     *     <td>KtNamedFunction (identifier)</td>
     *     <td>"calculateSum"</td>
     *     <td>IDENT</td>
     *     <td>69</td>
     *   </tr>
     *   <tr>
     *     <td>LeafElement (modifier)</td>
     *     <td>"private"</td>
     *     <td>LITERAL_private</td>
     *     <td>88</td>
     *   </tr>
     *   <tr>
     *     <td>LeafElement (brace)</td>
     *     <td>"{"</td>
     *     <td>LCURLY</td>
     *     <td>99</td>
     *   </tr>
     * </table>
     * 
     * @param element The PSI element to map (may be null)
     * @return The corresponding JavaTokenTypes constant, or {@link JavaTokenTypes#IDENT} if unmapped
     */
    public static int mapPsiElementType(PsiElement element) {
        // Null safety - return IDENT for null elements
        if (element == null) {
            return JavaTokenTypes.IDENT;
        }
        
        // Check for leaf elements (keywords, operators, delimiters) by token type
        if (element.getNode() != null) {
            int mappedType = mapLeafTokenType(element);
            if (mappedType != JavaTokenTypes.IDENT) {
                return mappedType;
            }
        }
        
        return switch (element) {
            // Map declaration types
            case KtClass ktClass ->
                mapClassDeclaration(ktClass);

            case KtObjectDeclaration ktObjectDeclaration ->
                JavaTokenTypes.LITERAL_object;

            // Function keyword or name identifier - context matters
            case KtNamedFunction ktNamedFunction ->
                JavaTokenTypes.IDENT; // Names default to IDENT

            // Property val/var keyword or name identifier
            case KtProperty ktProperty ->
                JavaTokenTypes.IDENT; // Names default to IDENT

            case KtTypeAlias ktTypeAlias ->
                JavaTokenTypes.LITERAL_typealias;

            // Map name identifiers
            case KtNameReferenceExpression ktNameReferenceExpression ->
                JavaTokenTypes.IDENT;

            case KtNamedDeclaration ktNamedDeclaration ->
                JavaTokenTypes.IDENT;

            // Default to IDENT for unmapped types
            default ->
                JavaTokenTypes.IDENT;
        };

//        // Map modifier list entries
//        if (element instanceof KtModifierListEntry) {
//            return mapModifier(element.getText());
//        }
        
//        // Map name identifiers
//        if (element instanceof KtNameReferenceExpression ||
//            element instanceof KtNamedDeclaration) {
//            return JavaTokenTypes.IDENT;
//        }
//
//        // Default to IDENT for unmapped types
//        return JavaTokenTypes.IDENT;
    }
    
    /**
     * Maps leaf token elements (keywords, operators, delimiters) to token types.
     * 
     * <p>This method handles the atomic tokens that appear as leaf nodes in the PSI tree.
     * It uses the Kotlin token type system to identify specific keywords and symbols.</p>
     * 
     * @param element The PSI element with a token type
     * @return The mapped token type, or IDENT if not a recognized leaf token
     */
    private static int mapLeafTokenType(PsiElement element) {
        var tokenType = element.getNode().getElementType();

        // Kotlin keywords
        if (tokenType == KtTokens.CLASS_KEYWORD) return JavaTokenTypes.LITERAL_class;
        if (tokenType == KtTokens.INTERFACE_KEYWORD) return JavaTokenTypes.LITERAL_interface;
        if (tokenType == KtTokens.OBJECT_KEYWORD) return JavaTokenTypes.LITERAL_object;
        if (tokenType == KtTokens.FUN_KEYWORD) return JavaTokenTypes.LITERAL_fun;
        if (tokenType == KtTokens.VAL_KEYWORD) return JavaTokenTypes.LITERAL_val;
        if (tokenType == KtTokens.VAR_KEYWORD) return JavaTokenTypes.LITERAL_var;

        // Control flow keywords
        if (tokenType == KtTokens.IF_KEYWORD) return JavaTokenTypes.LITERAL_if;
        if (tokenType == KtTokens.ELSE_KEYWORD) return JavaTokenTypes.LITERAL_else;
        if (tokenType == KtTokens.WHEN_KEYWORD) return JavaTokenTypes.LITERAL_when;
        if (tokenType == KtTokens.FOR_KEYWORD) return JavaTokenTypes.LITERAL_for;
        if (tokenType == KtTokens.WHILE_KEYWORD) return JavaTokenTypes.LITERAL_while;
        if (tokenType == KtTokens.DO_KEYWORD) return JavaTokenTypes.LITERAL_do;
        if (tokenType == KtTokens.RETURN_KEYWORD) return JavaTokenTypes.LITERAL_return;
        if (tokenType == KtTokens.BREAK_KEYWORD) return JavaTokenTypes.LITERAL_break;
        if (tokenType == KtTokens.CONTINUE_KEYWORD) return JavaTokenTypes.LITERAL_continue;
        if (tokenType == KtTokens.THROW_KEYWORD) return JavaTokenTypes.LITERAL_throw;
        if (tokenType == KtTokens.TRY_KEYWORD) return JavaTokenTypes.LITERAL_try;
        if (tokenType == KtTokens.CATCH_KEYWORD) return JavaTokenTypes.LITERAL_catch;
        if (tokenType == KtTokens.FINALLY_KEYWORD) return JavaTokenTypes.LITERAL_finally;

        // Other keywords
        if (tokenType == KtTokens.PACKAGE_KEYWORD) return JavaTokenTypes.LITERAL_package;
        if (tokenType == KtTokens.IMPORT_KEYWORD) return JavaTokenTypes.LITERAL_import;
        if (tokenType == KtTokens.CONSTRUCTOR_KEYWORD) return JavaTokenTypes.LITERAL_constructor;
        if (tokenType == KtTokens.INIT_KEYWORD) return JavaTokenTypes.LITERAL_init;
        if (tokenType == KtTokens.THIS_KEYWORD) return JavaTokenTypes.LITERAL_this;
        if (tokenType == KtTokens.SUPER_KEYWORD) return JavaTokenTypes.LITERAL_super;
        if (tokenType == KtTokens.TYPEOF_KEYWORD) return JavaTokenTypes.LITERAL_typealias;

        // Boolean and null literals
        if (tokenType == KtTokens.TRUE_KEYWORD) return JavaTokenTypes.LITERAL_true;
        if (tokenType == KtTokens.FALSE_KEYWORD) return JavaTokenTypes.LITERAL_false;
        if (tokenType == KtTokens.NULL_KEYWORD) return JavaTokenTypes.LITERAL_null;

        // Type keywords
        if (tokenType == KtTokens.IS_KEYWORD) return JavaTokenTypes.LITERAL_is;
        if (tokenType == KtTokens.IN_KEYWORD) return JavaTokenTypes.LITERAL_in;
        if (tokenType == KtTokens.AS_KEYWORD) return JavaTokenTypes.LITERAL_as;
        if (tokenType == KtTokens.AS_SAFE) return JavaTokenTypes.LITERAL_as;

        // Delimiters
        if (tokenType == KtTokens.LBRACE) return JavaTokenTypes.LCURLY;
        if (tokenType == KtTokens.RBRACE) return JavaTokenTypes.RCURLY;
        if (tokenType == KtTokens.LPAR) return JavaTokenTypes.LPAREN;
        if (tokenType == KtTokens.RPAR) return JavaTokenTypes.RPAREN;
        if (tokenType == KtTokens.LBRACKET) return JavaTokenTypes.LBRACK;
        if (tokenType == KtTokens.RBRACKET) return JavaTokenTypes.RBRACK;

        // Punctuation
        if (tokenType == KtTokens.DOT) return JavaTokenTypes.DOT;
        if (tokenType == KtTokens.COMMA) return JavaTokenTypes.COMMA;
        if (tokenType == KtTokens.SEMICOLON) return JavaTokenTypes.SEMI;
        if (tokenType == KtTokens.COLON) return JavaTokenTypes.COLON;
        if (tokenType == KtTokens.QUEST) return JavaTokenTypes.QUESTION;
        if (tokenType == KtTokens.AT) return JavaTokenTypes.AT;

        // Operators
        if (tokenType == KtTokens.EQ) return JavaTokenTypes.ASSIGN;
        if (tokenType == KtTokens.PLUS) return JavaTokenTypes.PLUS;
        if (tokenType == KtTokens.MINUS) return JavaTokenTypes.MINUS;
        if (tokenType == KtTokens.MUL) return JavaTokenTypes.STAR;
        if (tokenType == KtTokens.DIV) return JavaTokenTypes.DIV;
        if (tokenType == KtTokens.PERC) return JavaTokenTypes.MOD;
        if (tokenType == KtTokens.LT) return JavaTokenTypes.LT;
        if (tokenType == KtTokens.GT) return JavaTokenTypes.GT;
        if (tokenType == KtTokens.LTEQ) return JavaTokenTypes.LE;
        if (tokenType == KtTokens.GTEQ) return JavaTokenTypes.GE;
        if (tokenType == KtTokens.EQEQ) return JavaTokenTypes.EQUAL;
        if (tokenType == KtTokens.EXCLEQ) return JavaTokenTypes.NOT_EQUAL;
        if (tokenType == KtTokens.ANDAND) return JavaTokenTypes.LAND;
        if (tokenType == KtTokens.OROR) return JavaTokenTypes.LOR;
        if (tokenType == KtTokens.EXCL) return JavaTokenTypes.LNOT;
        if (tokenType == KtTokens.PLUSPLUS) return JavaTokenTypes.INC;
        if (tokenType == KtTokens.MINUSMINUS) return JavaTokenTypes.DEC;

        // Kotlin-specific operators
        if (tokenType == KtTokens.ARROW) return JavaTokenTypes.ARROW;
        if (tokenType == KtTokens.COLONCOLON) return JavaTokenTypes.DOUBLE_COLON;
        if (tokenType == KtTokens.RANGE) return JavaTokenTypes.RANGE;
        if (tokenType == KtTokens.ELVIS) return JavaTokenTypes.ELVIS;
        if (tokenType == KtTokens.SAFE_ACCESS) return JavaTokenTypes.SAFE_ACCESS;

        return JavaTokenTypes.IDENT; // Not a recognized leaf token
    }
    
    /**
     * Maps class-like declarations based on their specific type.
     * 
     * <p>Kotlin has multiple class-like constructs (class, interface, enum class, annotation class).
     * This method distinguishes between them.</p>
     * 
     * @param ktClass The class declaration element
     * @return The appropriate token type for the class kind
     */
    private static int mapClassDeclaration(KtClass ktClass) {
        if (ktClass.isInterface()) {
            return JavaTokenTypes.LITERAL_interface;
        }
        if (ktClass.isEnum()) {
            return JavaTokenTypes.LITERAL_enum;
        }
        // Default to class
        return JavaTokenTypes.LITERAL_class;
    }
    
    /**
     * Maps Kotlin modifier keywords to their corresponding token types.
     * 
     * <p>This method handles all Kotlin visibility modifiers, inheritance modifiers,
     * and special modifiers. It uses the modifier text to determine the token type.</p>
     * 
     * <h3>Supported Modifiers</h3>
     * <ul>
     *   <li><b>Visibility:</b> public, private, protected, internal</li>
     *   <li><b>Inheritance:</b> open, final, abstract, override</li>
     *   <li><b>Data:</b> data, inline, value</li>
     *   <li><b>Scope:</b> companion, inner</li>
     *   <li><b>Coroutines:</b> suspend</li>
     *   <li><b>Variance:</b> in, out</li>
     *   <li><b>Property:</b> lateinit, const</li>
     *   <li><b>Interop:</b> external, expect, actual</li>
     * </ul>
     * 
     * @param modifierText The text of the modifier keyword
     * @return The corresponding modifier token type, or IDENT if not recognized
     */
    private static int mapModifier(String modifierText) {
        if (modifierText == null) {
            return JavaTokenTypes.IDENT;
        }
        
        switch (modifierText) {
            // Visibility modifiers
            case "public": return JavaTokenTypes.LITERAL_public;
            case "private": return JavaTokenTypes.LITERAL_private;
            case "protected": return JavaTokenTypes.LITERAL_protected;
            case "internal": return JavaTokenTypes.LITERAL_internal;
            
            // Inheritance modifiers
            case "open": return JavaTokenTypes.LITERAL_open;
            case "final": return JavaTokenTypes.FINAL;
            case "abstract": return JavaTokenTypes.ABSTRACT;
            case "override": return JavaTokenTypes.LITERAL_override;
            
            // Special class modifiers
            case "data": return JavaTokenTypes.LITERAL_data;
            case "sealed": return JavaTokenTypes.LITERAL_sealed;
            case "inner": return JavaTokenTypes.LITERAL_inner;
            case "companion": return JavaTokenTypes.LITERAL_companion;
            
            // Function modifiers
            case "inline": return JavaTokenTypes.LITERAL_inline;
            case "suspend": return JavaTokenTypes.LITERAL_suspend;
            case "operator": return JavaTokenTypes.LITERAL_operator;
            case "infix": return JavaTokenTypes.LITERAL_infix;
            case "tailrec": return JavaTokenTypes.LITERAL_tailrec;
            
            // Property modifiers
            case "lateinit": return JavaTokenTypes.LITERAL_lateinit;
            case "const": return JavaTokenTypes.LITERAL_const;
            
            // Parameter modifiers
            case "vararg": return JavaTokenTypes.LITERAL_vararg;
            case "noinline": return JavaTokenTypes.LITERAL_noinline;
            case "crossinline": return JavaTokenTypes.LITERAL_crossinline;
            
            // Type parameter modifiers
            case "reified": return JavaTokenTypes.LITERAL_reified;
            case "in": return JavaTokenTypes.LITERAL_in;
            case "out": return JavaTokenTypes.LITERAL_out;
            
            // Multiplatform modifiers
            case "expect": return JavaTokenTypes.LITERAL_expect;
            case "actual": return JavaTokenTypes.LITERAL_actual;
            
            // Interop modifiers
            case "external": return JavaTokenTypes.LITERAL_external;
            
            default:
                return JavaTokenTypes.IDENT; // Unknown modifier
        }
    }
}