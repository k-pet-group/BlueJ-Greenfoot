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

public class KotlinKeywords implements Keywords {
    private static Map<String,Integer> keywords = new HashMap<>();

    static {
        keywords.put("package", JavaTokenTypes.LITERAL_package);
        keywords.put("import", JavaTokenTypes.LITERAL_import);
        keywords.put("class", JavaTokenTypes.LITERAL_class);
        keywords.put("interface", JavaTokenTypes.LITERAL_interface);
        keywords.put("fun", JavaTokenTypes.LITERAL_fun);
        keywords.put("val", JavaTokenTypes.LITERAL_val);
        keywords.put("var", JavaTokenTypes.LITERAL_var);
        keywords.put("const", JavaTokenTypes.LITERAL_const);
        keywords.put("constructor", JavaTokenTypes.LITERAL_constructor);
        keywords.put("by", JavaTokenTypes.LITERAL_by);
        keywords.put("companion", JavaTokenTypes.LITERAL_companion);
        keywords.put("init", JavaTokenTypes.LITERAL_init);
        keywords.put("object", JavaTokenTypes.LITERAL_object);
        keywords.put("typealias", JavaTokenTypes.LITERAL_typealias);
        keywords.put("data", JavaTokenTypes.LITERAL_data);
        keywords.put("sealed", JavaTokenTypes.LITERAL_sealed);
        keywords.put("inner", JavaTokenTypes.LITERAL_inner);
        keywords.put("enum", JavaTokenTypes.LITERAL_enum);
        keywords.put("open", JavaTokenTypes.LITERAL_open);

        // Control flow keywords
        keywords.put("if", JavaTokenTypes.LITERAL_if);
        keywords.put("else", JavaTokenTypes.LITERAL_else);
        keywords.put("while", JavaTokenTypes.LITERAL_while);
        keywords.put("do", JavaTokenTypes.LITERAL_do);
        keywords.put("for", JavaTokenTypes.LITERAL_for);
        keywords.put("when", JavaTokenTypes.LITERAL_when);
        keywords.put("break", JavaTokenTypes.LITERAL_break);
        keywords.put("continue", JavaTokenTypes.LITERAL_continue);
        keywords.put("return", JavaTokenTypes.LITERAL_return);
        keywords.put("throw", JavaTokenTypes.LITERAL_throw);
        keywords.put("try", JavaTokenTypes.LITERAL_try);
        keywords.put("catch", JavaTokenTypes.LITERAL_catch);
        keywords.put("finally", JavaTokenTypes.LITERAL_finally);

        // Literals
        keywords.put("true", JavaTokenTypes.LITERAL_true);
        keywords.put("false", JavaTokenTypes.LITERAL_false);
        keywords.put("null", JavaTokenTypes.LITERAL_null);
        keywords.put("this", JavaTokenTypes.LITERAL_this);
        keywords.put("super", JavaTokenTypes.LITERAL_super);

        // Modifiers
        keywords.put("public", JavaTokenTypes.LITERAL_public);
        keywords.put("private", JavaTokenTypes.LITERAL_private);
        keywords.put("protected", JavaTokenTypes.LITERAL_protected);
        keywords.put("internal", JavaTokenTypes.LITERAL_internal);
        keywords.put("final", JavaTokenTypes.FINAL);
        keywords.put("abstract", JavaTokenTypes.ABSTRACT);

        // Not keywords (workaround for types)
        keywords.put("Int", JavaTokenTypes.LITERAL_int);
        keywords.put("Long", JavaTokenTypes.LITERAL_long);
        keywords.put("Short", JavaTokenTypes.LITERAL_short);
        keywords.put("Byte", JavaTokenTypes.LITERAL_byte);
        keywords.put("Double", JavaTokenTypes.LITERAL_double);
        keywords.put("Float", JavaTokenTypes.LITERAL_float);
        keywords.put("Boolean", JavaTokenTypes.LITERAL_boolean);
        keywords.put("Char", JavaTokenTypes.LITERAL_char);

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
