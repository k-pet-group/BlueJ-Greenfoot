/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2014,2016,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;

import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extmgr.ExtensionMenu;
import bluej.extmgr.ExtensionPrefManager;
import bluej.extmgr.ExtensionWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;
import com.sun.jdi.Value;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;



/**
 * This class acts as a bridge between the extensions package and other
 * BlueJ-internal packages (extmgr) to provide access to methods which
 * shouldn't be documented in the Extensions API Javadoc. By using this class,
 * those methods can be made package-local.
 *
 * This class should be excluded when the Javadoc API documentation is generated.
 */
public final class ExtensionBridge
{
    public static void delegateEvent(BlueJ thisBluej, ExtensionEvent anEvent)
    {
        thisBluej.delegateEvent(anEvent);
    }

    public static Object getVal(PkgMgrFrame aFrame, String instanceName, Value val)
    {
        return BField.doGetVal(aFrame, instanceName, val);
    }

    public static BlueJ newBluej(ExtensionWrapper aWrapper,
                                 ExtensionPrefManager aPrefManager) {
        return new BlueJ(aWrapper, aPrefManager);
    }

    public static BObject newBObject(ObjectWrapper aWrapper) {
        return new BObject(aWrapper);
    }

    public static BProject newBProject(Project bluejPrj) {
        return new BProject(new Identifier(bluejPrj));
    }

    @OnThread(Tag.Any)
    public static BPackage newBPackage(Package bluejPkg) {
        return new BPackage(new Identifier(bluejPkg.getProject(), bluejPkg));
    }

    public static BClass newBClass(ClassTarget classTarget) {
        Package bluejPkg = classTarget.getPackage();
        Project bluejPrj = bluejPkg.getProject();

        return new BClass(new Identifier(bluejPrj, bluejPkg,
                classTarget.getQualifiedName()));
    }

    public static void ChangeBClassName(BClass bClass, String newName)
    {
        bClass.nameChanged(newName);
    }

    public static MenuItem getMenuItem(BlueJ aBluej, ExtensionMenu attachedObject)
    {
        return aBluej.getMenuItem(attachedObject);
    }

    public static void postMenuItem(BlueJ aBluej, ExtensionMenu attachedObject,
                                    MenuItem onThisItem)
    {
        aBluej.postMenuItem(attachedObject, onThisItem);
    }

    public static Project getProject(BProject bProject) throws ProjectNotOpenException
    {
        return bProject.getProject();
    }
}
