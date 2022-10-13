/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014,2015,2017,2019  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import java.util.Collection;
import java.util.concurrent.Executor;

import bluej.debugger.gentype.Reflective;
import bluej.parser.ConstructorOrMethodReflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An interface for javadoc resolvers, which retrieve javadoc for a method. 
 * 
 * @author Davin McCall
 */
public interface JavadocResolver
{
    /**
     * Retrieve the javadoc for the specified method, if possible. The javadoc and
     * method parameter names will be added to the supplied MethodReflective(s).
     * The collection of methods must all come from the same declaring type.
     * 
     * @param  declType   the declaring type from where the methods are found.
     * @param  methods    the methods to resolve Javadoc for.
     */
    public void getJavadoc(Reflective declType, Collection<? extends ConstructorOrMethodReflective> methods);

    /**
     * Get the Javadoc for the given type
     * @param moduleName The enclosing module of the type if known and applicable (may be null)
     * @param typeName The fully qualified name of the type
     * @return The Javadoc if found, or null if not.
     */
    public String getJavadoc(String moduleName, String typeName);

    public static interface AsyncCallback
    {
        @OnThread(Tag.FXPlatform)
        void gotJavadoc(ConstructorOrMethodReflective method);
    }
    
    /**
     * Retrieve the javadoc for the specified method, if possible, in the background
     * and notify a callback when the javadoc is available.
     * 
     * @param method    The method to find the javadoc for
     * @param callback  The callback to notify
     * @param executor  The executor to execute background tasks
     * 
     * @return   true if the javadoc is available immediately (callback will not be
     *            notified) or false if a background task was submitted.
     */
    public boolean getJavadocAsync(final ConstructorOrMethodReflective method, final AsyncCallback callback, Executor executor);
    
}
