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

// $Id: Transmogrifier.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.hook;

import net.sourceforge.transmogrify.symtab.parser.ASTManipulator;

import net.sourceforge.transmogrify.symtab.printer.ASTPrintManager;
import net.sourceforge.transmogrify.symtab.printer.FilePrintManager;

public abstract class Transmogrifier extends ASTManipulator {
  public Transmogrifier(ASTPrintManager manager) {
    super(manager);
  }

  public Transmogrifier() {
    this(new FilePrintManager());
  }

  public abstract void apply(Hook hook) throws Exception;
  public abstract boolean canApply(Hook hook);
}