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

import java.lang.reflect.*;

// $Id: ExternalPackage.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>PackageDef</code> contains all pertinent information about a
 * package.
 */
public class ExternalPackage extends ExternalDefinition implements IPackage {

  String _name;
  IPackage _parent;

  Map _packages;

  public ExternalPackage(String name, IPackage parent) {
    _name = name;
    _parent = parent;

    _packages = new HashMap();
  }

  public IClass getClass(String name) {
    IClass result = null;

    try {
      Class theClass
        = ClassManager.getClassLoader().loadClass(getQualifiedName()
                                                  + "."
                                                  + name);
      result = new ExternalClass(theClass);
    }
    catch (ClassNotFoundException e) {
      // look elsewhere for the class
    }

    return result;
  }

  public void addDefinition(IPackage pkg) {
    _packages.put(pkg.getName(), pkg);
  }

  public IPackage getEnclosingPackage() {
    return _parent;
  }

  public String getName() {
    return _name;
  }

  public String getQualifiedName() {
    StringBuffer result = new StringBuffer();

    if (_parent != null) {
      result.append(_parent.getQualifiedName());
      result.append(".");
    }

    result.append(getName());

    return result.toString();
  }
}
