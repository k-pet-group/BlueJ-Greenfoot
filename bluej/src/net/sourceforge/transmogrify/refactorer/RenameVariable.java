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


import java.util.*;

import net.sourceforge.transmogrify.hook.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;

public class RenameVariable extends Transmogrifier {
  public RenameVariable() {
    super();
  }

  public RenameVariable(ASTPrintManager printManager) {
    super(printManager);
  }

  public void apply(Hook hook) throws Exception {
    String renameTo = hook.getUserInput("Rename to what?", "Rename");

    Occurrence location = hook.makeOccurrence();

    refactor(location, renameTo);
  }

  public void refactor(Occurrence location, String newName) throws Exception {

    Iterator references = query.getReferences(location);

    while (references.hasNext()) {
      renameReference((Reference)references.next(), newName);
    }

    streamFiles();
  }

  public boolean canApply(Hook hook) {
    boolean result = false;
    Occurrence location = null;

    try {
      location = hook.makeOccurrence();
      result = canRefactor(location);
    }
    catch (Exception ignoreMe) {}

    return result;
  }

  public boolean canRefactor(Occurrence location) {
    boolean result = false;

    IDefinition def = query.getDefinition(location);
    if (def != null) {
      result = (def instanceof VariableDef
                || def instanceof MethodDef
                || def instanceof LabelDef);
    }

    result &= !table.isOutOfDate();

    return result;
  }

}
