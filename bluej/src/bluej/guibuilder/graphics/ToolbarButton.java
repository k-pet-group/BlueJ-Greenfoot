package javablue.GUIGraphics;

import java.awt.Button;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;


/**
 * A custom component representing a button. This button stays down after a
 * being clicked, and a double click makes it permanently selected until it
 * is clicked again.
 *
 * Created: Oct 2, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ToolbarButton extends Canvas
{
    /**
     * Ease-of-use constant for setState() and getState().
     * Specifies an unselected button.
     *
     * @see GUIGraphics.ToolbarButton#setState
     * @see GUIGraphics.ToolbarButton#getState
     */
    public static final int UNSELECTED = 0;

    /**
     * Ease-of-use constant for setState() and getState().
     * Specifies a selected button.
     *
     * @see GUIGraphics.ToolbarButton#setState
     * @see GUIGraphics.ToolbarButton#getState
     */
    public static final int SELECTED = 1;

    /**
     * Ease-of-use constant for setState() and getState().
     * Specifies a permanent selected button.
     *
     * @see GUIGraphics.ToolbarButton#setState
     * @see GUIGraphics.ToolbarButton#getState
     */
    public static final int PERMANENT = 2;

    private final static Color light = SystemColor.menu.brighter().brighter();
    private final static Color medium = SystemColor.menu;
    private final static Color shadow = SystemColor.menu.darker().darker();
    private Color background;
    private Color backgroundselected;
    private Color backgroundselectedpermanent;

    private String text;
    private int state = UNSELECTED;
    private int clicks = 0;
    private ToolbarButtonGroup buttonGroup= new ToolbarButtonGroup();
    private ToolbarButtonListener toolbarButtonListener = new ToolbarButtonListener();

    private int width, height;
    private int textwidth, textheight;
    private int textPosX, textPosY;
    private boolean sizesAreValid = false;

    private final static Color textColor = SystemColor.textText;
    private final static Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();


    /**
     * Constructs a new ToolbarButton with no label.
     */
    public ToolbarButton()
    {
	super();
	super.addMouseListener(toolbarButtonListener);
    }


    /**
     * Constructs a new ToolbarButton with the specified label.
     *
     * @param label A string label for the button.
     */
    public ToolbarButton(String label)
    {
	super();
	super.addMouseListener(toolbarButtonListener);
	text = label;
    }


    /**
     * Sets the label on the button.
     *
     * @param label A string label for the button.
     *
     * @see GUIGraphics.ToolbarButton#getLabel
     */
    public void setLabel(String label)
    {
	this.text = label;
	repaint();
    }


    /**
     * Returns the label on the button.
     *
     * @return A string representing the label.
     *
     * @see GUIGraphics.ToolbarButton#setLabel
     */
    public String getLabel()
    {
	return text;
    }


    /**
     * Returns the state of the button.
     *
     * @return An integer representing the state.
     *
     * @see GUIGraphics.ToolbarButton#UNSELECTED
     * @see GUIGraphics.ToolbarButton#SELECTED
     * @see GUIGraphics.ToolbarButton#PERMANENT
     */
    public int getState()
    {
	return state;
    }


    /**
     * Sets the state of the button.
     *
     * @param state The new state of the button.
     *
     * @see GUIGraphics.ToolbarButton#UNSELECTED
     * @see GUIGraphics.ToolbarButton#SELECTED
     * @see GUIGraphics.ToolbarButton#PERMANENT
     */
    public void setState(int state)
    {
	buttonGroup.changeState(this,state);
    }


    /**
     * Sets the state of the button. This function is to be called from the
     * ToolbarButtonGroup, selecting or deselecting the button depending on
     * the state of the other buttons in the same group.
     *
     * @param state The new state of the button.
     *
     * @see GUIGraphics.ToolbarButtonGroup
     */
    public void setStateFromGroup(int state)
    {
	if(background==null)
	{
	    background = getBackground();
	    backgroundselected = new Color((int)(background.getRed()*.95),(int)(background.getGreen()*.95),(int)(background.getBlue()*.95));
	    backgroundselectedpermanent = new Color((int)(background.getRed()*.80),(int)(background.getGreen()*.80),(int)(background.getBlue()*.80));
	}
	this.state = state;
	repaint();
    }


    /**
     * Sets the ToolbarButtonGroup that this button belongs to. Every
     * ToolbarButton must belong to a ToolbarButtonGroup.
     *
     * @param buttonGroup   The button group this button belongs to.
     */
    public void setGroup(ToolbarButtonGroup buttonGroup)
    {
	this.buttonGroup = buttonGroup;
    }


    /**
     * Gets the mininimum size of this component. 
     *
     * @return	A dimension object indicating this seperator's minimum size.
     **/
    public Dimension getMinimumSize ()
    {
	if (!sizesAreValid)
	    calcSize();

	return (new Dimension(textwidth, textheight));
    }


    /**
     * Gets the preferred size of this component.
     *
     * @return	A dimension object indicating this seperator's preferred size.
     **/
    public Dimension getPreferredSize ()
    {
	return (getMinimumSize());
    }


    /**
     * This method is called to repaint this button.
     *
     * @param g	    The graphics context.
     **/
    public void paint(Graphics g)
    {
	Dimension size = getSize();
	width = size.width;
	height = size.height;
	calcSize();

	if(state==SELECTED)
	    setBackground(backgroundselected);
	else if(state==PERMANENT)
	    setBackground(backgroundselectedpermanent);
	else
	    setBackground(background);

	if(state!=UNSELECTED)
	    g.setColor(light);
	else
	    g.setColor(shadow);
	g.drawLine (width-1, 0, width-1,height-1);
	g.drawLine (0, height-1, width-1, height-1);

	if(state!=UNSELECTED)
	    g.setColor(shadow);
	else
	    g.setColor(light);
	g.drawLine (0, 0, width-1, 0);
	g.drawLine (0, 0, 0, height-1);

	g.setColor(medium);
	g.drawLine(width-1,0,width-1,0);
	g.drawLine(0,height-1,0,height-1);

	g.setColor(textColor);
	g.drawString(text, textPosX, textPosY);
    }


    /**
     * Calculates the minimim size of this button based om the size of the
     * text string in the label.
     **/
    private void calcSize ()
    {
	FontMetrics fontMetrics = toolkit.getFontMetrics(getFont());
	
	textheight = fontMetrics.getHeight()+8;
	textwidth = fontMetrics.stringWidth(text)+8;
	textPosX = width/2-fontMetrics.stringWidth(text)/2;
	textPosY = height/2-fontMetrics.getHeight()/2+fontMetrics.getAscent();

	sizesAreValid = true;
    }


    /**
     * An inner class used to handle mouse clicks on the button.
     */
    private class ToolbarButtonListener extends MouseAdapter
    {
	public void mouseClicked(MouseEvent e)
	{
	    if(((e.getModifiers()&MouseEvent.BUTTON1_MASK)!=0))
	    {
		clicks = e.getClickCount();
		if((clicks%2)==1)
		{
		    if(state==SELECTED || state == PERMANENT)
			setState(UNSELECTED);
		    else
			setState(SELECTED);
		}
		else
		    setState(PERMANENT);
	    }
	}
    }
}
