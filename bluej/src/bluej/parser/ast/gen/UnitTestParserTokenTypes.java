/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
// $ANTLR : "unittest.tree.g" -> "UnitTestParser.java"$

    package bluej.parser.ast.gen;
    
    import bluej.parser.SourceSpan;
    import bluej.parser.SourceLocation;
	import bluej.parser.ast.LocatableAST;
	    
    import java.util.*;
    import antlr.BaseAST;

public interface UnitTestParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int BLOCK = 4;
	int MODIFIERS = 5;
	int OBJBLOCK = 6;
	int SLIST = 7;
	int CTOR_DEF = 8;
	int METHOD_DEF = 9;
	int VARIABLE_DEF = 10;
	int INSTANCE_INIT = 11;
	int STATIC_INIT = 12;
	int TYPE = 13;
	int CLASS_DEF = 14;
	int INTERFACE_DEF = 15;
	int PACKAGE_DEF = 16;
	int ARRAY_DECLARATOR = 17;
	int EXTENDS_CLAUSE = 18;
	int IMPLEMENTS_CLAUSE = 19;
	int PARAMETERS = 20;
	int PARAMETER_DEF = 21;
	int LABELED_STAT = 22;
	int TYPECAST = 23;
	int INDEX_OP = 24;
	int POST_INC = 25;
	int POST_DEC = 26;
	int METHOD_CALL = 27;
	int EXPR = 28;
	int ARRAY_INIT = 29;
	int IMPORT = 30;
	int UNARY_MINUS = 31;
	int UNARY_PLUS = 32;
	int CASE_GROUP = 33;
	int ELIST = 34;
	int FOR_INIT = 35;
	int FOR_CONDITION = 36;
	int FOR_ITERATOR = 37;
	int EMPTY_STAT = 38;
	int FINAL = 39;
	int ABSTRACT = 40;
	int STRICTFP = 41;
	int SUPER_CTOR_CALL = 42;
	int CTOR_CALL = 43;
	int VARIABLE_PARAMETER_DEF = 44;
	int STATIC_IMPORT = 45;
	int ENUM_DEF = 46;
	int ENUM_CONSTANT_DEF = 47;
	int FOR = 48;
	int FOR_EACH = 49;
	int ANNOTATION_DEF = 50;
	int ANNOTATION = 51;
	int ANNOTATION_MEMBER_VALUE_PAIR = 52;
	int ANNOTATION_FIELD_DEF = 53;
	int ANNOTATION_ARRAY_INIT = 54;
	int TYPE_ARGUMENT = 55;
	int TYPE_PARAMETERS = 56;
	int WILDCARD_TYPE = 57;
	int TYPE_UPPER_BOUNDS = 58;
	int TYPE_LOWER_BOUNDS = 59;
	int COMMENT_DEF = 60;
	int ML_COMMENT = 61;
	int LITERAL_package = 62;
	int SEMI = 63;
	int LITERAL_import = 64;
	int LITERAL_static = 65;
	int LBRACK = 66;
	int RBRACK = 67;
	int DOT = 68;
	int IDENT = 69;
	int QUESTION = 70;
	int LITERAL_extends = 71;
	int LITERAL_super = 72;
	int LT = 73;
	int COMMA = 74;
	int GT = 75;
	int SR = 76;
	int BSR = 77;
	int LITERAL_void = 78;
	int LITERAL_boolean = 79;
	int LITERAL_byte = 80;
	int LITERAL_char = 81;
	int LITERAL_short = 82;
	int LITERAL_int = 83;
	int LITERAL_float = 84;
	int LITERAL_long = 85;
	int LITERAL_double = 86;
	int STAR = 87;
	int LITERAL_private = 88;
	int LITERAL_public = 89;
	int LITERAL_protected = 90;
	int LITERAL_transient = 91;
	int LITERAL_native = 92;
	int LITERAL_synchronized = 93;
	int LITERAL_volatile = 94;
	int AT = 95;
	int LPAREN = 96;
	int RPAREN = 97;
	int ASSIGN = 98;
	int LCURLY = 99;
	int RCURLY = 100;
	int LITERAL_class = 101;
	int LITERAL_interface = 102;
	int LITERAL_enum = 103;
	int BAND = 104;
	int LITERAL_default = 105;
	int LITERAL_implements = 106;
	int LITERAL_this = 107;
	int LITERAL_throws = 108;
	int TRIPLE_DOT = 109;
	int COLON = 110;
	int LITERAL_if = 111;
	int LITERAL_else = 112;
	int LITERAL_while = 113;
	int LITERAL_do = 114;
	int LITERAL_break = 115;
	int LITERAL_continue = 116;
	int LITERAL_return = 117;
	int LITERAL_switch = 118;
	int LITERAL_throw = 119;
	int LITERAL_assert = 120;
	int LITERAL_for = 121;
	int LITERAL_case = 122;
	int LITERAL_try = 123;
	int LITERAL_finally = 124;
	int LITERAL_catch = 125;
	int PLUS_ASSIGN = 126;
	int MINUS_ASSIGN = 127;
	int STAR_ASSIGN = 128;
	int DIV_ASSIGN = 129;
	int MOD_ASSIGN = 130;
	int SR_ASSIGN = 131;
	int BSR_ASSIGN = 132;
	int SL_ASSIGN = 133;
	int BAND_ASSIGN = 134;
	int BXOR_ASSIGN = 135;
	int BOR_ASSIGN = 136;
	int LOR = 137;
	int LAND = 138;
	int BOR = 139;
	int BXOR = 140;
	int NOT_EQUAL = 141;
	int EQUAL = 142;
	int LE = 143;
	int GE = 144;
	int LITERAL_instanceof = 145;
	int SL = 146;
	int PLUS = 147;
	int MINUS = 148;
	int DIV = 149;
	int MOD = 150;
	int INC = 151;
	int DEC = 152;
	int BNOT = 153;
	int LNOT = 154;
	int LITERAL_true = 155;
	int LITERAL_false = 156;
	int LITERAL_null = 157;
	int LITERAL_new = 158;
	int NUM_INT = 159;
	int CHAR_LITERAL = 160;
	int STRING_LITERAL = 161;
	int NUM_FLOAT = 162;
	int NUM_LONG = 163;
	int NUM_DOUBLE = 164;
	int WS = 165;
	int SL_COMMENT = 166;
	int ESC = 167;
	int HEX_DIGIT = 168;
	int EXPONENT = 169;
	int FLOAT_SUFFIX = 170;
}
