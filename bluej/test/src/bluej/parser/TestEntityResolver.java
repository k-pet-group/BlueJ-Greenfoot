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
package bluej.parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bluej.parser.entity.ClassEntity;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.nodes.ParsedCUNode;

/**
 * An entity resolver for testing purposes.
 * 
 * @author Davin McCall
 */
public class TestEntityResolver implements EntityResolver
{
    private EntityResolver parent;
    
    /** A map from package name to a list of compilation units in that package */
    private Map<String,List<ParsedCUNode>> pkgMap = new HashMap<String,List<ParsedCUNode>>();
    
    public TestEntityResolver(EntityResolver parent)
    {
        this.parent = parent;
    }
    
    /**
     * Add a compilation unit for the given package.
     */
    public void addCompilationUnit(String pkg, ParsedCUNode cunit)
    {
        List<ParsedCUNode> clist = pkgMap.get(pkg);
        if (clist == null) {
            clist = new LinkedList<ParsedCUNode>();
        }
        clist.add(cunit);
        pkgMap.put(pkg, clist);
    }
    
    private String getPackageFromClassName(String className)
    {
        int lastdot = className.lastIndexOf('.');
        if (lastdot == -1) {
            return "";
        }
        return className.substring(0, lastdot);
    }
    
    
    public PackageOrClass resolvePackageOrClass(String name, String querySource)
    {
        String pkg = getPackageFromClassName(querySource);
        List<ParsedCUNode> culist = pkgMap.get(pkg);
        if (culist != null) {
            for (ParsedCUNode node : culist) {
                PackageOrClass poc = node.resolvePackageOrClass(name, querySource);
                if (poc != null) {
                    return poc;
                }
            }
        }
        return parent.resolvePackageOrClass(name, querySource);
    }
    
    public ClassEntity resolveQualifiedClass(String name)
    {
        String pkg = getPackageFromClassName(name);
        List<ParsedCUNode> culist = pkgMap.get(pkg);
        if (culist != null) {
            for (ParsedCUNode node : culist) {
                ClassEntity clent = node.resolveQualifiedClass(name);
                if (clent != null) {
                    return clent;
                }
            }
        }
        return parent.resolveQualifiedClass(name);
    }
    
    public JavaEntity getValueEntity(String name, String querySource)
    {
        return resolvePackageOrClass(name, querySource);
    }
}
