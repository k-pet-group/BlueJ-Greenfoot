/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2016  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import greenfoot.core.GProject;
import greenfoot.util.GreenfootUtil;

/**
 * A button which shows a twirling symbol, representing program activity, when enabled.
 * 
 * @author Davin McCall
 */
public class ExecutionTwirler extends JButton
{
    private GreenfootFrame gfFrame;
    
    private Image[] imgs = new Image[5];
    private int currentImg = 0;
    private boolean previouslyEnabled = false;
    
    private Image greyImage;
    
    private JLabel imgLabel;
    
    private Timer timer;
    
    /**
     * Constructor for ExecutionTwirler.
     */
    public ExecutionTwirler(GreenfootFrame gfFrame)
    {
        super();
        this.gfFrame = gfFrame;
        
        BufferedImage img = null;

        int lineHeight = getFontMetrics(new Font(Font.SANS_SERIF, 0, 12)).getHeight();
        
        try {
            img = ImageIO.read(getClass().getClassLoader().getResourceAsStream("swirl.png"));

            int w = lineHeight;
            int h = lineHeight;
            
            imgs[0] = GreenfootUtil.getScaledImage(img, w, h);
            greyImage = convertToGrey(imgs[0]); 
            
            // Create a series of images representing the twirl at different rotations.
            for (int i = 1; i < 5; i++) {
                BufferedImage newImg = copyImage(img);
                Graphics2D g = newImg.createGraphics();
                g.rotate((2 * Math.PI / 25) * i, img.getWidth() / 2, img.getHeight() / 2);
                g.drawImage(img, 0, 0, null);
                
                imgs[i] = GreenfootUtil.getScaledImage(newImg, w, h);
                g.dispose();
            }
            
        }
        catch (IOException ioe) {}
        
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        imgLabel = new JLabel(new ImageIcon(greyImage));
        imgLabel.setAlignmentY(0.5f);
        
        add(imgLabel);
        
        add(Box.createHorizontalStrut(lineHeight / 7));
        
        Image popupImg = null;
        try {
            img = ImageIO.read(getClass().getClassLoader().getResourceAsStream("dropdown.png"));
            float scaleFactor = (float) lineHeight / 30;
            popupImg = GreenfootUtil.getScaledImage(img, (int)(img.getWidth() * scaleFactor),
                    (int)(img.getHeight() * scaleFactor));
        }
        catch (IOException ioe) {}
        
        JLabel popupLabel = new JLabel(new ImageIcon(popupImg)); 
        popupLabel.setAlignmentY(0.5f);
        add(popupLabel);
        
        addPropertyChangeListener((event) -> {
            if (isEnabled()) {
                if (!previouslyEnabled) {
                    startTwirl();
                    previouslyEnabled = true;
                }
            }
            else {
                if (previouslyEnabled) {
                    stopTwirl();
                    previouslyEnabled = false;
                }
            }
        });
        
        addActionListener((event) -> {
            createPopup();
        });
    }
    
    /**
     * Copy a BufferedImage to an image with the same color model etc.
     */
    private BufferedImage copyImage(BufferedImage src)
    {
        ColorModel cm = src.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = src.getRaster().createCompatibleWritableRaster();
        
        BufferedImage newImg = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        return newImg;
    }
    
    /**
     * Convert an image to grey-scale.
     */
    private Image convertToGrey(Image src)
    {
        ImageFilter filter = new GrayFilter(true, 50);  
        ImageProducer producer = new FilteredImageSource(src.getSource(), filter);  
        return Toolkit.getDefaultToolkit().createImage(producer);        
    }
    
    /**
     * Start the twirler
     */
    public void startTwirl()
    {
        if (timer == null) {
            timer = new Timer(100, (event) -> {
                currentImg++;
                if (currentImg >= imgs.length) {
                    currentImg = 0;
                }
                imgLabel.setIcon(new ImageIcon(imgs[currentImg]));
            });
        }
        timer.start();
    }
    
    /**
     * Stop the twirler (and display the twirler image in grey-scale)
     */
    public void stopTwirl()
    {
        if (timer != null) timer.stop();
        imgLabel.setIcon(new ImageIcon(greyImage));
    }
    
    /**
     * Create and show a popup menu with various actions (restart Greenfoot, show debugger)
     */
    public void createPopup()
    {
        JPopupMenu menu = new JPopupMenu();
        
        Action restart = new AbstractAction(Config.getString("executionDisplay.restart")) {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try {
                    gfFrame.getProject().getRProject().restartVM();
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                } catch (ProjectNotOpenException e1) {
                    // This should be impossible. If the project is closed,
                    // we can't be executing...
                }
            }
        };

        Action debug = new AbstractAction(Config.getString("executionDisplay.openDebugger")) {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                GProject gproj = gfFrame.getProject();
                if (! gproj.isExecControlVisible()) {
                    gproj.toggleExecControls();
                }
                gproj.haltExecution();
            }
        };
        
        menu.add(debug);
        menu.add(restart);
        menu.show(this, 0, getHeight());
    }
}
