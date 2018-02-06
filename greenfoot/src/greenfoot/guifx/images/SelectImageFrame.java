/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2014,2015,2016,2017,2018  Poul Henriksen and Michael Kolling

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
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.javafx.FXCustomizedDialog;

import java.io.File;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * A (modal) dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
public class SelectImageFrame extends FXCustomizedDialog<File>
{
    private final ClassTarget classTarget;
    private ImageLibPane imageLibPane;
    /** A watcher that goes notified when an image is selected, to allow for previewing. May be null */
    private final ImageSelectionWatcher selectionWatcher;

    /**
     * Construct an SelectImageFrame for changing the image of an existing class.
     *
     * @param owner      The parent frame
     * @param classTarget  The ClassView of the existing class
     */
    public SelectImageFrame(Window owner, ClassTarget classTarget, ImageSelectionWatcher watcher)
    {
        super(owner, Config.getString("imagelib.title") + " " + classTarget.getDisplayName(), "image-lib");
        this.classTarget = classTarget;
        this.selectionWatcher = watcher;
        buildUI();
    }

    /**
     * build the UI components
     */
    private void buildUI()
    {
        imageLibPane = new ImageLibPane(this.asWindow(), classTarget, selectionWatcher);
        setContentPane(imageLibPane);

        // Ok and cancel buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(imageLibPane.selectedImageProperty().isNull());

        setResultConverter(bt -> bt == ButtonType.OK ? imageLibPane.selectedImageProperty().get() : null);
    }
}
