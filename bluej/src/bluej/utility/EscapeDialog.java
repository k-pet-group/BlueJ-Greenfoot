package bluej.utility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
  protected JRootPane createRootPane() {
    ActionListener actionListener = new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
      }
    };
    JRootPane rootPane = new JRootPane();
    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    rootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    return rootPane;
  }
}

