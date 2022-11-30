/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2015,2016  Michael Kolling and John Rosenberg 
 
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
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

import javafx.scene.control.Dialog;
import javafx.stage.Stage;

import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.prefmgr.PrefMgr;

/**
 * Some helper methods for setting visual details.
 */
@OnThread(Tag.Swing)
public class BlueJTheme
{
    private static final String SMALL_ICON_SUFFIX = "-icon-32.png";
    private static final String MEDIUM_ICON_SUFFIX = "-icon-48.png";
    private static final String LARGE_ICON_SUFFIX = "-icon-256.png";

    // icon to be used for BlueJ windows
    private static Image iconImage = null;

    // common strings - must be accessed through getOkLabel()
    private static String okayLabel;
    private static String cancelLabel;
    private static String closeLabel;
    @OnThread(Tag.Any)
    private static String continueLabel;

    // a dimension for ok and cancel buttons that is as large as
    // needed to display either
    private static Dimension okCancelDimension;

    // JSplitPane divider width constant
    public static final int splitPaneDividerWidth = 3;

    // Other general spacing constants. We should try to use these for consistency
    public static final int generalSpacingWidth = 5;

    public static final Border generalBorder =
        BorderFactory.createEmptyBorder(10,10,10,10);

    public static final Border generalBorderWithStatusBar =
        BorderFactory.createEmptyBorder(10,10,0,10);

    public static final Border dialogBorder =
        BorderFactory.createEmptyBorder(12,12,12,12);

    public static final int commandButtonSpacing = 5;
    public static final int commandButtonPadding = 12;

    public static final int componentSpacingSmall = 5;
    public static final int componentSpacingLarge = 11;

    public static final int dialogCommandButtonsVertical = 17;

    private BlueJTheme()
    {
        
    }

    @OnThread(Tag.FX)
    public static void setWindowIconFX(Stage frame)
    {
        javafx.scene.image.Image icon = getIconImageFX();
        if (icon != null) {
            frame.getIcons().add(icon);
        }
    }

    @OnThread(Tag.FX)
    public static void setWindowIconFX(Dialog<?> dialog)
    {
        Window window = dialog.getDialogPane().getScene().getWindow();
        if (window != null && window instanceof Stage)
            setWindowIconFX((Stage)window);
    }

    /**
     * Get the icon for most BlueJ frames.
     *
     * @return    an icon to be used as the frame icon for most BlueJ windows
     */
    @OnThread(Tag.FX)
    public static javafx.scene.image.Image getIconImageFX()
    {
        String appName = Config.getApplicationName().toLowerCase();
        return getApplicationFxIcon(appName, false);
    }
    
    @OnThread(Tag.Any)
    public static String getApplicationIconFileName(String baseName, boolean getStrideEditorIcon)
    {
        if (Config.isMacOS() && !getStrideEditorIcon) {
            return null;        // don't set window icon on Mac - Mac OS generates dynamic icons
        }
        if (Config.isModernWinOS()) {
            // Win Vista, 7, or newer
            return baseName + LARGE_ICON_SUFFIX;
        } else if (Config.isWinOS()) {
            return baseName + SMALL_ICON_SUFFIX;
        } else {
            // Linux, etc. or we want the icon only.
            return baseName + MEDIUM_ICON_SUFFIX;
        }
    }

    /**
     * Get the icon for most BlueJ frames.
     *
     * @return    an icon to be used as the frame icon for most BlueJ windows
     */
    public static Image getApplicationIcon(String baseName)
    {
        if (Config.isMacOS())
            return null;        // don't set window icon on Mac - Mac OS generates dynamic icons

        if (iconImage == null) {
                iconImage = Config.getFixedImageAsIcon(getApplicationIconFileName(baseName, false)).getImage();
            }

        return iconImage;
    }
    
    @OnThread(Tag.FX)
    public static javafx.scene.image.Image getApplicationFxIcon(String baseName, boolean getStrideEditorIcon)
    {
        if (Config.isMacOS() && !getStrideEditorIcon)
        {
            return null;        // don't set window icon on Mac - Mac OS generates dynamic icons
        }
        return Config.getFixedImageAsFXImage(getApplicationIconFileName(baseName, getStrideEditorIcon));
    }
    

    /**
     * Needed for Greenfoot
     */
    public static void setIconImage(Image newIconImage)
    {
        iconImage = newIconImage;
    }

    public static String getOkLabel()
    {
        if (okayLabel == null) {
            okayLabel = Config.getString("okay");
        }
        return okayLabel;
    }

    public static String getCancelLabel()
    {
        if (cancelLabel == null) {
            cancelLabel = Config.getString("cancel");
        }
        return cancelLabel;
    }

    public static String getCloseLabel()
    {
        if (closeLabel == null) {
            closeLabel = Config.getString("close");
        }
        return closeLabel;
    }

    @OnThread(Tag.Any)
    public static synchronized String getContinueLabel()
    {
        if (continueLabel == null) {
            continueLabel = Config.getString("continue");
        }
        return continueLabel;
    }

    /**
     * Get a standard BlueJ "ok" button.
     * 
     * @return    A JButton that says "ok"
     */
    public static JButton getOkButton()
    {
        computeButtonWidths();

        JButton okButton = new JButton(getOkLabel());
        // try to make the OK, cancel and continue buttons have equal size
        okButton.setPreferredSize(okCancelDimension);
        return okButton;
    }

    /**
     * Get a standard BlueJ "cancel" button.
     * 
     * @return    A JButton that says "cancel"
     */
    public static JButton getCancelButton()
    {
        computeButtonWidths();

        JButton cancelButton = new JButton(getCancelLabel());
        // try to make the OK, cancel and continue  buttons have equal size
        cancelButton.setPreferredSize(okCancelDimension);
        return cancelButton;    
    }

    /**
     * Get a standard BlueJ "close" button.
     * 
     * @return    A JButton that says "cancel"
     */
    public static JButton getCloseButton()
    {
        computeButtonWidths();

        JButton closeButton = new JButton(getCloseLabel());
        // try to make the OK, cancel and continue  buttons have equal size
        closeButton.setPreferredSize(okCancelDimension);
        return closeButton;    
    }


    /**
     * Get a standard BlueJ "continue" button.
     * 
     * @return    A JButton that says "Continue"
     */
    public static JButton getContinueButton()
    {
        computeButtonWidths();

        JButton continueButton = new JButton(getContinueLabel());
        // try to make the OK, cancel and continue  buttons have equal size
        continueButton.setPreferredSize(okCancelDimension);
        return continueButton;    
    }

    /**
     * Computer the maximum width of the ok, cancel and continue buttons
     * and set the okCancelDimension to be representative of that size.
     */
    private static void computeButtonWidths()
    {
        if (okCancelDimension != null)
            return;

        JButton okButton = new JButton(getOkLabel());
        JButton cancelButton = new JButton(getCancelLabel());
        JButton continueButton = new JButton(getContinueLabel());

        int maxWidth = Math.max(cancelButton.getPreferredSize().width,
                okButton.getPreferredSize().width);
        maxWidth = Math.max(maxWidth,
                continueButton.getPreferredSize().width);

        okCancelDimension = new Dimension(maxWidth, okButton.getPreferredSize().height);
    }
}
