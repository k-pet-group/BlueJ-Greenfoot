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

// $Id: IMethod.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>MethodDef</code> contains all the pertinent information for
 * a method, including return type, formal parameters, and exceptions
 * thrown
 *
 * @see ClassDef
 * @see MethodSignature
 * @see net.sourceforge.transmogrify.symtab.ExternalMethod
 * @see net.sourceforge.transmogrify.symtab.MethodDef
 */
public interface IMethod extends Typed {

  /**
   * Returns the signature of this method.
   *
   * @return the signature of this method
   */
  public ISignature getSignature();

  /**
   * verifies if the input signature is the same with signatures of this method
   * @return <code>true</code> if the two set signatures are equal
   *         <code>false</code> otherwise
   */
  public boolean hasSameSignature(ISignature signature);

  /**
   * verifies if the input signature type is compatible with this method signature
   * @return <code>true</code> if the two set of signatures are compatible
   *         <code>false</code> otherwise
   */
  public boolean hasCompatibleSignature(ISignature signature);

  public IClass[] getExceptions();
}