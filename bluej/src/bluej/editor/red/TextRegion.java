package bluej.editor.red;		// This file forms part of the red package

/**
 ** @version $Id: TextRegion.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Justin Tan
 **
 **	TextRegion is part of the BlueJ integrated environment.
 **
 **	TextRegion is used to specify an area in a text.  It uses four integer
 **	values, definig the start line and column and the end line and column
 **	of the region defined by this instance.
 **
 **	The value (-1, -1, -1, -1) for the four fields is used to indicate an
 **	invalid region (no region) in cases where no valid region can be 
 **	defined.
 **/
public final class TextRegion 
{
	int start_line;		// line number of start of region
	int start_col;		// column number of start of region
	int end_line;		// line number of end of region
	int end_col;		// column number of end of region

	// set the four values defining this region
	public void set (int sline, int scol, int eline, int ecol)
	{
		start_line = sline;
		start_col = scol;
		end_line = eline;
		end_col = ecol;
	}
}
