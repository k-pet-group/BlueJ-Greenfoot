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
package bluej.parser.kotlin;

import bluej.parser.Token;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtTokens;

/**
 * Kotlin token type mapping. Converts PSI KtTokens (IElementType) to BlueJ
 * Token.TokenType for CSS class assignment, and to integer token type constants
 * compatible with JavaTokenTypes for LocatableToken.
 *
 * <p>Token type constants start at 200 to avoid collision with JavaTokenTypes.</p>
 *
 * @author BlueJ Team
 */
public final class KotlinToken
{
    private KotlinToken() {} // utility class

    // -----------------------------------------------------------------------
    // Integer token type constants (parallel to JavaTokenTypes, starting at 200)
    // -----------------------------------------------------------------------

    // --- Hard keywords (200-229) ---
    public static final int KW_PACKAGE = 200;
    public static final int KW_AS = 201;
    public static final int KW_TYPE_ALIAS = 202;
    public static final int KW_CLASS = 203;
    public static final int KW_THIS = 204;
    public static final int KW_SUPER = 205;
    public static final int KW_VAL = 206;
    public static final int KW_VAR = 207;
    public static final int KW_FUN = 208;
    public static final int KW_FOR = 209;
    public static final int KW_NULL = 210;
    public static final int KW_TRUE = 211;
    public static final int KW_FALSE = 212;
    public static final int KW_IS = 213;
    public static final int KW_IN = 214;
    public static final int KW_THROW = 215;
    public static final int KW_RETURN = 216;
    public static final int KW_BREAK = 217;
    public static final int KW_CONTINUE = 218;
    public static final int KW_OBJECT = 219;
    public static final int KW_IF = 220;
    public static final int KW_TRY = 221;
    public static final int KW_ELSE = 222;
    public static final int KW_WHILE = 223;
    public static final int KW_DO = 224;
    public static final int KW_WHEN = 225;
    public static final int KW_INTERFACE = 226;
    public static final int KW_TYPEOF = 227;
    public static final int KW_NOT_IN = 228;
    public static final int KW_NOT_IS = 229;

    // --- Soft / modifier keywords (230-259) ---
    public static final int KW_IMPORT = 230;
    public static final int KW_WHERE = 231;
    public static final int KW_BY = 232;
    public static final int KW_GET = 233;
    public static final int KW_SET = 234;
    public static final int KW_CONSTRUCTOR = 235;
    public static final int KW_INIT = 236;
    public static final int KW_ABSTRACT = 237;
    public static final int KW_ENUM = 238;
    public static final int KW_OPEN = 239;
    public static final int KW_INNER = 240;
    public static final int KW_OVERRIDE = 241;
    public static final int KW_PRIVATE = 242;
    public static final int KW_PUBLIC = 243;
    public static final int KW_INTERNAL = 244;
    public static final int KW_PROTECTED = 245;
    public static final int KW_OUT = 246;
    public static final int KW_VARARG = 247;
    public static final int KW_COMPANION = 248;
    public static final int KW_SEALED = 249;
    public static final int KW_FINAL = 250;
    public static final int KW_LATEINIT = 251;
    public static final int KW_DATA = 252;
    public static final int KW_VALUE = 253;
    public static final int KW_INLINE = 254;
    public static final int KW_TAILREC = 255;
    public static final int KW_OPERATOR = 256;
    public static final int KW_INFIX = 257;
    public static final int KW_CONST = 258;
    public static final int KW_SUSPEND = 259;
    public static final int KW_ANNOTATION = 260;
    public static final int KW_REIFIED = 261;
    public static final int KW_EXTERNAL = 262;
    public static final int KW_CROSSINLINE = 263;
    public static final int KW_NOINLINE = 264;
    public static final int KW_EXPECT = 265;
    public static final int KW_ACTUAL = 266;
    public static final int KW_CONTRACT = 267;

    // --- Literals & strings (270-279) ---
    public static final int INTEGER_LITERAL = 270;
    public static final int FLOAT_LITERAL = 271;
    public static final int CHARACTER_LITERAL = 272;
    public static final int OPEN_QUOTE = 273;
    public static final int CLOSING_QUOTE = 274;
    public static final int REGULAR_STRING_PART = 275;
    public static final int ESCAPE_SEQUENCE = 276;
    public static final int SHORT_TEMPLATE_ENTRY_START = 277;
    public static final int LONG_TEMPLATE_ENTRY_START = 278;
    public static final int LONG_TEMPLATE_ENTRY_END = 279;

