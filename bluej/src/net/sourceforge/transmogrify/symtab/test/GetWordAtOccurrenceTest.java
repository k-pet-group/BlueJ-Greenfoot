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

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.*;
import java.io.File;

public class GetWordAtOccurrenceTest extends TestCase {

  private File file =
    new File("test/symtab/QueryEngine/GetWordAtOccurrence/Test.java");
  private QueryEngine query = null;


  public GetWordAtOccurrenceTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    FileParser parser = new FileParser();
    parser.doFile(file);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    query = new QueryEngine(table);
  }

  public void testClassVarDeclaration() throws Exception {
    Occurrence location = new Occurrence(file, 3, 15);
    String result = query.getWordAtOccurrence(location);
    assertNotNull(result);
    assertEquals("anInt", result);
  }

  public void testMethodDeclaration() throws Exception {
    Occurrence location = new Occurrence(file, 5, 15);
    String result = query.getWordAtOccurrence(location);
    assertNotNull(result);
    assertEquals("method", result);
  }

  public void testInsideLoop() throws Exception {
    Occurrence location = new Occurrence(file, 10, 7);
    String result = query.getWordAtOccurrence(location);
    assertNotNull(result);
    assertEquals("x", result);
  }

  public void testMethodCall() throws Exception {
    Occurrence location = new Occurrence(file, 15, 5);
    String result = query.getWordAtOccurrence(location);
    assertNotNull(result);
    assertEquals("method", result);
  }

  public void testMiddleOfWord() throws Exception {
    Occurrence location = new Occurrence(file, 15, 7);
    String result = query.getWordAtOccurrence(location);
    assertNotNull(result);
    assertEquals("method", result);
  }

  public void testEndOfWord() throws Exception {
    Occurrence location = new Occurrence(file, 15, 10);
    String result = query.getWordAtOccurrence(location);
    assertNotNull(result);
    assertEquals("method", result);
  }

  public void testNoWord() throws Exception {
    Occurrence location = new Occurrence(file, 13, 3);
    String result = query.getWordAtOccurrence(location);
    assert(result == null);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { GetWordAtOccurrenceTest.class.getName() });
  }
}
