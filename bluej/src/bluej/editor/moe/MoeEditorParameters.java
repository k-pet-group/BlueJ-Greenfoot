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
package bluej.editor.moe;

import java.util.Properties;

import bluej.editor.EditorWatcher;
import bluej.parser.entity.EntityResolver;
import bluej.pkgmgr.JavadocResolver;

/**
 * Parameters for the Moe editor.
 * 
 * @author Davin McCall
 */
public class MoeEditorParameters
{
    private String title;
    private boolean isCode;
    private EditorWatcher watcher;
    private boolean showToolbar;
    private boolean showLineNum;
    private Properties resources;
    private EntityResolver projectResolver;
    private JavadocResolver javadocResolver;

    public MoeEditorParameters(String title, EditorWatcher watcher,
            Properties resources, EntityResolver projectResolver,
            JavadocResolver javadocResolver)
    {
        this.title = title;
        this.watcher = watcher;
        this.resources = resources;
        this.projectResolver = projectResolver;
        this.javadocResolver = javadocResolver;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public boolean isCode()
    {
        return isCode;
    }

    public void setCode(boolean isCode)
    {
        this.isCode = isCode;
    }

    public EditorWatcher getWatcher()
    {
        return watcher;
    }

    public void setWatcher(EditorWatcher watcher)
    {
        this.watcher = watcher;
    }

    public boolean isShowToolbar()
    {
        return showToolbar;
    }

    public void setShowToolbar(boolean showToolbar)
    {
        this.showToolbar = showToolbar;
    }

    public boolean isShowLineNum()
    {
        return showLineNum;
    }

    public void setShowLineNum(boolean showLineNum)
    {
        this.showLineNum = showLineNum;
    }

    public Properties getResources()
    {
        return resources;
    }

    public void setResources(Properties resources)
    {
        this.resources = resources;
    }

    public EntityResolver getProjectResolver()
    {
        return projectResolver;
    }

    public void setProjectResolver(EntityResolver projectResolver)
    {
        this.projectResolver = projectResolver;
    }
    
    public JavadocResolver getJavadocResolver()
    {
        return javadocResolver;
    }
    
    public void setJavadocResolver(JavadocResolver javadocResolver)
    {
        this.javadocResolver = javadocResolver;
    }
}
