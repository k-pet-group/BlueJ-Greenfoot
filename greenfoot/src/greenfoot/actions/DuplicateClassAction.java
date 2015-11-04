/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2015 Poul Henriksen and Michael Kolling 
 
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
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;
import greenfoot.record.InteractionListener;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Config;
import bluej.extensions.SourceType;

/**
 * An action for creating a copy of an existing class.
 * 
 * @author Amjad Altadmri
 */
public class DuplicateClassAction extends AbstractAction
{
    private ClassView originalClass;
    private ClassBrowser classBrowser;
    private InteractionListener interactionListener;

    /**
     * Construct a CopyClassAction instance.
     * 
     * @param view
     *            The class that is to be the superclass
     * @param name
     *            Name of the action that appears in the menu
     * @param interactionListener
     *            The listener to be notified of interactions (instance creation, method calls) which
     *            occur on the new class.
     */
    public DuplicateClassAction(ClassView view, ClassBrowser classBrowser, InteractionListener interactionListener)
    {
        super(Config.getString("duplicate.class"));
        this.originalClass = view;
        this.classBrowser = classBrowser;
        this.interactionListener = interactionListener;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        GClass originalG = originalClass.getGClass();
        String originalClassName = originalG.getName();
        SourceType sourceType = originalG.getSourceType();
        
        JFrame f = classBrowser.getFrame();
        GPackage pkg = classBrowser.getProject().getDefaultPackage();
        NewClassDialog dialog = new NewClassDialog(f, pkg);
        dialog.setSuggestedClassName("CopyOf" + originalClassName);
        dialog.setSelectedLanguage(sourceType);
        dialog.disableLanguageSelectionBox();
        dialog.setVisible(true);
        if (!dialog.okPressed()) {
            return;
        }

        String className = dialog.getClassName();
        SourceType language = dialog.getSelectedLanguage();
        
        try {
            File dir = pkg.getProject().getDir();
            final String extension = language.toString().toLowerCase();
            File newFile = new File(dir, className + "." + extension);
            File originalFile = new File(dir, originalClassName + "." + extension);
            GreenfootUtilDelegateIDE.getInstance().duplicate(originalClassName, className, originalFile, newFile, sourceType);

            GClass newClass = pkg.newClass(className, extension, false);

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
