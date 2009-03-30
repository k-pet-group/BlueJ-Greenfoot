/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.utility;

import javax.swing.*;
import java.awt.*;

/**
 * Return a filled oval as an Icon
 *
 * @author  Andrew Patterson
 * @cvs     $Id: OvalIcon.java 6215 2009-03-30 13:28:25Z polle $
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

