package bluej.utility;

import java.util.*;
import java.io.*;

/**
 * A properties object which allows the default properties
 * to be fetched independantly of the normal calls
 *
 * @author  Andrew Patterson
 * @version $Id: DefaultProperties.java 305 1999-12-09 23:50:57Z ajp $
 */
public class DefaultProperties extends Properties
{
    public DefaultProperties()
    {
    }

    public DefaultProperties(Properties defaults)
    {
        super(defaults);
    }

    public String getDefaultProperty(String key)
    {
        return defaults.getProperty(key);
    }

    public String getDefaultProperty(String key, String defaultValue)
    {
        return defaults.getProperty(key, defaultValue);
    }
}
