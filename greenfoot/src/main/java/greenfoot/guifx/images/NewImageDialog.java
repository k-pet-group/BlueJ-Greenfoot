/*
 This file is part of the Greenfoot program.
 Copyright (C) 2009,2010,2016,2018  Poul Henriksen and Michael Kolling

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
package greenfoot.guifx.images;

import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXCustomizedDialog;
import greenfoot.util.ExternalAppLauncher;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;

/**
 * A new image dialog, used for specifying the properties of an image before its
 * creation. After it has been created it will automatically be opened in the
 * default image editing program so the user can edit it.
 * 
 * @author Michael Berry (mjrb4)
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class NewImageDialog extends FXCustomizedDialog<File>
{
    private static final int MAX_IMAGE_HEIGHT = 2000;
    private static final int MAX_IMAGE_WIDTH = 2000;
    private static final int DEFAULT_HEIGHT = 100;
    private static final int DEFAULT_WIDTH = 100;
    
    private TextField name;
    private Spinner width;
    private Spinner height;

    private File projImagesDir;
    private int imageWidth;
    private int imageHeight;

    /**
     * Create a new image dialog. This is used for specifying the properties for
     * creating a new image, which will then be opened in the image editor.
     *
     * @param parent the parent window associated with this dialog
     * @param projImagesDir the directory in which the images for the project are placed.
     */
    NewImageDialog(Window parent, File projImagesDir)
    {
        super(parent, Config.getString("imagelib.new.image.title"), null);
        this.projImagesDir = projImagesDir;

        imageWidth = Config.getPropInteger("greenfoot.image.create.width", DEFAULT_WIDTH);
        imageHeight = Config.getPropInteger("greenfoot.image.create.height", DEFAULT_HEIGHT);
        buildUI();
    }

    /**
     * Build the user interface for the dialog.
     */
    private void buildUI()
    {
        GridPane detailsPanel = new GridPane();
        detailsPanel.setVgap(10);
        detailsPanel.setHgap(1);
        detailsPanel.setAlignment(Pos.BASELINE_CENTER);

        name = new TextField();
        name.setPrefWidth(220);
        name.setPromptText(Config.getString("imagelib.new.image.name.prompt"));
        detailsPanel.addRow(0, new Label(Config.getString("imagelib.new.image.name")), name, new Label(".png"));
        GridPane.setHgrow(name, Priority.ALWAYS);

        width = new Spinner(1, MAX_IMAGE_WIDTH, imageWidth);
        width.setEditable(true);
        detailsPanel.addRow(1, new Label(Config.getString("imagelib.new.image.width")), width);

        height = new Spinner(1, MAX_IMAGE_HEIGHT, imageHeight);
        height.setEditable(true);
        detailsPanel.addRow(2, new Label(Config.getString("imagelib.new.image.height")), height);

        setContentPane(detailsPanel);

        final Window window = this.getDialogPane().getScene().getWindow();
        Stage stage = (Stage) window;
        stage.setMinWidth(345);
        stage.setMinHeight(240);

        // add buttons
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(name.textProperty().isEmpty());
        setResultConverter(bt -> bt == ButtonType.OK ? createImageFile() : null);
    }

    /**
     * Creates an image file with the name specified in the name text field.
     * If there is a file with the same name in the images folder, it prompts the
     * user to overwrite or cancel. If the file is created successfully, it will
     * be written on the disk and opened using the OS default program for its type.
     *
     * @return The image file created.
     */
    private File createImageFile()
    {
        File file = new File(projImagesDir, name.getText() + ".png");
        if (file.exists())
        {
            boolean overwrite = DialogManager.askQuestionFX(this.asWindow(), "file-exists-overwrite", new String[] {file.getName()}) == 0;
            return overwrite && writeImageAndEdit(file) ? file : null;
        }
        return writeImageAndEdit(file) ? file : null;
    }

    /**
     * Writes the passed file as an image on the disk, with the width and height
     * specified by the user, and opens it using the OS default program for its type.
     *
     * @param file The image file to be written.
     * @return True if the file is written successfully on the disk, false otherwise.
     */
    private boolean writeImageAndEdit(File file)
    {
        BufferedImage im = new BufferedImage((Integer) width.getValue(), (Integer) height.getValue(), BufferedImage.TYPE_INT_ARGB);
        try
        {
            if (ImageIO.write(im, "png", file))
            {
                SwingUtilities.invokeLater(() -> ExternalAppLauncher.editImage(file));
                return true;
            }
        }
        catch (IOException ex)
        {
            // No need to repeat the error message here and in case writing the image returned false.
        }
        DialogManager.showErrorFX(asWindow(), "imagelib-writing-image-failed");
        return false;
    }
}
