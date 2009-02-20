/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.debugmgr.CallHistory;
import bluej.debugmgr.MethodDialog;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.views.CallableView;

/**
 * MethodDialog for Greenfoot. With Greenfoot in the title instead of BlueJ.
 * 
 * @author Poul Henriksen
 */
public class GreenfootMethodDialog extends MethodDialog
{

    public GreenfootMethodDialog(JFrame parentFrame, ObjectBenchInterface ob, CallHistory callHistory,
            String instanceName, CallableView method, Map typeMap)
    {
        super(parentFrame, ob, callHistory, instanceName, method, typeMap);

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                GreenfootUtil.makeGreenfootTitle(GreenfootMethodDialog.this);
            }
        });
    }

}
