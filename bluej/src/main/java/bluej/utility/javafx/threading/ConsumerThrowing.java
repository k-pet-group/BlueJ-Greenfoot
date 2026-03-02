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
package bluej.utility.javafx.threading;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A thread-agnostic variant of {@link java.util.function.Consumer} whose
 * {@link #accept(Object)} method is permitted to throw checked exceptions.
 *
 * <p>This interface is used as the non-thread-annotated base for
 * {@link FXPlatformConsumerThrowing} and {@link FXConsumerThrowing},
 * enabling exception-propagating lambdas to be passed to
 * {@link JavaFXThreadingUtil#runPlatformAndWait} and {@link JavaFXThreadingUtil#runPlatform}
 * without requiring callers to wrap checked exceptions manually.</p>
 *
 * @param <T> the type of the input to the operation
 * @see FXPlatformConsumerThrowing
 * @see FXConsumerThrowing
 */
@FunctionalInterface
public interface ConsumerThrowing<T>
{
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception if the operation fails
     */
    @OnThread(Tag.Any)
    void accept(T t) throws Exception;
}
