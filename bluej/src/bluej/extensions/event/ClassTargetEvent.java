/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions.event;

import java.util.ArrayList;
import java.util.List;

import bluej.extensions.BClassTarget;
import bluej.extensions.BDependency;
import bluej.extensions.BPackage;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;

/**
 * This class encapsulates events which occur on class targets of the class
 * diagram.
 * 
 * @author Simon Gerlach
 */
public class ClassTargetEvent implements ExtensionEvent
{
    /**
     * This enumeration contains constants which describe the different types of
     * "class target changed" events.
     * 
     * @author Simon Gerlach
     */
    public enum Type
    {
        /** This event occurs when a class target was set to invisible. */
        CLASS_TARGET_HIDDEN,

        /** This event occurs when a class target was set to visible. */
        CLASS_TARGET_SHOWN;
    }

    private Type eventType;
    private ClassTarget classTarget;
    private Package bluejPackage;

    /**
     * Constructor. Creates a new {@link ClassTargetEvent}.
     * 
     * @param classTarget
     *            The class target which caused this event.
     * @param bluejPackage
     *            The package to which the class target belongs.
     * @param visible
     *            The new visibility of the class target.
     */
    public ClassTargetEvent(ClassTarget classTarget, Package bluejPackage, boolean visible) {
        this.classTarget = classTarget;
        this.bluejPackage = bluejPackage;

        if (visible) {
            eventType = Type.CLASS_TARGET_SHOWN;
        } else {
            eventType = Type.CLASS_TARGET_HIDDEN;
        }
    }

    /**
     * Returns the type of this event.
     * 
     * @return The type of this event.
     */
    public Type getEventType()
    {
        return eventType;
    }

    /**
     * Returns the class target which caused this event.
     * 
     * @return The class target which caused this event.
     */
    public BClassTarget getClassTarget()
    {
        return classTarget.getBClassTarget();
    }
    
    /**
     * Returns the associated target of the class target which caused this event
     * or <code>null</code> if there is no associated target. For example, this
     * can be the the class target of the corresponding test class of the class
     * whose class target caused this event.
     * 
     * @return The associated target of the class target which caused this event
     *         or <code>null</code> if there is no associated target.
     */
    public BClassTarget getAssociation()
    {
        DependentTarget association = classTarget.getAssociation();
        
        if (association instanceof ClassTarget) {
            return ((ClassTarget) association).getBClassTarget();
        }

        return null;
    }

    /**
     * Returns a {@link List} containing all dependencies which are related to
     * super classes and implemented interfaces.
     * 
     * @return A {@link List} containing all dependencies which are related to
     *         super classes and implemented interfaces. This may be a view
     *         (i.e. it may be updated automatically) and should not be modified.
     */
    public List<BDependency> getParentDependencies()
    {
        return getBDependencies(classTarget.getParents());
    }

    /**
     * Returns a {@link List} containing all dependencies which are related to
     * sub classes and implementors.
     * 
     * @return A {@link List} containing all dependencies which are related to
     *         sub classes and implementors. Thi smay be a view (i.e. it may be
     *         updated automatically) and should not be modified.
     */
    public List<BDependency> getChildDependencies()
    {
        return getBDependencies(classTarget.getChildren());
    }

    /**
     * Takes a {@link List} of dependencies and returns a {@link List}
     * containing the corresponding {@link BDependency} objects.
     * 
     * @param dependencies
     *            The {@link List} of dependencies.
     * @return A {@link List} containing the corresponding {@link BDependency}
     *         objects.
     */
    private List<BDependency> getBDependencies(List<Dependency> dependencies)
    {
        List<BDependency> result = new ArrayList<BDependency>();

        for (Dependency dependency : dependencies) {
            result.add(dependency.getBDependency());
        }

        return result;
    }

    /**
     * Returns the package to which the class target belongs that caused this
     * event.
     * 
     * @return The package to which the class target belongs that caused this
     *         event.
     */
    public BPackage getPackage()
    {
        return bluejPackage.getBPackage();
    }

    /**
     * Returns a {@link String} representation of this event.
     */
    @Override
    public String toString()
    {
        return "ClassTargetEvent: " + eventType + " (" + classTarget + ")";
    }
}
