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

// $Id: DefaultScope.java 1011 2001-11-22 10:36:26Z ajp $

import net.sourceforge.transmogrify.symtab.parser.*;
import java.util.*;

public class DefaultScope extends Scope {
  public DefaultScope(String name, Scope parentScope, SymTabAST node) {
    super(name, parentScope, node);
  }

  public void addDefinition(IPackage def) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public IClass getClassDefinition( String name ) {
    IClass result = (ClassDef)classes.get(name);

    if ( result == null && getParentScope() != null ) {
      result = getParentScope().getClassDefinition( name );
    }

    return result;
  }

  public IMethod getMethodDefinition(String name,
                                     ISignature signature) {
    IMethod result = null;
    if (getParentScope() != null) {
      result = getParentScope().getMethodDefinition(name, signature);
    }

    return result;
  }

  public IVariable getVariableDefinition( String name ) {
    IVariable result = (VariableDef)elements.get(name);

    if ( result == null && getParentScope() != null ) {
      result = getParentScope().getVariableDefinition( name );
    }

    return result;
  }

  public LabelDef getLabelDefinition(String name) {
    LabelDef result = (LabelDef)labels.get(name);

    if (result == null && getParentScope() != null) {
      result = getParentScope().getLabelDefinition(name);
    }

    return result;
  }
}
