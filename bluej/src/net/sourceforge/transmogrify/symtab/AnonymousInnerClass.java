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


import net.sourceforge.transmogrify.symtab.parser.*;


public class AnonymousInnerClass extends ClassDef {

  protected SymTabAST _objblock;
  protected SymTabAST _classExtended;

  public AnonymousInnerClass(SymTabAST objblock,
                             SymTabAST classExtended,
                             Scope parentScope) {
    super(parentScope.getName() + "$" + parentScope.getEnclosingClass().getNextAnonymousId(),
          parentScope,
          objblock);

    _objblock = objblock;
    _classExtended = classExtended;
  }

  public void finishMakingDefinition() {
    String extendedClassName = ASTUtil.constructDottedName(_classExtended);
    IClass superclass = getClassDefinition(extendedClassName);

    if (superclass != null) {
      setSuperclass(superclass);
      superclass.addSubclass(this);
    }
  }

}
