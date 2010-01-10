/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;

public class TaggingResolver implements EntityResolver
{
    private Project project;
    private Package pkg;
    private EntityResolver projectResolver;
    
    public TaggingResolver(Package pkg)
    {
        this.project = pkg.getProject();
        this.projectResolver = project.getEntityResolver();
        this.pkg = pkg;
    }
    
    @Override
    public JavaEntity getValueEntity(String name, String querySource)
    {
        return resolvePackageOrClass(name, querySource);
    }

    @Override
    public PackageOrClass resolvePackageOrClass(String name, String querySource)
    {
        Target target = pkg.getTarget(name);
        if (target instanceof ClassTarget) {
            // DAV - don't forget to tag it
        }
        
        // DAV todo check java.lang.*
        
        return new PackageEntity(name, this);
    }

    @Override
    public TypeEntity resolveQualifiedClass(String name)
    {
        int lastDot = name.lastIndexOf('.');
        String pkgName = "";
        String elName = name;
        if (lastDot != -1) {
            pkgName = name.substring(0, lastDot);
            elName = name.substring(lastDot + 1);
        }
        
        Package rpkg = project.getPackage(pkgName);
        if (rpkg != null) {
            Target target = pkg.getTarget(elName);
            if (target instanceof ClassTarget) {
                // DAV tag it
                // return a dummy type
            }
        }
        
        return null;
    }

}
