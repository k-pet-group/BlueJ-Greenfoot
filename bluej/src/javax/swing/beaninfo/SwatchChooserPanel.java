package javax.swing.beaninfo;

import javax.swing.*;
import javax.swing.colorchooser.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;


/**
 * Modified from the standard color swatch chooser.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with 
 * future Swing releases.  The current serialization support is appropriate
 * for short term storage or RMI between applications running the same
 * version of Swing.  A future release of Swing will provide support for
 * long term persistence.
 *
 * @version 1.14 08/28/98
 * @author Steve Wilson
 */
public class SwatchChooserPanel extends AbstractColorChooserPanel {

  SwatchPanel swatchPanel;
  RecentSwatchPanel recentSwatchPanel;
  MouseListener mainSwatchListener;
  MouseListener recentSwatchListener;
  SwingColorEditor       chooser;
  ChooserComboPopup popup;

  private static String recentStr = UIManager.getString("ColorChooser.swatchesRecentText");

  public SwatchChooserPanel(SwingColorEditor c, ChooserComboPopup p) {
    super();
    this.chooser = c;
    this.popup = p;
  }

  public String getDisplayName() {
    return UIManager.getString("ColorChooser.swatchesNameText");
  }

  public Icon getSmallDisplayIcon() {
    return null;
  }

  public Icon getLargeDisplayIcon() {
    return null;
  }

  /**
   * The background color, foreground color, and font are already set to the
   * defaults from the defaults table before this method is called.
   */									
  public void installChooserPanel(JColorChooser enclosingChooser) {
    super.installChooserPanel(enclosingChooser);
  }

  protected void buildChooser() {
      
    JPanel superHolder = new JPanel();
    superHolder.setLayout(new BoxLayout(superHolder, BoxLayout.Y_AXIS)); // new BorderLayout());
    swatchPanel =  new MainSwatchPanel();
    swatchPanel.getAccessibleContext().setAccessibleName(getDisplayName());

    recentSwatchPanel = new RecentSwatchPanel();
    recentSwatchPanel.getAccessibleContext().setAccessibleName(recentStr);

    mainSwatchListener = new MainSwatchListener();
    swatchPanel.addMouseListener(mainSwatchListener);
    recentSwatchListener = new RecentSwatchListener();
    recentSwatchPanel.addMouseListener(recentSwatchListener);


    JPanel mainHolder = new JPanel(new BorderLayout());
    Border border = new CompoundBorder( new LineBorder(Color.black),
					new LineBorder(Color.white) );
    mainHolder.setBorder(border); 
    mainHolder.add(swatchPanel, BorderLayout.CENTER);
    //	mainHolder.add(recentSwatchPanel, BorderLayout.NORTH);

    JPanel recentHolder = new JPanel( new BorderLayout() );
    recentHolder.setBorder(border);
    recentHolder.add(recentSwatchPanel, BorderLayout.CENTER);

    superHolder.add(recentHolder); // , BorderLayout.NORTH);
    superHolder.add(Box.createRigidArea(new Dimension(0,3)));
    superHolder.add( mainHolder); // , BorderLayout.CENTER );



    //	JPanel recentLabelHolder = new JPanel(new BorderLayout());
    //	recentLabelHolder.add(recentHolder, BorderLayout.CENTER);
    //	JLabel l = new JLabel(recentStr);
    //	l.setLabelFor(recentSwatchPanel);
    //	recentLabelHolder.add(l, BorderLayout.NORTH);
    //	JPanel recentHolderHolder = new JPanel(new BorderLayout()); // was centerlayout
    //	recentHolderHolder.setBorder(new EmptyBorder(2,10,2,2));
    //	recentHolderHolder.add(recentLabelHolder);

    //        superHolder.add( recentHolderHolder, BorderLayout.NORTH );	
	
    add(superHolder);
	
  }

  public void uninstallChooserPanel(JColorChooser enclosingChooser) {
    super.uninstallChooserPanel(enclosingChooser);
    swatchPanel.removeMouseListener(mainSwatchListener);
    swatchPanel = null;
    mainSwatchListener = null;
    removeAll();  // strip out all the sub-components
  }

  public void updateChooser() {

  }

  class RecentSwatchListener extends MouseAdapter implements Serializable {
    public void mousePressed(MouseEvent e) {
      Color color = recentSwatchPanel.getColorForLocation(e.getX(), e.getY());
      //	    getColorSelectionModel().setSelectedColor(color);
      chooser.setValue(color);
      popup.setVisible(false);

    }
  }


  class MainSwatchListener extends MouseAdapter implements Serializable {
    public void mousePressed(MouseEvent e) {
      Color color = swatchPanel.getColorForLocation(e.getX(), e.getY());
      //	    getColorSelectionModel().setSelectedColor(color);
      chooser.setValue(color);
      recentSwatchPanel.setMostRecentColor(color);
      popup.setVisible(false);
    }
  }

}



