/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2015  Michael Kolling and John Rosenberg 
 
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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/**
 * This class implements a splash window that can be displayed while BlueJ is
 * starting up.
 *
 * @author  Michael Kolling
 */
public class SplashWindow extends Frame
{
    private boolean painted = false;
    private JProgressBar progress;
    
    /**
     * Construct a splash window.
     * @param image
     */
    public SplashWindow(SplashLabel image)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setUndecorated(true);

        add(image);
        progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setDoubleBuffered(true); //stop the flickering on raspberry pi.
        progress.setVisible(false); //set progress bar to invisible, initially.
        add(progress);
        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width) / 2, (screenDim.height - getSize().height) / 2);
        setVisible(true);
        
        Timer progressTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (isVisible()) {
                    progress.setVisible(true); //timeout expired, show progress bar.
                    pack();
                }
            }
        });
        progressTimer.setRepeats(false);
        progressTimer.start();
    }
    
    @Override
    public synchronized void paint(Graphics g)
    {
        super.paint(g);
        painted = true;
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

