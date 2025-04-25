package bluej.utility;

import java.io.File;
import java.net.URL;

public class ResourceFileReader {
    public static File getResourceFile(Class<?> clazz, String name)
    {
        URL url = clazz.getResource(name);
        return url != null && !url.getFile().isEmpty() ? new File(url.getFile()) : null;
    }
}
