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
package bluej.groupwork;

/**
 * A helper class to represent an object which either a Repository
 * or a TeamworkCommandError.
 *
 * @author Amjad Altadmri
 */
public class RepositoryOrError
{
    private Repository repository; // if null, then it is an error
    private TeamworkCommandError error;

    public RepositoryOrError(Repository repository)
    {
        this.repository =  repository;
    }

    public RepositoryOrError(TeamworkCommandError error)
    {
        this.error = error;
    }

    public Repository getRepository()
    {
        // Could be null
        return repository;
    }

    public TeamworkCommandError getError()
    {
        return error;
    }
}
