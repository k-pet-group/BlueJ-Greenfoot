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

public class NullClass implements IClass {

  public Iterator getReferences() {
    return new ArrayList().iterator();
  }

  public int getNumReferences() {
    return 0;
  }

  public void addReference(Reference ref) {}

  public String getName() {
    return "null";
  }

  public String getQualifiedName() {
    return getName();
  }

  public boolean isSourced() {
    return false;
  }

  public IClass getSuperclass() {
    return null;
  }

  public IClass[] getInterfaces() {
    return new IClass[0];
  }

  public IClass[] getInnerClasses() {
    return new IClass[0];
  }

  public List getSubclasses() {
    return new ArrayList();
  }

  public IClass getClassDefinition(String name) {
    return null;
  }

  public IMethod getMethodDefinition(String name,
                                     ISignature signature) {
    return null;
  }

  public IVariable getVariableDefinition(String name) {
    return null;
  }

  public void addSubclass(ClassDef subclass) {}
  public void addImplementor(ClassDef implementor) {}

  public List getImplementors() {
    return new ArrayList();
  }

  public boolean isCompatibleWith(IClass type) {
    return true;
  }

  public boolean isPrimitive() {
    return false;
  }

}




