/*
 This file is part of the Greenfoot program.
 Copyright (C) 2014,2016,2017,2018  Poul Henriksen and Michael Kolling

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
package greenfoot.guifx;

import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXCustomizedDialog;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * A dialog to ask the user for a pasted image name.
 *
 * @Author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class PastedImageNameDialog extends FXCustomizedDialog<File>
{
    private final File projImagesDir;
    private final Image image;
    private final TextField fileNameField = new TextField();

    /**
     * Create a paste image dialog. This is used to show the clipboard image contents
     * and to request a name for the new pasted image.
     *
     * @param parent        The parent window associated with this dialog
     * @param image         The pasted content as an image object.
     * @param projImagesDir The directory in which the images for the project are placed.
     */
    public PastedImageNameDialog(Window parent, Image image, File projImagesDir)
    {
        super(parent, Config.getString("editor.paste.image.title"), "");

        this.projImagesDir = projImagesDir;
        this.image = image;
        buildUI();
    }

    /**
     * Build the user interface for the dialog.
     */
    private void buildUI()
    {
        fileNameField.setAlignment(Pos.BASELINE_LEFT);
        fileNameField.setPromptText(Config.getString("editor.paste.image.name.prompt"));
        fileNameField.requestFocus();

        HBox fileNameRow = new HBox(new Label(Config.getString("editor.paste.image.prompt")), fileNameField, new Label(".png"));
        fileNameRow.setAlignment(Pos.BASELINE_LEFT);
        HBox.setHgrow(fileNameField, Priority.ALWAYS);

        VBox bodyPanel = new VBox(20, new ImageView(image), fileNameRow);
        bodyPanel.setAlignment(Pos.CENTER);
        ScrollPane sp = new ScrollPane();
        sp.setContent(bodyPanel);
        setContentPane(sp);

        // Setting the minimum size for the stage to avoid controls appearing messed up.
        final Window window = this.getDialogPane().getScene().getWindow();
        Stage stage = (Stage) window;
        stage.setMinWidth(350);
        stage.setMinHeight(300);

        // Setting the maximum size for the dialog pane to avoid large size windows.
        // The dialog pane is used here instead of the stage because if the stage is set up to a maximum size
        // and the image is too big, then the Ok/Cancel buttons and scroll bars will disappear.
        // appearing messed up.
        this.getDialogPane().setMaxWidth(900);
        this.getDialogPane().setMaxHeight(900);

        // Position the stage in the top-left of the owner
        stage.setX(this.getOwner().getX() + 50 );
        stage.setY(this.getOwner().getY() + 50);

        // add buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(fileNameField.textProperty().isEmpty());
        setResultConverter(bt -> bt == ButtonType.OK ? createImageFile() : null);
    }

    /**
     * Creates an image file with the name specified in the name text field.
     * If there is a file with the same name in the images folder, it prompts the
     * user to overwrite or cancel. If the file is created successfully, the pasted
     * contents will be written in it and it will be written on the disk.
     *
     * @return If the image file was created successfully, it will be returned.
     *         If the file has not been written successfully or the user
     *         chose not to overwrite an existing file, null will be returned.
     */
    private File createImageFile()
    {
        File file = new File(projImagesDir, fileNameField.getText() + ".png");
        if (file.exists())
        {
            boolean overwrite = DialogManager.askQuestionFX(this.asWindow(), "file-exists-overwrite", new String[] {file.getName()}) == 0;
            return overwrite && writeImage(file) ? file : null;
        }
        return writeImage(file) ? file : null;
    }

    /**
     * Writes the passed file as an image on the disk.
     *
     * @param file The image file to be written.
     * @return True if the file is written successfully on the disk, false otherwise.
     */
    private boolean writeImage(File file)
    {
        try
        {
            if (ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file))
            {
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
