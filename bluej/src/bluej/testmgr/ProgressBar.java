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
package bluej.testmgr;

import java.awt.Color;

import javax.swing.JProgressBar;

/**
 * A progress bar showing the green/red status.
 * 
 * @author Andrew Patterson (derived from JUnit src)
 * @version $Id: ProgressBar.java 6215 2009-03-30 13:28:25Z polle $
 */
class ProgressBar extends JProgressBar
{
    public static final Color redBarColour = new Color(208, 16, 16);
    public static final Color greenBarColour = new Color(32, 192, 32);

    private boolean fError = false;

    public ProgressBar() 
    {
        super();
        setForeground(getStatusColor());
    }

    private Color getStatusColor()
    {
        if(fError)
            return redBarColour;
        return greenBarColour;
    }

    public void reset()
    {
        fError = false;
        setForeground(getStatusColor());
        setValue(0);
    }

    public void step(int value, boolean successful)
    {
        setValue(value);
        if(!fError && !successful) {
            fError = true;
            setForeground(getStatusColor());
        }
    }
}
