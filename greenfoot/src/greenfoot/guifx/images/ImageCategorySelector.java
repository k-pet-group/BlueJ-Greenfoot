/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2014,2015,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.utility.javafx.JavaFXUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Arrays;

/**
 * A list which allows selecting image categories. The categories
 * available are determined by scanning a directory for subdirectories.
 * Selecting a category will make a corresponding ImageLibList show
 * the contents of that category.
 * 
 * @author davmac
 */
@OnThread(Tag.FXPlatform)
public class ImageCategorySelector extends ListView<File>
{
    private ImageLibList imageLibList;
    
    /**
     * Construct an ImageCategorySelector to show categories from the
     * given directory.
     * 
     * @param categoriesRoot  The directory containing the categories
     *                     (subdirectories)
     */
    public ImageCategorySelector(File categoriesRoot)
    {
        setOrientation(Orientation.VERTICAL);
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setCellFactory(param -> new ImageCell());

        // Show directories only
        File[] categoriesFolders = categoriesRoot.listFiles(File::isDirectory);
        if (categoriesFolders == null)
        {
            return;
        }
        
        Arrays.sort(categoriesFolders);
        setItems(FXCollections.observableArrayList(categoriesFolders));

        JavaFXUtil.addChangeListenerPlatform(getSelectionModel().selectedItemProperty(), selected -> {
            if (imageLibList != null && selected != null)
            {
                imageLibList.setDirectory(selected);
            }
        });
    }

    /**
     * Set the ImageLibList to be associated with this category selector.
     * When a category is selected, the associated ImageLibList will be
     * made to show images from the category.
     * 
     * @param imageLibList  The ImageLibList to associate with this category
     *                      selector
     */
    public void setImageLibList(ImageLibList imageLibList)
    {
        this.imageLibList = imageLibList;
    }
    
    @OnThread(Tag.FXPlatform)
    private static class ImageCell extends ListCell<File>
    {
        private static final String iconFile = "openRight.png";
        private static final Image openRightIcon = new Image(ImageCategorySelector.class.getClassLoader().getResource(iconFile).toString());

        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public void updateItem(File file, boolean empty)
        {
            super.updateItem(file, empty);
            if (empty || file == null)
            {
                setText(null);
                setGraphic(null);
            }
            else
            {
                setText(file.getName());
                setGraphic(new ImageView(openRightIcon));
            }
        }
    }
}
