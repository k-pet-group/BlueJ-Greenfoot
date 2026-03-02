/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 

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
 * Equivalent to {@link java.util.function.Consumer}, annotated with
 * {@code @OnThread(Tag.FX)} for the broader FX thread context.
 *
 * <p>Extends {@link FXPlatformConsumer} because {@code Tag.FXPlatform} is a
 * <em>subset</em> of {@code Tag.FX}: any code running on the FX platform
 * thread is also running on an FX thread.  This means an {@code FXConsumer}
 * can safely be passed where {@code FXPlatformConsumer} is accepted — the
 * call site guarantees FXPlatform, which satisfies the weaker FX
 * requirement.</p>
 *
 * @param <T> the type of the input to the operation
 * @see FXPlatformConsumer
 * @see FXConsumerThrowing
 */
@FunctionalInterface
public interface FXConsumer<T> extends FXPlatformConsumer<T>
{
    /**
     * Performs this operation on the given argument on the FX thread.
     *
     * @param t the input argument
     */
    @Override
    @OnThread(Tag.FX)
    void accept(T t);
}
