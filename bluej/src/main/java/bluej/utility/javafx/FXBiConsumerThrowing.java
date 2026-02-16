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
 * A variant of {@link FXBiConsumer} whose {@link #accept(Object, Object)}
 * method is permitted to throw checked exceptions.
 *
 * <p>This interface is annotated with {@code @OnThread(Tag.FX)}, indicating
 * that its method is intended for use on the broader FX thread. Unlike
 * {@link FXBiConsumer}, checked exceptions are propagated.</p>
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @see FXBiConsumer
 * @see FXPlatformBiConsumerThrowing
 * @see BiConsumerThrowing
 */
@FunctionalInterface
public interface FXBiConsumerThrowing<T, U>
{
    /**
     * Performs this operation on the given arguments on the FX thread.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @throws Exception if the operation fails
     */
    @OnThread(Tag.FX)
    void accept(T t, U u) throws Exception;
}
