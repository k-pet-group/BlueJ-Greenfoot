/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.util.GreenfootUtil;

import java.io.IOException;
import java.rmi.RemoteException;

import bluej.Config;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;

/**
 * Specialisation of a GClass that represent one of the two core Greenfoot
 * classes: Actor or World. Overrides most of the public methods of GClass to do
 * nothing and return null, since they are not really valid for these two
 * classes.
 * 
 * @author Poul Henriksen
 */
public class GCoreClass extends GClass
{
    private Class<?> cls;
    private GPackage pkg;

    public GCoreClass(Class<?> cls, GProject project)
    {
        super();
        this.cls = cls;
        this.pkg = new GPackage(project);
    }

    @Override
    public void compile(boolean waitCompileEnd, boolean forceQuiet) throws ProjectNotOpenException,
    PackageNotFoundException, RemoteException,
    CompilationNotStartedException
    {
        return;
    }

    @Override
    public void edit()
    {
        String page = isWorldClass() ? "greenfoot/World.html" : "greenfoot/Actor.html";
        try {
            GreenfootUtil.showApiDoc(page);
        }
        catch (IOException e) {
            String errMsg = Config.getString("greenfoot.cannotFindAPIDocumentation");
            DialogManager.showErrorText(GreenfootMain.getInstance().getFrame(), errMsg);
        }    
    }

    @Override
    public String getClassProperty(String propertyName)
    {
        return "";
    }

    @Override
    public Class<?> getJavaClass()
    {
        return cls;
    }

    @Override
    public String getName()
    {
        return cls.getSimpleName();
    }

    @Override
    public GPackage getPackage()
    {
        return pkg;
    }

    @Override
    public String getQualifiedName()
    {
        return cls.getName();
    }

    @Override
    public GClass getSuperclass()
    {
        return null;
    }

    @Override
    public boolean isActorClass()
    {
        return cls.equals(Actor.class);
    }

    @Override
    public boolean isActorSubclass()
    {
        return false;
    }

    @Override
    public boolean isCompiled()
    {
        return true;
    }

    @Override
    public boolean isSubclassOf(String className)
    {
        return false;
    }

    @Override
    public boolean isWorldClass()
    {
        return cls.equals(World.class);
    }

    @Override
    public boolean isWorldSubclass()
    {
        return false;
    }

    @Override
    public void nameChanged(String oldName)
    {
        return;
    }

    @Override
    public void reload()
    {
        return;
    }

    @Override
    public void remove()
    {
        return;
    }

    @Override
    public void setClassProperty(String propertyName, String value)
    {
        return;
    }

    @Override
    public boolean setSuperclassGuess(String superclassName)
    {
        return true;
    }
}
