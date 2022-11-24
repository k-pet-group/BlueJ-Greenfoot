/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2014,2015,2016,2017,2018,2019  Poul Henriksen and Michael Kolling

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
import bluej.extensions2.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.guifx.ClassNameVerifier;

import java.io.File;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A (modal) dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class NewImageClassFrame extends FXCustomizedDialog<NewImageClassFrame.NewImageClassInfo>
{
    private final Project project;

    private final TextField classNameField = new TextField();
    private final Label errorMsgLabel = JavaFXUtil.withStyleClass(new Label(), "dialog-error-label");
    private ImageLibPane imageLibPane;
    private Node okButton;
    private ComboBox<SourceType> languageSelectionBox;

    /**
     * The information selected in the dialog: class name,
     * template name and source type.
     */
    public static class NewImageClassInfo
    {
        public final String className;
        public final SourceType sourceType;
        public final File imageFile;

        private NewImageClassInfo(String className, SourceType sourceType, File imageFile)
        {
            this.className = className;
            this.sourceType = sourceType;
            this.imageFile = imageFile;
        }
    }

    /**
     * Construct an SelectImageFrame to be used for creating a new class.
     *
     * @param owner    The parent frame
     * @param project  The project to contain the new class
     */
    public NewImageClassFrame(Window owner, Project project)
    {
        super(owner, Config.getString("imagelib.newClass"), "image-lib");
        this.project = project;
        buildUI();
    }

    /**
     * build the UI components
     */
    private void buildUI()
    {
        // Ok and cancel buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        imageLibPane = new ImageLibPane(this.asWindow(), project);
        setContentPane(new VBox(10, buildClassDetailsPanel(project.getUnnamedPackage()), imageLibPane)); /////// Here put the Pane
        VBox.setVgrow(imageLibPane, Priority.ALWAYS);

        classNameField.requestFocus();

        setResultConverter(bt -> bt == ButtonType.OK
                ? new NewImageClassInfo(classNameField.getText(), languageSelectionBox.getValue(), imageLibPane.selectedImageProperty().get())
                : null);
    }

    /**
     * Build the class details panel.
     *
     * @param pkg The default project's package.
     */
    private Pane buildClassDetailsPanel(Package pkg)
    {
        classNameField.setPrefWidth(220);
        classNameField.setPromptText(Config.getString("pkgmgr.newClass.prompt"));

        languageSelectionBox = new ComboBox<>(FXCollections.observableArrayList(SourceType.Stride, SourceType.Java));
        languageSelectionBox.getSelectionModel().select(pkg.getDefaultSourceType());

        okButton = getDialogPane().lookupButton(ButtonType.OK);

        StringProperty classNameProperty = classNameField.textProperty();
        ReadOnlyObjectProperty<SourceType> sourceTypeProperty = languageSelectionBox.getSelectionModel().selectedItemProperty();
        final ClassNameVerifier classNameVerifier = new ClassNameVerifier(pkg, classNameProperty, sourceTypeProperty);
        updateControls(classNameVerifier);

        JavaFXUtil.addChangeListenerPlatform(classNameProperty, text -> updateControls(classNameVerifier));
        JavaFXUtil.addChangeListenerPlatform(sourceTypeProperty, type -> updateControls(classNameVerifier));
        JavaFXUtil.addChangeListenerPlatform(imageLibPane.selectedImageProperty(), image -> updateControls(classNameVerifier));

        HBox fileDetailsRow = new HBox(5, new Label(Config.getString("imagelib.className")), classNameField, languageSelectionBox);
        fileDetailsRow.setAlignment(Pos.BASELINE_LEFT);

        return new VBox(fileDetailsRow, errorMsgLabel, new Separator(Orientation.HORIZONTAL));
    }

    /**
     * Enable/disable the ok button and show/hide the error message based on
     * the validity of the class name. It also updates the error message contents.
     *
     * @param classNameVerifier the class verifier that validates the class name's
     *                          text field contents.
     */
    private void updateControls(ClassNameVerifier classNameVerifier)
    {
        boolean valid = classNameVerifier.checkValidity();
        errorMsgLabel.setVisible(!valid);
        errorMsgLabel.setText(classNameVerifier.getMessage());
        okButton.setDisable(!valid);
    }
}
