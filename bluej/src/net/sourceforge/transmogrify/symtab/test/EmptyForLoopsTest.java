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
package net.sourceforge.transmogrify.symtab.test;

import junit.extensions.*;
import junit.framework.*;

import java.io.File;

public class EmptyForLoopsTest extends DefinitionLookupTest {

  public EmptyForLoopsTest(String name) {
    super(name);
  }

  public void testSymbolTableCreation() throws Exception {
    createQueryEngine(new File[] {new File("test/EmptyForLoops.java")});
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { EmptyForLoopsTest.class.getName() });
  }
}
