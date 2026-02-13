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
 * A variant of {@link FXPlatformConsumer} whose {@link #accept(Object)} method
 * is permitted to throw checked exceptions.
 *
 * <p>This interface is annotated with {@code @OnThread(Tag.FXPlatform)},
 * indicating that its method must be called on the JavaFX application thread.
 * Unlike {@link FXPlatformConsumer}, checked exceptions are propagated rather
 * than requiring the implementor to handle them internally.</p>
 *
 * <p>Primary use case: passing exception-throwing lambdas to
 * {@link JavaFXUtil#runPlatform(FXPlatformConsumerThrowing, Object)} and
 * {@link JavaFXUtil#runPlatformFuture(FXPlatformConsumerThrowing, Object)},
 * which schedule execution on the FX thread with a single argument.</p>
 *
 * @param <T> the type of the input to the operation
 * @see FXPlatformConsumer
 * @see FXConsumerThrowing
 * @see ConsumerThrowing
 */
@FunctionalInterface
@OnThread(Tag.FXPlatform)
public interface FXPlatformConsumerThrowing<T>
{
    /**
     * Performs this operation on the given argument on the JavaFX platform thread.
     *
     * @param t the input argument
     * @throws Exception if the operation fails
     */
    @OnThread(Tag.FXPlatform)
    void accept(T t) throws Exception;
}