    // --- Comments (280-284) ---
    public static final int EOL_COMMENT = 280;
    public static final int BLOCK_COMMENT = 281;
    public static final int DOC_COMMENT = 282;
    public static final int SHEBANG_COMMENT = 283;

    // --- Identifiers (285-286) ---
    public static final int IDENTIFIER = 285;
    public static final int FIELD_IDENTIFIER = 286;

    // --- Delimiters (290-299) ---
    public static final int LPAR = 290;
    public static final int RPAR = 291;
    public static final int LBRACE = 292;
    public static final int RBRACE = 293;
    public static final int LBRACKET = 294;
    public static final int RBRACKET = 295;
    public static final int DOT = 296;
    public static final int COMMA = 297;
    public static final int COLON = 298;
    public static final int SEMICOLON = 299;

    // --- Operators (300-329) ---
    public static final int PLUS = 300;
    public static final int MINUS = 301;
    public static final int MUL = 302;
    public static final int DIV = 303;
    public static final int PERC = 304;
    public static final int PLUSPLUS = 305;
    public static final int MINUSMINUS = 306;
    public static final int EXCL = 307;
    public static final int LT = 308;
    public static final int GT = 309;
    public static final int LTEQ = 310;
    public static final int GTEQ = 311;
    public static final int EQEQ = 312;
    public static final int EXCLEQ = 313;
    public static final int EQEQEQ = 314;
    public static final int EXCLEQEQEQ = 315;
    public static final int ANDAND = 316;
    public static final int OROR = 317;
    public static final int AND = 318;
    public static final int EQ = 319;
    public static final int PLUSEQ = 320;
    public static final int MINUSEQ = 321;
    public static final int MULTEQ = 322;
    public static final int DIVEQ = 323;
    public static final int PERCEQ = 324;
    public static final int ARROW = 325;
    public static final int DOUBLE_ARROW = 326;
    public static final int RANGE = 327;
    public static final int RANGE_UNTIL = 328;
    public static final int COLONCOLON = 329;

    // --- Kotlin-specific operators (330-339) ---
    public static final int SAFE_ACCESS = 330;
    public static final int ELVIS = 331;
    public static final int EXCLEXCL = 332;
    public static final int QUEST = 333;
    public static final int AS_SAFE = 334;
    public static final int AT = 335;
    public static final int HASH = 336;

    // --- Special (340+) ---
    public static final int WHITE_SPACE = 340;
    public static final int DANGLING_NEWLINE = 341;
    public static final int EOF = 1;  // Match JavaTokenTypes.EOF
    public static final int RESERVED = 342;
    public static final int INVALID = 343;

    // -----------------------------------------------------------------------
    // Mapping: PSI IElementType → BlueJ integer token type
    // -----------------------------------------------------------------------

