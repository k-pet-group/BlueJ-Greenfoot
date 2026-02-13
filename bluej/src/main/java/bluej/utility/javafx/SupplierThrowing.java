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
 * A thread-agnostic variant of {@link java.util.function.Supplier} whose
 * {@link #get()} method is permitted to throw checked exceptions.
 *
 * <p>This interface is used as the non-thread-annotated base for
 * {@link FXPlatformSupplierThrowing} and {@link FXSupplierThrowing},
 * enabling exception-propagating lambdas to be passed to
 * {@link JavaFXUtil#runPlatform} and {@link JavaFXUtil#runPlatformFuture}
 * without requiring callers to wrap checked exceptions manually.</p>
 *
 * @param <T> the type of results supplied by this supplier
 * @see FXPlatformSupplierThrowing
 * @see FXSupplierThrowing
 */
@FunctionalInterface
public interface SupplierThrowing<T>
{
    /**
     * Gets a result.
     *
     * @return a result
     * @throws Exception if the operation fails
     */
    @OnThread(Tag.Any)
    T get() throws Exception;
}
