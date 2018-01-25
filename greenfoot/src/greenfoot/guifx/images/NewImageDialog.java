/*
 This file is part of the Greenfoot program.
 Copyright (C) 2009,2010,2016  Poul Henriksen and Michael Kolling

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
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXCustomizedDialog;
import greenfoot.util.ExternalAppLauncher;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;

/**
 * A new image dialog, used for specifying the properties of an image before its
 * creation. After it has been created it will automatically be opened in the
 * default image editing program so the user can edit it.
 * 
 * @author Michael Berry (mjrb4)
 */
public class NewImageDialog extends FXCustomizedDialog
{
    private static final int MAX_IMAGE_HEIGHT = 2000;
    private static final int MAX_IMAGE_WIDTH = 2000;
    private static final int DEFAULT_HEIGHT = 100;
    private static final int DEFAULT_WIDTH = 100;
    
    private TextField name;
    private Spinner width;
    private Spinner height;

    private File projImagesDir;
    private File file;
    
    private int imageWidth;
    private int imageHeight;

    /**
     * Create a new image dialog. This is used for specifying the properties for
     * creating a new image, which will then be opened in the image editor.
     *
     * @param parent the parent frame associated with this dialog
     * @param projImagesDir the directory in which the images for the project are placed.
     */
    NewImageDialog(Dialog parent, File projImagesDir, String rootName)
    {
        super(parent.getOwner(), Config.getString("imagelib.new.image.title"), null /* TODO */);
        this.projImagesDir = projImagesDir;

        imageWidth = Config.getPropInteger("greenfoot.image.create.width", DEFAULT_WIDTH);
        imageHeight = Config.getPropInteger("greenfoot.image.create.height", DEFAULT_HEIGHT);
        buildUI(rootName);
    }

    /**
     * Build the user interface for the dialog.
     */
    private void buildUI(String rootName)
    {
        GridPane detailsPanel = new GridPane();
        detailsPanel.setVgap(10);
        detailsPanel.setAlignment(Pos.BASELINE_CENTER);

        name = new TextField(rootName);
        detailsPanel.addRow(0, new Label(Config.getString("imagelib.new.image.name") + " "), name, new Label(".png"));

        width = new Spinner(1, MAX_IMAGE_WIDTH, imageWidth);
        detailsPanel.addRow(1, new Label(Config.getString("imagelib.new.image.width")), width);

        height = new Spinner(1, MAX_IMAGE_HEIGHT, imageHeight);
        detailsPanel.addRow(2, new Label(Config.getString("imagelib.new.image.height")), height);

        setContentPane(detailsPanel);

        // add buttons
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(name.textProperty().isEmpty());
        okButton.setOnAction(event -> createAndEdit());
    }
    
    File displayModal()
    {
        setModal(true);  
        DialogManager.centreDialog(this);
        show();
        //dispose(); hide?? close??
        setModal(false);
        return file;
    }

    public File getFile() 
    {
        return file;
    }

    private void createAndEdit()
    {
        BufferedImage im = new BufferedImage((Integer) width.getValue(), (Integer) height.getValue(),
                BufferedImage.TYPE_INT_ARGB);
        String fileName = name.getText();
        fileName += ".png";
        file = new File(projImagesDir, fileName);

        if (file.exists())
        {
            boolean overwrite = DialogManager.askQuestionFX(getOwner(), "imagelib-write-exists", new String[] {file.getName()}) == 0;
            if (overwrite)
            {
                writeAndEdit(im);
            }
            else
            {
                hide();
            }
        }
        else
        {
            writeAndEdit(im);
        }
    }
    
    private void writeAndEdit(BufferedImage im)
    {
        try
        {
            if (ImageIO.write(im, "png", file))
            {
                ExternalAppLauncher.editImage(file);
                hide();
            }
            else
            {
                // TODO id NOT text ?
                DialogManager.showErrorFX(getOwner(), "png " + Config.getString("imagelib.image.unsupportedformat.text"));
                // TODO Config.getString("imagelib.image.unsupportedformat.title")
            }
        }
        catch (IOException ex)
        {
            Debug.reportError("Error editing new image", ex);
        }
    }
}