    /**
     * Map a PSI IElementType to a BlueJ integer token type.
     * Uses identity comparison since KtTokens fields are singleton instances.
     *
     * @param psiType the IElementType from KtTokens (may be null for EOF)
     * @return integer token type constant from this class
     */
    public static int mapTokenType(IElementType psiType)
    {
        if (psiType == null)
        {
            return EOF;
        }

        // --- Hard keywords ---
        if (psiType == KtTokens.PACKAGE_KEYWORD) return KW_PACKAGE;
        if (psiType == KtTokens.AS_KEYWORD) return KW_AS;
        if (psiType == KtTokens.TYPE_ALIAS_KEYWORD) return KW_TYPE_ALIAS;
        if (psiType == KtTokens.CLASS_KEYWORD) return KW_CLASS;
        if (psiType == KtTokens.THIS_KEYWORD) return KW_THIS;
        if (psiType == KtTokens.SUPER_KEYWORD) return KW_SUPER;
        if (psiType == KtTokens.VAL_KEYWORD) return KW_VAL;
        if (psiType == KtTokens.VAR_KEYWORD) return KW_VAR;
        if (psiType == KtTokens.FUN_KEYWORD) return KW_FUN;
        if (psiType == KtTokens.FOR_KEYWORD) return KW_FOR;
        if (psiType == KtTokens.NULL_KEYWORD) return KW_NULL;
        if (psiType == KtTokens.TRUE_KEYWORD) return KW_TRUE;
        if (psiType == KtTokens.FALSE_KEYWORD) return KW_FALSE;
        if (psiType == KtTokens.IS_KEYWORD) return KW_IS;
        if (psiType == KtTokens.IN_KEYWORD) return KW_IN;
        if (psiType == KtTokens.THROW_KEYWORD) return KW_THROW;
        if (psiType == KtTokens.RETURN_KEYWORD) return KW_RETURN;
        if (psiType == KtTokens.BREAK_KEYWORD) return KW_BREAK;
        if (psiType == KtTokens.CONTINUE_KEYWORD) return KW_CONTINUE;
        if (psiType == KtTokens.OBJECT_KEYWORD) return KW_OBJECT;
        if (psiType == KtTokens.IF_KEYWORD) return KW_IF;
        if (psiType == KtTokens.TRY_KEYWORD) return KW_TRY;
        if (psiType == KtTokens.ELSE_KEYWORD) return KW_ELSE;
        if (psiType == KtTokens.WHILE_KEYWORD) return KW_WHILE;
        if (psiType == KtTokens.DO_KEYWORD) return KW_DO;
        if (psiType == KtTokens.WHEN_KEYWORD) return KW_WHEN;
        if (psiType == KtTokens.INTERFACE_KEYWORD) return KW_INTERFACE;
        if (psiType == KtTokens.TYPEOF_KEYWORD) return KW_TYPEOF;
        if (psiType == KtTokens.NOT_IN) return KW_NOT_IN;
        if (psiType == KtTokens.NOT_IS) return KW_NOT_IS;

        // --- Soft / modifier keywords ---
        if (psiType == KtTokens.IMPORT_KEYWORD) return KW_IMPORT;
        if (psiType == KtTokens.WHERE_KEYWORD) return KW_WHERE;
        if (psiType == KtTokens.BY_KEYWORD) return KW_BY;
        if (psiType == KtTokens.GET_KEYWORD) return KW_GET;
        if (psiType == KtTokens.SET_KEYWORD) return KW_SET;
        if (psiType == KtTokens.CONSTRUCTOR_KEYWORD) return KW_CONSTRUCTOR;
        if (psiType == KtTokens.INIT_KEYWORD) return KW_INIT;
        if (psiType == KtTokens.ABSTRACT_KEYWORD) return KW_ABSTRACT;
        if (psiType == KtTokens.ENUM_KEYWORD) return KW_ENUM;
        if (psiType == KtTokens.OPEN_KEYWORD) return KW_OPEN;
        if (psiType == KtTokens.INNER_KEYWORD) return KW_INNER;
        if (psiType == KtTokens.OVERRIDE_KEYWORD) return KW_OVERRIDE;
        if (psiType == KtTokens.PRIVATE_KEYWORD) return KW_PRIVATE;
        if (psiType == KtTokens.PUBLIC_KEYWORD) return KW_PUBLIC;
        if (psiType == KtTokens.INTERNAL_KEYWORD) return KW_INTERNAL;
        if (psiType == KtTokens.PROTECTED_KEYWORD) return KW_PROTECTED;
        if (psiType == KtTokens.OUT_KEYWORD) return KW_OUT;
        if (psiType == KtTokens.VARARG_KEYWORD) return KW_VARARG;
        if (psiType == KtTokens.COMPANION_KEYWORD) return KW_COMPANION;
        if (psiType == KtTokens.SEALED_KEYWORD) return KW_SEALED;
        if (psiType == KtTokens.FINAL_KEYWORD) return KW_FINAL;
        if (psiType == KtTokens.LATEINIT_KEYWORD) return KW_LATEINIT;
        if (psiType == KtTokens.DATA_KEYWORD) return KW_DATA;
        if (psiType == KtTokens.VALUE_KEYWORD) return KW_VALUE;
        if (psiType == KtTokens.INLINE_KEYWORD) return KW_INLINE;
        if (psiType == KtTokens.TAILREC_KEYWORD) return KW_TAILREC;
        if (psiType == KtTokens.OPERATOR_KEYWORD) return KW_OPERATOR;
        if (psiType == KtTokens.INFIX_KEYWORD) return KW_INFIX;
        if (psiType == KtTokens.CONST_KEYWORD) return KW_CONST;
        if (psiType == KtTokens.SUSPEND_KEYWORD) return KW_SUSPEND;
        if (psiType == KtTokens.ANNOTATION_KEYWORD) return KW_ANNOTATION;
        if (psiType == KtTokens.REIFIED_KEYWORD) return KW_REIFIED;
        if (psiType == KtTokens.EXTERNAL_KEYWORD) return KW_EXTERNAL;
        if (psiType == KtTokens.CROSSINLINE_KEYWORD) return KW_CROSSINLINE;
        if (psiType == KtTokens.NOINLINE_KEYWORD) return KW_NOINLINE;
        if (psiType == KtTokens.EXPECT_KEYWORD) return KW_EXPECT;
        if (psiType == KtTokens.ACTUAL_KEYWORD) return KW_ACTUAL;
        if (psiType == KtTokens.CONTRACT_KEYWORD) return KW_CONTRACT;

        // --- Literals & strings ---
        if (psiType == KtTokens.INTEGER_LITERAL) return INTEGER_LITERAL;
        if (psiType == KtTokens.FLOAT_LITERAL) return FLOAT_LITERAL;
        if (psiType == KtTokens.CHARACTER_LITERAL) return CHARACTER_LITERAL;
        if (psiType == KtTokens.OPEN_QUOTE) return OPEN_QUOTE;
        if (psiType == KtTokens.CLOSING_QUOTE) return CLOSING_QUOTE;
        if (psiType == KtTokens.REGULAR_STRING_PART) return REGULAR_STRING_PART;
        if (psiType == KtTokens.ESCAPE_SEQUENCE) return ESCAPE_SEQUENCE;
        if (psiType == KtTokens.SHORT_TEMPLATE_ENTRY_START) return SHORT_TEMPLATE_ENTRY_START;
        if (psiType == KtTokens.LONG_TEMPLATE_ENTRY_START) return LONG_TEMPLATE_ENTRY_START;
        if (psiType == KtTokens.LONG_TEMPLATE_ENTRY_END) return LONG_TEMPLATE_ENTRY_END;

        // --- Comments ---
        if (psiType == KtTokens.EOL_COMMENT) return EOL_COMMENT;
        if (psiType == KtTokens.BLOCK_COMMENT) return BLOCK_COMMENT;
        if (psiType == KtTokens.DOC_COMMENT) return DOC_COMMENT;
        if (psiType == KtTokens.SHEBANG_COMMENT) return SHEBANG_COMMENT;

        // --- Identifiers ---
        if (psiType == KtTokens.IDENTIFIER) return IDENTIFIER;
        if (psiType == KtTokens.FIELD_IDENTIFIER) return FIELD_IDENTIFIER;

        // --- Delimiters ---
        if (psiType == KtTokens.LPAR) return LPAR;
        if (psiType == KtTokens.RPAR) return RPAR;
        if (psiType == KtTokens.LBRACE) return LBRACE;
        if (psiType == KtTokens.RBRACE) return RBRACE;
        if (psiType == KtTokens.LBRACKET) return LBRACKET;
        if (psiType == KtTokens.RBRACKET) return RBRACKET;
        if (psiType == KtTokens.DOT) return DOT;
        if (psiType == KtTokens.COMMA) return COMMA;
        if (psiType == KtTokens.COLON) return COLON;
        if (psiType == KtTokens.SEMICOLON) return SEMICOLON;

        // --- Operators ---
        if (psiType == KtTokens.PLUS) return PLUS;
        if (psiType == KtTokens.MINUS) return MINUS;
        if (psiType == KtTokens.MUL) return MUL;
        if (psiType == KtTokens.DIV) return DIV;
        if (psiType == KtTokens.PERC) return PERC;
        if (psiType == KtTokens.PLUSPLUS) return PLUSPLUS;
        if (psiType == KtTokens.MINUSMINUS) return MINUSMINUS;
        if (psiType == KtTokens.EXCL) return EXCL;
        if (psiType == KtTokens.LT) return LT;
        if (psiType == KtTokens.GT) return GT;
        if (psiType == KtTokens.LTEQ) return LTEQ;
        if (psiType == KtTokens.GTEQ) return GTEQ;
        if (psiType == KtTokens.EQEQ) return EQEQ;
        if (psiType == KtTokens.EXCLEQ) return EXCLEQ;
        if (psiType == KtTokens.EQEQEQ) return EQEQEQ;
        if (psiType == KtTokens.EXCLEQEQEQ) return EXCLEQEQEQ;
        if (psiType == KtTokens.ANDAND) return ANDAND;
        if (psiType == KtTokens.OROR) return OROR;
        if (psiType == KtTokens.AND) return AND;
        if (psiType == KtTokens.EQ) return EQ;
        if (psiType == KtTokens.PLUSEQ) return PLUSEQ;
        if (psiType == KtTokens.MINUSEQ) return MINUSEQ;
        if (psiType == KtTokens.MULTEQ) return MULTEQ;
        if (psiType == KtTokens.DIVEQ) return DIVEQ;
        if (psiType == KtTokens.PERCEQ) return PERCEQ;
        if (psiType == KtTokens.ARROW) return ARROW;
        if (psiType == KtTokens.DOUBLE_ARROW) return DOUBLE_ARROW;
        if (psiType == KtTokens.RANGE) return RANGE;
        if (psiType == KtTokens.RANGE_UNTIL) return RANGE_UNTIL;
        if (psiType == KtTokens.COLONCOLON) return COLONCOLON;

        // --- Kotlin-specific operators ---
        if (psiType == KtTokens.SAFE_ACCESS) return SAFE_ACCESS;
        if (psiType == KtTokens.ELVIS) return ELVIS;
        if (psiType == KtTokens.EXCLEXCL) return EXCLEXCL;
        if (psiType == KtTokens.QUEST) return QUEST;
        if (psiType == KtTokens.AS_SAFE) return AS_SAFE;
        if (psiType == KtTokens.AT) return AT;
        if (psiType == KtTokens.HASH) return HASH;

        // --- Special ---
        if (psiType == KtTokens.WHITE_SPACE) return WHITE_SPACE;
        if (psiType == KtTokens.DANGLING_NEWLINE) return DANGLING_NEWLINE;
        if (psiType == KtTokens.EOF) return EOF;
        if (psiType == KtTokens.RESERVED) return RESERVED;

        // Fallback for unknown IElementType
        return IDENTIFIER;
    }

