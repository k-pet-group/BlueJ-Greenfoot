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

// $Id: BlockBracerTest.java 1014 2001-11-30 03:28:10Z ajp $
package net.sourceforge.transmogrify.twiddle.test;

import net.sourceforge.transmogrify.symtab.parser.FileParser;
import net.sourceforge.transmogrify.symtab.parser.ASTUtil;
import net.sourceforge.transmogrify.symtab.parser.TreePane;

import net.sourceforge.transmogrify.symtab.test.DefinitionLookupTest;

import net.sourceforge.transmogrify.twiddle.*;

import java.io.*;

public class BlockBracerTest extends DefinitionLookupTest {
  File original;
  File expected;

  public BlockBracerTest(String name) {
    super(name);
  }

  public void testFor() throws Exception {
    original = new File("test/twiddle/UnbracedFor.java");
    createQueryEngine(new File[] { original });

    BlockBracer bracer = new BlockBracer();
    bracer.setup(table);
    bracer.braceBlocks();

    FileParser fp = new FileParser();
    expected = new File("test/twiddle/UnbracedFor.twiddled.java");
    fp.doFile(expected);

    assert("trees not equal",
           ASTUtil.treesBelowFilesAreEqual(bracer.getTree(),
                                           new File[] { original },
                                           fp.getTree(),
                                           new File[] { expected }));
  }

  public void testDo() throws Exception {
    original = new File("test/twiddle/UnbracedDo.java");
    createQueryEngine(new File[] { original });

    BlockBracer bracer = new BlockBracer();
    bracer.setup(table);
    bracer.braceBlocks();

    FileParser fp = new FileParser();
    expected = new File("test/twiddle/UnbracedDo.twiddled.java");
    fp.doFile(expected);

    assert("trees not equal",
           ASTUtil.treesBelowFilesAreEqual(bracer.getTree(),
                                           new File[] { original },
                                           fp.getTree(),
                                           new File[] { expected }));
  }

  public void testWhile() throws Exception {
    original = new File("test/twiddle/UnbracedWhile.java");
    createQueryEngine(new File[] { original });

    BlockBracer bracer = new BlockBracer();
    bracer.setup(table);
    bracer.braceBlocks();

    FileParser fp = new FileParser();
    expected = new File("test/twiddle/UnbracedWhile.twiddled.java");
    fp.doFile(expected);

    assert("trees not equal",
           ASTUtil.treesBelowFilesAreEqual(bracer.getTree(),
                                           new File[] { original },
                                           fp.getTree(),
                                           new File[] { expected }));
  }

  public void testIf() throws Exception {
    original = new File("test/twiddle/UnbracedIf.java");
    createQueryEngine(new File[] { original });

    BlockBracer bracer = new BlockBracer();
    bracer.setup(table);
    bracer.braceBlocks();

    FileParser fp = new FileParser();
    expected = new File("test/twiddle/UnbracedIf.twiddled.java");
    fp.doFile(expected);

    assert("trees not equal",
           ASTUtil.treesBelowFilesAreEqual(bracer.getTree(),
                                           new File[] { original },
                                           fp.getTree(),
                                           new File[] { expected }));
  }

  public void testElse() throws Exception {
    original = new File("test/twiddle/UnbracedElse.java");
    createQueryEngine(new File[] { original });

    BlockBracer bracer = new BlockBracer();
    bracer.setup(table);
    bracer.braceBlocks();

    FileParser fp = new FileParser();
    expected = new File("test/twiddle/UnbracedElse.twiddled.java");
    fp.doFile(expected);

    assert("trees not equal",
           ASTUtil.treesBelowFilesAreEqual(bracer.getTree(),
                                           new File[] { original },
                                           fp.getTree(),
                                           new File[] { expected }));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(BlockBracerTest.class);
  }
}
