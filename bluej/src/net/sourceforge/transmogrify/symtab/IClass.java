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

// $Id: IClass.java 1011 2001-11-22 10:36:26Z ajp $

import java.util.*;

/**
 * interface implemented by classes/interfaces definition, for source or
 * non-sourced classes and interfaces
 * The implementor of this class have all information about its inner classes,
 * methods, variables, subclasses, etc.
 * @see net.sourceforge.transmogrify.symtab.ClassDef
 * @see net.sourceforge.transmogrify.symtab.ExternalClass
 */
public interface IClass extends IDefinition {

  /**
   * gets superclass definition of this class
   * @return superclass of this class definition
   */
  public IClass getSuperclass();

  /**
   * gets interfaces definition implemented by this class definition
   * @return interfaces implemented
   */
  public IClass[] getInterfaces();

  /**
   * gets subclasses definition of this class definition
   * @return list of subclasses definition
   */
  public List getSubclasses();

  /**
   * gets class definition referenced by this class, including its inner classes,
   * imported classes, packages, and its parent scope referenced class definitions
   * @param name name of the class definition to be searched
   * @return class definition that matches the input name
   */
  public IClass getClassDefinition(String name);

  /**
   * gets the method associated with the given name and signature
   *
   * @param name the name of the method
   * @param signature the signature (formal parameter types) of the method
   *
   * @return <code>MethodDef</code>
   *
   * @see MethodSignature
   */
  public IMethod getMethodDefinition(String name,
                                     ISignature signature);

  /**
   * gets the <code>VariableDef</code> associated with the given name
   *
   * @param name the name of the variable
   *
   * @return <code>VariableDef</code>
   */
  public IVariable getVariableDefinition(String name);

  // end definitions interface

  /**
   * adds <code>ClassDef</code> to the collection of (direct?) subclasses of
   * this class
   *
   * @param subclass the class to add
   * @return <code>void</code>
   */
  public void addSubclass(ClassDef subclass);

  /**
   * adds <code>ClassDef</code> to the collection of implemented interfaces
   * of this class
   *
   * @param implementor the interface to add
   * @return <code>void</code>
   */
  public void addImplementor(ClassDef implementor);

  /**
   * gets the list of <code>ClassDefs</code> that implmement this interface
   *
   * @return Vector the list of implementors
   */
  public List getImplementors();

  /**
   * verifies if the input type is equal to this class or its superclass or
   * its interfaces
   * @param type class to be compared with
   * @return <code>true</code> if the input type is equals
   *         <code>false</code> otherwise
   */
  public boolean isCompatibleWith(IClass type);

  /**
   * verifies if this class is of primitive Java type
   * @return <code>true</code> if the class is a primitive type
   *         <code>false</code> otherwise
   */
  public boolean isPrimitive();

  /**
   * gets inner classes definition associated with this class
   * @return array of inner classes
   */
  public IClass[] getInnerClasses();

}
