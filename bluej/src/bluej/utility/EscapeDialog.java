/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.utility;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.Swing)
public class EscapeDialog extends JDialog {
  public EscapeDialog() {
    this((Frame)null, false);
  }
  public EscapeDialog(Frame owner) {
    this(owner, false);
  }
  public EscapeDialog(Frame owner, boolean modal) {
    this(owner, null, modal);
  }
  public EscapeDialog(Frame owner, String title) {
    this(owner, title, false);     
  }
  public EscapeDialog(Frame owner, String title, boolean modal) {
    super(owner, title, modal);
  }
  public EscapeDialog(Dialog owner) {
    this(owner, false);
  }
  public EscapeDialog(Dialog owner, boolean modal) {
    this(owner, null, modal);
  }
  public EscapeDialog(Dialog owner, String title) {
    this(owner, title, false);     
  }
  public EscapeDialog(Dialog owner, String title, boolean modal) {
    super(owner, title, modal);
  }
  
  @Override
  protected JRootPane createRootPane() {
      ActionListener actionListener = new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
              setVisible(false);
          }
      };
      JRootPane rootPane = super.createRootPane();
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      rootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
      return rootPane;
  }
}
