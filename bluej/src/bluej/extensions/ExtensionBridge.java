/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions;

import java.awt.Graphics2D;

import javax.swing.JMenuItem;

import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.BDependency.Type;
import bluej.extensions.event.ExtensionEvent;
import bluej.extmgr.ExtensionMenu;
import bluej.extmgr.ExtensionPrefManager;
import bluej.extmgr.ExtensionWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.graphPainter.ClassTargetPainter.Layer;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;

import com.sun.jdi.Value;


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

    public static BPackage newBPackage(Package bluejPkg) {
        return new BPackage(new Identifier(bluejPkg.getProject(), bluejPkg));
    }

    public static BClass newBClass(ClassTarget classTarget) {
        Package bluejPkg = classTarget.getPackage();
        Project bluejPrj = bluejPkg.getProject();

        return new BClass(new Identifier(bluejPrj, bluejPkg,
                classTarget.getQualifiedName()));
    }

    public static BClassTarget newBClassTarget(ClassTarget classTarget) {
        Package bluejPackage = classTarget.getPackage();
        Project bluejProject = bluejPackage.getProject();

        return new BClassTarget(new Identifier(bluejProject, bluejPackage,
                classTarget.getQualifiedName()));
    }

    public static BDependency newBDependency(Dependency dependency, Type type)
    {
        DependentTarget from = dependency.getFrom();
        DependentTarget to = dependency.getTo();
        Package bluejPackage = from.getPackage();
        Project bluejProject = bluejPackage.getProject();
        String qualifiedNameFrom = bluejPackage.getQualifiedName(from.getIdentifierName());
        String qualifiedNameTo = bluejPackage.getQualifiedName(to.getIdentifierName());

        return new BDependency(new Identifier(bluejProject, bluejPackage, qualifiedNameFrom),
                new Identifier(bluejProject, bluejPackage, qualifiedNameTo), type);
    }

    public static void ChangeBClassName(BClass bClass, String newName)
    {
        bClass.nameChanged(newName);
    }
    
    public static void changeBClassTargetName(BClassTarget bClassTarget, String newName)
    {
        bClassTarget.nameChanged(newName);
    }

    public static void changeBDependencyOriginName(BDependency bDependency, String newOriginName)
    {
        bDependency.originNameChanged(newOriginName);
    }

    public static void changeBDependencyTargetName(BDependency bDependency, String newTargetName)
    {
        bDependency.targetNameChanged(newTargetName);
    }

    public static JMenuItem getMenuItem(BlueJ aBluej, ExtensionMenu attachedObject)
    {
        return aBluej.getMenuItem(attachedObject);
    }

    public static void postMenuItem(BlueJ aBluej, ExtensionMenu attachedObject,
        JMenuItem onThisItem)
    {
        aBluej.postMenuItem(attachedObject, onThisItem);
    }
    
    public static boolean hasSourceCode(BClass bClass) throws ProjectNotOpenException, PackageNotFoundException
    {
        return bClass.hasSourceCode();
    }

    public static void clearObjectBench(BProject project) throws ProjectNotOpenException
    {
        project.clearObjectBench();
    }

    public static void drawExtensionClassTarget(BlueJ bluej, Layer layer,
            BClassTarget bClassTarget, Graphics2D graphics, int width, int height)
    {
        bluej.drawExtensionClassTarget(layer, bClassTarget, graphics, width, height);
    }
}
