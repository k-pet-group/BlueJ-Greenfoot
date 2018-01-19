/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2014,2015,2016,2017  Poul Henriksen and Michael Kolling

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
import bluej.extensions.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.Debug;
import bluej.utility.FileUtility;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;

import greenfoot.guifx.ClassNameVerifier;
import greenfoot.guifx.PastedImageNameDialog;
import greenfoot.guifx.images.ImageLibList.ImageListEntry;
import greenfoot.util.ExternalAppLauncher;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

import javax.imageio.ImageIO;

/**
 * A (modal) dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
public class ImageLibFrame extends FXCustomizedDialog<File>
                    //TODO extends Dialog<Image> ?
{
    private ClassTarget classTarget;
    private Project project;
    /** The default image icon - none, or parent's image */
    private File defaultIcon;

    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;

    private File selectedImageFile;
    private File projImagesDir;

    private TextField classNameField;
    private SourceType language;

    /** Menu items that are in the context menu. */
    private MenuItem editItem;
    private MenuItem duplicateItem;
    private MenuItem deleteItem;

    //TODO private Timer refreshTimer;

    /** Suffix used when creating a copy of an existing image (duplicate) */
    private static final String COPY_SUFFIX = Config.getPropString("imagelib.duplicate.image.name.suffix");

    /** PopupMenu icon */
    // TODO
    private static final String DROPDOWN_ICON_FILE = "menu-button.png";
    
    /** A watcher that goes notified when an image is selected, to allow for previewing. May be null */
    private ImageSelectionWatcher selectionWatcher;

    /**
     * Construct an ImageLibFrame for changing the image of an existing class.
     *
     * @param owner      The parent frame
     * @param classTarget  The ClassView of the existing class
     */
    public ImageLibFrame(Window owner, ClassTarget classTarget, ImageSelectionWatcher watcher)
    {
        super(owner, Config.getString("imagelib.title") + " " + classTarget.getDisplayName(), "image-lib");
        this.selectionWatcher = watcher;
        this.classTarget = classTarget;
        this.project = classTarget.getPackage().getProject();

        // TODO
        //Class superClass = classTarget.getClass().getSuperclass();
        //gclass.getPackage().getDependentTarget(gclass.getBaseName())////
        //defaultIcon = getClassImage(superClass.getClass);

        buildUI(false, null);
        projImageList.select(getSpecifiedImage(classTarget));
    }

    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     *
     * @param owner        The parent frame
     * @param superClass   The superclass of the new class
     * @param title        The title of the dialog
     * @param defaultName  The default name of the new class (or blank if null)
     * @param description  A helper prompt to display at the top of dialog (or none if null)
     */
    //TODO
    public ImageLibFrame(Window owner, ClassTarget superClass, String title, String defaultName, List<String> description)
    {
        super(owner, title, "image-lib");
        this.classTarget = superClass;
        this.project = classTarget.getPackage().getProject();
        defaultIcon = getClassImage(superClass);
        
        buildUI(true, description);
        projImageList.select(null);
        if (defaultName != null)
        {
            classNameField.setText(defaultName);
        }
        classNameField.requestFocus();
    }

    // TODO tidy up the code
    private void buildUI(final boolean includeClassNameField, List<String> description)
    {
        VBox contentPane = new VBox();
        setContentPane(contentPane);

        if (description != null)
        {
            description.forEach(t -> {
                Label l = new Label(t);
                l.setFocusTraversable(false);
                l.setBorder(null);
                contentPane.getChildren().add(l);
            });
        }

        // Class details - name, current icon
        contentPane.getChildren().add(buildClassDetailsPanel(includeClassNameField, project.getUnnamedPackage()));

        // Image selection panels - project and greenfoot image library
        {
            HBox imageSelPanels = new HBox();
            // Project images panel
            {
                File projDir = project.getProjectDir();
                projImagesDir = new File(projDir, "images");
                projImageList = new ImageLibList(projImagesDir, false, defaultIcon);//true?
                ScrollPane imageScrollPane = new ScrollPane(projImageList);

                VBox piPanel = new VBox();
                piPanel.getChildren().addAll(new Label(Config.getString("imagelib.projectImages")), imageScrollPane);
                imageSelPanels.getChildren().add(piPanel);
            }

            // Category selection panel
            File imageDir = new File(Config.getGreenfootLibDir(), "imagelib");
            ImageCategorySelector imageCategorySelector = new ImageCategorySelector(imageDir);

            // List of images
            greenfootImageList = new ImageLibList(false);
            Pane greenfootLibPanel = new GreenfootImageLibPane(imageCategorySelector, greenfootImageList);
            imageSelPanels.getChildren().add(greenfootLibPanel);
            contentPane.getChildren().add(imageSelPanels);

            JavaFXUtil.addChangeListener(projImageList.getSelectionModel().selectedItemProperty(), imageListEntry -> valueChanged(imageListEntry, true));
            JavaFXUtil.addChangeListener(greenfootImageList.getSelectionModel().selectedItemProperty(), imageListEntry -> valueChanged(imageListEntry, false));
            imageCategorySelector.setImageLibList(greenfootImageList);
        }
        
        // Creates the PopupMenuButton, adding the edit, duplicate, delete and new
        // menu items, along with their actions to it. Also creates the browse button
        // and adds both of these components to a flow panel to display in the content
        // panel.
        {
            BorderPane borderPanel = new BorderPane();

            editItem = new MenuItem(Config.getString("imagelib.edit"));
            // TODO editItem.setToolTipText(Config.getString("imagelib.edit.tooltip"));
            editItem.setDisable(true);
            editItem.setOnAction(event -> {
                ImageListEntry entry = projImageList.getSelectionModel().getSelectedItem();
                if (entry != null && entry.imageFile != null)
                {
                    ExternalAppLauncher.editImage(entry.imageFile);
                }
            });
            
            duplicateItem = new MenuItem(Config.getString("imagelib.duplicate"));
            // TODO duplicateItem.setToolTipText(Config.getString("imagelib.duplicate.tooltip"));
            duplicateItem.setDisable(true);
            duplicateItem.setOnAction(event -> {
                ImageListEntry entry = projImageList.getSelectionModel().getSelectedItem();
                if (entry != null && entry.imageFile != null)
                {
                    duplicateSelected(entry);
                }
            });
            
            deleteItem = new MenuItem(Config.getString("imagelib.delete"));
            // TODO deleteItem.setToolTipText(Config.getString("imagelib.delete.tooltip"));
            deleteItem.setDisable(true);
            deleteItem.setOnAction(event -> {
                ImageListEntry entry = projImageList.getSelectionModel().getSelectedItem();
                if (entry != null && entry.imageFile != null)
                {
                    confirmDelete(entry);
                }
            });

            MenuItem newImageItem = new MenuItem(Config.getString("imagelib.create.button"));
            // TODO newImageItem.setToolTipText(Config.getString("imagelib.create.tooltip"));
            newImageItem.setOnAction(event -> {
                String name = includeClassNameField ? getClassName() : classTarget.getQualifiedName();

                NewImageDialog newImage = new NewImageDialog(ImageLibFrame.this, projImagesDir, name);
                final File file = newImage.displayModal();
                if (file != null) {
                    projImageList.refresh();
                    projImageList.select(file);
                    selectImage(file);
                }                                           
            });

            MenuItem pasteImageItem = new MenuItem(Config.getString("paste.image"));
            // TODO pasteImageItem.setToolTipText(Config.getString("imagelib.paste.tooltip"));
            pasteImageItem.setDisable(false);
            pasteImageItem.setOnAction(event -> pasteImage());

            MenuItem importImageItem = new MenuItem(Config.getString("imagelib.browse.button"));
            importImageItem.setOnAction(event -> importImage());

            MenuButton dropDownButton = new MenuButton(Config.getString("imagelib.more"),
                    new ImageView(new Image(ImageLibFrame.class.getClassLoader().getResourceAsStream(DROPDOWN_ICON_FILE))),
                    editItem, duplicateItem, deleteItem, new SeparatorMenuItem(), newImageItem, pasteImageItem, importImageItem);

            borderPanel.getChildren().add(dropDownButton);
            contentPane.getChildren().add(borderPanel);
        }

        // Ok and cancel buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // TODO
        // refreshTimer = new Timer(2000, e -> projImageList.refreshPreviews());
        // refreshTimer.start();

        setResultConverter(bt -> bt == ButtonType.OK ? selectedImageFile : null);
    }

    /**
     * Build the class details panel.
     *
     * @param includeClassNameField  Whether to include a field for
     *                              specifying the class name.
     * @param pkg
     */
    private Pane buildClassDetailsPanel(boolean includeClassNameField, Package pkg)
    {
        VBox classDetailsPanel = new VBox();
        {
            if (includeClassNameField) {
                HBox b = new HBox();
                Label classNameLabel = new Label(Config.getString("imagelib.className"));
                b.getChildren().add(classNameLabel);

                // "ok" button should be disabled until class name entered
                Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
                okButton.setDisable(true);

                classNameField = new TextField();
                final Label errorMsgLabel = JavaFXUtil.withStyleClass(new Label(), "dialog-error-label");
                errorMsgLabel.setVisible(false);

                b.getChildren().add(classNameField);

                SourceType[] items = { SourceType.Stride, SourceType.Java };
                ComboBox<SourceType> languageSelectionBox = new ComboBox<>(FXCollections.observableArrayList(items));
                language = pkg.getDefaultSourceType();
                languageSelectionBox.getSelectionModel().select(language);
                b.getChildren().add(languageSelectionBox);

                final ClassNameVerifier classNameVerifier = new ClassNameVerifier(classNameField, pkg, language);
                JavaFXUtil.addChangeListener(classNameVerifier.validityProperty(), valid -> {
                    errorMsgLabel.setText(classNameVerifier.getMessage());
                    errorMsgLabel.setVisible(!valid);
                    okButton.setDisable(!valid);
                });
                languageSelectionBox.setOnAction(event -> {
                    language = languageSelectionBox.getSelectionModel().getSelectedItem();
                    classNameVerifier.change(language);
                });
                
                classDetailsPanel.getChildren().addAll(b, errorMsgLabel);
            }

            // help label
            Label helpLabel = new Label(Config.getString("imagelib.help.selectImage"));

            classDetailsPanel.getChildren().addAll(helpLabel, new Separator(Orientation.HORIZONTAL));
        }

        return classDetailsPanel;
    }

    /**
     * A new image was selected in one of the ImageLibLists
     */
    private void valueChanged(ImageListEntry entry, boolean isProjImageList)
    {
        // handle the no-image image entry.
        if (entry != null && entry.imageFile != null) // TODO AA look at it
        {
            File imageFile = entry.imageFile;
            selectImage(imageFile);
            setItemButtons(isProjImageList);
        }
        else
        {
            selectImage(null);
            setItemButtons(false);
        }

        if(isProjImageList)
            greenfootImageList.getSelectionModel().clearSelection();
        else
            projImageList.getSelectionModel().clearSelection();
    }

    /**
     * Change the three selection based menu items to the
     * parameter provided.
     * @param state To enable or disable the menu item buttons.
     */
    private void setItemButtons(boolean state)
    {
        editItem.setDisable(!state);
        duplicateItem.setDisable(!state);
        deleteItem.setDisable(!state);
    }

    /**
     * Selects the given file (or no file) for use in the preview.
     * 
     * @param imageFile  The file to select, and to show in the small preview box in the
     *                   ImageLibFrame. If null, then "no image" is selected.
     */
    private void selectImage(File imageFile)
    {
        if (imageFile == null || GreenfootUtil.isImage(imageFile)) {
            selectedImageFile = imageFile;
            if (selectionWatcher != null) {
                selectionWatcher.imageSelected(selectedImageFile);
            }
        }
        else {
            // TODO AA change this to filter out invalid
            new Alert(Alert.AlertType.ERROR, imageFile.getName() + " " +
                    Config.getString("imagelib.image.invalid.text"), ButtonType.OK).show();
        }
    }
    
    /**
     * Gets specified image file (which will be project images/ directory) for this specific
     * class, without searching super classes (see getClassImage for that).  Returns null if none
     * specified.
     */
    private static File getSpecifiedImage(ClassTarget gclass)
    {
        // TODO test this
        String imageName = gclass.getProperty("image");
        
        // If an image is specified for this class, and we can read it, return
        if (imageName != null && !imageName.equals(""))
        {
            return new File(new File("images"), imageName).getAbsoluteFile();
        }
        
        return null;
    }

    /**
     * Get a preview icon for a class. This is a fixed size image. The
     * user-specified image is normally used; if none exists, the class
     * hierarchy is searched.
     *
     * @param classTarget   The class whose icon to get
     */
    private static File getClassImage(ClassTarget classTarget)
    {
        while (classTarget != null)
        {
            File imageFile = getSpecifiedImage(classTarget);
            if (imageFile != null && imageFile.canRead())
            {
                return imageFile;
            }
            // Otherwise, search up class hierarchy to see if we find an image:
            classTarget = GreenfootUtil.getSuperclass(classTarget);
        }

        return new File(GreenfootUtil.getGreenfootLogoPath());
    }

    private void importImage()
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Config.getString("imagelib.browse.button"));
        // TODO make it ImageView instead?
        // new ImageFilePreview(chooser);

        chooser.getExtensionFilters().addAll(
                new ExtensionFilter("Images", "*.png", "*.jpg", "*.gif"),
                new ExtensionFilter("All Files", "*.*"));

        File selectedFile = chooser.showOpenDialog(this.getOwner());//Config.getString("imagelib.choose.button")
        if (selectedFile != null) {
            File newFile = new File(projImagesDir, selectedFile.getName());
            GreenfootUtil.copyFile(selectedFile, newFile);
            if(projImageList != null) {
                projImageList.select(newFile);
            }
        }
    }

    /**
     * Get the selected image file
     */
    public File getSelectedImageFile()
    {
        return selectedImageFile;
    }

    /**
     * Get the name of the class as entered in the dialog.
     */
    public String getClassName()
    {
        return classNameField.getText();
    }
    
    /**
     * Get the selected language of the class.
     */
    public SourceType getSelectedLanguage()
    {
        return language;
    }

    /**
     * Create a new file which is an exact copy of the
     * parameter image and select it if successful in creating
     * it.
     * @param entry Cannot be null, nor can its imageFile.
     */
    private void duplicateSelected(ImageListEntry entry)
    {
        File srcFile = entry.imageFile;
        File dstFile = null;
        File dir = srcFile.getParentFile();
        String fileName = srcFile.getName();
        int index = fileName.indexOf('.');
        
        String baseName;
        String ext;
        if (index != -1)
        {
            baseName = fileName.substring(0, index);
            ext = fileName.substring(index + 1);
        } 
        else
        {
            baseName = fileName;
            ext = "";
        }
        baseName += COPY_SUFFIX;
        
        try
        {
            dstFile = GreenfootUtil.createNumberedFile(dir, baseName, ext);
            FileUtility.copyFile(srcFile, dstFile);
        }
        catch (IOException e)
        {
            Debug.reportError(e);
        }

        if (dstFile != null)
        {
            projImageList.select(dstFile);
        }
    }
    
    /**
     * Confirms whether or not to delete the selected file.
     * @param entry Cannot be null, nor can its imageFile.
     */
    private void confirmDelete(ImageListEntry entry)
    {
        String text = Config.getString("imagelib.delete.confirm.text") + 
                      " " + entry.imageFile.getName() + "?";
        ButtonType optionResult = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO);
        if (optionResult == ButtonType.YES)
        {
            entry.imageFile.delete();
            projImageList.refresh();
        }
    }

    private void pasteImage()
    {
        if (Clipboard.getSystemClipboard().hasImage())
        {
            Image image = Clipboard.getSystemClipboard().getImage();
            PastedImageNameDialog dlg = new PastedImageNameDialog(this.getOwner(), image, null);
            dlg.showAndWait().ifPresent(name -> {
                try
                {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File(projImagesDir, name + ".png"));
                    projImageList.refresh();
                }
                catch (IOException ex)
                {
                    Debug.reportError(ex);
                }
            });
        }
        else
        {
            new Alert(Alert.AlertType.ERROR, "no-clipboard-image-data").show();
        }
    }
}
