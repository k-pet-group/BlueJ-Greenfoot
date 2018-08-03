/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2015,2016  Michael Kolling and John Rosenberg

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
package bluej.pkgmgr;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import bluej.utility.javafx.UnfocusableScrollPane;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.classmgr.ClassMgrPrefPanel;
import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Dialog for exporting the project to a jar file. Here, the jar
 * creation options can be specified.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
class ExportDialog extends Dialog<ExportDialog.ExportInfo>
{
    // Internationalisation
    private static final String dialogTitle = Config.getString("pkgmgr.export.title");
    private static final String helpLine1 = Config.getString("pkgmgr.export.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.export.helpLine2");
    private static final String classLabelText = Config.getString("pkgmgr.export.classLabel");
    private static final String libsLabel = Config.getString("pkgmgr.export.includeLibs");
    private static final String sourceLabel = Config.getString("pkgmgr.export.sourceLabel");
    private static final String pkgFilesLabel = Config.getString("pkgmgr.export.pkgFilesLabel");
    private static final String noClassText = Config.getString("pkgmgr.export.noClassText");
    
    private final ComboBox<String> classSelect;
    private final CheckBox sourceBox;
    private final CheckBox pkgFilesBox;
    private final List<UserLibInfo> userLibs = new ArrayList<>();

    private GridPane userLibPanel;
    
    @OnThread(Tag.Any)
    public static class ExportInfo
    {
        public final String mainClassName;
        public final List<File> selectedFiles;
        public final boolean includeSource;
        public final boolean includePkgFiles;

        private ExportInfo(String mainClassName, List<File> selectedFiles, boolean includeSource, boolean includePkgFiles)
        {
            this.includePkgFiles = includePkgFiles;
            this.mainClassName = mainClassName;
            this.selectedFiles = selectedFiles;
            this.includeSource = includeSource;
        }
    }

    /**
     * We must fetch this information on the swing thread,
     * ahead of using it on the FX thread to update the dialog.
     */
    public static class ProjectInfo
    {
        public final List<String> classNames;
        public final List<File> jarFiles;
        
        @OnThread(Tag.FXPlatform)
        public ProjectInfo(Project project)
        {
            classNames = project.getPackageNames().stream().sorted().flatMap(pkgName ->
                project.getPackage(pkgName).getAllClassnames().stream().sorted()
                    .map(className -> pkgName.isEmpty() ? className : pkgName + "." + className))
                .collect(Collectors.toList());

            // get user specified libs
            List<URL> libList = ClassMgrPrefPanel.getUserConfigContent();

            // also get any libs in userlib directory
            libList.addAll(Project.getUserlibContent());

            jarFiles = Utility.mapList(libList, url -> {
                try {
                    return new File(new URI(url.toString()));
                }
                catch (URISyntaxException use) {
                    // Should never happen. If there is a problem with the conversion we want to know about it.
                    Debug.reportError("ExportDialog.createUserLibPanel(Project) invalid url=" + url.getPath());
                }
                return null;
            });
        }
    }

