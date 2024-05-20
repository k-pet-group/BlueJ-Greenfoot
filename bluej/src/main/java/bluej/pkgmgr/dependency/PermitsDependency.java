/*
 This file is part of the BlueJ program. 
 Copyright (C) 2023  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.dependency;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.DependentTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A permits dependency.
 * 
 * Permits dependencies are a little odd.  Let's imagine we have:
 *   sealed class Parent permits Child
 *   final class Child extends Parent
 * We will have an ExtendsDependency from Child to Parent, and a
 * PermitsDependency from Parent to Child.  Despite going in opposite directions, 
 * both will have the same effect: when Parent gets marked as needing 
 * recompile, Child will be a dependent and also get marked.
 * 
 * So, why need both?  Is Extends not enough?  The reason is that it is possible
 * to be in this state:
 *   sealed class Parent permits Child
 *   final class Child
 * In this case, changing Parent should require compiling Child (which needs
 * to fix up its dependencies) but without the ExtendsDependency, Child will not
 * get recompiled.  So we use the PermitsDependency to make sure Child is compiled too.
 */
public final class PermitsDependency extends Dependency
{
    public PermitsDependency(Package pkg, DependentTarget sealedParent, DependentTarget permittedChild)
    {
        super(pkg, sealedParent, permittedChild);
    }

    @Override
    @OnThread(Tag.Any)
    public Type getType()
    {
        return Type.PERMITS;
    }

    @Override
    public void remove()
    {
        // We don't show arrows for this dependency, so nothing to do.
    }

    @Override
    public boolean isRemovable()
    {
        return false;
    }
}
