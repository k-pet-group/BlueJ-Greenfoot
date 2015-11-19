/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.stride;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;

import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A file completion for image filenames in the scenario's "images/" subdirectory
 */
@OnThread(Tag.FX) class ImageCompletion implements InteractionManager.FileCompletion
{
    private final File file;

    public ImageCompletion(File file)
    {
        this.file = file;
    }

    @Override
    public File getFile()
    {
        return file;
    }

    @Override
    public String getType()
    {
        return "Image";
    }

    @Override
    public Node getPreview(double maxWidth, double maxHeight)
    {
        try
        {
            Image img = new Image(file.toURI().toURL().toString());
            ImageView view = new ImageView(img);
            // Only resize if it's bigger than will fit (don't want to scale up):
            if (img.getWidth() > maxWidth || img.getHeight() > maxHeight)
            {
                view.setFitHeight(maxHeight);
                view.setFitWidth(maxWidth);
                view.setPreserveRatio(true);
            } else
            {
                view.setFitHeight(0);
                view.setFitWidth(0);
                view.setPreserveRatio(true);
            }
            return view;
        } catch (MalformedURLException | IllegalArgumentException e)
        {
            return new Label("Error loading image");
        }
    }

    @Override
    public Map<KeyCode, Runnable> getShortcuts()
    {
        return null;
    }

}
