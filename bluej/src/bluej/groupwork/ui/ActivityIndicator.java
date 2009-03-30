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
package bluej.groupwork.ui;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.OverlayLayout;

public class ActivityIndicator extends JComponent implements Runnable
{
    private JProgressBar progressBar;
    private boolean running;
    
    public ActivityIndicator()
    {
        setBorder(null);
        setLayout(new OverlayLayout(this));
        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        add(progressBar);
    }
    
    /**
     * Set the activity indicator's running state. This is safe to call
     * from any thread.
     * 
     * @param running  The new running state
     */
    public void setRunning(boolean running)
    {
        this.running = running;
        if (EventQueue.isDispatchThread()) {
            progressBar.setVisible(running);
        }
        else {
            EventQueue.invokeLater(this);
        }
    }
    
    /*
     * The run() method will only be called on the event dispatch thread, and
     * is used to update the current running state.
     */
    public void run()
    {
        progressBar.setVisible(running);
    }
    
    public Dimension getPreferredSize()
    {
        return progressBar.getPreferredSize();
    }
    
    public Dimension getMinimumSize()
    {
        return progressBar.getMinimumSize();
    }
    
    public Dimension getMaximumSize()
    {
        return progressBar.getMaximumSize();
    }
    
    public boolean isValidateRoot()
    {
        return true;
    }
}
