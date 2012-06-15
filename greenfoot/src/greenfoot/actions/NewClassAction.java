/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;
import greenfoot.record.InteractionListener;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Config;

/**
 * An action for creating a new (non-Actor, non-World) class.
 * 
 * @author Davin McCall
 */
public class NewClassAction extends AbstractAction
{
    private GreenfootFrame gfFrame;
    private InteractionListener interactionListener;

    /**
     * Construct a NewClassAction instance.
     */
    public NewClassAction(GreenfootFrame gfFrame, InteractionListener interactionListener)
    {
        super(Config.getString("new.class"));
        setEnabled(false);
        this.gfFrame = gfFrame;
        this.interactionListener = interactionListener;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        JFrame f = gfFrame;
        ClassBrowser classBrowser = gfFrame.getClassBrowser();
        GPackage pkg = null;
        pkg = classBrowser.getProject().getDefaultPackage();
        
        NewClassDialog dialog = new NewClassDialog(f, pkg);
        dialog.setVisible(true);
        if (!dialog.okPressed()) {
            return;
        }
        
        String className = dialog.getClassName();
        
        try {
            File dir = pkg.getProject().getDir();
            File newJavaFile = new File(dir, className + ".java");
            GreenfootUtilDelegateIDE.getInstance().createSkeleton(className, null, newJavaFile,
                    NormalClassRole.getInstance().getTemplateFileName(), pkg.getProject().getCharsetName());

            GClass newClass = pkg.newClass(className, false);

            ClassView classView = new ClassView(classBrowser, newClass, interactionListener);
            classBrowser.addClass(classView);
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (IOException ioe) {
            // TODO definitely should report an error condition via dialog
            ioe.printStackTrace();
        }
    }
}
