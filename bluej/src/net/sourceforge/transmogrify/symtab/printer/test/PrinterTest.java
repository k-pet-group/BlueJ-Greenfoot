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
package net.sourceforge.transmogrify.symtab.printer.test;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.printer.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import antlr.collections.*;

import junit.extensions.*;
import junit.framework.*;

import java.io.*;

public class PrinterTest extends TestCase {
  FileParser _original;
  FileParser _extraCrispy;

  public PrinterTest(String name) {
    super(name);
  }

  public void tearDown() {}

  public void testWhateverIsInThingy() throws Exception {
    doTheTest(new File("test/Thingy.java"));
  }

  public void testWhateverIsInWeird() throws Exception {
    doTheTest(new File("test/Weird.java"));
  }

  public void testWhateverIsInMamby() throws Exception {
    doTheTest(new File("test/Mamby.java"));
  }

  public void testWhateverIsInPamby() throws Exception {
    doTheTest(new File("test/Pamby.java"));
  }

  public void testWhateverIsInBingo() throws Exception {
    doTheTest(new File("test/Bingo.java"));
  }

  public void testTestThePrinter() throws Exception {
    doTheTest(new File("test/printer/TestThePrinter.java"));
  }

  public void testArrayDeclaration() throws Exception {
    doTheTest(new File("test/printer/Arrays.java"));
  }

  public void testExceptionPrinting() throws Exception {
    doTheTest(new File("test/printer/Exceptions.java"));
  }

  public void testImportSort() throws Exception {
    FileParser original = new FileParser();
    original.doFile(new File("test/printer/ImportSort.java"));
    Printer printer = PrinterFactory.makePrinter(original.getTree());
    printer.print(new PrettyPrinter(new FileWriter("test/intermediate.java")));

    FileParser expected = new FileParser();
    expected.doFile(new File("test/printer/ImportSort.sorted.java"));

    FileParser actual = new FileParser();
    actual.doFile(new File("test/intermediate.java"));

    walkLists(expected, actual);
  }

  private void doTheTest(File file) throws Exception {
    _original = new FileParser();
    _original.doFile(file);

    Printer printer = PrinterFactory.makePrinter(_original.getTree());
    printer.print(new PrettyPrinter(new FileWriter("test/intermediate.java")));

    _extraCrispy = new FileParser();
    _extraCrispy.doFile(new File("test/intermediate.java"));

    walkLists(_original, _extraCrispy);
  }

  private void walkLists(FileParser original, FileParser extraCrispy) throws Exception {
    AST originalNode = original.getTree().getFirstChild().getFirstChild();
    AST extraCrispyNode = extraCrispy.getTree().getFirstChild().getFirstChild();

    while(originalNode != null) {
      assert(originalNode.equalsTree(extraCrispyNode));
      originalNode = originalNode.getNextSibling();
      extraCrispyNode = extraCrispyNode.getNextSibling();
    }

    assert(originalNode == null);
    assert(extraCrispyNode == null);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.run(PrinterTest.class);
  }
}
