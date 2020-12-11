/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2017,2018,2020  Michael Kolling and John Rosenberg

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
package bluej.classmgr;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.NoMultipleSelectionModel;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * A PrefPanel subclass to allow the user to interactively add a new library
 * to the browser.  The new library can be specified as a file (ZIP or JAR
 * archive) with an associated description.
 *
 * @author  Andrew Patterson
 */
@OnThread(Tag.FXPlatform)
public class ClassMgrPrefPanel extends VBox
implements PrefPanelListener
{
    private static final String userlibPrefix = "bluej.userlibrary";

    /**
     * The user libraries are referenced cross-project, so someone needs to a hold
     * a static reference to keep track of them.  This class seems as good a place
     * as any.  
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static List<ClassPathEntry> savedUserLibraries = null;
    
    private ListView<ClassPathEntry> userLibrariesListView = null;
    private ObservableList<ClassPathEntry> editingUserLibraries;
    private boolean classPathModified = false;

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     */
    public ClassMgrPrefPanel()
    {
        JavaFXUtil.addStyleClass(this, "prefmgr-pref-panel", "prefmgr-library-panel");
        loadSavedUserLibraries();


        // TODO: There a re a few historical issues here, the first one is that this list is calculated
        // here but in reality now it is much more dynamic, there is no need to restart BlueJ to have
        // the new value applied, so this list should also be dynamic.
        // The second point is that it does not make much sense to say loaded or unloaded since
        // if it is a valid jar the it is in the project classloader.
        // Somthing to fix in the future.
        // It may have more meaning to show what is the project classloader, that would include all
        // libraries, and paths, including +libs 
        List<ClassPathEntry> userlibExtLibrariesList =
            Project.getUserlibContent().stream()
                .map(url -> { try {return new ClassPathEntry(new File(url.toURI()), "");} catch (URISyntaxException e){ return null;}})
                .filter(f -> f != null)
                .distinct().collect(Collectors.toList());

        // Construct a user editable table of user libraries and add/remove buttons

        // hold the scrolling table and the column of add/remove buttons in a row
        BorderPane userLibPane = new BorderPane();
        {
            JavaFXUtil.addStyleClass(userLibPane, "prefmgr-library-userlib-hbox");
            
            editingUserLibraries = FXCollections.observableArrayList();
            editingUserLibraries.setAll(savedUserLibraries);
            // list of user library classpath entries
            userLibrariesListView = makeClassPathEntryListView(editingUserLibraries);
            userLibrariesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            // hold the two Add and Delete buttons in a column
            Pane buttonPane = new VBox();
            {
                JavaFXUtil.addStyleClass(buttonPane, "prefmgr-library-userlib-buttons");
                Button addFileButton = new Button(Config.getString("classmgr.addFile"));
                addFileButton.setOnAction(e -> addUserLibraryFile());

                Button deleteButton = new Button(Config.getString("classmgr.delete"));
                deleteButton.setOnAction(e -> deleteUserLibrary());
                deleteButton.disableProperty().bind(userLibrariesListView.getSelectionModel().selectedItemProperty().isNull());

                buttonPane.getChildren().addAll(addFileButton, deleteButton);
            }
            userLibPane.setCenter(userLibrariesListView);
            userLibPane.setRight(buttonPane);
        }

        // Construct a list of system libraries

        ListView<ClassPathEntry> userlibExtLibrariesListView = makeClassPathEntryListView(FXCollections.observableArrayList(userlibExtLibrariesList));

        // Don't need selection in bottom table:
        userlibExtLibrariesListView.setSelectionModel(new NoMultipleSelectionModel<>());
        userlibExtLibrariesListView.setFocusTraversable(false);
        
        String userlibLocation = Config.getString("classmgr.userliblibraries") 
                + " " + Config.getBlueJLibDir() + File.separator + "userlib";

        getChildren().add(PrefMgrDialog.headedVBox("classmgr.userlibraries", Arrays.asList(userLibPane)));
        getChildren().add(PrefMgrDialog.headedVBoxTranslated(userlibLocation, Arrays.asList(userlibExtLibrariesListView)));
    }

    @OnThread(Tag.Any)
    private static synchronized void loadSavedUserLibraries()
    {
        if (savedUserLibraries == null)
        {
            // Get the list of user libraries from the configuration.
            // This list os the one that is saved into the config file.
            savedUserLibraries = new ArrayList<>();
            addConfigEntries(savedUserLibraries, userlibPrefix);
        }
    }

    private ListView<ClassPathEntry> makeClassPathEntryListView(ObservableList<ClassPathEntry> userlibExtLibrariesList)
    {
        ListView<ClassPathEntry> userlibExtLibrariesListView = new ListView<>(userlibExtLibrariesList);
        JavaFXUtil.addStyleClass(userlibExtLibrariesListView, "prefmgr-library-listview");
        userlibExtLibrariesListView.setCellFactory(lv -> {
            return new TextFieldListCell<>(new StringConverter<ClassPathEntry>()
            {
                @Override
                public ClassPathEntry fromString(String string)
                {
                    return null;
                }

                @Override
                public String toString(ClassPathEntry cpe)
                {
                    return cpe.getCanonicalPathNoException() + " (" + cpe.getStatusString() + ")";
                }
            });
        });
        userlibExtLibrariesListView.setEditable(false);
        return userlibExtLibrariesListView;
    }

    /**
     * Returns an ArrayList of URLS holding jars that the user wish to be added to 
     * the Project classloader.
     * @return a non null but possibly empty arrayList of URL.
     */
    @OnThread(Tag.Any)
    public synchronized static List<URL> getUserConfigContent()
    {
        loadSavedUserLibraries();
        return Utility.mapList(savedUserLibraries, ClassPathEntry::safeGetURL);
    }


    /**
     * Retrieve from the system wide Config entries corresponding to classpath
     * entries. The entries to retrieve start with prefix and have 1.location,
     * 2.location etc appended to them until an entry is not found.
     *
     * @param   prefix    the prefix of the property names to look up
     */
    @OnThread(Tag.Any)
    private static void addConfigEntries(List<ClassPathEntry> cp, String prefix)
    {
        int resourceID = 1;
        try {
            while (true) {
                String location = Config.getPropString(prefix + resourceID + ".location", null);

                if (location == null)
                    break;

                try {
                    StringTokenizer st = new StringTokenizer(location, File.pathSeparator);

                    while(st.hasMoreTokens()) {
                        String entry = st.nextToken();
                        ClassPathEntry cpentry = new ClassPathEntry(entry, "");

                        if(!cp.contains(cpentry))
                            cp.add(cpentry);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                resourceID++;
            }
        } catch (MissingResourceException mre) {
            // it is normal that this is exception is thrown, it just means we've come
            // to the end of the file
        }
    }

    @Override
    public void beginEditing(Project project)
    {
    }

    public synchronized void revertEditing(Project project)
    {
        editingUserLibraries.setAll(savedUserLibraries);
    }

    @Override
    public void commitEditing(Project project)
    {
        if (classPathModified) {
            DialogManager.showMessageFX(null, "classmgr-changes-no-effect");
            classPathModified = false;
        }

        saveUserLibraries();
    }


    /**
     * Save user classpath entries into the system wide Config properties object.
     * The entries stored start with prefix and have 1.location,
     * 2.location etc appended to them.
     */
    private synchronized void saveUserLibraries()
    {
        savedUserLibraries.clear();
        savedUserLibraries.addAll(editingUserLibraries);
        
        String r1;
        int resourceID = 1;

        while(true) {
            r1 = Config.removeProperty(userlibPrefix + resourceID + ".location");

            if(r1 == null)
                break;

            resourceID++;
        }

        Iterator<ClassPathEntry> it = savedUserLibraries.iterator();
        resourceID = 1;

        while (it.hasNext()) {
            ClassPathEntry nextEntry = it.next();
            Config.putPropString(userlibPrefix + resourceID + ".location",
                    nextEntry.getPath());
            resourceID++;
        }
    }



    /**
     * Pop up a dialog to allow the user to add a library
     * to their user library classpath.
     **/
    private void addUserLibraryFile()
    {
        List<File> files = FileUtility.getOpenFilesFX(getScene().getWindow(), Config.getString("prefmgr.misc.addLibTitle"),
                Arrays.asList(new FileChooser.ExtensionFilter(Config.getString("prefmgr.misc.libFileFilter"), "*.zip", "*.jar")), false);

        if (files != null) {
            for (File file : files)
            {
                editingUserLibraries.add(new ClassPathEntry(file.getAbsolutePath(), "", true));
            }
            classPathModified = true;
        }
    }

    /**
     * Delete the currently selected row (if there is one)
     * of the user library table from the user library
     * classpath.
     */
    private void deleteUserLibrary()
    {
        int which = userLibrariesListView.getSelectionModel().getSelectedIndex();

        if(which != -1) {
            classPathModified = true;
            editingUserLibraries.remove(which);
        }
    }

}
