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
import java.lang.reflect.Constructor;

public class InterfaceConstructor extends ExternalDefinition implements IMethod {

  protected Class _classConstructed;

  public InterfaceConstructor(Class classConstructed) {
    _classConstructed = classConstructed;
  }

  public String getName() {
    return _classConstructed.getName();
  }

  public IClass getType() {
    return new ExternalClass(_classConstructed);
  }

  public ISignature getSignature() {
    return new MethodSignature(new Vector());
  }

  public boolean hasSameSignature(ISignature signature) {
    return getSignature().isSame(signature);
  }

  public boolean hasCompatibleSignature(ISignature signature) {
    return signature.isCompatibleWith(getSignature());
  }

  public Constructor getJavaConstructor() {
    return null;
  }

  public IClass[] getExceptions() {
    return new IClass[0];
  }


  public String getQualifiedName() {
    return getName() + getSignature();
  }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof InterfaceConstructor) {
      InterfaceConstructor constructor = (InterfaceConstructor)o;
      result = getType().equals(constructor.getType());
    }

    return result;
  }

  public int hashCode() {
    return getType().hashCode();
  }

}
