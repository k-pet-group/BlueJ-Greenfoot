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

// $Id: AnonymousLocalScopeTest.java 1014 2001-11-30 03:28:10Z ajp $

import java.io.File;
import java.util.*;
import net.sourceforge.transmogrify.symtab.*;

public class AnonymousLocalScopeTest extends DefinitionLookupTest {
  File _file;
  Definition _xLocal;
  Definition _xMethod;

  public AnonymousLocalScopeTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    _file = new File("test/LocalScope.java");

    createQueryEngine(new File[] { _file });

    _xLocal = (Definition)getDefinition(_file, "x", 6, 11);
    _xMethod = (Definition)getDefinition(_file, "x", 9, 9);
  }

  public void testLookupLocal() throws Exception {
    IDefinition ref = getDefinition(_file, "x", 7, 7);

    assertNotNull("local IDefinition not found", _xLocal);
    assertEquals("reference doesn't point to IDefinition", _xLocal, ref);
  }

  public void testLookupMethod() throws Exception {
    IDefinition ref = getDefinition(_file, "x", 10, 5);

    assertNotNull("method IDefinition not found", _xMethod);
    assertEquals("reference doesn't point to IDefinition", _xMethod, ref);
  }

  public void testBlockScope() throws Exception {
    assertNotNull(_xLocal);
    assertNotNull(_xMethod);

    Scope parent = _xLocal.getParentScope();
    assert("not its own scope", !parent.equals(_xMethod.getParentScope()));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(AnonymousLocalScopeTest.class);
  }
}
