package lang.stride;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A class with useful methods for accessing the terminal.
 *
 * Inside BlueJ and Greenfoot, the terminal is a special window where
 * the user can provide text input, and where you can print text output.
 * If you run the program outside the IDE, it will use stdin/stdout,
 * the standard command-line input and output streams.
 */
public class Terminal
{
    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Writes the given message to the terminal.
     *
     * The message will have a newline character added to the end automatically.
     *
     * @param message The message to print out in the terminal.
     */
    public static void write(String message)
    {
        System.out.println(message);
        System.out.flush();
    }

    /**
     * Reads one line of user input from the terminal.
     *
     * The line will only be complete once the user presses enter
     * (i.e. once the input stream reaches a newline character).
     * The returned string will NOT contain the newline character.
     *
     * If there is a problem reading the line (e.g. an I/O error,
     * or end of stream), null will be returned.
     *
     * @return The line of input from the user.
     */
    public static String read()
    {
        try
        {
            return in.readLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads a user-input integer from the terminal.
     *
     * This takes a complete line of input (like the read() method),
     * and then converts it to an integer, if possible.
     *
     * If there is an I/O error, or if the user string cannot
     * be converted to an integer (contains non-digits, or is
     * too large), Integer.MIN_VALUE will be returned.
     *
     * @return The integer conversion of the user-entered line,
     * or Integer.MIN_VALUE if there are any problems.
     */
    public static int readInt()
    {
        String s = read();
        if (s != null)
        {
            try
            {
                return Integer.parseInt(s.trim());
            }
            catch (NumberFormatException e)
            {
            }
        }
        return Integer.MIN_VALUE;
    }
}
