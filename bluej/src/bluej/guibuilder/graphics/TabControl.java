package javablue.GUIGraphics;

import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Canvas;
import java.awt.Panel;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * A custom component representing a tab control. Basically it is a CardLayout
 * with graphical tabs at the top used to flip through the cards.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class TabControl extends Panel
{
    private Tab selectedTab;
    private CardLayout cardLayout = new CardLayout(5, 5);
    private Panel center = new Panel(cardLayout);


    /**
     * Constructs a new TabControl with cards named by the String array, and
     * panels inserted from the Panel array. The two arrays must be
     * of equal size.
     *
     * @param label	Array containing the names of the cards.
     * @param component	Array containing the Panels to be inserted at each card.
     **/
    public TabControl (String[] label, Panel[] component)
    {
	super();
	setLayout(new BorderLayout());
	setBackground(Color.lightGray);

	// Add the components:
	for (int i=0; i<component.length; i++)
	    center.add(component[i], ""+i);
	cardLayout.show(center, "1");

	// Make the tabs:
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints cons = new GridBagConstraints();
	Panel northPanel = new Panel(gridbag);

	cons.fill = GridBagConstraints.BOTH;

	NorthwestBorder NW = new NorthwestBorder();
	gridbag.setConstraints(NW, cons);
	northPanel.add(NW);

	Tab[] tab = new Tab[label.length];
	for (int i=0; i<label.length; i++)
	{
	    tab[i] = new Tab(label[i]);
	    tab[i].setName(""+i);
	    gridbag.setConstraints(tab[i], cons);
	    northPanel.add(tab[i]);
	}

	cons.weightx = 1.0;
	cons.gridwidth = GridBagConstraints.REMAINDER; 
	NortheastBorder NE = new NortheastBorder();
	gridbag.setConstraints(NE, cons);
	northPanel.add(NE);

	selectedTab=tab[0];
	selectTab (selectedTab);

	// Add all grpahical elements:
	add ("North", northPanel);
	add ("South", new SouthBorder());
	add ("East", new EastBorder());
	add ("West", new WestBorder());
	add ("Center", center);

	// Listener for the tabs:
	class TabListener extends MouseAdapter
	{
	    public void mouseClicked (MouseEvent e)
	    {
		if ((e.getModifiers()&MouseEvent.BUTTON1_MASK)!=0)
		    selectTab((Tab)e.getComponent());
	    }
	}
	TabListener myListener = new TabListener();

	for (int i=0; i<label.length; i++)
	    tab[i].addMouseListener(myListener);
    }


    /**
     * Shows the specified card.
     *
     * @param tab   The tab activated.
     **/
    private void selectTab (Tab tab)
    {
	// Draw the tabs:
	selectedTab.setSelected(false);
	selectedTab.repaint();
	selectedTab = tab;
	selectedTab.setSelected(true);
	selectedTab.repaint();
	// Select the card:
	cardLayout.show (center, selectedTab.getName());
    }
}




/**
 * A superclass for all the border classes. This class contains the colors and
 * size functions common to its subclasses.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
abstract class Border extends Canvas
{
    protected final static Color light = SystemColor.controlLtHighlight;
    protected final static Color medium = SystemColor.control;
    protected final static Color shadow = SystemColor.controlShadow;
    protected final static Dimension minSize = new Dimension(2, 2);


    public Dimension getMinimumSize ()
    {
	return (minSize);
    }


    public Dimension getPreferredSize ()
    {
	return (getMinimumSize());
    }
}



/**
 * A custom component representing a tab.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class Tab extends Border
{
    private String text;
    private boolean selected = false;
    private int width, height;
    private int textPosX, textPosY;
    private boolean sizesAreValid = false;
    private Dimension minSize;

    private final static Color textColor = SystemColor.textText;
    private final static Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();


    /**
     * Constructs a new Tab with the sepcified label.
     *
     * @param text  The label for the tab.
     **/
    public Tab (String text)
    {
	super();
	this.text = text;
    }


    /**
     * Sets the state of the tab.
     *
     * @param state	The state of the tab. true means selected, and false means not selected.
     * @see javablue.GUIGraphics.Tab#isSelected()
     **/
    public void setSelected (boolean state)
    {
	selected = state;
    }


    /**
     * Returns the state of the tab.
     *
     * @return	The state of the tab. true means selected, and false means not selected.
     * @see javablue.GUIGraphics.Tab#setSelected()
     **/
    public boolean isSelected ()
    {
	return selected;
    }


    public void paint (Graphics g)
    {
	if (!sizesAreValid)
	    calcSize();

	g.setColor(shadow);
	g.drawLine (width-height/2, 0, width-1, height-2);
	g.drawLine (width-height/2, 1, width-1, height-1);

	g.setColor(light);
	g.drawLine (0, height-1, height/2, 1);
	g.drawLine (0, height-2, height/2, 0);
	g.fillRect (height/2, 0, width-height, 2);

	if (!selected)
	    g.fillRect (0, height-2, width, 2);

	g.setColor(textColor);
	g.drawString(text, textPosX, textPosY);
    }


    public Dimension getMinimumSize ()
    {
	if (!sizesAreValid)
	    calcSize();

	return (minSize);
    }


    /**
     * Calculates the minimim size of this tab based om the size of the
     * text string in the label.
     **/
    private void calcSize ()
    {
	FontMetrics fontMetrics = toolkit.getFontMetrics(getFont());

	height = fontMetrics.getHeight()+8;
	width = fontMetrics.stringWidth(text)+height;
	textPosX = height/2;
	textPosY = fontMetrics.getAscent()+4;
   
	minSize = new Dimension(width, height);

	sizesAreValid = true;
    }
}



/**
 * A custom component representing a line.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class SouthBorder extends Border
{
    public void paint (Graphics g)
    {
	Dimension dim = getSize();
        g.setColor (light);
	g.drawLine (0, 0, 0, 0);
	g.setColor (medium);
	g.drawLine (0, 1, 1, 0);
	g.setColor (shadow);
	g.drawLine (2, 0, dim.width, 0);
	g.drawLine (1, 1, dim.width, 1);
    }
}



/**
 * A custom component representing a line.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class NorthwestBorder extends Border
{
    public void paint (Graphics g)
    {
	Dimension dim = getSize();
	g.setColor (light);
	g.fillRect (0, dim.height-2, dim.width, 2);
    }
}



/**
 * A custom component representing a line.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class NortheastBorder extends Border
{
    public void paint (Graphics g)
    {
	Dimension dim = getSize();
        g.setColor (shadow);
	g.drawLine (dim.width-1, dim.height-1, dim.width-1, dim.height-1);
	g.setColor (medium);
	g.drawLine (dim.width-2, dim.height-1, dim.width-1, dim.height-2);
	g.setColor (light);
	g.drawLine (0, dim.height-2, dim.width-2, dim.height-2);
	g.drawLine (0, dim.height-1, dim.width-3, dim.height-1);
    }
}



/**
 * A custom component representing a line.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class WestBorder extends Border
{
    public void paint (Graphics g)
    {
        g.setColor (light);
	g.fillRect (0, 0, 2, getSize().height);
    }
}



/**
 * A custom component representing a line.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class EastBorder extends Border
{
    public void paint (Graphics g)
    {
        g.setColor (shadow);
	g.fillRect (0, 0, 2, getSize().height);
    }
}
