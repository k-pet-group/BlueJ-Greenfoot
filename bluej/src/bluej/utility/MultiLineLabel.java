package bluej.utility;

import bluej.Config;

import java.awt.*;
import javax.swing.*;

/**
 ** @version $Id: MultiLineLabel.java 305 1999-12-09 23:50:57Z ajp $
 ** @author Justin Tan
 ** A multi-line Label-like AWT component.
 **/
public class MultiLineLabel extends JPanel
{
    protected int fontAttributes = Font.PLAIN;
    protected float alignment;
    protected Color col = null;
	
    /**
     ** Constructor - make a multiline label
     **/
    public MultiLineLabel(String text, float alignment)
    {
        this.alignment = alignment;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if(text != null)
            setText(text);
    }

    /**
     ** Constructor, defaults to centered text
     **/
    public MultiLineLabel(String text)
    {
        this(text, LEFT_ALIGNMENT);
    }

    /**
     ** Constructor, empty with the given alignment
     **/
    public MultiLineLabel(float alignment)
    {
        this(null, alignment);
    }

    /**
     ** Constructor - make an empty multiline label
     **/
    public MultiLineLabel()
    {
        this(null, LEFT_ALIGNMENT);
    }
	
    public void setText(String text)
    {
        // clear the existing lines from the panel
        removeAll();
        addText(text);
    }
	
    public void addText(String text)
    {
        String strs[] = Utility.splitLines(text);
        JLabel l;
        Font font = new Font("SansSerif", fontAttributes, 12);

        for (int i = 0; strs != null && i < strs.length; i++) {
            l = new JLabel(strs[i]);
            l.setFont(font);
            l.setAlignmentX(alignment);

            if (col != null)
                l.setForeground(col);

            add(l);
        }	
    }
	
    public void addText(String text, boolean bold, boolean italic)
    {
        int oldAttributes = fontAttributes;
        setBold(bold);
        setItalic(italic);
        addText(text);
        fontAttributes = oldAttributes;
    }

    public void setForeground(Color col)
    {
        this.col = col;        
    }
    	
    public void setItalic(boolean italic)
    {
        if(italic)
           fontAttributes |= Font.ITALIC;
        else
            fontAttributes &= ~Font.ITALIC;
    }
	
    public void setBold(boolean bold)
    {
        if(bold)
            fontAttributes |= Font.BOLD;
        else
            fontAttributes &= ~Font.BOLD;
    }
}
