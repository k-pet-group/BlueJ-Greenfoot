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
package net.sourceforge.transmogrify.symtab.parser;

import antlr.*;
import antlr.collections.AST;
import antlr.debug.misc.*;

import java.awt.event.*;

// $Id: TreePane.java 1011 2001-11-22 10:36:26Z ajp $


/**
 * <code>TreePane</code> is a <code>Utility Class</code> that contains utility code
 * for creating a window to display the contents of an AST.
 *
 * @see AST
 */

public class TreePane {

  /**
   * Opens a new Frame titled "Java AST" and displays the tree represented by
   * root.
   *
   * @see AST
   *
   * @return void
   * @param root The AST tree to be displayed
   */
  public static void showTree(AST root) {
    final ASTFrame frame = new ASTFrame("Java AST", root);
    frame.setVisible(true);
    //frame.addWindowListener(
    //  new WindowAdapter() {
    //             public void windowClosing (WindowEvent e) {
    //                 frame.setVisible(false); // hide the Frame
    //                 frame.dispose();
    //                 System.exit(0);
    //             }
    //      }
    //);
  }
}
