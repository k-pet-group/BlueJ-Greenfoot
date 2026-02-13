/*
 This file is part of the BlueJ program. 
 Copyright (C) 2026 Michael Kölling and John Rosenberg

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
package bluej.utility.javafx;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A variant of {@link FXRunnable} whose {@link #run()} method is permitted to
 * throw checked exceptions. Extends {@link FXPlatformRunnableThrowing} following
 * the same inheritance pattern as {@link FXRunnable} extends {@link FXPlatformRunnable}.
 *
 * <p>This interface is annotated with {@code @OnThread(Tag.FX)}, indicating
 * that its method is intended for use on the broader FX thread (which includes
 * background FX loading threads, not just the platform thread). Unlike
 * {@link FXRunnable}, checked exceptions are propagated.</p>
 *
 * @see FXRunnable
 * @see FXPlatformRunnableThrowing
 * @see RunnableThrowing
 */
@FunctionalInterface
public interface FXRunnableThrowing extends FXPlatformRunnableThrowing
{
    /**
     * Executes the action on the FX thread.
     *
     * @throws Exception if the action fails
     */
    @Override
    @OnThread(Tag.FX)
    void run() throws Exception;
}
