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
 * A variant of {@link FXFunction} whose {@link #apply(Object)} method is
 * permitted to throw checked exceptions.
 *
 * <p>This interface is annotated with {@code @OnThread(Tag.FX)}, indicating
 * that its method is intended for use on the broader FX thread. Unlike
 * {@link FXFunction}, checked exceptions are propagated.</p>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @see FXFunction
 * @see FXPlatformFunctionThrowing
 * @see FunctionThrowing
 */
@FunctionalInterface
public interface FXFunctionThrowing<T, R> extends FXPlatformFunctionThrowing<T, R>
{
    /**
     * Applies this function to the given argument on the FX thread.
     *
     * @param t the function argument
     * @return the function result
     * @throws Exception if the operation fails
     */
    @Override
    @OnThread(Tag.FX)
    R apply(T t) throws Exception;
}
