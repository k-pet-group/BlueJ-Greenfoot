import java.io.*;
import java.net.URL;

/**
 * This is a little demo showing how to read text files. It will find files
 * that are situated anywhere in the classpath.
 *
 * Currently, two demo methods are available. Both simply print a text file to the
 * terminal. One returns exceptions in case of a problem, the other prints out
 * error messages.
 * 
 * @author Michael Kölling
 * @version 1.0 (19. Feb 2002)
 */
public class FileReader
{
    /**
     * Create a file reader
     */
    public FileReader()
    {
        // nothing to do...
    }

    /**
     * Show the contents of the file 'fileName' on standard out (the text terminal).
     *
     * @param fileName  The name of the file to show
     * @throws  IOException if the file could not be opened
     */
    public void showFile(String fileName)
        throws IOException
    {
        InputStream fstream = openFile(fileName);

        // wrap the stream into an InputStreamReader, so that we read characters
        // rather than bytes (important for non-ascii characters); then wrap it into
        // a BufferedReader, so that we can read lines, rather than single characters

        BufferedReader in = new BufferedReader(new InputStreamReader(fstream));

        // okay, we're ready to go...

        System.out.println("File: " + fileName);
        String line = in.readLine();
        while(line != null) {
            System.out.println(line);
            line = in.readLine();
        }
        System.out.println("<end of file>");
    }
        
    /**
     * Same as 'showfile', but don't throw exceptions. If an error occurs,
     * write an error message to the terminal.
     *
     * @param fileName  The name of the file to show
     */
    public void checkedShowFile(String fileName)
    {
        try {
            showFile(fileName);
        }
        catch(IOException exc) {
            System.out.println("There was a problem showing this file.");
            System.out.println("The error encountered is:");
            System.out.println(exc);
        }
    }

    /**
     * Open a text file and return a stream to read from that file.
     * The file can reside anywhere in the classpath.
     *
     * @param fileName  The name of the file to open
     * @return An open stream to read from the file
     * @throws  IOException if the file could not be opened
     */
    public InputStream openFile(String fileName)
        throws IOException
    {
        if(fileName == null)
            throw new IOException("Cannot open file - filename was null.");
        URL url = getClass().getClassLoader().getResource(fileName);
        if(url == null)
            throw new IOException("File not found: " + fileName);
        return url.openStream();
    }
}