    // -----------------------------------------------------------------------
    // Mapping: BlueJ integer token type → Token.TokenType (for CSS styling)
    // -----------------------------------------------------------------------

    /**
     * Map a BlueJ integer token type to Token.TokenType for CSS styling.
     * This determines the CSS class applied via {@link Token.TokenType#getCSSClass()}.
     *
     * @param tokenType integer constant from this class
     * @return Token.TokenType for CSS class assignment
     */
    public static Token.TokenType toDisplayType(int tokenType)
    {
        return switch (tokenType)
        {
            // KEYWORD1: Control flow + Modifiers (matches Java's role-based scheme)
            // -- Control flow --
            case KW_IF, KW_ELSE, KW_FOR, KW_WHILE, KW_DO, KW_WHEN,
                 KW_BREAK, KW_CONTINUE, KW_RETURN, KW_THROW, KW_TRY,
                 KW_IS, KW_NOT_IS, KW_IN, KW_NOT_IN, KW_AS, KW_TYPEOF,
            // -- Visibility modifiers --
                 KW_PRIVATE, KW_PUBLIC, KW_INTERNAL, KW_PROTECTED,
            // -- Other modifiers --
                 KW_ABSTRACT, KW_OPEN, KW_FINAL, KW_SEALED,
                 KW_OVERRIDE, KW_INNER, KW_LATEINIT,
                 KW_DATA, KW_VALUE, KW_INLINE, KW_TAILREC,
                 KW_OPERATOR, KW_INFIX, KW_CONST, KW_SUSPEND,
                 KW_ANNOTATION, KW_REIFIED, KW_EXTERNAL,
                 KW_CROSSINLINE, KW_NOINLINE,
                 KW_EXPECT, KW_ACTUAL, KW_CONTRACT,
                 KW_VARARG, KW_OUT
                -> Token.TokenType.KEYWORD1;

            // KEYWORD2: Declarations (type, member, structural)
            // -- Type declarations --
            case KW_CLASS, KW_INTERFACE, KW_ENUM, KW_OBJECT,
                 KW_TYPE_ALIAS, KW_PACKAGE, KW_IMPORT,
            // -- Member declarations --
                 KW_FUN, KW_VAL, KW_VAR,
            // -- Structural --
                 KW_CONSTRUCTOR, KW_INIT, KW_COMPANION,
                 KW_WHERE, KW_BY, KW_GET, KW_SET
                -> Token.TokenType.KEYWORD2;

            // KEYWORD3: Reference keywords (matches Java: this/super/null/true/false)
            case KW_THIS, KW_SUPER, KW_NULL, KW_TRUE, KW_FALSE
                -> Token.TokenType.KEYWORD3;

            // Numeric literals
            case INTEGER_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL
                -> Token.TokenType.CHAR_LITERAL;

            // String parts
            case OPEN_QUOTE, CLOSING_QUOTE, REGULAR_STRING_PART,
                 ESCAPE_SEQUENCE, SHORT_TEMPLATE_ENTRY_START,
                 LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END
                -> Token.TokenType.STRING_LITERAL;

            // Comments
            case EOL_COMMENT, BLOCK_COMMENT, SHEBANG_COMMENT
                -> Token.TokenType.COMMENT_NORMAL;

            // KDoc (Kotlin's Javadoc equivalent)
            case DOC_COMMENT
                -> Token.TokenType.COMMENT_JAVADOC;

            // Annotations (@)
            case AT
                -> Token.TokenType.LABEL;

            // Everything else: identifiers, operators, delimiters, whitespace
            default -> Token.TokenType.DEFAULT;
        };
    }

