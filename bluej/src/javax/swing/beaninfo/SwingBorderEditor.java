
package javax.swing.beaninfo;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

public class SwingBorderEditor extends SwingEditorSupport {
  
  private JComboBox borderCombo;
  private JButton  borderButton;
  private BorderDialog borderDialog;

  Border etched = BorderFactory.createEtchedBorder();
  Border bevelLowered = BorderFactory.createLoweredBevelBorder();
  Border bevelRaised =BorderFactory.createRaisedBevelBorder();
  Border line = BorderFactory.createLineBorder(Color.black);
  Border borders[] = {
          etched, bevelLowered, bevelRaised, line };

  private Border border;

  public SwingBorderEditor(){
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    UIDefaults table = UIManager.getDefaults();
    table.put("beaninfo.BorderIcon", LookAndFeel.makeIcon(getClass(), "icons/BorderIcon.gif"));
        table.put("beaninfo.BorderBevelLowered", LookAndFeel.makeIcon(getClass(), "icons/BorderBevelLowered.gif"));
        table.put("beaninfo.BorderBevelRaised", LookAndFeel.makeIcon(getClass(), "icons/BorderBevelRaised.gif"));
        table.put("beaninfo.BorderEtched", LookAndFeel.makeIcon(getClass(), "icons/BorderEtched.gif"));
        table.put("beaninfo.BorderLine", LookAndFeel.makeIcon(getClass(), "icons/BorderLine.gif"));
    Icon buttonIcon = UIManager.getIcon("beaninfo.BorderIcon");

        borderCombo = createComboBox();

    // need rigid area match up
    borderButton = new JButton(buttonIcon);
    Dimension d = new Dimension(buttonIcon.getIconWidth(), buttonIcon.getIconHeight());
    borderButton.setPreferredSize(d);
    borderButton.setMinimumSize(d);
    borderButton.setMaximumSize(d);    
    borderButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        if (borderDialog == null)
          borderDialog = new BorderDialog(panel.getParent(), "Border Chooser");
        border = borderDialog.showDialog();
        if (!(borderDialog.isCancelled()))
          setValue(border);
      }
    });
    setAlignment(borderCombo);
    setAlignment(borderButton);
    panel.add(borderCombo);
    panel.add(Box.createRigidArea(new Dimension(5,0)));
    panel.add(borderButton);
    panel.add(Box.createHorizontalGlue());
  }

  public void setValue(Object value){
          super.setValue(value);
          // update our GUI state
          // set ComboBox to any equal border value
          // set BorderChooser - setSelectedBorder to any equal value as well
  }
  
  private JComboBox createComboBox(){
          DefaultComboBoxModel model = new DefaultComboBoxModel();
          for (int i = 0; i < 4; i++){
                  model.addElement(new Integer(i));
          }
          
          JComboBox c = new JComboBox(model); // borders);
          c.setRenderer(new TestCellRenderer(c));
          c.setPreferredSize(SwingEditorSupport.MEDIUM_DIMENSION); // new Dimension(120,20));
          c.setMinimumSize(SwingEditorSupport.MEDIUM_DIMENSION);
          c.setMaximumSize(SwingEditorSupport.MEDIUM_DIMENSION);
          c.setSelectedIndex(-1);
          c.addActionListener(new ActionListener(){
                  public void actionPerformed(ActionEvent e){
                          JComboBox cb = (JComboBox)e.getSource();
                          border = borders[cb.getSelectedIndex()];
                          setValue(border);
                  }
          }); 
          return c;
  }


  class TestCellRenderer extends JLabel implements ListCellRenderer   {
          JComboBox combobox;
          Icon images[] = {
                  UIManager.getIcon("beaninfo.BorderEtched"),                  
                  UIManager.getIcon("beaninfo.BorderBevelLowered"),
                  UIManager.getIcon("beaninfo.BorderBevelRaised"),
                  UIManager.getIcon("beaninfo.BorderLine") };

          String desc[] = {
                  "Etched",                  
                  "BevelLowered",
                  "BevelRaised",
                  "Line" };
          
          public TestCellRenderer(JComboBox x) {
                  this.combobox = x;
                  setOpaque(true);
          }

          public Component getListCellRendererComponent(
                  JList list,
                  Object value,
                  int modelIndex,
                  boolean isSelected,
                  boolean cellHasFocus)
                          {
                          if (value == null) {
                                  setText("");
                                  setIcon(null);
                          }
                          else {
                                  int index = ((Integer)value).intValue();
                                  if (index < 0){
                                          setText("");
                                          setIcon(null);
                                  }
                                  else {
                                          String text = " " + desc[index];
                                          setIcon(images[index]);                          
                                          setText(text);
                                          if (isSelected){
                                                  setBackground(UIManager.getColor("ComboBox.selectionBackground"));
                                                  setForeground(UIManager.getColor("ComboBox.selectionForeground"));
                                          }
                                          else {
                                                  setBackground(UIManager.getColor("ComboBox.background"));
                                                  setForeground(UIManager.getColor("ComboBox.foreground"));
                                          }
                                  }
                          }
                          return this;
                          }



















  }


  public void editorChangeValue(Object value){
  } 

  class BorderDialog extends JDialog {
    JPanel pane;
    JButton okButton;
    BorderChooser borderChooser;
    Border border = null;
    boolean cancel = false;

    public BorderDialog(Component c, String title){
      super(JOptionPane.getFrameForComponent(c), title, true);
      Container contentPane = getContentPane();
      pane = new JPanel();

      contentPane.setLayout(new BorderLayout());
      okButton = new JButton("ok"); // new BorderTracker(pane);
      ActionListener okListener = new ActionListener(){
                  public void actionPerformed(ActionEvent e){
                          // get the Border from the pane
                          border = getBorder();
                  }
      };

      getRootPane().setDefaultButton(okButton);
      okButton.setActionCommand("OK");
      if (okListener != null) {
        okButton.addActionListener(okListener);
      }
      okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          hide();
        }
      });
      JButton cancelButton = new JButton("Cancel");
      cancelButton.setActionCommand("cancel");
      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          cancel = true;
          hide();
        }
      });
      // borderlayout
      addBorderChooser(pane);      
      pane.add(cancelButton);
      pane.add(okButton);
      contentPane.add(pane, BorderLayout.CENTER);
      pack();
      this.addWindowListener(new Closer());
      this.addComponentListener(new DisposeOnClose());
    }

    public void addBorderChooser(JPanel panel){
      borderChooser = new BorderChooser();
      panel.add(borderChooser);
    }

    public void setBorder(){ // called from pane
    }

    public Border getBorder(){
      return borderChooser.getSelectedBorder();
      //      return this.border;
    }

    public Border showDialog(){
      this.cancel = false;
      this.show();
      return getBorder(); // border should be ok
    }

    public boolean isCancelled(){
      return this.cancel;
    }

    class Closer extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            Window w = e.getWindow();
            w.hide();
        }
    }

    class DisposeOnClose extends ComponentAdapter {
        public void componentHidden(ComponentEvent e) {
            Window w = (Window)e.getComponent();
            w.dispose();
        }
    }
    
  }


  public static void main(String args[]){
      JFrame f = new JFrame();
    f.addWindowListener(new WindowAdapter(){
      public void windowClosing(WindowEvent e){
        System.exit(0);
      }
    });
    SwingBorderEditor editor = new SwingBorderEditor();
    f.getContentPane().add(editor.getCustomEditor());
      f.pack();
    f.show();
  }

}
