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

public class DefaultConstructor extends MethodDef {

  protected ClassDef _classConstructed;

  public DefaultConstructor(ClassDef classConstructed) {
    super(classConstructed.getName(), classConstructed, null);
    _classConstructed = classConstructed;
  }

  public IClass getType() {
    return _classConstructed;
  }

  public ISignature getSignature() {
    return new MethodSignature(new Vector());
  }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof DefaultConstructor) {
      DefaultConstructor constructor = (DefaultConstructor)o;
      result = getType().equals(constructor.getType());
    }

    return result;
  }

  public void setType() {}
  public void addParameter() {}
  public void addException() {}

}
