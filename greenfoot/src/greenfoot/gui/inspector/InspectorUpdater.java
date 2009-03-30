/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.inspector;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Timer;

import bluej.debugmgr.inspector.Inspector;

/**
 * Updater that will call update on an inspector with fixed time intervals. <br>
 * The updater will stop when the window is closed.
 * 
 * @author Poul Henriksen
 */
public class InspectorUpdater
    implements WindowListener
{
    private Inspector inspector;
    private Timer timer;
    private static final int PERIOD = 500;

    /**
     * Creates a new updater. It will start updating as soon as the inspector
     * becomes visible.
     * 
     * @param inspector
     */
    public InspectorUpdater(Inspector inspector)
    {
        this.inspector = inspector;
        inspector.addWindowListener(this);
        if (inspector.isVisible()) {
            start();
        }

    }

    /**
     * Starts the updater.
     * 
     */
    public void start()
    {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new UpdateTask(inspector), PERIOD, PERIOD);
    }

    /**
     * Stops the updater.
     * 
     */
    public void stop()
    {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void windowActivated(WindowEvent e)
    {
    // Nothing to do
    }

    public void windowDeactivated(WindowEvent e)
    {
    // Nothing to do
    }

    public void windowOpened(WindowEvent e)
    {
        start();
    }

    public void windowClosed(WindowEvent e)
    {
        stop();
    }

    public void windowClosing(WindowEvent e)
    {
    // Nothing to do
    }

    public void windowDeiconified(WindowEvent e)
    {
        start();
    }

    public void windowIconified(WindowEvent e)
    {
        stop();
    }

}
