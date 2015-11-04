/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.BlueJTheme;

/**
 * A utility class useful for easily creating JFXPanels and putting them in a window.
 * 
 * @author neil
 */
@OnThread(Tag.Swing)
public abstract class EmbeddableJFXPanel extends JFXPanel
{    
    private final JFrame window;
    private boolean initialised = false;
    private EmbeddableJFXPanel parentToDisable;
    private Runnable afterClose;
    
    /**
     * Construct these panels on the Swing thread:
     */
    protected EmbeddableJFXPanel(String windowTitle)
    {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Cannot construct EmbeddableJFXPanel outside Swing thread");
        
        window = new JFrame();
        window.setTitle(windowTitle);
        window.add(this);
        // Implement a sort of modality.  Disabling the JavaFX root is the best way
        // because disabling the Swing window doesn't disable the JFXPanel inside,
        // and doesn't even seem to disable the window properly either
        window.addWindowListener(new WindowAdapter() {

            @Override
            @OnThread(Tag.Swing)
            public void windowOpened(WindowEvent e)
            {
                if (parentToDisable != null)
                {
                    Scene scene = parentToDisable.getScene();
                    Platform.runLater(new Runnable() { public void run() {
                        scene.getRoot().setDisable(true);
                    }});
                }
            }

            @Override
            @OnThread(Tag.Swing)
            public void windowClosing(WindowEvent e)
            {
                if (parentToDisable != null)
                {
                    Scene scene = parentToDisable.getScene();
                    Platform.runLater(new Runnable() { public void run() {
                        scene.getRoot().setDisable(false);
                    }});
                }
                if (afterClose != null)
                {
                    Platform.runLater(afterClose);
                }
            }
            
        });
        
        Image icon = BlueJTheme.getIconImage();
        if (icon != null) {
            window.setIconImage(icon);
        }
    }
    
    /**
     * Method to initialise all contents of the panel and everything
     * JavaFX-related.  This method will be called on the JavaFX thread.
     */
    @OnThread(value = Tag.FX, ignoreParent = true)
    public abstract void initialiseFX();
    
    /**
     * Method called after the window has been shown.  This will be called
     * on the JavaFX thread.
     */
    @OnThread(value = Tag.FX, ignoreParent = true)
    public abstract void afterShow();
    
    /**
     * Shows this panel embedded in a Swing window.  Must be called
     * from the Swing thread
     * @param afterOpen Runnable to run after window is open.  Will be run on JavaFX platform thread.
     * @param afterClose 
     */
    @OnThread(Tag.Swing)
    public void showInWindow(EmbeddableJFXPanel parentToDisable, final Runnable afterOpen, final Runnable afterClose)
    {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Cannot show EmbeddableJFXPanel outside Swing thread");

        if (window.isVisible())
        {
            if (afterOpen != null)
                Platform.runLater(afterOpen);
            return;
        }
        
        this.parentToDisable = parentToDisable;
        this.afterClose = afterClose;
        
        // Must go to FX thread to initialise:
        Platform.runLater(() -> {
            if (!initialised)
            {
                initialiseFX();
                initialised = true;
            }
            // Once initialised, it's back to Swing thread to show window:
            SwingUtilities.invokeLater(() -> {
                window.setSize(getPreferredSize());
                window.setLocationRelativeTo(null);
                window.setVisible(true);
                // Then back to FX thread to call afterShow() and afterOpen:
                Platform.runLater(() -> {
                    afterShow();
                    if (afterOpen != null)
                        afterOpen.run();
                });
            });
        });
    }
    
    @OnThread(value = Tag.Any, ignoreParent = true)
    public void setWindowVisible(boolean vis, final Runnable runAfterwards)
    {
        if (vis)
        {
            EventQueue.invokeLater(new Runnable() { public void run() { showInWindow(null, runAfterwards, null); } });
        }
        else
        {
            EventQueue.invokeLater(new Runnable() { public void run() { window.setVisible(false); new Thread(runAfterwards).start(); } });
        }
    }
    
    public boolean isWindowVisible()
    {
        return window.isVisible();
    }
    
    public Rectangle getWindowBounds()
    {
        return window.getBounds();
    }
    
    public JFrame getWindow()
    {
        return window;
    }
}