class SwatchPanel extends JPanel {
  protected Color[] colors;
  protected Dimension swatchSize = new Dimension(13,13);
  protected Dimension numSwatches;
  protected Dimension gap;

  public SwatchPanel() {
    initValues();
    initColors();
    setToolTipText(""); // register for events
    setOpaque(true);
    setBackground(Color.gray);
    setRequestFocusEnabled(false);
  }

  public boolean isFocusTraversable() {
    return false;
  }

  protected void initValues() {
  }

  public void paintComponent(Graphics g) {
    g.setColor(getBackground());
    g.fillRect(0,0,getWidth(), getHeight());
    for (int row = 0; row < numSwatches.height; row++) {
      for (int column = 0; column < numSwatches.width; column++) {
	g.setColor( getColorForCell(column, row) ); 
	int x = column * (swatchSize.width + gap.width);
	int y = row * (swatchSize.height + gap.height);
	g.fillRect( x, y, swatchSize.width, swatchSize.height);
	g.setColor(Color.black);
	g.drawLine( x+swatchSize.width-1, y, x+swatchSize.width-1, y+swatchSize.height-1);
	g.drawLine( x, y+swatchSize.height-1, x+swatchSize.width-1, y+swatchSize.width-1);
      }
    }
  }

  public Dimension getPreferredSize() {
    int x = numSwatches.width * (swatchSize.width + gap.width) -1;
    int y = numSwatches.height * (swatchSize.height + gap.height) -1;
    return new Dimension( x, y );
  }

  protected void initColors() {
  }

  public String getToolTipText(MouseEvent e) {
    Color color = getColorForLocation(e.getX(), e.getY());
    return color.getRed()+", "+ color.getGreen() + ", " + color.getBlue();
  }

  public Color getColorForLocation( int x, int y ) {
    int column = x / (swatchSize.width + gap.width);
    int row = y / (swatchSize.height + gap.height);
    return getColorForCell(column, row);
  }

  private Color getColorForCell( int column, int row) {
      return colors[ (row * numSwatches.width) + column ]; // (STEVE) - change data orientation here
  }
}

class RecentSwatchPanel extends SwatchPanel {
  protected void initValues() {
    //        swatchSize = UIManager.getDimension("ColorChooser.swatchesRecentSwatchSize");
    swatchSize = new Dimension(13,13);
    numSwatches = new Dimension( 6, 1 );
    gap = new Dimension(1, 1);
  }


  protected void initColors() {
    Color defaultRecentColor = UIManager.getColor("ColorChooser.swatchesDefaultRecentColor");
    int numColors = numSwatches.width * numSwatches.height;
	
    colors = new Color[numColors];
    for (int i = 0; i < numColors ; i++) {
      colors[i] = defaultRecentColor;
    } 
  }

  public void setMostRecentColor(Color c) {

    System.arraycopy( colors, 0, colors, 1, colors.length-1);
    colors[0] = c;
    repaint();
  }
}

class MainSwatchPanel extends SwatchPanel {
  protected void initValues() {
    //        swatchSize = UIManager.getDimension("ColorChooser.swatchesSwatchSize");
    swatchSize = new Dimension(13,13);
    //	numSwatches = new Dimension( 31, 10 );
    numSwatches = new Dimension(6,6);
    gap = new Dimension(1, 1);
  }

  protected void initColors() {
    int[] rawValues = initRawValues();
    int numColors = rawValues.length / 3;
	
    colors = new Color[numColors];
    for (int i = 0; i < numColors ; i++) {
      colors[i] = new Color( rawValues[(i*3)], rawValues[(i*3)+1], rawValues[(i*3)+2] );
    }
  }

  private int[] initRawValues() {

    int[] rawValues = {     255, 255, 255,
			    192,192,192,
			    128,128,128,
			    64,64,64,
			    0,0,0,
			    255,0,0,
			    100,100,100,
			    255,175,175,
			    255,200,0,
			    255,255,0,
			    0,255,0,
			    255,0,255,
			    0,255,255,
			    0,0,255,  // repeat here
			    255, 255, 255,
			    192,192,192,
			    128,128,128,
			    64,64,64,
			    0,0,0,
			    255,0,0,
			    100,100,100,
			    255,175,175,
			    255,200,0,
			    255,255,0,
			    0,255,0,
			    255,0,255,
			    0,255,255,
			    0,0,255,  // repeat here
			    100,100,100,
			    255,175,175,
			    255,200,0,
			    255,255,0,
			    0,255,0,
			    255,0,255,
			    0,255,255,
			    0,0,255,
    };
    return rawValues;
  }
}
