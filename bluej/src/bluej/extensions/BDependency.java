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
package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.target.ClassTarget;

/**
 * A wrapper for a dependency (edge) in the class diagram of BlueJ.
 * 
 * @author Simon Gerlach
 */
public class BDependency
{
    /**
     * This enumeration contains constants which describe the nature of a
     * dependency.
     * 
     * @author Simon Gerlach
     */
    public enum Type
    {
        /**
         * The type of the dependency could not be determined. This usually
         * happens if the represented dependency does not exists anymore.
         */
        UNKNOWN,

        /** Represents a uses-dependency */
        USES,

        /** Represents an extends-dependency */
        EXTENDS,

        /** Represents an implements-dependency */
        IMPLEMENTS;
    }

    private Identifier originId;
    private Identifier targetId;
    private Type type;

    /**
     * Constructor. Creates a new {@link BDependency} from the given origin and
     * target IDs.
     * 
     * @param originId
     *            The {@link Identifier} which represents the origin of this
     *            dependency. It is duty of the caller to guarantee that this
     *            <code>originId</code> is reasonable.
     * @param targetId
     *            The {@link Identifier} which represents the target of this
     *            dependency. It is duty of the caller to guarantee that this
     *            <code>targetId</code> is reasonable.
     * @param type
     *            The type of the dependency (one of the constants in
     *            {@link BDependency.Type}).
     * @throws IllegalArgumentException
     *             if the specified {@link Identifier}s represent classes from
     *             different projects and/or packages.
     */
    BDependency(Identifier originId, Identifier targetId, Type type) throws IllegalArgumentException
    {
        if (!originId.equalsIgnoreClass(targetId)) {
            throw new IllegalArgumentException(
                    "The origin and target of a dependency must be in the same project in the same package");
        }

        this.originId = originId;
        this.targetId = targetId;
        this.type = type;
    }

    /**
     * Notification that the name of the underlying class of the origin of this
     * dependency has changed.
     * 
     * @param newOriginName
     *            The new class name, fully qualified.
     */
    void originNameChanged(String newOriginName)
    {
        try {
            Project bluejProject = originId.getBluejProject();
            Package bluejPackage = originId.getBluejPackage();

            originId = new Identifier(bluejProject, bluejPackage, newOriginName);
        } catch (ProjectNotOpenException e) {
            // cannot happen: the renaming of a class requires an open project
            // and package
        } catch (PackageNotFoundException e) {
            // cannot happen: the renaming of a class requires an open project
            // and package
        }
    }

    /**
     * Notification that the name of the underlying class of the target of this
     * dependency has changed.
     * 
     * @param newTargetName
     *            The new class name, fully qualified.
     */
    void targetNameChanged(String newTargetName)
    {
        try {
            Project bluejProject = targetId.getBluejProject();
            Package bluejPackage = targetId.getBluejPackage();

            targetId = new Identifier(bluejProject, bluejPackage, newTargetName);
        } catch (ProjectNotOpenException e) {
            // cannot happen: the renaming of a class requires an open project
            // and package
        } catch (PackageNotFoundException e) {
            // cannot happen: the renaming of a class requires an open project
            // and package
        }
    }

    /**
     * Returns the type of this dependency (one of the constants in
     * {@link BDependency.Type}). May be used by extensions to determine the art
     * of the dependency represented by this proxy object.
     * 
     * @return The type of this dependency. If the dependency represented by
     *         this proxy object is no longer valid,
     *         {@link BDependency.Type#UNKNOWN} is returned.
     * @throws ProjectNotOpenException
     *             if the project to which this dependency belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this dependency belongs has been
     *             deleted by the user.
     */
    public Type getType() throws ProjectNotOpenException, PackageNotFoundException
    {
        // Although we know the type we don't just return it. The dependency may
        // not exist anymore which means this BDependency object is invalid. A
        // client should be notified about this fact by returning
        // BDependency.Type.UNKNOWN.
        Type result = Type.UNKNOWN;
        Dependency dependency = originId.getDependency(targetId, type);

        if (dependency != null) {
            result = dependency.getType();
        }

        return result;
    }

    /**
     * Returns the origin of this dependency.
     * 
     * @return The origin of this dependency or <code>null</code> if the origin
     *         does not exist anymore.
     * @throws ProjectNotOpenException
     *             if the project to which the origin of this dependency belongs
     *             has been closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which the origin of this dependency belongs
     *             has been deleted by the user.
     */
    public BClassTarget getFrom() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget origin = originId.getClassTarget();
        return (origin != null) ? origin.getBClassTarget() : null;
    }

    /**
     * Returns the target of this dependency.
     * 
     * @return The target of this dependency or <code>null</code> if the target
     *         does not exist anymore.
     * @throws ProjectNotOpenException
     *             if the project to which the target of this dependency belongs
     *             has been closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which the target of this dependency belongs
     *             has been deleted by the user.
     */
    public BClassTarget getTo() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget target = targetId.getClassTarget();
        return (target != null) ? target.getBClassTarget() : null;
    }
    
    /**
     * Indicates whether this dependency shall be visible in the graph.
     * 
     * @return <code>true</code> if this dependency is visible,
     *         <code>false</code> otherwise.
     * @throws ProjectNotOpenException
     *             if the project to which this dependency belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this dependency belongs has been
     *             deleted by the user.
     */
    public boolean isVisible() throws ProjectNotOpenException, PackageNotFoundException
    {
        Dependency dependency = originId.getDependency(targetId, type);
        return (dependency != null) ? dependency.isVisible() : false;
    }

    /**
     * Sets the visible setting of this dependency.
     * 
     * @param visible
     *            The new visible setting.
     * @throws ProjectNotOpenException
     *             if the project to which this dependency belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this dependency belongs has been
     *             deleted by the user.
     */
    public void setVisible(boolean visible, boolean recalc) throws ProjectNotOpenException,
            PackageNotFoundException
    {
        Dependency dependency = originId.getDependency(targetId, type);

        if (dependency != null) {
            dependency.setVisible(visible);                
        
            if (recalc) {
                dependency.getFrom().recalcOutUses();
                dependency.getTo().recalcInUses();
            }
        }
    }
    
    /**
     * Returns a {@link String} representation of this object. 
     */
    @Override
    public String toString()
    {
        try {
            ClassTarget origin = originId.getClassTarget();
            ClassTarget target = targetId.getClassTarget();

            return "BDependency (" + type + "): " + origin.getIdentifierName() + " --> " + target.getIdentifierName();
        } catch (ExtensionException e) {
            return "BDependency: INVALID";
        }
    }
}
