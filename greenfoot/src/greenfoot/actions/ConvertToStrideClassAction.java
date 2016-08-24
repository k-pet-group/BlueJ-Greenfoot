/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2015,2016  Poul Henriksen and Michael Kolling
 
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

import bluej.BlueJTheme;
import bluej.Config;
import greenfoot.gui.classbrowser.ClassView;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;


/**
 * An action to convert a Stride class to a Java class.
 * 
 * @author Amjad Altadmri
 */
public class ConvertToStrideClassAction extends AbstractAction
{
    private ClassView cls;
    private JFrame frame;

    public ConvertToStrideClassAction(ClassView view, JFrame frame)
    {
        super(Config.getString("convert.to.stride.class"));
        this.cls = view;
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e)
    {
        //if (confirmConvertClass(cls, frame)) {
            cls.convertJavaToStride();
        //}
    }

}
