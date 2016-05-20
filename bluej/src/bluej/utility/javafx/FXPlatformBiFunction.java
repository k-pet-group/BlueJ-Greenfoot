package bluej.utility.javafx;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 21/04/16.
 */
@FunctionalInterface
@OnThread(Tag.FXPlatform)
public interface FXPlatformBiFunction<T, U, R>
{
    public R apply(T t, U u);
}
