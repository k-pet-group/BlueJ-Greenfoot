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

/**
 * implemented by signatures of methods
 * @see net.sourceforge.transmogrify.symtab.MethodSignature
 * @see net.sourceforge.transmogrify.symtab.ExternalSignature
 */
public interface ISignature {

  public IClass[] getParameters();

  /**
   * Whether this method signature is compatible with the signature of the
   * argument.  That is to say, each type for this signature are subclasses,
   * subinterfaces, or implement the interface for each corresponding type
   * in the argument signature.
   *
   * @param signature the signature of the method definition being compared
   * @return whether the signatures are compatible
   */
  public boolean isCompatibleWith(ISignature signature);

  /**
   * if both signature has the same set of parameters
   * @param signature signature to be compared
   * @return <code>true</code> if the two set of parameters are equal
   *         <code>false</code> otherwise
   */
  public boolean isSame(ISignature signature);
}
