/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;

/**
 * Resolve project entities.
 * 
 * @author Davin McCall
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class ProjectEntityResolver implements EntityResolver
{
    private Project project;
    
    /**
     * Construct a ProjectEntityResolver for the given project.
     */
    public ProjectEntityResolver(Project project)
    {
        this.project = project;
    }
    
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        return resolvePackageOrClass(name, querySource);
    }

    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        // Try in java.lang
        Class<?> cl = project.loadClass("java.lang." + name);
        if (cl != null) {
            return new TypeEntity(cl);
        }
        
        // Have to assume it's a package
        return new PackageEntity(name, this);
    }

    public TypeEntity resolveQualifiedClass(String name)
    {
        int lastDot = name.lastIndexOf('.');
        String pkgName = lastDot != -1 ? name.substring(0, lastDot) : "";
        String baseName = name.substring(lastDot + 1);
        Package pkg = project.getPackage(pkgName);
        if (pkg != null) {
            Target target = pkg.getTarget(baseName);
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                Reflective ref = ct.getTypeReflective();
                if (ref != null) {
                    return new TypeEntity(ref);
                }
            }
        }

        // Try as a class which might be external to the project 
        Class<?> cl = project.loadClass(name);
        if (cl != null) {
            return new TypeEntity(cl);
        }
        
        return null;
    }

}
