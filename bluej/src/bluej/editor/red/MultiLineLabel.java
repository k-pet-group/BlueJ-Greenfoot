package bluej.editor.red;			// This file forms part of the red package

/**
 ** @version $Id: MultiLineLabel.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **
 ** A label-like component that can handle multiple lines of text
 **/

import java.awt.*;
import java.util.*;

public final class MultiLineLabel extends Component
{
  private String[] lines;         // The lines of text to display
  private int num_lines;          // The number of lines
  private int margin_width;       // Left and right margins
  private int margin_height;      // Top and bottom margins
  private int line_height;        // Total height of the font
  private int line_ascent;        // Font height above baseline
  private int[] line_widths;      // How wide each line is
  private int max_width;          // The width of the widest line
    
/**
 * CONSTRUCTOR: MultiLineLabel(String)
 * Break the label up into separate lines, and save the other info.
 */

public MultiLineLabel(String label)
{
  margin_width = 15;
  margin_height = 15;

  // break the label up into an array of lines.
  StringTokenizer t = new StringTokenizer(label, "\n");
  num_lines = t.countTokens();
  lines = new String[num_lines];
  line_widths = new int[num_lines];
  for(int i = 0; i < num_lines; i++) 
    lines[i] = t.nextToken();
}

/**
 * FUNCTION: measure()
 * This method figures out how the font is, and how wide each
 * line of the label is, and how wide the widest line is.
 */
private void measure() 
{
  FontMetrics fm = this.getFontMetrics(this.getFont());
  // If we don't have font metrics yet, just return.
  if (fm == null) return;
        
  line_height = fm.getHeight();
  line_ascent = fm.getAscent();
  max_width = 0;
  for(int i = 0; i < num_lines; i++) {
    line_widths[i] = fm.stringWidth(lines[i]);
    if (line_widths[i] > max_width) max_width = line_widths[i];
  }
}
    
/**
 * FUNCTION: addNotify()
 * This method is invoked after our Component is first created
 * but before it can actually be displayed.  After we've
 * invoked our superclass's addNotify() method, we have font
 * metrics and can successfully call measure() to figure out
 * how big the label is.
 */

public void addNotify() 
{ 
  super.addNotify();
  measure(); 
}
    
/**
 * FUNCTION: getPreferredSize()
 * This method is called by a layout manager when it wants to
 * know how big we'd like to be.  
 */

public Dimension getPreferredSize() 
{
  return new Dimension(max_width + 2*margin_width, 
                     num_lines * line_height + 2*margin_height);
}
    
/**
 * FUNCTION: getMinimumSize()
 * This method is called when the layout manager wants to know
 * the bare minimum amount of space we need to get by.
 */

public Dimension getMinimumSize() 
{
  return new Dimension(max_width, num_lines * line_height);
}

/**
 * FUNCTION: paint(Graphics)
 * This method draws the label
 */

public void paint(Graphics g) 
{
  Dimension d = this.getSize();
  int x = margin_width;     	// Left aligned
  int y = line_ascent + (d.height - num_lines * line_height)/2;
  for(int i = 0; i < num_lines; i++, y += line_height)
    g.drawString(lines[i], x, y);
}

} // end class MultiLineLabel
