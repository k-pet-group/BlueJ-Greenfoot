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


import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.symtab.*;


public abstract class LookupRefactorer extends Transmogrifier {

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
    IDefinition def = query.getDefinition(location);
    return (def != null &&
            def instanceof Definition &&
            !table.isOutOfDate());
  }

}
