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

// $Id: TwiddleSuite.java 1014 2001-11-30 03:28:10Z ajp $
package net.sourceforge.transmogrify.twiddle.test;

import junit.framework.*;

public class TwiddleSuite extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new TestSuite(TryCatchEncloserTest.class));
    suite.addTest(new TestSuite(BlockBracerTest.class));

    return suite;
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(TwiddleSuite.class);
  }
}