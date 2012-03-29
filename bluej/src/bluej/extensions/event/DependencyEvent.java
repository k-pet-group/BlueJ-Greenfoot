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

import bluej.extensions.BClassTarget;
import bluej.extensions.BDependency;
import bluej.extensions.BPackage;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;

/**
 * This class encapsulates events which occur on dependencies of the class
 * diagram.
 * 
 * @author Simon Gerlach
 */
public class DependencyEvent implements ExtensionEvent
{
    /**
     * This enumeration contains constants which describe the different types of
     * "dependency changed" events.
     * 
     * @author Simon Gerlach
     */
    public enum Type
    {
        /** This event occurs when a dependency was added to the package. */
        DEPENDENCY_ADDED,

        /** This event occurs when a dependency was set to invisible. */
        DEPENDENCY_HIDDEN,

        /** This event occurs when a dependency was set to visible. */
        DEPENDENCY_SHOWN,

        /** This event occurs when a dependency was removed from the package. */
        DEPENDENCY_REMOVED;
    }

    private Type eventType;
    private Dependency dependency;
    private Package bluejPackage;

    /**
     * Constructor. Creates a new {@link DependencyEvent}. The type of the event
     * is determined by the given visibility.
     * 
     * @param dependency
     *            The dependency which caused this event.
     * @param bluejPackage
     *            The package to which the dependency belongs.
     * @param visible
     *            The new visibility of the dependency.
     */
    public DependencyEvent(Dependency dependency, Package bluejPackage, boolean visible)
    {
        this(dependency, bluejPackage, (visible ? Type.DEPENDENCY_SHOWN : Type.DEPENDENCY_HIDDEN));
    }

    /**
     * Constructor. Creates a new {@link DependencyEvent} with the given values.
     * 
     * @param dependency
     *            The dependency which caused this event.
     * @param bluejPackage
     *            The package to which the dependency belongs.
     * @param eventType
     *            The type of this event.
     */
    public DependencyEvent(Dependency dependency, Package bluejPackage, Type eventType)
    {
        this.dependency = dependency;
        this.bluejPackage = bluejPackage;
        this.eventType = eventType;
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
     * Returns the type of the dependency which caused this event.
     * 
     * @return The type of the dependency which caused this event.
     */
    public BDependency.Type getDependencyType()
    {
        return dependency.getType();
    }

    /**
     * Returns the origin of the dependency which caused this event.
     * 
     * @return The origin of the dependency which caused this event.
     */
    public BClassTarget getOrigin()
    {
        DependentTarget origin = dependency.getFrom();
        return ((ClassTarget) origin).getBClassTarget();
    }

    /**
     * Returns the target of the dependency which caused this event.
     * 
     * @return The target of the dependency which caused this event.
     */
    public BClassTarget getTarget()
    {
        DependentTarget target = dependency.getTo();
        return ((ClassTarget) target).getBClassTarget();
    }
    
    /**
     * Returns the package to which the dependency belongs that caused this
     * event.
     * 
     * @return The package to which the dependency belongs that caused this
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
        return "DependencyEvent: " + eventType + " (" + dependency + ")";
    }
}
