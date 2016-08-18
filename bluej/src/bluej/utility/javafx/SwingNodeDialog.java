/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg 
 
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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.Point;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ComponentListener;

import bluej.BlueJTheme;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An FX dialog containing only a SwingNode.  This is intended to be a drop-in
 * replacement for EscapeDialog which displays the content as a SwingNode in
 * an FX window rather than a JDialog.
 * 
 * This is a transition class, used to convert some of the version control
 * dialogs to being FX windows until we have time to go back and convert
 * them properly to FX.
 */
public class SwingNodeDialog
{
    @OnThread(Tag.Any)
    private SwingNode swingNode;
    @OnThread(Tag.Swing)
    private boolean modal = false;
    @OnThread(Tag.FXPlatform)
    private Dialog<Object> dialog;
    private boolean clickedSwingClose = false;

    @OnThread(Tag.Swing)
    protected SwingNodeDialog()
    {
        // Provide a content pane by default:
        swingNode = new SwingNodeFixed();
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        swingNode.setContent(content);
        
        Platform.runLater(() -> {            
            dialog = new Dialog<>();
            BlueJTheme.setWindowIconFX(dialog);
            // We must return non-null result to allow the dialog
            // to be closed even though we have no FX buttons
            dialog.setResultConverter(bt -> new Object());
            dialog.setResult(new Object());
            dialog.getDialogPane().setContent(swingNode);
            dialog.initModality(Modality.NONE);

            dialog.setOnShown(e -> SwingUtilities.invokeLater(() -> pack()));
        });
    }

    @OnThread(Tag.Swing)
    protected SwingNodeDialog(FXPlatformSupplier<Window> owner)
    {
        this();
        Platform.runLater(() -> dialog.initOwner(owner.get()));
    }
    
    @OnThread(Tag.Swing)
    protected void setModal(boolean makeModal)
    {
        this.modal = makeModal;
        Platform.runLater(() -> dialog.initModality(makeModal ? Modality.APPLICATION_MODAL : Modality.NONE));
    }

    @OnThread(Tag.Swing)
    protected void rememberPosition(String locationPrefix)
    {
        Platform.runLater(() -> dialog.setOnShown(e -> {
            Config.rememberPosition(asWindow(), locationPrefix);
            SwingUtilities.invokeLater(() -> pack());
        }));
    }
    
    @OnThread(Tag.Swing)
    public void setVisible(boolean show)
    {
        // Can't use dialog::show because may not be assigned yet
        if (show)
        {
            if (modal)
            {
                // We need to wait for the FX dialog, but in the mean time
                // keep the Swing thread responsive:
                SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
                Platform.runLater(() -> {
                    dialog.setOnHidden(e -> {
                        SwingUtilities.invokeLater(() -> loop.exit());
                    });
                    dialog.show();
                });
                loop.enter();
            }
            else
                // Fire and forget:
                Platform.runLater(() -> dialog.show());
        }
        else
            Platform.runLater(() -> dialog.hide());
    }

    @OnThread(Tag.Swing)
    protected void setTitle(String title)
    {
        Platform.runLater(() -> dialog.setTitle(title));
    }

    @OnThread(Tag.Swing)
    public void pack()
    {
        Dimension preferredSize = swingNode.getContent().getPreferredSize();
        // Adjust for extra space needed for button bar
        // Better too big than too small:
        preferredSize.setSize(preferredSize.getWidth(), preferredSize.getHeight() + 22);
        swingNode.getContent().validate();
        Platform.runLater(() -> {
            dialog.getDialogPane().setPrefWidth(preferredSize.getWidth());
            dialog.getDialogPane().setPrefHeight(preferredSize.getHeight());
            dialog.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
            dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            if (asWindow() != null && asWindow().getHeight() < preferredSize.getHeight())
                asWindow().setHeight(preferredSize.getHeight());
        });
    }
    
    @OnThread(Tag.Swing)
    protected JComponent getContentPane()
    {
        return swingNode.getContent();
    }

    @OnThread(Tag.Swing)
    protected void setContentPane(JComponent content)
    {
        swingNode.setContent(content);
    }

    @OnThread(Tag.Swing)
    public void dispose()
    {
        setVisible(false);
    }

    @OnThread(Tag.FXPlatform)
    public Window asWindow()
    {
        Scene scene = dialog.getDialogPane().getScene();
        if (scene == null)
            return null;
        else
            return scene.getWindow();
    }
    
    @OnThread(Tag.Swing)
    protected void setDefaultButton(JButton button)
    {
        
    }

    @OnThread(Tag.Swing)
    public FocusTraversalPolicy getFocusTraversalPolicy()
    {
        return swingNode.getContent().getFocusTraversalPolicy();
    }

    @OnThread(Tag.Swing)
    public void setFocusTraversalPolicy(FocusTraversalPolicy policy)
    {
        swingNode.getContent().setFocusTraversalPolicy(policy);
    }
    
    @OnThread(Tag.Swing)
    public void setLocationRelativeTo(JComponent comp)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void setLocationRelativeTo(Window window)
    {

    }
    
    @OnThread(Tag.Swing)
    protected void dialogThenHide(FXPlatformRunnable action)
    {
        Platform.runLater(() -> {
            action.run();
            SwingUtilities.invokeLater(() -> setVisible(false));
        });
    }
    
    @OnThread(Tag.FXPlatform)
    protected void setCloseIsButton(JButton button)
    {
        // JavaFX windows only let themselves be closed through the
        // window controls if they have one button, or a cancel button among
        // multiple buttons.  So we add a single cancel button, but make sure
        // it is not visible.  JavaFX doesn't check for visibility so still
        // allows the close, and we don't have to show the button (the cancel
        // button in a SwingNodeDialog will be Swing, not JavaFX).
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
        Button dummy = (Button)dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        dummy.setVisible(false);
        dummy.setManaged(false);
        
        dialog.setOnHiding(e -> {
            // We need to remember if we've already clicked close,
            // because the button probably tries to hide the window,
            // and we can end up in an infinite loop with the close
            // handler clicking the button closing the window, etc....
            if (!clickedSwingClose)
            {
                SwingUtilities.invokeLater(() -> button.doClick());
                clickedSwingClose = true;
            }
        });
    }


    @OnThread(Tag.Swing)
    protected void setResizable(boolean resizable)
    {
        Platform.runLater(() -> dialog.setResizable(resizable));
    }
}
