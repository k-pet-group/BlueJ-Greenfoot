/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Toolkit;

/**
 * This class implements a splash window that can be displayed while BlueJ is
 * starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 6163 2009-02-19 18:09:55Z polle $
 */

public class SplashWindow extends Frame
{
    private boolean painted = false;
    
    public SplashWindow(SplashLabel image)
    {
        setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    	setUndecorated(true);

        add(image);
        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width) / 2, (screenDim.height - getSize().height) / 2);
        setVisible(true);
        //try { Thread.sleep(11000);} catch(Exception e) {}  // for testing: show longer
    }
    
    public synchronized void paint(Graphics g)
    {
        painted = true;
        super.paint(g);
        notify();
    }
    
    public synchronized void waitUntilPainted()
    {
        while (!painted) {
            try {
                wait();
            }
            catch (InterruptedException ie) {
                painted = true;
            }
        }
    }
}

