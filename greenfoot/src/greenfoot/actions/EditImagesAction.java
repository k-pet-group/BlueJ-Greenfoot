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

import greenfoot.core.GProject;
import greenfoot.gui.ImageEditFrame;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import bluej.Config;
import javax.swing.JFrame;

/**
 * @author Michael Berry
 * @version $Id$
 */
public class EditImagesAction extends AbstractAction
{
	private GProject proj;
    private JFrame owner;

    public EditImagesAction(GProject proj, JFrame owner)
    {
        super(Config.getString("Images..."));
        setProject(proj);
    }

    public void setProject(GProject proj)
    {
    	this.proj = proj;
    	setEnabled(proj != null);
    }

    /**
     * Displays the image editing dialog.
     */
    public void actionPerformed(ActionEvent e)
    {
        ImageEditFrame frame = new ImageEditFrame(proj, owner);
        frame.setVisible(true);
    }
}