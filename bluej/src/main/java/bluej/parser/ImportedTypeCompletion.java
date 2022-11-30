/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2017,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.parser;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import bluej.pkgmgr.JavadocResolver;
import bluej.pkgmgr.target.role.Kind;
import threadchecker.OnThread;
import threadchecker.Tag;

public class ImportedTypeCompletion extends AssistContent
{
    private final String type;
    private final String canonicalName;
    private final String packageName;
    private final List<String> enclosingClasses;
    private final int modifiers;
    private final List<String> superTypes;
    private final Kind typeKind;
    private final String moduleName;
    private boolean extractedJavadoc = false;
    private String javadoc = null; // Can be null, even after extraction
    private final JavadocResolver resolver;
    
    public ImportedTypeCompletion(Class<?> cls, JavadocResolver resolver)
    {
        // It's important that we take what we need from cls and don't keep a reference,
        // to allow the Class instance to be garbage collected after imports are scanned. 
        this.type = cls.getSimpleName();
        this.canonicalName = cls.getCanonicalName();
        this.packageName = cls.getPackage().getName();
        this.modifiers = cls.getModifiers();
        this.moduleName = cls.getModule() != null ? cls.getModule().getName() : null;

        this.enclosingClasses = new ArrayList<>();
        for (Class<?> i  = cls.getEnclosingClass(); i != null; i = i.getEnclosingClass())
            enclosingClasses.add(0, i.getSimpleName());

        this.superTypes = new ArrayList<>();
        for (Class<?> i : cls.getInterfaces())
            superTypes.add(i.getName());
        for (Class<?> c = cls.getSuperclass(); c != null; c = c.getSuperclass())
        {
            superTypes.add(c.getName());
        }
        this.resolver = resolver;
        if (cls.isInterface())
        {
            this.typeKind = Kind.INTERFACE;
        }
        else if (cls.isEnum())
        {
            this.typeKind = Kind.ENUM;
        }
        else if (cls.isPrimitive())
        {
            // Shouldn't happen with an import anyway:
            this.typeKind = Kind.PRIMITIVE;
        }
        else
        {
            this.typeKind = ((cls.getModifiers() & Modifier.FINAL) != 0) ?  Kind.CLASS_FINAL : Kind.CLASS_NON_FINAL;
        }
        //Debug.message("Type " + canonicalName + " is " + typeKind);
    }

    @Override
    @OnThread(Tag.Any)
    public String getName()
    {
        return type;
    }

    @Override
    public List<ParamInfo> getParams()
    {
        return null;
    }

    @Override
    public String getType()
    {
        return null;
    }

    @Override
    public String getDeclaringClass()
    {
        if (enclosingClasses.isEmpty())
            return null;
        else
            return enclosingClasses.stream().collect(Collectors.joining("."));
    }

    @Override
    public CompletionKind getKind()
    {
        return CompletionKind.TYPE;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public String getJavadoc()
    {
        if (!extractedJavadoc)
        {
            javadoc = resolver.getJavadoc(moduleName, canonicalName);
        }
        return javadoc;
    }
    
    @Override
    public Access getAccessPermission()
    {
        return fromModifiers(modifiers);
    }

    @Override
    public String getPackage()
    {
        return packageName;
    }
    
    @Override
    public List<String> getSuperTypes()
    {
        return superTypes;
    }

    @Override
    public Kind getTypeKind()
    {
        return typeKind;
    }
 
    
}
