package bluej.utility;

import bluej.Config;

import java.awt.*;
import javax.swing.*;

/**
 ** A multi-line Label-like Swing component. This class is similar to a
 ** MultiLineLabel, but it has a fixed numer of rows.
 **
 ** @author Michael Kolling
 **/

public class FixedMultiLineLabel extends MultiLineLabel
{
    protected JLabel[] labels;
    protected int rows;

    /**
     ** Constructor - make a multiline label
     **/
    public FixedMultiLineLabel(String text, float alignment, int numRows)
    {
	super(null, alignment);
	rows = numRows;
	labels = new JLabel[rows];
	for(int i=0; i<rows; i++) {
	    labels[i] = new JLabel(" ");
            labels[i].setAlignmentX(alignment);
	    add(labels[i]);
	}
	addText(text);
    }

    /**
     ** Constructor, defaults to left justified text
     **/
    public FixedMultiLineLabel(String text, int numRows)
    {
	this(text, LEFT_ALIGNMENT, numRows);
    }

    /**
     ** Constructor, empty with the given alignment
     **/
    public FixedMultiLineLabel(float alignment, int numRows)
    {
	this(null, alignment, numRows);
    }

    /**
     ** Constructor - make an empty multiline label
     **/
    public FixedMultiLineLabel(int numRows)
    {
	this(null, LEFT_ALIGNMENT, numRows);
    }
	
    public void setText(String text)
    {
	addText(text);
    }
	
    public void addText(String text)
    {
	JLabel l;
	int lines = 0;

	if(text != null) {
	    String strs[] = Utility.splitLines(text);
	    lines = (strs.length < rows ? strs.length : rows);
	    Font font = new Font("SansSerif", fontAttributes, 12);

	    for (int i = 0; i < lines; i++) {
		labels[i].setText((strs[i].length() == 0 ? " " : strs[i]));
		labels[i].setFont(font);
	    }
	}
	
	for (int i = lines; i < rows; i++)
	    labels[i].setText(" ");
    }
}
