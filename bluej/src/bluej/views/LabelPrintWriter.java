package bluej.views;

import bluej.utility.MultiLineLabel;
import java.awt.Label;

/**
 ** @version $Id: LabelPrintWriter.java 156 1999-07-06 14:37:16Z ajp $
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
		// PrintWriter needs to be passed a valid outputstream
		// even if we are going to not actually print to it.
		// We pass it the standard System output stream
		super(System.out);

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

	protected void indentLine()
	{
		label.addText("\t");
	}
	
	public void println(String str)
	{
		label.addText(str);
	}
}
