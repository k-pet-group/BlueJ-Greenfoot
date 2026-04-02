/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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

import java.util.Collections;
import java.util.List;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.target.actions.ClassTargetOperation;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A role for Kotlin files containing only top-level functions. The diagram
 * displays a "functions" stereotype label.
 *
 * @author BlueJ Team
 */
public class KotlinFileRole extends ClassRole
{
    public static final String KOTLIN_FILE_ROLE_NAME = "KotlinFileTarget";

    /**
     * Create the Kotlin file role.
     */
    public KotlinFileRole()
    {
    }

    @Override
    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return KOTLIN_FILE_ROLE_NAME;
    }

    @Override
    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "functions";
    }

    /**
     * Kotlin facade classes have a private constructor -- hide it from students.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getClassConstructorOperations(
            ClassTarget ct, Class<?> cl)
    {
        return Collections.emptyList();
    }

    /**
     * No "Create Test Class" or "Convert to Stride" for function files.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getRoleOperationsEnd(ClassTarget ct, State state)
    {
        return Collections.emptyList();
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return false;
    }
}
