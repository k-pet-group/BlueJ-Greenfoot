package javax.swing.beaninfo; 

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.colorchooser.*;

public class ChooserComboPopup extends JPopupMenu {
  SwingColorEditor ce;

  public ChooserComboPopup(SwingColorEditor c){
    super();
    this.ce = c;
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    SwatchChooserPanel s = new SwatchChooserPanel(c,this);
    s.buildChooser();
    p.add(s,BorderLayout.NORTH); 
    JButton b = new JButton("Other ...");
    b.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
	Color color = null;
	color = JColorChooser.showDialog(getParent(), "Color Chooser", color);
	getCE().setValue(color);
	// XXX: update the recentSwatch
	setVisible(false);
      }
    });
    p.add(b, BorderLayout.SOUTH); // , BorderLayout.SOUTH);
    add(p);
    addMouseListener(new PopupListener());
  }

  public SwingColorEditor getCE(){
    return this.ce;
  }

  class PopupListener extends MouseAdapter {
    public void mousePressed(MouseEvent e){
    }

    public void mouseReleased(MouseEvent e){
      setVisible(false);
    }
  }

}
