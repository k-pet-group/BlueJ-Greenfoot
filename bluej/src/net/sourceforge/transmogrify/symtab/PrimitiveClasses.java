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

import java.util.*;

public class PrimitiveClasses {

  public static ExternalClass BOOLEAN = new ExternalClass(Boolean.TYPE);
  public static ExternalClass CHAR = new ExternalClass(Character.TYPE);
  public static ExternalClass BYTE = new ExternalClass(Byte.TYPE);
  public static ExternalClass SHORT = new ExternalClass(Short.TYPE);
  public static ExternalClass INT = new ExternalClass(Integer.TYPE);
  public static ExternalClass LONG = new ExternalClass(Long.TYPE);
  public static ExternalClass FLOAT = new ExternalClass(Float.TYPE);
  public static ExternalClass DOUBLE = new ExternalClass(Double.TYPE);

  private static List order;

  static {
    order = new ArrayList();
    order.add(DOUBLE);
    order.add(FLOAT);
    order.add(LONG);
    order.add(INT);
    order.add(SHORT);
    order.add(BYTE);
  }

  public static boolean typesAreCompatible(ExternalClass hole,
                                           ExternalClass peg) {
    boolean result = false;

    if (hole.equals(BOOLEAN)) {
      result = peg.equals(BOOLEAN);
    }
    else if (hole.equals(CHAR)) {
      result = peg.equals(CHAR);
    }
    else if (peg.equals(CHAR)) {
      result = (hole.equals(CHAR) ||
                order.indexOf(hole) <= order.indexOf(INT));
    }
    else {
      result = (order.indexOf(hole) <= order.indexOf(peg));
    }

    return result;
  }

  public static IClass unaryPromote(IClass type) {
    IClass result = type;

    if (type.equals(BYTE) || type.equals(SHORT) || type.equals(CHAR)) {
      result = INT;
    }

    return result;
  }

  public static IClass binaryPromote(IClass a, IClass b) {
    IClass result = null;

    if (a.equals(DOUBLE) || b.equals(DOUBLE)) {
      result = DOUBLE;
    }
    else if (a.equals(FLOAT) || b.equals(FLOAT)) {
      result = FLOAT;
    }
    else if (a.equals(LONG) || b.equals(LONG)) {
      result = LONG;
    }
    else {
      result = INT;
    }

    return result;
  }

}