    public ExportDialog(Window parent, ProjectInfo projectInfo)
    {
        setTitle(dialogTitle);
        initOwner(parent);
        initModality(Modality.WINDOW_MODAL);
        Config.addDialogStylesheets(getDialogPane());
        setResultConverter(this::calculateResult);
        VBox mainPanel = new VBox();
        JavaFXUtil.addStyleClass(mainPanel, "export-dialog-content");
        {
            Label helpText = new Label(helpLine1 + " " + helpLine2);
            helpText.setWrapText(true);
            helpText.setMaxWidth(400.0);
            mainPanel.getChildren().add(helpText);

            mainPanel.getChildren().add(new Separator());

            {
                HBox mainClassPanel = new HBox();
                JavaFXUtil.addStyleClass(mainClassPanel, "export-dialog-main-row");
                mainClassPanel.setAlignment(Pos.CENTER);
                Label classLabel = new Label(classLabelText);
                mainClassPanel.getChildren().add(classLabel);

                classSelect = new ComboBox<>();
                fillClassPopup(projectInfo.classNames);
                classSelect.getSelectionModel().select(noClassText);
                mainClassPanel.getChildren().add(classSelect);
                mainPanel.getChildren().add(mainClassPanel);
            }
            
            {
                userLibPanel = new GridPane();
                JavaFXUtil.addStyleClass(userLibPanel, "export-dialog-userlibs");
                userLibPanel.setAlignment(Pos.CENTER);
                ScrollPane userLibPanelScrollPane = new UnfocusableScrollPane(userLibPanel);
                JavaFXUtil.expandScrollPaneContent(userLibPanelScrollPane);
                userLibPanelScrollPane.setMaxHeight(150.0);
                fillUserLibPanel(projectInfo, Collections.emptyList());
                mainPanel.getChildren().add(new Label(libsLabel));
                mainPanel.getChildren().add(userLibPanelScrollPane);
            }

            sourceBox = new CheckBox(sourceLabel);
            sourceBox.setSelected(true);
            mainPanel.getChildren().add(sourceBox);
            pkgFilesBox = new CheckBox(pkgFilesLabel);
            pkgFilesBox.setSelected(true);
            mainPanel.getChildren().add(pkgFilesBox);

            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            ((Button)getDialogPane().lookupButton(ButtonType.OK)).setText(BlueJTheme.getContinueLabel());
        }

        getDialogPane().setContent(mainPanel);
    }

    private ExportInfo calculateResult(ButtonType buttonType)
    {
        if (buttonType == ButtonType.OK)
        {
            String mainClassName = classSelect.getSelectionModel().getSelectedItem();
            if (mainClassName.equals(noClassText))
                mainClassName = "";
            List<File> selected = getSelectedLibs();
            return new ExportInfo(mainClassName, selected, sourceBox.isSelected(), pkgFilesBox.isSelected());
        }
        else
            return null;
    }

    private List<File> getSelectedLibs()
    {
        return userLibs.stream().filter(UserLibInfo::isSelected).map(UserLibInfo::getFile).collect(Collectors.toList());
    }

    public void updateDialog(ProjectInfo projectInfo)
    {
        String prevSelected = classSelect.getSelectionModel().getSelectedItem();
        fillClassPopup(projectInfo.classNames);
        if (classSelect.getItems().contains(prevSelected))
            classSelect.getSelectionModel().select(prevSelected);
        else
            classSelect.getSelectionModel().select(noClassText);
        fillUserLibPanel(projectInfo, getSelectedLibs());
    }

    /**
     * Fill the class name popup selector with all classes of the project
     */
    private void fillClassPopup(List<String> classNames)
    {
        classSelect.getItems().clear();
        classSelect.getItems().add(noClassText);
        classSelect.getItems().addAll(classNames);
    }

    /**
     * Return a prepared panel listing the user libraries with check boxes.
     * @param project the project the libraries belong to.
     */
    private void fillUserLibPanel(ProjectInfo projectInfo, List<File> startChecked)
    {
        userLibPanel.getChildren().clear();

        // collect info about jar files from the project classloader.
        List<UserLibInfo> userlibList = Utility.mapList(projectInfo.jarFiles, file -> new UserLibInfo(file, startChecked.contains(file)));
        
        if ( userlibList.size() < 1 ) { 
            userLibPanel.setVisible(false);
        }
        else {
            userLibPanel.setVisible(true);
            userLibs.clear();
            userLibs.addAll(userlibList);

            for(int i = 0; i < userLibs.size(); i++) {
                userLibPanel.add(userLibs.get(i).getCheckBox(), i % 2, i / 2);
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    private static class UserLibInfo
    {
        private final File sourceFile;
        private final CheckBox checkBox;

        public UserLibInfo(File source, boolean selected)
        {
            sourceFile = source;
            this.checkBox = new CheckBox(sourceFile.getName());
            this.checkBox.setSelected(selected);
        }

        /**
         * Return a checkBox with this lib's name as a label.
         */
        public CheckBox getCheckBox()
        {
            return checkBox;
        }

        /**
         * Return the file of this lib.
         */
        public File getFile()
        {
            return sourceFile;
        }

        /**
         * Tell whether this lib has been selected.
         */
        public boolean isSelected()
        {
            return checkBox.isSelected();
        }
    }
}
