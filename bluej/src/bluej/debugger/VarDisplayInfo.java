/*
 This file is part of the BlueJ program.
 Copyright (C) 2017,2018,2020  Michael Kolling and John Rosenberg

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
package bluej.debugger;

import bluej.debugger.gentype.JavaType;
import bluej.utility.javafx.FXPlatformSupplier;
import com.sun.jdi.LocalVariable;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * Created by neil on 18/05/2017.
 */
public class VarDisplayInfo
{
    private final String access; // May be null (for local vars)
    private final String type;
    private final String name;
    private final String value;
    // If null, means item was not an inspectable object (probably null or primitive):
    @OnThread(Tag.FXPlatform)
    private final FXPlatformSupplier<DebuggerObject> getObjectToInspect;

    @OnThread(Tag.FXPlatform)
    public VarDisplayInfo(DebuggerField field)
    {
        int mods = field.getModifiers();
        String access = "";
        if (Modifier.isPrivate(mods)) {
            access = "private";
        }
        else if (Modifier.isPublic(mods)) {
            access = "public";
        }
        else if (Modifier.isProtected(mods)) {
            access = "protected";
        }

        if (field.isHidden()) {
            access += "(hidden)";
        }

        this.access = access;

        type = field.getType().toString(true);
        name = field.getName();
        value = field.getValueString();
        if (field.isReferenceType() && ! field.isNull())
        {
            getObjectToInspect = () -> field.getValueObject(null);
        }
        else
        {
            getObjectToInspect = null;
        }
    }

    @OnThread(Tag.FXPlatform)
    public VarDisplayInfo(JavaType vartype, LocalVariable var, String value, FXPlatformSupplier<DebuggerObject> getObjectToInspect)
    {
        access = null;
        type = vartype.toString(true);
        name = var.name();
        this.value = value;
        this.getObjectToInspect = getObjectToInspect;
    }

    public String getAccess()
    {
        return access;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    @OnThread(Tag.FXPlatform)
    public FXPlatformSupplier<DebuggerObject> getFetchObject()
    {
        return getObjectToInspect;
    }
}
