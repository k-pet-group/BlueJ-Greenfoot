/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2014  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

import greenfoot.core.WorldHandler;
import greenfoot.gui.AskPanel;
import greenfoot.gui.WorldCanvas;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

import bluej.utility.Debug;

/**
 * This class provides most of the implementation for the Greenfoot.ask() method. It implements
 * the necessary bridging between the EDT and another thread.
 * 
 * @author Neil Brown
 */
public class AskHandler
{
    private final AskPanel askPanel;
    private final WorldCanvas worldCanvas;
    private final ArrayBlockingQueue<String> answer = new ArrayBlockingQueue<String>(1);
    
    public AskHandler(AskPanel askPanel, WorldCanvas worldCanvas)
    {
        this.askPanel = askPanel;
        this.worldCanvas = worldCanvas;
    }

    /**
     * Asks the user to input a String.  Should be called from the EDT.  Returns a Callable
     * that you should call from a non-EDT thread, which will wait for the answer and then
     * return it.
     */
    public Callable<String> ask(final String prompt, final int worldWidth)
    {
        Image snapshot = getWorldGreyedSnapShot();
        
        if (snapshot != null)
            worldCanvas.setOverrideImage(snapshot);
        
        askPanel.showPanel(Math.max(400, worldWidth), prompt, new AskPanel.AnswerListener() {
            
            @Override
            public void answered(String answer)
            {
                worldCanvas.setOverrideImage(null); 
                try
                {
                    AskHandler.this.answer.put(answer);
                }
                catch (InterruptedException e)
                {
                    Debug.reportError(e);
                }
            }
        });
        
        return new Callable<String>() {
    
            @Override
            public String call() throws Exception
            {
                return answer.take();
            }
        };
    }
    
    // When we merge with the Greenfoot 3 branch, this will produce a duplicate method
    // error.  Add a parameter to toggle the striping (GF3 stripes; GF 2.4.1 doesn't, and it should stay that way),
    // and merge the two methods
    /**
     * Take a snapshot of the world and turn it grey.  Must be called from EDT.
     */
    private Image getWorldGreyedSnapShot()
    {
        BufferedImage screenShot = WorldHandler.getInstance().getSnapShot();
        if (screenShot != null) {
            GreenfootUtil.convertToGreyImage(screenShot);
        }
        return screenShot;
    }
    
    /**
     * Stops waiting for an answer, e.g. in the case that we want to stop execution.
     * Must be called from the EDT.
     */
    public void stopWaitingForAnswer()
    {
        if (askPanel.isPanelShowing())
        {
            askPanel.hidePanel();
            try
            {
                answer.put("");
            }
            catch (InterruptedException e)
            {
                Debug.reportError(e);
            }
        }
    }
}