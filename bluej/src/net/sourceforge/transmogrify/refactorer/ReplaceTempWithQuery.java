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

// $Id: ReplaceTempWithQuery.java 1011 2001-11-22 10:36:26Z ajp $

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.symtab.*;

public class ReplaceTempWithQuery extends Transmogrifier {
  public ReplaceTempWithQuery() {
    super();
  }

  public void apply(Hook hook) throws Exception {
    try {
      refactor(hook.makeOccurrence());
    }
    catch (RefactoringException e) {
      hook.displayException(e, e.getMessage());
    }

    // REDTAG -- we'll want to do these in (almost?) every refactoring
    // consider template algorithm (or something)
    streamFiles();
  }

  public void refactor(Occurrence location) throws Exception {
    // declare temp as final
    // compile
    // extract rhs into a method

    TrivialExtractMethod refOne = new TrivialExtractMethod();
    refOne.setup(table);
    refOne.refactor(location);

    // compile/test
    // use inline temp
    InlineTemp refTwo = new InlineTemp();
    refTwo.setup(table);

    // must set caret position....
    // REDTAG -- think about caret position being an AST node
    //           or converter caret pos <--> node
    refTwo.refactor(location);
  }

  public boolean canApply(Hook hook) {
    return !table.isOutOfDate();
  }
}
