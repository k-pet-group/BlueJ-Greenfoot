package bluej.views;

import bluej.utility.MultiLineLabel;
import java.awt.Label;

/**
 ** @version $Id: LabelPrintWriter.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** LabelPrintWriter - create a MultiLineLabel containing the output to a
 ** 	FormattedPrintWriter
 **/
public class LabelPrintWriter extends FormattedPrintWriter
{
	MultiLineLabel label;
	
	public LabelPrintWriter(int align)
	{
		label = new MultiLineLabel(align);
	}
	
	public LabelPrintWriter()
	{
		this(Label.LEFT);
	}
	
	public MultiLineLabel getLabel()
	{
		return label;
	}
	
	protected void startBold()
	{
		label.setBold(true);
	}
	
	protected void endBold()
	{
		label.setBold(false);
	}
	
	protected void startItalic()
	{
		label.setItalic(true);
	}

	protected void endItalic()
	{
		label.setItalic(false);
	}
	
	public void println(String str)
	{
		label.addText(str);
	}
}
