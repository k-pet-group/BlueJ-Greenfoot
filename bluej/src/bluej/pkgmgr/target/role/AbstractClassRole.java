/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target.role;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.actions.ClassTargetOperation;
import javafx.collections.ObservableList;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * A role object to represent the behaviour of abstract classes.
 *
 * @author  Andrew Patterson 
 */
public class AbstractClassRole extends ClassRole
{
    public final static String ABSTRACT_ROLE_NAME = "AbstractTarget";
    
    /**
     * Create the abstract class role.
     */
    public AbstractClassRole()
    {
    }

    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return ABSTRACT_ROLE_NAME;
    }

    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "abstract";
    }

    /**
     * Creates a class menu containing any constructors.
     *
     * <p>Because we are an abstract class we cannot have any constructors
     * so we override this method to do nothing.
     *
     * @param cl Class object associated with this class target
     */
    @Override
    public @OnThread(Tag.FXPlatform) List<ClassTargetOperation> getClassConstructorOperations(ClassTarget ct, Class<?> cl)
    {
        return List.of();
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return true;
    }
}
