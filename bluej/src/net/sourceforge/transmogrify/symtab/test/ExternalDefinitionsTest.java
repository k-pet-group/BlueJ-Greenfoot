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

// $Id: ExternalDefinitionsTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class ExternalDefinitionsTest extends DefinitionLookupTest {
  File _file;

  public ExternalDefinitionsTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    _file = new File("test/ExternalDefinition.java");
    createQueryEngine(new File[] { _file });
  }

  public void testExternalDefinition() throws Exception {
    IDefinition def = query.getDefinition(new Occurrence(_file, 6, 5));
    assertNotNull("couldn't find definition", def);
  }

  public void testInternalDefinition() throws Exception {
    // this is a sanity check, when it fails, there are *big* problems
    IDefinition def = query.getDefinition(new Occurrence(_file, 7, 5));
    assertNotNull("couldn't find definition", def);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(ExternalDefinitionsTest.class);
  }
}
