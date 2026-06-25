/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinFileRole — the ClassRole subclass for Kotlin files
 * containing only top-level functions.
 */
public class KotlinFileRoleTest
{
    private final KotlinFileRole role = new KotlinFileRole();

    @Test
    public void testRoleName()
    {
        assertEquals("KotlinFileTarget", role.getRoleName());
    }

    @Test
    public void testRoleNameConstant()
    {
        assertEquals("KotlinFileTarget", KotlinFileRole.KOTLIN_FILE_ROLE_NAME);
    }

    @Test
    public void testStereotypeLabel()
    {
        assertEquals("functions", role.getStereotypeLabel());
    }

    @Test
    public void testConstructorOperationsEmpty()
    {
        // Facade classes have private constructors — students should never see them
        assertTrue("Constructor operations should be empty",
            role.getClassConstructorOperations(null, null).isEmpty());
    }

    @Test
    public void testRoleOperationsEndEmpty()
    {
        // No "Create Test Class" or "Convert to Stride" for function files
        assertTrue("Role operations should be empty",
            role.getRoleOperationsEnd(null, null).isEmpty());
    }

    @Test
    public void testCannotConvertToStride()
    {
        assertFalse("Should not be convertible to Stride",
            role.canConvertToStride());
    }

    @Test
    public void testIsClassRole()
    {
        assertTrue("KotlinFileRole should be a ClassRole",
            role instanceof ClassRole);
    }
}
