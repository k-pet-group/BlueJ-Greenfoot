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

// $Id: UnknownClass.java 1011 2001-11-22 10:36:26Z ajp $

import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;


public class UnknownClass implements IClass {

  String _name;
  SymTabAST _node;

  public UnknownClass(String name, SymTabAST node) {
    _name = name;
    _node = node;
    //System.out.println("Creating unknown class [" + name + " : " + node + "]");
  }

  /**
   * returns the <code>ClassDef</code> that for the superclass
   *
   * @return the <code>ClassDef</code> for the superclass
   */
  public IClass getSuperclass() {
    return null;
  }

  public IClass[] getInterfaces() {
    return new IClass[0];
  }

  /**
   * returns a collection of the direct subclasses of this class
   *
   * @return a collection of the direct subclasses of this class
   */
  public List getSubclasses() {
    return new Vector();
  }

  public IClass getClassDefinition(String name) {
    return null;
  }

  /**
   * gets the method associated with the given name and signature
   *
   * @param name the name of the method
   * @param signature the signature (formal parameter types) of the method
   *
   * @return MethodDef
   *
   * @see MethodSignature
   */
  public IMethod getMethodDefinition(String name,
                                     ISignature signature) {
    return null;
  }

  /**
   * gets the <code>VariableDef</code> associated with the given name
   *
   * @param name the name of the variable
   *
   * @return VariableDef
   */
  public IVariable getVariableDefinition(String name) {
    return null;
  }

  // end definitions interface

  /**
   * adds <code>ClassDef</code> to the collection of (direct?) subclasses of
   * this class
   *
   * @param subclass the class to add
   */
  public void addSubclass(ClassDef subclass) {}

  /**
   * adds <code>ClassDef</code> to the collection of implemented interfaces
   * of this class
   *
   * @param implementor the interface to add
   */
  public void addImplementor(ClassDef implementor) {}

  /**
   * gets the list of <code>ClassDefs</code> that implmement this interface
   *
   * @return Vector the list of implementors
   */
  public List getImplementors() {
    return new Vector();
  }

  public boolean isCompatibleWith(IClass type) {
    return false;
  }

  public void addReference(Reference reference) {}
  public Iterator getReferences() {
    return new Vector().iterator();
  }

  public int getNumReferences() {
    return 0;
  }

  public boolean isPrimitive() {
    return false;
  }

  public boolean isSourced() {
    return false;
  }

  public IClass[] getInnerClasses() {
    return new IClass[0];
  }

  public String getName() {
    return _name;
  }

  public String getQualifiedName() {
    return _name;
  }

  public boolean equals(Object o) {
    return false;
  }

  public String toString() {
    return UnknownClass.class + "[" + getName() + "]";
  }
}
