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

import java.lang.reflect.Constructor;

public class ExternalConstructor extends ExternalDefinition implements IMethod {

  private Constructor _javaConstructor;
  private ISignature _signature;

  public ExternalConstructor(Constructor javaConstructor) {
    _javaConstructor = javaConstructor;
    _signature = new ExternalSignature(_javaConstructor.getParameterTypes());
  }

  public String getName() {
    return _javaConstructor.getDeclaringClass().getName();
  }

  public IClass getType() {
    return new ExternalClass(_javaConstructor.getDeclaringClass());
  }

  public ISignature getSignature() {
    return _signature;
  }

  public boolean hasSameSignature(ISignature signature) {
    return getSignature().isSame(signature);
  }

  public boolean hasCompatibleSignature(ISignature signature) {
    return signature.isCompatibleWith(getSignature());
  }

  public String getQualifiedName() {
    return getName() + getSignature();
  }

  public Constructor getJavaConstructor() {
    return _javaConstructor;
  }

  public IClass[] getExceptions() {
    Class[] javaExceptions = getJavaConstructor().getExceptionTypes();
    IClass[] result = new IClass[javaExceptions.length];

    for (int i = 0; i < result.length; i++) {
      result[i] = new ExternalClass(javaExceptions[i]);
    }

    return result;
  }

  public String toString() {
    return getQualifiedName();
  }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof ExternalConstructor) {
      ExternalConstructor constructor = (ExternalConstructor)o;
      result = getJavaConstructor().equals(constructor.getJavaConstructor());
    }

    return result;
  }

  public int hashCode() {
    return getJavaConstructor().hashCode();
  }

}
