package bluej.browser;

import bluej.Config;
import bluej.utility.Debug;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;
import bluej.utility.MultiEnumeration;
import bluej.utility.SortableVector;
import bluej.utility.Utility;

import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: Target.java 276 1999-11-16 00:55:47Z ajp $
 ** @author Michael Cahill
 **
 ** A general target for the browser
 **/
abstract public class Target extends JComponent
{
    static final int DEF_WIDTH = 100;
    static final int DEF_HEIGHT = 50;
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 8;
    static final int SHAD_SIZE = 5;

    static final Color textbg = Config.getItemColour("colour.text.bg");
    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    static final Color graphbg = Config.getItemColour("colour.graph.background");

    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color librarybg = Config.getItemColour("colour.class.bg.imported");
    static final Color compbg = Config.getItemColour("colour.target.bg.compiling");

    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    static public final Font normalFont = new Font("SansSerif", Font.BOLD, Config.fontsize);

    protected String displayName;		    // the display name of the target
    protected int targetWidth;
    protected boolean selected;
    
    public Target(String displayName)
    {
        this.displayName = displayName;
        this.selected = false;

        targetWidth = (int)((TEXT_BORDER * 4) + 
                        normalFont.getStringBounds(displayName,new FontRenderContext(new AffineTransform(), false, false)).getWidth());

        if (targetWidth < DEF_WIDTH)
            targetWidth = DEF_WIDTH;

        setBorder(BorderFactory.createEmptyBorder(0,0, SHAD_SIZE, SHAD_SIZE));

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);    
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(targetWidth, DEF_HEIGHT);    
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(targetWidth, DEF_HEIGHT);    
    }
    

    protected void processMouseEvent(MouseEvent evt)
    {
        super.processMouseEvent(evt);
	
        if (evt.isPopupTrigger())
            popupMenu(evt.getX(), evt.getY());
    }

    public boolean getSelected() { return selected; }
    public void setSelected(boolean sel) { this.selected = sel; repaint(); }

    protected Color getBackgroundColour() { return defaultbg; }
    protected Color getBorderColour() { return colBorder; }
    protected Color getTextColour() { return textfg; }

    abstract void popupMenu(int x, int y);

    /**
     *  Draw this target, including its box, border, shadow and text.
     */
    public void paintComponent(Graphics g)
    {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
    
       	g.setColor(getBackgroundColour());
    	g.fillRect(insets.left, insets.top, width, height);
    		
//    	if(state != S_NORMAL) {
    	    // Debug.message("Target: drawing invalid target " + this);
//    	    g.setColor(shadowCol); // Color.lightGray
//    	    Utility.stripeRect(g, 0, 0, width, height, 8, 3);
//    	}
    
    	g.setColor(textbg);
    	g.fillRect(insets.left + TEXT_BORDER, insets.top + TEXT_BORDER,
    		   width - 2 * TEXT_BORDER, TEXT_HEIGHT);
    
    	g.setColor(shadowCol);
       	drawShadow(g);
    		
    	g.setColor(getBorderColour());
    	g.drawRect(insets.left + TEXT_BORDER, insets.top + TEXT_BORDER,
    		   width - 2 * TEXT_BORDER, TEXT_HEIGHT);
    	drawBorders(g);
    		
    	g.setColor(getTextColour());
    	g.setFont(normalFont);

    	Utility.drawCentredText(g, displayName,
    				insets.left + TEXT_BORDER, insets.top + TEXT_BORDER,
    				width - 2 * TEXT_BORDER, TEXT_HEIGHT);
    }

    void drawShadow(Graphics g)
    {
        int width = getWidth();
        int height = getHeight();
    	g.fillRect(SHAD_SIZE, height-SHAD_SIZE, width-SHAD_SIZE, SHAD_SIZE);
    	g.fillRect(width-SHAD_SIZE, SHAD_SIZE, height-SHAD_SIZE, height);
    }
	
    void drawBorders(Graphics g)
    {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;

    	int thickness = selected ? 4 : 1;
    	Utility.drawThickRect(g, insets.left, insets.top, width, height, thickness);
    }
}
