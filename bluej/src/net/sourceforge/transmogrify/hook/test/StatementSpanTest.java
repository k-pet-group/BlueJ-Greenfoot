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

// $Id: StatementSpanTest.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.hook.test;

import net.sourceforge.transmogrify.hook.*;
import net.sourceforge.transmogrify.symtab.Occurrence;

import java.io.File;

import junit.framework.*;

public class StatementSpanTest extends TestCase {
  File file;

  public StatementSpanTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/hook/TryCatch.java");
  }

  public void testSpanCalculationOne() throws Exception {
    HookImpl hook = new HookImpl();

    hook.openFile(file.getPath());
    hook.selectText(11, 1, 20, 1);

    StatementSpan span = new StatementSpan(hook);

    Occurrence actualStart = span.getStart();
    Occurrence actualEnd = span.getEnd();

    Occurrence expectedStart = new Occurrence(file, 11, 5);
    Occurrence expectedEnd = new Occurrence(file, 19, 23);

    assertEquals("start location incorrect", expectedStart, actualStart);
    assertEquals("end location incorrect", expectedEnd, actualEnd);
  }

  public void testSpanCalculationTwo() throws Exception {
    HookImpl hook = new HookImpl();

    hook.openFile(file.getPath());
    hook.selectText(11, 1, 14, 1);

    StatementSpan span = new StatementSpan(hook);

    Occurrence actualStart = span.getStart();
    Occurrence actualEnd = span.getEnd();

    Occurrence expectedStart = new Occurrence(file, 11, 5);
    Occurrence expectedEnd = new Occurrence(file, 13, 10);

    assertEquals("start location incorrect", expectedStart, actualStart);
    assertEquals("end location incorrect", expectedEnd, actualEnd);
  }

  public void testSpanCalculationThree() throws Exception {
    HookImpl hook = new HookImpl();

    hook.openFile(file.getPath());
    hook.selectText(11, 1, 18, 1);

    StatementSpan span = new StatementSpan(hook);

    Occurrence actualStart = span.getStart();
    Occurrence actualEnd = span.getEnd();

    Occurrence expectedStart = new Occurrence(file, 11, 5);
    Occurrence expectedEnd = new Occurrence(file, 17, 7);

    assertEquals("start location incorrect", expectedStart, actualStart);
    assertEquals("end location incorrect", expectedEnd, actualEnd);
  }

  public void testSpanCalculationFour() throws Exception {
    HookImpl hook = new HookImpl();

    hook.openFile(file.getPath());
    hook.selectText(25, 1, 32, 1);

    StatementSpan span = new StatementSpan(hook);

    Occurrence actualStart = span.getStart();
    Occurrence actualEnd = span.getEnd();

    Occurrence expectedStart = new Occurrence(file, 25, 5);
    Occurrence expectedEnd = new Occurrence(file, 31, 4);

    assertEquals("start location incorrect", expectedStart, actualStart);
    assertEquals("end location incorrect", expectedEnd, actualEnd);
  }

  public void testSpanCalculationFive() throws Exception {
    HookImpl hook = new HookImpl();

    hook.openFile(file.getPath());
    hook.selectText(33, 1, 38, 1);

    StatementSpan span = new StatementSpan(hook);

    Occurrence actualStart = span.getStart();
    Occurrence actualEnd = span.getEnd();

    Occurrence expectedStart = new Occurrence(file, 33, 5);
    Occurrence expectedEnd = new Occurrence(file, 37, 37);

    assertEquals("start location incorrect", expectedStart, actualStart);
    assertEquals("end location incorrect", expectedEnd, actualEnd);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(StatementSpanTest.class);
  }
}