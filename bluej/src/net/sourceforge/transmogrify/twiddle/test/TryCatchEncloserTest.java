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

// $Id: TryCatchEncloserTest.java 1014 2001-11-30 03:28:10Z ajp $
package net.sourceforge.transmogrify.twiddle.test;

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.test.DefinitionLookupTest;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;
import net.sourceforge.transmogrify.twiddle.*;

import java.io.File;
import java.io.FileWriter;

public class TryCatchEncloserTest extends DefinitionLookupTest {
  File original;
  File exception;

  public TryCatchEncloserTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    original = new File("test/twiddle/TryCatch.java");
    exception = new File("test/twiddle/ExceptionA.java");

    createQueryEngine(new File[] { original,
                                   exception });
  }

  public void testEnclosure() throws Exception {
    TryCatchEncloser encloser = new TryCatchEncloser();
    encloser.setup(table);

    File compared = new File("test/twiddle/TryCatch.twiddled1.java");

    Occurrence start = new Occurrence(original, 11, 5);
    Occurrence end   = new Occurrence(original, 13, 10);

    encloser.enclose(start, end);

    FileParser fp = new FileParser();
    fp.doFile(compared);
    fp.doFile(exception);

    assert("ASTs do not match",
           ASTUtil.treesBelowFilesAreEqual(fp.getTree(),
                                           new File[] { compared, exception },
                                           encloser.getTree(),
                                           new File[] { original, exception }));
  }

  public void testEncloserOfExternalException() throws Exception {
    TryCatchEncloser encloser = new TryCatchEncloser();
    encloser.setup(table);

    Occurrence start = new Occurrence(original, 11, 5);
    Occurrence end   = new Occurrence(original, 17, 7);

    File compared = new File("test/twiddle/TryCatch.twiddled2.java");

    encloser.enclose(start, end);

    FileParser fp = new FileParser();
    fp.doFile(compared);
    fp.doFile(exception);

    assert("ASTs do not match",
           ASTUtil.treesBelowFilesAreEqual(fp.getTree(),
                                           new File[] { compared, exception },
                                           encloser.getTree(),
                                           new File[] { original, exception }));
  }

  public void testEnclosureOfExplicitThrow() throws Exception {
    TryCatchEncloser encloser = new TryCatchEncloser();
    encloser.setup(table);

    File compared = new File("test/twiddle/TryCatch.twiddled3.java");

    Occurrence start = new Occurrence(original, 11, 5);
    Occurrence end   = new Occurrence(original, 19, 23);

    encloser.enclose(start, end);

    FileParser fp = new FileParser();
    fp.doFile(compared);
    fp.doFile(exception);

    assert("ASTs do not match",
           ASTUtil.treesBelowFilesAreEqual(fp.getTree(),
                                           new File[] { compared, exception },
                                           encloser.getTree(),
                                           new File[] { original, exception }));
  }

  public void testNoExceptionsThrown() throws Exception {
    TryCatchEncloser encloser = new TryCatchEncloser();
    encloser.setup(table);

    Occurrence start = new Occurrence(original, 21, 5);
    Occurrence end   = new Occurrence(original, 25, 37);

    try {
      encloser.enclose(start, end);
      fail("NoExceptionsFoundException not raised.");
    }
    catch (NoExceptionsThrownException e) {
      FileParser fp = new FileParser();
      fp.doFile(original);
      fp.doFile(exception);

      assert("ASTs do not match",
             ASTUtil.treesBelowFilesAreEqual(fp.getTree(),
                                             new File[] { original, exception },
                                             encloser.getTree(),
                                             new File[] { original, exception }));
    }
  }

  public void testFruit() {
    int apples = 3;
    int oranges = 3;

    assertEquals("but you're comparing apples to oranges!", apples, oranges);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(TryCatchEncloserTest.class);
  }
}