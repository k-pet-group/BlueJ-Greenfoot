/**
 * Class Reader - Simplifies reading in user input.
 * @author: Michael Kolling
 */
import java.io.*;

public class Reader
{
    /**
     * Constructor for objects of class Reader
     */
    public Reader()
    {
    }

    /**
     * Read a line of input, and return it. You can supply a prompt, if you
	 * like. (The prompt can be an empty String if you don't want one.)
     */
    public String readLine(String prompt)
    {
        System.out.print(prompt);
        StringBuffer buf = new StringBuffer();
        int c;

        try {
            while(((c = System.in.read()) != -1) && (c != '\n'))
                buf.append((char)c);
        } catch(IOException e) {
            // ignore it
        }
        return buf.toString();
    }
}
