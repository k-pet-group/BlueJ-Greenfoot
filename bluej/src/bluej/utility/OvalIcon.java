package bluej.utility;

import javax.swing.*;
import java.awt.*;

import bluej.utility.Debug;
import bluej.Config;

/**
 * Return a filled oval as an Icon
 *
 * @author  Andrew Patterson
 * @cvs     $Id: OvalIcon.java 1067 2002-01-08 05:49:39Z ajp $
 */
public class OvalIcon implements Icon
{
    private static OvalIcon redIcon = new OvalIcon(Color.red);
    private static OvalIcon blankIcon = new OvalIcon(null);

    public static OvalIcon getRedOvalIcon()
    {
        return redIcon;        
    }

    public static OvalIcon getBlankOvalIcon()
    {
        return blankIcon;
    }

    private Color color;

    public OvalIcon (Color c) {
       color = c;
    }

    public void paintIcon (Component c, Graphics g, int x, int y)
    {
  if(color != null) {
  int width = getIconWidth();
  int height = getIconHeight();
   g.setColor (color);
  g.fillOval (x, y, width, height);
  }
}
public int getIconWidth() {
  return 10;
}
public int getIconHeight() { 
  return 10;
}
}

