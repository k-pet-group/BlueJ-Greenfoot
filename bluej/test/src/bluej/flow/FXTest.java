/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.flow;

import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FXTest extends ApplicationTest
{
    // Switches to FX thread, executes, then brings it back:
    protected <T> T fx(FXPlatformSupplier<T> fxFetcher)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        try
        {
            JavaFXUtil.runNowOrLater(() -> {
                future.complete(fxFetcher.get());
            });
            return future.get(5, TimeUnit.SECONDS);
        }
        catch (ExecutionException | TimeoutException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Switches to FX thread and executes, waiting for completion before returning.
    protected void fx_(FXPlatformRunnable fxTask)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try
        {
            JavaFXUtil.runNowOrLater(() -> {
                fxTask.run();
                future.complete(true);
            });
            future.get(5, TimeUnit.SECONDS);
        }
        catch (ExecutionException | TimeoutException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
