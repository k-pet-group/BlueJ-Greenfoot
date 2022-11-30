/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.Config;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.testfx.framework.junit.ApplicationTest;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FXTest extends ApplicationTest
{
    private Stage originalWindow;
    
    @Rule
    public TestWatcher screenshotOnFail = new TestWatcher()
    {
        @Override
        protected void failed(Throwable e, Description description)
        {
            super.failed(e, description);
            e.printStackTrace();
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            System.err.println("Screenshot of failure:");
            dumpScreenshot();
        }
    };

    @OnThread(Tag.Any)
    protected final void dumpScreenshot()
    {
        fx_(() -> {
            try
            {
                // For some reason Mac doesn't support a full-screen shot, so we capture window instead:
                Window window = targetWindow();
                if (window == null)
                    window = originalWindow;
                Scene scene = window == null ? null : window.getScene();
                if (Config.isMacOS() && scene != null && scene.getRoot() != null)
                {
                    System.err.println("Scene offset in screen: " + scene.getRoot().localToScreen(scene.getRoot().getBoundsInLocal()));
                    System.err.println(asBase64(capture(scene.getRoot()).getImage()));
                }
                else
                    System.err.println(asBase64(capture(Screen.getPrimary().getBounds()).getImage()));
            }
            catch (Exception e)
            {
                // This seems to happen on some systems when trying to capture screen
                e.printStackTrace();
            }
        });
    }

    protected static String asBase64(Image image)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", baos);
        }
        catch (IOException e)
        {
            System.err.println("Cannot write screenshot: " + e.getLocalizedMessage());
            return "";
        }
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "<img src=\"data:image/png;base64, " + base64Image + "\">";
    }
    
    // Switches to FX thread, executes, then brings it back:
    protected <T> T fx(FXPlatformSupplier<T> fxFetcher)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        try
        {
            JavaFXUtil.runNowOrLater(() -> {
                future.complete(fxFetcher.get());
            });
            return future.get(10, TimeUnit.SECONDS);
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

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        this.originalWindow = stage;
    }

    // We remove awkward unprintable characters that mess up the location tracking for click positions.
    // To see this again, pass seed=1L to testEditor.
    protected String removeInvalid(String rawContent)
    {
        int[] valid = rawContent.codePoints().filter(n -> {
            if (n >= 32 && n != 127 && n <= 0xFFFF)
                return true;
            else if (n == '\n')
                return true;
            else
                return false;
                
        }).toArray();
        return new String(valid, 0, valid.length);
    }
}
