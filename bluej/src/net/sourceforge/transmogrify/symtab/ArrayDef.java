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

// $Id: ArrayDef.java 1011 2001-11-22 10:36:26Z ajp $

import java.util.*;
import net.sourceforge.transmogrify.symtab.parser.JavaTokenTypes;

public class ArrayDef implements IClass {

  private final static IVariable LENGTH_MEMBER = new ArrayLengthMember();

  private IClass _type;

  public ArrayDef(IClass type){
    _type = type;
  }

  public IClass getType(){
    return _type;
  }

  public IClass getSuperclass() {
    return new ArrayDef(getType().getSuperclass());
  }

  public IClass[] getInterfaces() {
    return new IClass[0];
  }

  public IClass[] getInnerClasses() {
    return new IClass[0];
  }

  public IClass getClassDefinition(String name) {
    return null;
  }

  public IMethod getMethodDefinition(String name,
                                     ISignature signature) {
      return new ExternalClass(Object.class).getMethodDefinition(name,
                                                                 signature);
  }

  public IVariable getVariableDefinition(String name) {
    IVariable result = null;
    
    if (name.equals("length")) {
      result = LENGTH_MEMBER;
    }

    return result;
  }

  public void addSubclass(ClassDef subclass) {}

  public void addReference(Reference reference) {}

  public Iterator getReferences() {
    return new Vector().iterator();
  }

  public int getNumReferences() {
    return 0;
  }

  public List getSubclasses() {
    return new ArrayList();
  }

  public void addImplementor(ClassDef implementor) {}

  public List getImplementors() {
    return new ArrayList();
  }

  public boolean isCompatibleWith(IClass type) {
    boolean result = false;
    if (type.equals(new ExternalClass(Object.class))) {
      result = true;
    }
    else if (type instanceof ArrayDef) {
      result = getType().isCompatibleWith(((ArrayDef)type).getType());
    }

    return result;
  }

  public boolean isSourced() {
    return getType().isSourced();
  }

  public String getName() {
    return getType().getName() + "[]";
  }

  public String getQualifiedName() {
    return getType().getQualifiedName() + "[]";
  }

  public boolean isPrimitive() {
    return false;
  }

  public boolean equals(Object obj) {
    boolean result = false;

    if (obj instanceof ArrayDef) {
      ArrayDef compared = (ArrayDef)obj;
      result = (getType().equals(compared.getType()));
    }

    return result;
  }

  public int hashCode() {
    return getType().hashCode();
  }

  public String toString() {
    return getQualifiedName() + "[]";
  }

}
