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

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ImageClassRole;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.Config;
import bluej.extensions.ProjectNotOpenException;


/**
 * Action that creates a new class as a subclass of an existing class
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewSubclassAction.java 6216 2009-03-30 13:41:07Z polle $
 */
public class NewSubclassAction extends AbstractAction
{
    private ClassView superclass;
    private ClassBrowser classBrowser;

    /**
     * Creates a new subclass of the class represented by the view
     * 
     * @param view
     *            The class that is to be the superclass
     * @param name
     *            Name of the action that appears in the menu
     */
    public NewSubclassAction(ClassView view, ClassBrowser classBrowser)
    {
        super(Config.getString("new.subclass"));
        this.superclass = view;
        this.classBrowser = classBrowser;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        GClass superG = superclass.getGClass();
        
        boolean imageClass = superG.isActorClass() || superG.isActorSubclass();
        imageClass |= superG.isWorldClass() || superG.isWorldSubclass();
            
        if (imageClass) {
            createImageClass();
        } else {
            createNonActorClass();
        }
    }
    
    public void createImageClass()
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        
        ImageLibFrame dialog = new ImageLibFrame(f, superclass.getGClass());
        if (! (dialog.getResult() == ImageLibFrame.OK)) {
            return;
        }
        
        String className = dialog.getClassName();
        GClass gClass = superclass.createSubclass(className);
       
        ClassView classView = new ClassView(classBrowser, gClass);
        
        SelectImageAction.setClassImage(classView,
                (ImageClassRole) classView.getRole(),
                dialog.getSelectedImageFile());

        classBrowser.addClass(classView);
    }
    
    public void createNonActorClass()
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        GPackage pkg = null;
        try {
            pkg = classBrowser.getProject().getDefaultPackage();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
            return;
        }
        catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        NewClassDialog dialog = new NewClassDialog(f, pkg);
        dialog.setVisible(true);
        if (!dialog.okPressed()) {
            return;
        }

        String className = dialog.getClassName();
        GClass gClass = superclass.createSubclass(className);   
        
        ClassView classView = new ClassView(classBrowser, gClass);
        classBrowser.addClass(classView);        
    }

}