/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.event;

/**
 * This interface allows an extension to listen for compile events.
 * The order of occurrence of these method calls for a given compilation is:
 * <pre>
 *     compileStarted()
 *     compileError()                        # If a compilation error occurs
 *     compileWarning()                      # If a compilation warning occurs
 *     compileFailed() or compileSucceeded()
 * </pre>
 */
public interface CompileListener
{
    /**
     * This method will be called when a compilation starts.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link CompileEvent} object of type {@link bluej.extensions2.event.CompileEvent.EventType#COMPILE_START_EVENT}.
     */
    public void compileStarted(CompileEvent event);

    /**
     * This method will be called when there is a report of a compile error.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link CompileEvent} object of type {@link bluej.extensions2.event.CompileEvent.EventType#COMPILE_ERROR_EVENT}.
     */
    public void compileError(CompileEvent event);

    /**
     * This method will be called when there is a report of a compile warning.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link CompileEvent} object of type {@link bluej.extensions2.event.CompileEvent.EventType#COMPILE_WARNING_EVENT}.
     */
    public void compileWarning(CompileEvent event);

    /**
     * This method will be called when the compile ends successfully.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link CompileEvent} object of type {@link bluej.extensions2.event.CompileEvent.EventType#COMPILE_DONE_EVENT}.
     */
    public void compileSucceeded(CompileEvent event);


    /**
     * This method will be called when the compile fails.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link CompileEvent} object of type {@link bluej.extensions2.event.CompileEvent.EventType#COMPILE_FAILED_EVENT}.
     */
    public void compileFailed(CompileEvent event);


}
