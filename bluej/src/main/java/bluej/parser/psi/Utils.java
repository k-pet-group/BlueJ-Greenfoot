package bluej.parser.psi;

import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Utils {
    static public <T> T onPlatformThread(@OnThread(value = Tag.FXPlatform, ignoreParent = true) Supplier<T> supplier) {
//        var future = new CompletableFuture<T>();
//
//        Platform.runLater(() -> {
//            var thing = supplier.get();
//
//            future.complete(thing);
//        });
//
//        return future.join();

        return supplier.get();
    }
}
