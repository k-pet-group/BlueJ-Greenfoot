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

public class ExternalSignature implements ISignature {

  private IClass[] _parameters;

  public ExternalSignature(Class[] parameters) {
    _parameters = new IClass[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].isArray()) {
        _parameters[i] =
          new ArrayDef(new ExternalClass(parameters[i].getComponentType()));
      }
      else {
        _parameters[i] = new ExternalClass(parameters[i]);
      }
    }
  }

  /**
   * Whether this method signature is compatible with the signature of the
   * argument.  That is to say, each type for this signature are subclasses,
   * subinterfaces, or implement the interface for each corresponding type
   * in the argument signature.
   *
   * @param signature the signature of the method definition being compared
   * @return whether the signatures are compatible
   */
  public boolean isCompatibleWith(ISignature signature) {
    boolean result = true;

    if (_parameters.length == signature.getParameters().length) {
      for (int i = 0; i < _parameters.length; i++) {
        if (!getParameters()[i].isCompatibleWith(signature.getParameters()[i])) {
          result = false;
          break;
        }
      }
    }
    else {
      result = false;
    }

    return result;
  }

  public boolean isSame(ISignature signature) {
    return java.util.Arrays.equals(_parameters, signature.getParameters());
  }

  public IClass[] getParameters() {
    return _parameters;
  }

  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("(");
    for (int i = 0; i < _parameters.length; i++) {
      result.append(_parameters[i]);
      if (i < _parameters.length - 1) {
        result.append(", ");
      }
    }
    result.append(")");

    return result.toString();
  }

}
