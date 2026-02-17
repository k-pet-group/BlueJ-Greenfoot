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
 * Equivalent to {@link java.util.function.BiFunction}, annotated with
 * {@code @OnThread(Tag.FX)} for the broader FX thread context.
 *
 * <p>Extends {@link FXPlatformBiFunction} because {@code Tag.FXPlatform} is a
 * <em>subset</em> of {@code Tag.FX}: any code running on the FX platform
 * thread is also running on an FX thread.  This means an {@code FXBiFunction}
 * can safely be passed where {@code FXPlatformBiFunction} is accepted — the
 * call site guarantees FXPlatform, which satisfies the weaker FX
 * requirement.</p>
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 * @see FXPlatformBiFunction
 * @see FXBiFunctionThrowing
 */
@FunctionalInterface
public interface FXBiFunction<T, U, R> extends FXPlatformBiFunction<T, U, R>
{
    /**
     * Applies this function to the given arguments on the FX thread.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     */
    @Override
    @OnThread(Tag.FX)
    R apply(T t, U u);
}
