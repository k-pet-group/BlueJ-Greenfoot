/*
Copyright (C) 2001  ThoughtWorks, Inc

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
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sourceforge.transmogrify.symtab.printer;

import net.sourceforge.transmogrify.symtab.parser.*;

// $Id: PrinterFactory.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * makes Printer objects
 */

public class PrinterFactory {
  public static int numPrinters = 0;

  /**
   * returns a new Printer for the type of AST node.  Passes the context
   * on to that printer
   *
   * @param nodeToPrint the <code>AST</code> node to print
   *
   * @return the new Printer object
   */
  public static Printer makePrinter(SymTabAST nodeToPrint) {
    Printer result;
    numPrinters++;

    switch(nodeToPrint.getType()) {
      case 0:
        result = new RootPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.MODIFIERS:
      case JavaTokenTypes.EXPR:
      case JavaTokenTypes.FOR_CONDITION:
      case JavaTokenTypes.FOR_ITERATOR:
      case JavaTokenTypes.RCURLY:
        result = new NullPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.FOR_INIT:
        result = new ForInitPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.INSTANCE_INIT:
        result = new InstanceInitPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.SLIST:
        result = new BlockPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.CTOR_DEF:
        result = new ConstructorDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.METHOD_DEF:
        result = new MethodDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.VARIABLE_DEF:
        result = new FreeStandingVariableDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.STATIC_INIT:
        result = new StaticInitPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.TYPE:
        result = new TypePrinter(nodeToPrint);
        break;

      case JavaTokenTypes.CLASS_DEF:
        result = new ClassDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.INTERFACE_DEF:
        result = new InterfaceDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.PACKAGE_DEF:
        result = new PackageDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.ARRAY_DECLARATOR:
        result = new ArrayDeclPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.EXTENDS_CLAUSE:
        result = new ExtendsPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.IMPLEMENTS_CLAUSE:
        result = new ImplementsPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.ELIST:
      case JavaTokenTypes.PARAMETERS:
        result = new ParametersPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.PARAMETER_DEF:
        result = new ParameterDefPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LABELED_STAT:
        result = new LabeledStatPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.TYPECAST:
        result = new TypeCastPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.INDEX_OP:
        result = new IndexOpPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.METHOD_CALL:
        result = new MethodCallPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.ARRAY_INIT:
        result = new ArrayInitPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.IMPORT:
        result = new ImportPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.POST_INC:
      case JavaTokenTypes.POST_DEC:
        result = new PostfixOperatorPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.UNARY_MINUS:
      case JavaTokenTypes.UNARY_PLUS:
      case JavaTokenTypes.INC:
      case JavaTokenTypes.DEC:
      case JavaTokenTypes.BNOT:
      case JavaTokenTypes.LNOT:
        result = new PrefixOperatorPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.PLUS_ASSIGN:
      case JavaTokenTypes.MINUS_ASSIGN:
      case JavaTokenTypes.STAR_ASSIGN:
      case JavaTokenTypes.DIV_ASSIGN:
      case JavaTokenTypes.MOD_ASSIGN:
      case JavaTokenTypes.SR_ASSIGN:
      case JavaTokenTypes.BSR_ASSIGN:
      case JavaTokenTypes.SL_ASSIGN:
      case JavaTokenTypes.BAND_ASSIGN:
      case JavaTokenTypes.BXOR_ASSIGN:
      case JavaTokenTypes.BOR_ASSIGN:
      case JavaTokenTypes.LOR:
      case JavaTokenTypes.LAND:
      case JavaTokenTypes.BOR:
      case JavaTokenTypes.BXOR:
      case JavaTokenTypes.BAND:
      case JavaTokenTypes.NOT_EQUAL:
      case JavaTokenTypes.EQUAL:
      case JavaTokenTypes.LT:
      case JavaTokenTypes.GT:
      case JavaTokenTypes.LE:
      case JavaTokenTypes.GE:
      case JavaTokenTypes.SL:
      case JavaTokenTypes.SR:
      case JavaTokenTypes.BSR:
      case JavaTokenTypes.PLUS:
      case JavaTokenTypes.MINUS:
      case JavaTokenTypes.DIV:
      case JavaTokenTypes.MOD:
      case JavaTokenTypes.LITERAL_instanceof:
        result = new BinaryOperatorPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.CASE_GROUP:
        result = new CaseGroupPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.EMPTY_STAT:
        result = new EmptyStatPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.ML_COMMENT:
        result = new MultiLineCommentPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.SL_COMMENT:
        result = new SingleLineCommentPrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.LITERAL_package:
      //case JavaTokenTypes.SEMI:
      //case JavaTokenTypes.LITERAL_import:
      //case JavaTokenTypes.LBRACK:
      //case JavaTokenTypes.RBRACK:
      case JavaTokenTypes.FINAL:
      case JavaTokenTypes.ABSTRACT:
      case JavaTokenTypes.LITERAL_private:
      case JavaTokenTypes.LITERAL_public:
      case JavaTokenTypes.LITERAL_protected:
      case JavaTokenTypes.LITERAL_static:
      case JavaTokenTypes.LITERAL_transient:
      case JavaTokenTypes.LITERAL_native:
      case JavaTokenTypes.LITERAL_threadsafe:
      case JavaTokenTypes.LITERAL_volatile:
        result = new TrailingSpacePrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_case:
        result = new CasePrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_synchronized:
        result = new SynchronizedPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_void:
      case JavaTokenTypes.LITERAL_boolean:
      case JavaTokenTypes.LITERAL_byte:
      case JavaTokenTypes.LITERAL_char:
      case JavaTokenTypes.LITERAL_short:
      case JavaTokenTypes.LITERAL_int:
      case JavaTokenTypes.LITERAL_float:
      case JavaTokenTypes.LITERAL_long:
      case JavaTokenTypes.LITERAL_double:
      case JavaTokenTypes.IDENT:
        result = new DefaultPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.DOT:
        result = new DotPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.STAR:
        result = new StarPrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.LITERAL_class:
      //case JavaTokenTypes.LITERAL_extends:
      //case JavaTokenTypes.LITERAL_interface:
      //case JavaTokenTypes.LCURLY:
      //case JavaTokenTypes.COMMA:
      //case JavaTokenTypes.LITERAL_implements:
      case JavaTokenTypes.LPAREN:
        result = new ParenPrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.RPAREN:
      case JavaTokenTypes.ASSIGN:
        result = new AssignPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_throws:
        result = new ThrowsClausePrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.COLON:
      case JavaTokenTypes.LITERAL_if:
        result = new IfPrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.LITERAL_else:
      case JavaTokenTypes.LITERAL_for:
        result = new ForPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_while:
        result = new WhilePrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_do:
        result = new DoPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_break:
      case JavaTokenTypes.LITERAL_continue:
        result = new GotoPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_switch:
        result = new SwitchPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_default:
        result = new DefaultCasePrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_try:
        result = new TryPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_finally:
        result = new FinallyPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_catch:
        result = new CatchPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.QUESTION:
        result = new QuestionPrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.LITERAL_this:
      //case JavaTokenTypes.LITERAL_super:
      //case JavaTokenTypes.LITERAL_true:
      //case JavaTokenTypes.LITERAL_false:
      //case JavaTokenTypes.LITERAL_null:

      case JavaTokenTypes.LITERAL_new:
        result = new NewPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_return:
        result = new ReturnPrinter(nodeToPrint);
        break;

      case JavaTokenTypes.LITERAL_throw:
        result = new ThrowPrinter(nodeToPrint);
        break;

      //case JavaTokenTypes.NUM_INT:
      //case JavaTokenTypes.CHAR_LITERAL:
      //case JavaTokenTypes.STRING_LITERAL:
      //case JavaTokenTypes.NUM_FLOAT:
      //case JavaTokenTypes.WS:
      //case JavaTokenTypes.ESC:
      //case JavaTokenTypes.HEX_DIGIT:
      //case JavaTokenTypes.VOCAB:
      //case JavaTokenTypes.EXPONENT:
      //case JavaTokenTypes.FLOAT_SUFFIX:
      default:
        result = new DefaultPrinter(nodeToPrint);
    }

    return result;
  }
}
