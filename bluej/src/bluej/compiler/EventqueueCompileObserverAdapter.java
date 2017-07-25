/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2014,2016  Michael Kolling and John Rosenberg
 
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
package bluej.compiler;

import bluej.utility.javafx.FXPlatformRunnable;
import javafx.application.Platform;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This class adapts CompileObserver messages to run on the JavaFX GUI thread.
 * 
 * @author Davin McCall
 */
final public class EventqueueCompileObserverAdapter implements CompileObserver
{
    private FXCompileObserver link;

    /**
     * Constructor for EventqueueCompileObserver. The link parameter is a compiler
     * observer; all messages will be passed on to it, but on the GUI thread.
     */
    public EventqueueCompileObserverAdapter(FXCompileObserver link)
    {
        this.link = link;
    }
    
    /**
     * This method switches execution to the GUI thread.
     */
    private void runOnEventQueue(FXPlatformRunnable action)
    {
        CompletableFuture<Optional<Throwable>> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try
            {
                action.run();
            }
            catch (Throwable t)
            {
                f.complete(Optional.of(t));
            }
            finally
            {
                if (!f.isDone())
                    f.complete(Optional.empty());
            }
        });
        try
        {
            Optional<Throwable> optThrow = f.get();
            if (optThrow.isPresent())
                throw new RuntimeException(optThrow.get());
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    // ---------------- CompileObserver interface ---------------------

    @Override
    public synchronized void compilerMessage(Diagnostic diagnostic, CompileType type)
    {
        runOnEventQueue(() -> link.compilerMessage(diagnostic, type));
    }

    @Override
    public synchronized void startCompile(CompileInputFile[] csources, CompileReason reason, CompileType type, int compilationSequence)
    {
        runOnEventQueue(() -> link.startCompile(csources, reason, type, compilationSequence));
    }

    @Override
    public synchronized void endCompile(CompileInputFile[] sources, boolean successful, CompileType type, int compilationSequence)
    {
        runOnEventQueue(() -> link.endCompile(sources, successful, type, compilationSequence));
    }
}
