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
package net.sourceforge.transmogrify.symtab;

import antlr.*;

import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;


/**
 * Resolves primitive identifiers (int, double) to their corresponding
 * <code>ClassDef</code>.  This class uses the Singleton pattern.
 *
 * @author <a href="mailto:smileyy@thoughtworks.com">andrew</a>
 * @version 1.0
 * @since 1.0
 */

public class LiteralResolver {

  private static Map intMap;
  private static Map nameMap;
  private static Map classMap;

  static {
    nameMap = new HashMap();

    nameMap.put("boolean", new ExternalClass(Boolean.TYPE));
    nameMap.put("byte", new ExternalClass(Byte.TYPE));
    nameMap.put("char", new ExternalClass(Character.TYPE));
    nameMap.put("short", new ExternalClass(Short.TYPE));
    nameMap.put("int", new ExternalClass(Integer.TYPE));
    nameMap.put("float", new ExternalClass(Float.TYPE));
    nameMap.put("long", new ExternalClass(Long.TYPE));
    nameMap.put("double", new ExternalClass(Double.TYPE));

    intMap = new HashMap();

    intMap.put(new Integer(JavaTokenTypes.LITERAL_boolean),
               new ExternalClass(Boolean.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_byte),
               new ExternalClass(Byte.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_char),
               new ExternalClass(Character.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_short),
               new ExternalClass(Short.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_int),
               new ExternalClass(Integer.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_float),
               new ExternalClass(Float.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_long),
               new ExternalClass(Long.TYPE));
    intMap.put(new Integer(JavaTokenTypes.LITERAL_double),
               new ExternalClass(Double.TYPE));
    intMap.put(new Integer(JavaTokenTypes.STRING_LITERAL),
               new ExternalClass("".getClass()));

    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    classMap = new HashMap();
    classMap.put(new ExternalClass(Boolean.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_boolean, "boolean"));
    classMap.put(new ExternalClass(Byte.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_byte, "byte"));
    classMap.put(new ExternalClass(Character.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_char, "char"));
    classMap.put(new ExternalClass(Short.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_short, "short"));
    classMap.put(new ExternalClass(Integer.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_int, "int"));
    classMap.put(new ExternalClass(Float.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_float, "float"));
    classMap.put(new ExternalClass(Long.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_long, "long"));
    classMap.put(new ExternalClass(Double.TYPE),
                 factory.create(JavaTokenTypes.LITERAL_double, "double"));

  }

  /**
   * Returns a <code>LiteralResolver</code>
   *
   * @return a <code>LiteralResolver</code>
   */

  public static LiteralResolver getResolver() {
    return new LiteralResolver();
  }

  /**
   * Returns the <code>ClassDef</code> for a primitive type reference.
   *
   * <p>
   * We could probably do without passing in the context, if we could figure
   * out a way to access the base scope.
   * </p>
   *
   * @param literalType the JavaTokenType for the literal type
   * @param context the scope in which the search performed
   * @return returns the <code>ClassDef</code>corresponding to the primitive
   *         type
   */

  public static IClass getDefinition(int literalType) {
    Integer key = new Integer(literalType);
    return (IClass)intMap.get(key);
  }

  public static IClass getDefinition(String name) {
    return (IClass)nameMap.get(name);
  }

  public static SymTabAST getASTNode(IClass primitive) {
    return (SymTabAST)classMap.get(primitive);
  }
}
