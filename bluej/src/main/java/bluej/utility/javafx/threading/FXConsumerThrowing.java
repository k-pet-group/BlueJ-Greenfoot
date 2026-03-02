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
 * A variant of {@link FXConsumer} whose {@link #accept(Object)} method is
 * permitted to throw checked exceptions.
 *
 * <p>This interface is annotated with {@code @OnThread(Tag.FX)}, indicating
 * that its method is intended for use on the broader FX thread. Unlike
 * {@link FXConsumer}, checked exceptions are propagated.</p>
 *
 * @param <T> the type of the input to the operation
 * @see FXConsumer
 * @see FXPlatformConsumerThrowing
 * @see ConsumerThrowing
 */
@FunctionalInterface
public interface FXConsumerThrowing<T> extends FXPlatformConsumerThrowing<T>
{
    /**
     * Performs this operation on the given argument on the FX thread.
     *
     * @param t the input argument
     * @throws Exception if the operation fails
     */
    @Override
    @OnThread(Tag.FX)
    void accept(T t) throws Exception;
}
