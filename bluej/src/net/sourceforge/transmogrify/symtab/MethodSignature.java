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

import java.util.Vector;

// $Id: MethodSignature.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>MethodSignature</code> is used to resolve various methods
 * in the same scope of the same name based on formal parameter lists
 *
 * @see MethodDef
 */

public class MethodSignature implements ISignature{

  private IClass[] _argTypes = null;

  public MethodSignature(IClass[] argTypes) {
    _argTypes = argTypes;
  }

  public MethodSignature(Vector argTypes) {
    _argTypes = new IClass[argTypes.size()];
    argTypes.toArray(_argTypes);
  }

  /**
   * returns an array of the types of the arguments in the signature
   *
   * @return ClassDef[]
   */
  public IClass[] getParameters() {
    return _argTypes;
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

    IClass[] comparedArgTypes = signature.getParameters();
    if (_argTypes.length != comparedArgTypes.length) {
      result = false;
    }
    else {
      for (int i = 0; i < _argTypes.length; i++) {
        if (!_argTypes[i].isCompatibleWith(comparedArgTypes[i])) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  public boolean isSame(ISignature signature) {
    return equals(signature);
  }

  /**
   * compares two objects for equality.  If the compared object is a
   * <code>MethodSignature</code> and the argTypes match, they are the
   * same
   *
   * @return boolean
   */
  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof MethodSignature) {
      MethodSignature signature = (MethodSignature)o;
      result = java.util.Arrays.equals(getParameters(), signature.getParameters());
    }

    return result;
  }

  /**
   * returns a String representation of this object.  Includes information
   * about the types of the arguments in the signature
   *
   * @return String
   */
  public String toString() {
    StringBuffer result = new StringBuffer( "(" );

    for ( int i = 0; i < _argTypes.length; i++ ) {
      result.append( _argTypes[i] != null ? _argTypes[i].getName() : "[null]" );
      if ( i < (_argTypes.length - 1) ) {
        result.append( ", " );
      }
    }
    result.append( ")" );

    return result.toString();
  }

}
