
package javax.swing.beaninfo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;


// XXX: how to set one of these dynamically from the bean target?
public class BorderChooser extends JTabbedPane {
  private Border border = null;
  private static Color color = null;
  private static JPanel previous = null;
  
  
    public BorderChooser() {
        Border blackline, etched, raisedbevel, loweredbevel, empty;

        //A border that puts 10 extra pixels at the sides and
        //bottom of each pane.
        Border paneEdge = BorderFactory.createEmptyBorder(0,10,10,10);

	blackline = BorderFactory.createLineBorder(Color.black);
	etched = BorderFactory.createEtchedBorder();
        raisedbevel = BorderFactory.createRaisedBevelBorder();
        loweredbevel = BorderFactory.createLoweredBevelBorder();
        empty = BorderFactory.createEmptyBorder();

        //First pane: simple borders
        JPanel simpleBorders = new JPanel();
        simpleBorders.setBorder(paneEdge);
        simpleBorders.setLayout(new BoxLayout(simpleBorders,
                                              BoxLayout.Y_AXIS));

        addCompForBorder(blackline, "line border",
                         simpleBorders);
        addCompForBorder(etched, "etched border",
                         simpleBorders);
        addCompForBorder(raisedbevel, "raised bevel border",
                         simpleBorders);
        addCompForBorder(loweredbevel, "lowered bevel border",
                         simpleBorders);
        addCompForBorder(empty, "empty border",
                         simpleBorders);

        //Second pane: matte borders
        JPanel matteBorders = new JPanel();
        matteBorders.setBorder(paneEdge);
        matteBorders.setLayout(new BoxLayout(matteBorders,
                                              BoxLayout.Y_AXIS));

        //XXX: We *should* size the component so that the icons tile OK.
        //XXX: Without that, the icons are likely to be cut off and look bad.
        ImageIcon icon = new ImageIcon("images/left.gif"); //20x22
        Border border = BorderFactory.createMatteBorder(-1, -1, -1, -1, icon);
        addCompForBorder(border,
                         "matte border (-1,-1,-1,-1,icon)",
                         matteBorders);
        border = BorderFactory.createMatteBorder(1, 5, 1, 1, Color.red);
        addCompForBorder(border,
                         "matte border (1,5,1,1,Color.red)",
                         matteBorders);
        border = BorderFactory.createMatteBorder(0, 20, 0, 0, icon);
        addCompForBorder(border,
                         "matte border (0,20,0,0,icon)",
                         matteBorders);

        //Third pane: titled borders
        JPanel titledBorders = new JPanel();
        titledBorders.setBorder(paneEdge);
        titledBorders.setLayout(new BoxLayout(titledBorders,
                                              BoxLayout.Y_AXIS));
        TitledBorder titled;

        titled = BorderFactory.createTitledBorder("title");
        addCompForBorder(titled,
                         "default titled border"
                         + " (default just., default pos.)",
                         titledBorders);

        titled = BorderFactory.createTitledBorder(
                              blackline, "title");
        addCompForTitledBorder(titled,
                               "titled line border"
                                   + " (centered, default pos.)",
                               TitledBorder.CENTER,
                               TitledBorder.DEFAULT_POSITION,
                               titledBorders);

        titled = BorderFactory.createTitledBorder(etched, "title");
        addCompForTitledBorder(titled,
                               "titled etched border"
                                   + " (right just., default pos.)",
                               TitledBorder.RIGHT,
                               TitledBorder.DEFAULT_POSITION,
                               titledBorders);

        titled = BorderFactory.createTitledBorder(
                        loweredbevel, "title");
        addCompForTitledBorder(titled,
                               "titled lowered bevel border"
                                   + " (default just., above top)",
                               TitledBorder.DEFAULT_JUSTIFICATION,
                               TitledBorder.ABOVE_TOP,
                               titledBorders);

        titled = BorderFactory.createTitledBorder(
                        empty, "title");
        addCompForTitledBorder(titled, "titled empty border"
                               + " (default just., bottom)",
                               TitledBorder.DEFAULT_JUSTIFICATION,
                               TitledBorder.BOTTOM,
                               titledBorders);

        //Fourth pane: compound borders
        JPanel compoundBorders = new JPanel();
        compoundBorders.setBorder(paneEdge);
        compoundBorders.setLayout(new BoxLayout(compoundBorders,
                                              BoxLayout.Y_AXIS));
        Border redline = BorderFactory.createLineBorder(Color.red);

        Border compound;
        compound = BorderFactory.createCompoundBorder(
                                  raisedbevel, loweredbevel);
        addCompForBorder(compound, "compound border (two bevels)",
                         compoundBorders);

        compound = BorderFactory.createCompoundBorder(
                                  redline, compound);
        addCompForBorder(compound, "compound border (add a red outline)",
                         compoundBorders);

        titled = BorderFactory.createTitledBorder(
                                  compound, "title",
                                  TitledBorder.CENTER,
                                  TitledBorder.BELOW_BOTTOM);
        addCompForBorder(titled, 
                         "titled compound border"
                         + " (centered, below bottom)",
                         compoundBorders);

        addTab("Simple", null, simpleBorders, null);
        addTab("Matte", null, matteBorders, null);
        addTab("Titled", null, titledBorders, null);
        addTab("Compound", null, compoundBorders, null);
        setSelectedIndex(0);
    }

    void addCompForTitledBorder(TitledBorder border,
                                String description,
                                int justification,
                                int position,
                                Container container) {
        border.setTitleJustification(justification);
        border.setTitlePosition(position);
        addCompForBorder(border, description,
                         container);
    }

    void addCompForBorder(Border border,
                          String description,
                          Container container) {
        JPanel comp = new JPanel(false);
	JLabel label = new JLabel(description, JLabel.CENTER);
	label.setPreferredSize(new Dimension(180,20));
	label.setMaximumSize(new Dimension(180,20));
	label.setMinimumSize(new Dimension(180,20));
	//	label.setToolTipText("mouse click selects Border - double click for more...");
	comp.setLayout(new GridLayout(1, 1));
	comp.add(label);
        comp.setBorder(border);
	comp.addMouseListener(new ActiveBorderListener(border,comp));
        container.add(Box.createRigidArea(new Dimension(0, 10))); 
        container.add(comp);
    }

  public Border getSelectedBorder(){
    return this.border;
  }

  public void setSelectedBorder(Border b, JPanel p){
    if (border != b){
      if (previous != null)
	previous.setBackground(color);
      previous = p;
      color = p.getBackground();
      p.setBackground(UIManager.getColor("Button.select"));
      p.revalidate();
      p.repaint();
    }
    this.border = b;
  }
  

  class ActiveBorderListener extends MouseAdapter {
    Border b;
    JPanel p;
    
    public ActiveBorderListener(Border b, JPanel p){
      super();
      this.b = b;
      this.p = p;
    }

    public void mousePressed(MouseEvent e){
      setSelectedBorder(b,p);
      if (e.getClickCount() ==  2){
	// bring up title/color editor
	if (b instanceof TitledBorder){
	  TitledBorder t = (TitledBorder)b;
	  if (t.getTitle() != null){
	    String title = JOptionPane.showInputDialog(p, "Please Enter Title String: ");
	    if (title != null){
	      t.setTitle(title);
	    }
	  }
	}
      }
    }
  };    



    public static void main(String[] args) {
        JFrame frame = new JFrame("BorderChooser");

        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });

        frame.getContentPane().add("Center", new BorderChooser());
        frame.pack();
        frame.show();
    }
}
