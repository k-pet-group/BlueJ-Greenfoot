/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2015,2016,2019  Michael Kolling and John Rosenberg
 
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

import java.util.Properties;

/**
 * An "implements" dependency between two (class) targets in a package
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class ImplementsDependency extends Dependency
{
    @OnThread(Tag.Any)
    public ImplementsDependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(pkg, from, to);
    }

    public ImplementsDependency(Package pkg)
    {
        this(pkg, null, null);
    }

    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);
        props.put(prefix + ".type", "ImplementsDependency");
    }
    
    public void remove()
    {
        pkg.removeArrow(this);
    }
    
    public boolean isResizable()
    {
        return false;
    }
    
    @Override
    @OnThread(Tag.Any)
    public Type getType()
    {
        return Type.IMPLEMENTS;
    }

    @Override
    public boolean isRemovable()
    {
        return true;
    }
}
