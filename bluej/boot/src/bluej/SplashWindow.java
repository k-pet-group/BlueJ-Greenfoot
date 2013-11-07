/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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
 */
public class SplashWindow extends Frame
{
    private boolean painted = false;
    
    /**
     * Construct a splash window.
     * @param image
     */
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
    
    @Override
    public synchronized void paint(Graphics g)
    {
        painted = true;
        super.paint(g);
        notify();
    }
    
    /**
     * Wait until the splash screen has actually been painted, with a timeout of
     * 3 seconds.
     */
    public synchronized void waitUntilPainted()
    {
        long startTime = System.currentTimeMillis();
        long timePast = System.currentTimeMillis() - startTime; 
        while (!painted && timePast < 3000) {
            try {
                wait(3000 - timePast);
            }
            catch (InterruptedException ie) { }
            timePast = System.currentTimeMillis() - startTime;
        }
        painted = true;
    }
}

