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
 * A variant of {@link FXPlatformRunnable} whose {@link #run()} method is
 * permitted to throw checked exceptions.
 *
 * <p>This interface is annotated with {@code @OnThread(Tag.FXPlatform)},
 * indicating that its {@link #run()} method must be called on the JavaFX
 * application thread (the "platform" thread). Unlike {@link FXPlatformRunnable},
 * checked exceptions are propagated rather than requiring the implementor to
 * handle them internally.</p>
 *
 * <p>Primary use case: passing exception-throwing lambdas to
 * {@link JavaFXUtil#runPlatform(FXPlatformRunnableThrowing)} and
 * {@link JavaFXUtil#runPlatformFuture(FXPlatformRunnableThrowing)}, which
 * schedule execution on the FX thread and propagate exceptions through
 * {@link java.util.concurrent.Future} or {@link RuntimeException}.</p>
 *
 * @see FXPlatformRunnable
 * @see FXRunnableThrowing
 * @see RunnableThrowing
 * @see JavaFXUtil#runPlatform(FXPlatformRunnableThrowing)
 * @see JavaFXUtil#runPlatformFuture(FXPlatformRunnableThrowing)
 */
@FunctionalInterface
@OnThread(Tag.FXPlatform)
public interface FXPlatformRunnableThrowing
{
    /**
     * Executes the action on the JavaFX platform thread.
     *
     * @throws Exception if the action fails
     */
    @OnThread(Tag.FXPlatform)
    void run() throws Exception;
}
