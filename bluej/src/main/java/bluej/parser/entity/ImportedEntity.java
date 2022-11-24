/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011,2017  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.entity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;

/**
 * Represents an unresolved sequence of names from an import statement, which should
 * when resolved refer to a class or (for wildcard imports) a package.
 * 
 * @author Davin McCall
 */
public class ImportedEntity extends JavaEntity
{
    private EntityResolver resolver;
    private List<String> names;
    private Reflective querySource;
    
    /**
     * Create an ImportEntity with the given attributes.
     */
    public ImportedEntity(EntityResolver resolver, List<String> names,
            Reflective querySource)
    {
        this.resolver = resolver;
        this.names = names;
        this.querySource = querySource;
    }
    
    @Override
    public String getName()
    {
        return names.stream().collect(Collectors.joining("."));
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        List<String> newNames = new LinkedList<String>();
        newNames.addAll(names);
        newNames.add(name);
        return new ImportedEntity(resolver, newNames, querySource);
    }

    @Override
    public JavaType getType()
    {
        return null;
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }

    @Override
    public ValueEntity resolveAsValue()
    {
        return null;
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        PackageOrClass poc = resolveAsPackageOrClass();
        if (poc != null) {
            return poc.resolveAsType();
        }
        
        return null;
    }
    
    @Override
    public PackageOrClass resolveAsPackageOrClass()
    {
        Iterator<String> i = names.iterator();
        if (! i.hasNext()) {
            return null;
        }
        
        String fqName = i.next();
        PackageOrClass poc = new PackageEntity(fqName, resolver);
        
        while (i.hasNext()) {
            poc = poc.getPackageOrClassMember(i.next());
            if (poc == null) {
                return null;
            }
        }
        
        return poc;
    }
}
