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

import java.util.Comparator;

public class MethodSpecificityComparator implements Comparator {

  public int compare(Object o1, Object o2) {
    int result = 0;

    IMethod method1 = (IMethod)o1;
    IMethod method2 = (IMethod)o2;

    if (method1.equals(method2)) {
      result = 0;
    }
    else {
      ISignature sig1 = method1.getSignature();
      ISignature sig2 = method2.getSignature();

      if (sig1.isCompatibleWith(sig2)) {
        result = -1;
      }
      else {
        result = 1;
      }
    }

    return result;
  }

}
