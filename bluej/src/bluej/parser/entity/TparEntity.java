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
package bluej.parser.entity;

import java.util.List;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A JavaEntity subclass for representing type parameters. In particular this avoids
 * the problems associated with recursive definitions such as:
 * 
 * <blockquote>
 *     <code>class Sort<T extends Comparable<T>> {  }</code></dt>
 * </blockquote>
 * 
 * @author Davin McCall
 */
public class TparEntity extends JavaEntity
{
    private JavaEntity bounds;
    private GenTypeDeclTpar tpar;
    private String name;
    
    public TparEntity(String name, JavaEntity bounds)
    {
        super();
        this.name = name;
        this.tpar = new GenTypeDeclTpar(name);
        this.bounds = bounds;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeDeclTpar getType()
    {
        if (bounds != null) {
            JavaEntity boundsCopy = bounds;
            bounds = null; // prevent infinite recursion
            
            TypeEntity tent = boundsCopy.resolveAsType();
            if (tent != null) {
                GenTypeSolid boundType = tent.getType().asSolid();
                if (boundType != null) {
                    tpar.setBounds(boundType.getUpperBounds());
                }
                else {
                    tpar = null;
                }
            }
            else {
                tpar = null;
            }
        }
        return tpar;
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        JavaType myType = getType();
        if (myType != null) {
            return new TypeEntity(myType);
        }
        else {
            return null;
        }
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }
    
    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        return null;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
}
