package bluej.utility;

import bluej.Config;

import java.awt.*;
import javax.swing.*;

/**
 ** @version $Id: MultiLineLabel.java 138 1999-06-22 01:05:50Z mik $
 ** @author Justin Tan
 ** A multi-line Label-like AWT component.
 **/
public class MultiLineLabel extends JPanel
{
    int fontAttributes = Font.PLAIN;
    int alignment;
	
    /**
     ** Constructor - make a multiline label
     **/
    public MultiLineLabel(String text, int alignment)
    {
	this.alignment = alignment;
	setLayout(new GridLayout(0,1));
	if(text != null)
	    setText(text);
    }

    /**
     ** Constructor, defaults to centered text
     **/
    public MultiLineLabel(String text)
    {
	this(text, JLabel.CENTER);
    }

    /**
     ** Constructor, empty with the given alignment
     **/
    public MultiLineLabel(int alignment)
    {
	this(null, alignment);
    }

    /**
     ** Constructor - make an empty multiline label
     **/
    public MultiLineLabel()
    {
	this(null, JLabel.CENTER);
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
	Font font = new Font("SansSerif", fontAttributes, Config.fontsize);

	for (int i = 0; strs != null && i < strs.length; i++) {
	    l = new JLabel(strs[i]);
	    l.setFont(font);
	    l.setHorizontalAlignment(alignment);
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
