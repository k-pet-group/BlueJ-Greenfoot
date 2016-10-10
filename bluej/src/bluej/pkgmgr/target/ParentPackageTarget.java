/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.target;

import java.util.Properties;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.prefmgr.PrefMgr;
import bluej.utility.JavaNames;
import javafx.scene.input.MouseEvent;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 */
public class ParentPackageTarget extends PackageTarget
{
    final static String openStr = Config.getString("pkgmgr.parentpackagetarget.open");
    final static String openUnamedStr = Config.getString("pkgmgr.parentpackagetarget.openunamed");

    public ParentPackageTarget(Package pkg)
    {
        super(pkg, "<go up>");
    }

    public void load(Properties props, String prefix)
    {
    }

    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
    }

    /**
     * Deletes applicable files (directory and ALL contentes) prior to
     * this PackageTarget being removed from a Package. For safety (it
     * should never be called on this target) we override this to do
     * nothing
     */
    public void deleteFiles()
    {
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For package targets, this has not yet been implemented.
     *
     * @arg directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
        return true;
    }

    @OnThread(Tag.FX)
    public boolean isResizable()
    {
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isMoveable()
    {
        return false;
    }

    @OnThread(Tag.Any)
    public boolean isSaveable()
    {
        return false;
    }

    @Override
    @OnThread(Tag.Any)
    protected String getOpenPkgName()
    {
        return JavaNames.getPrefix(getPackage().getQualifiedName());
    }

    public void remove(){
            // The user is not permitted to remove a paretnPackage
    }

    @Override
    @OnThread(Tag.Any)
    protected boolean isRemovable()
    {
        return false;
    }
}
