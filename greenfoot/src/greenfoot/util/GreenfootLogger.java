package greenfoot.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 
 * Logger used throughout greenfoot.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootLogger.java 3124 2004-11-18 16:08:48Z polle $
 */
public class GreenfootLogger
{
    public static void init()
    {
        Logger logger = Logger.getLogger("greenfoot");

        Handler h = new ConsoleHandler();
        h.setFormatter(new Formatter() {

            public String format(LogRecord record)
            {
                return record.getMessage() + System.getProperty("line.separator");
            }
        });
        h.setLevel(Level.SEVERE);

        logger.setUseParentHandlers(false);
        logger.addHandler(h);
        logger.info("Logger intialized");
    }
}