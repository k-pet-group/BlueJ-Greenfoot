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
 * the bottom scope of the scope stack, contains some extra information
 * to make resolution easier.
 */

// $Id: BaseScope.java 1011 2001-11-22 10:36:26Z ajp $

public class BaseScope extends DefaultScope {
  private SymbolTable table;

  public BaseScope( SymbolTable symbolTable ) {
    super("~BASE~", null, null);
    this.table = symbolTable;
  }

  public boolean isBaseScope() {
    return true;
  }

  public void addDefinition(IPackage def) {
    elements.put(def.getName(), def);
  }

  /**
   * gets the package associated with a fully qualified name
   *
   * @param fullyQualifiedName the name of the package
   *
   * @return the package that was gotten
   */
  public IPackage getPackageDefinition(String fullyQualifiedName) {
    return (IPackage)(table.getPackages().get(fullyQualifiedName));
  }

  public IClass getClassDefinition(String name) {
    IClass result = null;

    result = LiteralResolver.getDefinition(name);

    if (result == null) {
      int lastDot = name.lastIndexOf(".");
      if (lastDot > 0) {
        String packageName = name.substring(0, lastDot);
        String className = name.substring(lastDot + 1);

        IPackage pkg = getPackageDefinition(packageName);
        if (pkg != null) {
          result = pkg.getClass(className);
        }
      }
    }

    if (result == null) {
      Class theClass = null;
      try {
        theClass = ClassManager.getClassLoader().loadClass(name);
        result = new ExternalClass(theClass);
      }
      catch (ClassNotFoundException e) {
        // no-op
      }
    }

    return result;
  }
}

