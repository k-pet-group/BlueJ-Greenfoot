package greenfoot.util;

/**
 * Indicates a problem with accessing the storage
 * 
 * This indicates that there was a problem reading from/writing to the storage, typically:
 * 
 * - A problem accessing the CSV file in the local storage (e.g. bad permissions, disconnected USB stick)
 * - A problem accessing the Gallery storage (may be caused by maintenance work on the gallery)
 * - If storage is not supported, this exception will be thrown every time.
 *
 * In general, the way to deal with this exception is to live with not using
 * storage for this session.  Warn the user that their data cannot be loaded/saved,
 * and proceed as best as you can.
 */
public class GreenfootStorageException extends Exception
{

    public GreenfootStorageException(String message)
    {
        super(message);
    }

}
