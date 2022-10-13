/*
 This file is part of the BlueJ program.
 Copyright (C) 2017  Michael Kolling and John Rosenberg

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

import bluej.Config;
import bluej.pkgmgr.Project;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An enum indicating which thread methods should be invoked on.
 */
public enum RunOnThread
{
    DEFAULT, FX, SWING;

    // Easiest to do this in toString, even if it looks weird, because JavaFX ComboBox uses
    // toString to display, and it's awkward to make it use an outside method:
    @Override
    @OnThread(Tag.Any)
    public String toString()
    {
        switch (this)
        {
            case FX:
                return Config.getString("prefmgr.misc.run.fx");
            case SWING:
                return Config.getString("prefmgr.misc.run.swing");
            default:
                return Config.getString("prefmgr.misc.run.default");
        }
    }

    // Like valueOf but returns DEFAULT if item not found
    public static RunOnThread load(String name)
    {
        try
        {
            return valueOf(name);
        }
        catch (Exception e)
        {
            return DEFAULT;
        }
    }
}
