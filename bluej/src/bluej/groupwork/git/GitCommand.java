/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.git;

import bluej.groupwork.TeamworkCommand;

/**
 *
 * @author heday
 */
public abstract class GitCommand implements TeamworkCommand
{

    private boolean cancelled = false;
    private final GitRepository repository;

    public GitCommand(GitRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public void cancel()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Check whether this command has been cancelled.
     *
     * @return true if this command has been cancelled.
     */
    public boolean isCancelled()
    {
        return cancelled;
    }

    /**
     * Get a handle to the repository.
     */
    protected GitRepository getRepository()
    {
        return repository;
    }

}