    // -----------------------------------------------------------------------
    // Utility: check token category
    // -----------------------------------------------------------------------

    /**
     * Returns true if the token type is any keyword (hard or soft).
     */
    public static boolean isKeyword(int tokenType)
    {
        return tokenType >= KW_PACKAGE && tokenType <= KW_CONTRACT;
    }

    /**
     * Returns true if the token type is a comment of any kind.
     */
    public static boolean isComment(int tokenType)
    {
        return tokenType >= EOL_COMMENT && tokenType <= SHEBANG_COMMENT;
    }

    /**
     * Returns true if the token type is part of a string literal.
     */
    public static boolean isStringPart(int tokenType)
    {
        return tokenType >= OPEN_QUOTE && tokenType <= LONG_TEMPLATE_ENTRY_END;
    }

    // -----------------------------------------------------------------------
    // Soft keyword reclassification by text
    // -----------------------------------------------------------------------

    /**
     * Map an identifier's text to a soft keyword type, if it matches a known
     * Kotlin soft keyword. Returns the keyword type constant, or {@code -1}
     * if the text is not a soft keyword.
     *
     * <p>Kotlin's PSI lexer returns soft/modifier keywords as {@code IDENTIFIER}
     * tokens because they are context-sensitive in the language grammar. For
     * BlueJ's purposes (syntax highlighting and ClassInfo extraction), we always
     * reclassify them as keywords — this matches IntelliJ's own highlighting
     * behavior and is correct for BlueJ's one-class-per-file model.</p>
     *
     * @param text the token text to check
     * @return the keyword token type, or -1 if not a soft keyword
     */
    public static int mapSoftKeywordByText(String text)
    {
        return switch (text)
        {
            case "abstract" -> KW_ABSTRACT;
            case "actual" -> KW_ACTUAL;
            case "annotation" -> KW_ANNOTATION;
            case "by" -> KW_BY;
            case "companion" -> KW_COMPANION;
            case "const" -> KW_CONST;
            case "constructor" -> KW_CONSTRUCTOR;
            case "contract" -> KW_CONTRACT;
            case "crossinline" -> KW_CROSSINLINE;
            case "data" -> KW_DATA;
            case "enum" -> KW_ENUM;
            case "expect" -> KW_EXPECT;
            case "external" -> KW_EXTERNAL;
            case "final" -> KW_FINAL;
            case "get" -> KW_GET;
            case "import" -> KW_IMPORT;
            case "infix" -> KW_INFIX;
            case "init" -> KW_INIT;
            case "inline" -> KW_INLINE;
            case "inner" -> KW_INNER;
            case "internal" -> KW_INTERNAL;
            case "lateinit" -> KW_LATEINIT;
            case "noinline" -> KW_NOINLINE;
            case "open" -> KW_OPEN;
            case "operator" -> KW_OPERATOR;
            case "out" -> KW_OUT;
            case "override" -> KW_OVERRIDE;
            case "private" -> KW_PRIVATE;
            case "protected" -> KW_PROTECTED;
            case "public" -> KW_PUBLIC;
            case "reified" -> KW_REIFIED;
            case "sealed" -> KW_SEALED;
            case "set" -> KW_SET;
            case "suspend" -> KW_SUSPEND;
            case "tailrec" -> KW_TAILREC;
            case "value" -> KW_VALUE;
            case "vararg" -> KW_VARARG;
            case "where" -> KW_WHERE;
            default -> -1;
        };
    }
}
