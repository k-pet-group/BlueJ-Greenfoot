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

// $Id: ExternalMethod.java 1011 2001-11-22 10:36:26Z ajp $


import java.lang.reflect.*;

/**
 * <code>MethodDef</code> contains all the pertinent information for
 * a method, including return type, formal parameters, and exceptions
 * thrown
 *
 * @see ClassDef
 * @see MethodSignature
 */
public class ExternalMethod extends ExternalDefinition implements IMethod {
  private Method _javaMethod;
  private ISignature _signature;

  public ExternalMethod(Method javaMethod) {
    _javaMethod = javaMethod;
    _signature = new ExternalSignature(_javaMethod.getParameterTypes());
  }

  public String getName() {
    return _javaMethod.getName();
  }

  /**
   * Returns the <code>ClassDef</code> for the return type of this method.
   *
   * @return the <code>ClassDef</code> for the return type of this method
   */
  public IClass getType() {
    IClass result = null;
    if (_javaMethod.getReturnType().isArray()) {
      result = new ArrayDef(new ExternalClass(_javaMethod.getReturnType().getComponentType()));
    }
    else {
      result = new ExternalClass(_javaMethod.getReturnType());
    }

    return result;
  }

  /**
   * Returns the signature of this method.
   *
   * @return the signature of this method
   */
  public ISignature getSignature() {
    return _signature;
  }

  public boolean hasSameSignature(ISignature signature) {
    return _signature.isSame(signature);
  }

  public boolean hasCompatibleSignature(ISignature signature) {
    return signature.isCompatibleWith(getSignature());
  }

  public String getQualifiedName() {
    return getName() + getSignature();
  }

  public Method getJavaMethod() {
    return _javaMethod;
  }

  public IClass[] getExceptions() {
    Class[] javaExceptions = getJavaMethod().getExceptionTypes();
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

    if (o instanceof ExternalMethod) {
      ExternalMethod compared = (ExternalMethod)o;
      result = getJavaMethod().equals(compared.getJavaMethod());
    }

    return result;
  }

  public int hashCode() {
    return getJavaMethod().hashCode();
  }
}
