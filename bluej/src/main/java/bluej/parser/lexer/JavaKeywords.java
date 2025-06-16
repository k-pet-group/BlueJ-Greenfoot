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
package bluej.parser.lexer;

import java.util.HashMap;
import java.util.Map;

public class JavaKeywords implements Keywords {
    private static Map<String,Integer> keywords = new HashMap<>();

    static {
        keywords.put("abstract", JavaTokenTypes.ABSTRACT);
        keywords.put("assert", JavaTokenTypes.LITERAL_assert);
        keywords.put("boolean", JavaTokenTypes.LITERAL_boolean);
        keywords.put("break", JavaTokenTypes.LITERAL_break);
        keywords.put("byte", JavaTokenTypes.LITERAL_byte);
        keywords.put("case", JavaTokenTypes.LITERAL_case);
        keywords.put("catch", JavaTokenTypes.LITERAL_catch);
        keywords.put("char", JavaTokenTypes.LITERAL_char);
        keywords.put("class", JavaTokenTypes.LITERAL_class);
        keywords.put("continue", JavaTokenTypes.LITERAL_continue);
        keywords.put("default", JavaTokenTypes.LITERAL_default);
        keywords.put("do", JavaTokenTypes.LITERAL_do);
        keywords.put("double", JavaTokenTypes.LITERAL_double);
        keywords.put("else", JavaTokenTypes.LITERAL_else);
        keywords.put("enum", JavaTokenTypes.LITERAL_enum);
        keywords.put("extends", JavaTokenTypes.LITERAL_extends);
        keywords.put("false", JavaTokenTypes.LITERAL_false);
        keywords.put("final", JavaTokenTypes.FINAL);
        keywords.put("finally", JavaTokenTypes.LITERAL_finally);
        keywords.put("float", JavaTokenTypes.LITERAL_float);
        keywords.put("for", JavaTokenTypes.LITERAL_for);
        keywords.put("goto", JavaTokenTypes.GOTO);
        keywords.put("if", JavaTokenTypes.LITERAL_if);
        keywords.put("implements", JavaTokenTypes.LITERAL_implements);
        keywords.put("import", JavaTokenTypes.LITERAL_import);
        keywords.put("instanceof", JavaTokenTypes.LITERAL_instanceof);
        keywords.put("int", JavaTokenTypes.LITERAL_int);
        keywords.put("interface", JavaTokenTypes.LITERAL_interface);
        keywords.put("long", JavaTokenTypes.LITERAL_long);
        keywords.put("native", JavaTokenTypes.LITERAL_native);
        keywords.put("new", JavaTokenTypes.LITERAL_new);
        keywords.put("non-sealed", JavaTokenTypes.LITERAL_non_sealed);
        keywords.put("null", JavaTokenTypes.LITERAL_null);
        keywords.put("package", JavaTokenTypes.LITERAL_package);
        keywords.put("permits", JavaTokenTypes.LITERAL_permits);
        keywords.put("private", JavaTokenTypes.LITERAL_private);
        keywords.put("protected", JavaTokenTypes.LITERAL_protected);
        keywords.put("public", JavaTokenTypes.LITERAL_public);
        keywords.put("record", JavaTokenTypes.LITERAL_record);
        keywords.put("return", JavaTokenTypes.LITERAL_return);
        keywords.put("sealed", JavaTokenTypes.LITERAL_sealed);
        keywords.put("short", JavaTokenTypes.LITERAL_short);
        keywords.put("static", JavaTokenTypes.LITERAL_static);
        keywords.put("strictfp", JavaTokenTypes.STRICTFP);
        keywords.put("super", JavaTokenTypes.LITERAL_super);
        keywords.put("switch", JavaTokenTypes.LITERAL_switch);
        keywords.put("synchronized", JavaTokenTypes.LITERAL_synchronized);
        keywords.put("this", JavaTokenTypes.LITERAL_this);
        keywords.put("throw", JavaTokenTypes.LITERAL_throw);
        keywords.put("throws", JavaTokenTypes.LITERAL_throws);
        keywords.put("transient", JavaTokenTypes.LITERAL_transient);
        keywords.put("true", JavaTokenTypes.LITERAL_true);
        keywords.put("try", JavaTokenTypes.LITERAL_try);
        keywords.put("volatile", JavaTokenTypes.LITERAL_volatile);
        keywords.put("when", JavaTokenTypes.LITERAL_when);
        keywords.put("while", JavaTokenTypes.LITERAL_while);
        keywords.put("void", JavaTokenTypes.LITERAL_void);
        keywords.put("yield", JavaTokenTypes.LITERAL_yield);
    }

    @Override
    public boolean isKeyword(String s) {
        return keywords.containsKey(s);
    }

    @Override
    public Integer getKeywordType(String s) {
        return keywords.get(s);
    }
}
