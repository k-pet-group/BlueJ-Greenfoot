/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling

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
package greenfoot.actions;

import greenfoot.gui.images.ImageFilePreview;
import greenfoot.gui.images.ImageFilter;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.Selectable;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import bluej.Config;

/**
 * Action that will launch a file chooser for choosing image files. When an
 * image file has been chosen it will be copied to the specified directory.
 * 
 * @author Poul Henriksen
 */
public class BrowseImagesAction extends AbstractAction
{


    private JDialog owner;
    private File projImagesDir;
    private Selectable<File> selectable;

    /**
     * 
     * @param name Name of the action.
     * @param owner The dialog that will be the parent of the file chooser.
     * @param destDir Directory where the chosen image will be copied to.
     * @param selectable When the file has been copied, it will select it in this selectable (if it is not null).
     */
    public BrowseImagesAction(String name, JDialog owner, File destDir, Selectable<File> selectable)
    {
        super(name);
        this.owner = owner;
        this.projImagesDir = destDir;
        this.selectable = selectable;
    }

    public void actionPerformed(ActionEvent e)
    {
        JFileChooser chooser = new JFileChooser();
        new ImageFilePreview(chooser);
        chooser.setAcceptAllFileFilterUsed(false);
        ImageFilter filter = new ImageFilter();
        chooser.addChoosableFileFilter(filter);
        chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
        chooser.setFileFilter(filter);
        int choice = chooser.showDialog(owner, Config.getString("imagelib.choose.button"));
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            File newFile = new File(projImagesDir, file.getName());
            GreenfootUtil.copyFile(file, newFile);
            if(selectable != null) {
                selectable.select(newFile);
            }
        }
    }

}
