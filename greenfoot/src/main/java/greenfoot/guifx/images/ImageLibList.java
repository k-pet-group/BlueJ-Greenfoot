/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2012,2014,2015,2018  Poul Henriksen and Michael Kolling
 
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

import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A list component which displays a list of images (found in a directory) with their
 * filenames.
 * 
 * @author Davin McCall
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ImageLibList extends ListView<ImageListEntry>
{   
    /** The directory whose images are currently displayed in this list */
    private File directory;
    private final String[] imageFileExtensions = new String[] { "jpg", "jpeg", "png", "gif" };
    private boolean projectList;

    /**
     * Construct an empty ImageLibList.
     *
     * @param projectList True if this list for a project images, and false if it is for greenfoot library ones.
     */
    public ImageLibList(final boolean projectList)
    {      
        super();
        this.projectList = projectList;
        this.setCellFactory(param -> new ImageLibCell());
    }

    /**
     * Construct an empty ImageLibList, and populate it with entries from
     * the given directory.
     * 
     * @param directory   The directory to retrieve images from
     * @param projectList True if this list for a project images, and false if it is for greenfoot library ones.
     */
    public ImageLibList(File directory, final boolean projectList)
    {
        this(projectList);
        setDirectory(directory);
    }

    /**
     * Clear the list and re-populate it with images from the given directory.
     * 
     * @param directory   The directory to retrieve images from
     */
    public void setDirectory(File directory)
    {
        this.directory = directory;
        loadImages();
    }

    /**
     * Load the images from the scenario images' directory. If any of them has changed,
     * replace the existing items in the listView with them and return true. Otherwise,
     * just return false.
     *
     * @return True if the list view items have been replaced by new ones loaded from
     *         the disk, otherwise returns false.
     */
    private boolean loadImages()
    {
        // We accept only image files.
        FilenameFilter filter = (dir, name) -> Stream.of(imageFileExtensions).anyMatch(extension -> name.toLowerCase().endsWith(extension));
        File[] imageFiles = directory.listFiles(filter);
        if (imageFiles == null)
        {
            imageFiles = new File[0];
        }
        Arrays.sort(imageFiles);

        // Only replace the items in the listView if any of the files in the directory has changed.
        List<ImageListEntry> newEntries = Arrays.stream(imageFiles).map(file -> new ImageListEntry(file, projectList))
                .collect(Collectors.toList());
        if (getItems().equals(newEntries))
        {
            return false;
        }
        setItems(FXCollections.observableArrayList(newEntries));
        return true;
    }

    /**
     * Get the current directory this list is pointing to.
     */
    public File getDirectory()
    {
        return directory;
    }

    /**
     * Refresh the contents of the list.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void refresh()
    {
        if (loadImages())
        {
            super.refresh();
        }
    }

    /**
     * If the given file exists in this list, it will be selected.
     */
    public void select(File imageFile)
    {
        refresh();
        this.getSelectionModel().select(new ImageListEntry(imageFile, projectList));
    }

    /**
     * A Cell in a ListView of image files. Used to decide how the entries will be constructed in the list.
     */
    @OnThread(Tag.FXPlatform)
    static class ImageLibCell extends ListCell<ImageListEntry>
    {
        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public void updateItem(ImageListEntry item, boolean empty)
        {
            super.updateItem(item, empty);
            if (item != null)
            {
                if (item.isInProjectList())
                {
                    setText(GreenfootUtil.removeExtension(item.getImageName()));
                }
                else
                {
                    setAlignment(Pos.CENTER);
                }
                setTooltip(new Tooltip(item.getImageName()));
                setGraphic(item.getIcon());
            }
            else
            {
                setText(null);
                setTooltip(null);
                setGraphic(null);
            }
        }
    }
}
