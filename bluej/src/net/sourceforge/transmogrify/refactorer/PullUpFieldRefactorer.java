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

package net.sourceforge.transmogrify.refactorer;

import java.io.File;

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;

public class PullUpFieldRefactorer extends Transmogrifier {

  public PullUpFieldRefactorer() {
    super();
  }

  public PullUpFieldRefactorer(ASTPrintManager manager) {
    super(manager);
  }

  public void apply(Hook hook) throws Exception {
    refactor(hook.makeOccurrence());
    streamFiles();
  }

  public void refactor(Occurrence location) {
    VariableDef variable = getVariableAtLocation(location);
    SymTabAST variableNode = variable.getTreeNode();

    ClassDef superclass =
      (ClassDef)variable.getEnclosingClass().getSuperclass();

    if (variable.getVisibility() == VariableDef.PRIVATE_VISIBILITY ||
        variable.getVisibility() == VariableDef.DEFAULT_VISIBILITY) {
      variable.setVisibility(VariableDef.PROTECTED_VISIBILITY);
    }

    removeNode(variableNode);
    superclass.addNewVariable(variableNode);

    if (superclass.getClassDefinition(variable.getType().getName()) == null) {
      File fileForClass = superclass.getTreeNode().getFile();
      addImportToFile(fileForClass, variable.getType());
    }
  }

  public boolean canApply(Hook hook) {
    boolean result = false;
    Occurrence location = null;

    try {
      location = hook.makeOccurrence();
      result = canRefactor(location);
    }
    catch (Exception ignoreMe) {}

    result &= !table.isOutOfDate();

    return result;
  }

  public boolean canRefactor(Occurrence location) {
    boolean result = false;

    // REDTAG -- at some point, check whether the superclass already
    // has a (private) variable of the same name defined

    VariableDef variable = getVariableAtLocation(location);
    if (variable != null && !variable.isAssignedAtDeclaration()) {
      Scope scope = variable.getParentScope();

      if (scope instanceof ClassDef) {
        ClassDef enclosingClass = (ClassDef)scope;
        IClass superclass = enclosingClass.getSuperclass();
        if (superclass != null && superclass.isSourced()) {
          result = true;
        }
      }

    }

    return result;
  }

  private VariableDef getVariableAtLocation(Occurrence location) {
    VariableDef result = null;

    IDefinition definition = query.getDefinition(location);
    if (definition instanceof VariableDef) {
      result = (VariableDef)definition;
    }

    return result;
  }

}
