package bluej.compiler;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import com.sun.tools.javac.Main;

import bluej.utility.*;

/**
 * An implementation for the BlueJ "Compiler"
 * class providing an interface to Sun's javac
 * compiler by executing the com.sun.tools methods directly.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: JavacCompilerInternal.java 1087 2002-01-12 13:29:08Z ajp $
 */
public class JavacCompilerInternal extends Compiler
{
    private ErrorStream firstStream = null;

	private String destdir;
	private String classpath;
	private boolean debug;
	private boolean deprecation;

	public JavacCompilerInternal()
	{
		setDebug(true);
	}

	public void setDestDir(String destdir)
	{
		this.destdir = destdir;
	}

	public void setClassPath(String classpath)
	{
		this.classpath = classpath;
	}

	public void setDebug(boolean debug)
	{
		this.debug = debug;
	}

	public void setDeprecation(boolean deprecation)
	{
		this.deprecation = deprecation;
	}

	public boolean compile(String[] sources, CompileObserver watcher)
	{
		Vector args = new Vector();

		if(destdir != null) {
			args.addElement("-d");
			args.addElement(destdir);
		}

		if(classpath != null) {
			args.addElement("-classpath");
			args.addElement(classpath);
		}

		if(debug)
			args.addElement("-g");

		if(deprecation)
			args.addElement("-deprecation");

//        args.addElement("-verbose");

        for(int i = 0; i < sources.length; i++)
            args.addElement(sources[i]);

        int length = args.size();
        String[] params = new String[length];
        args.copyInto(params);

        PrintStream systemErr = System.err;
        ErrorStream output = new ErrorStream();

        /* there is a problem with the jdk1.4 beta. It seems to use a
           PrintWriter wrapped around the system error stream. It also
           seems to cache the creation of this, so on subsequent compiles,
           the original output stream is still being used. To cope with
           both 1.3 and 1.4, we check the results of both the first stream
           and the newly created stream */

        if (firstStream == null)
            firstStream = output;

        System.setErr(output);      // redirect errors to our stream

        //Main javac = new Main(output, "javac"); // old version
 		com.sun.tools.javac.Main javac = new com.sun.tools.javac.Main();
        int result = javac.compile(params);

        // only one of 'output' or 'firstStream' should be receiving output so
        // we will only generate one error message

		if (output.hasError()) {
			watcher.errorMessage(output.getFilename(),
						output.getLineNo(),
						output.getMessage(),true);
		}

        if (firstStream != null && firstStream != output && firstStream.hasError()) {
			watcher.errorMessage(firstStream.getFilename(),
						firstStream.getLineNo(),
						firstStream.getMessage(),true);
		}

        System.setErr(systemErr);   // restore

        // in case we are reusing the first stream, reset the hasError boolean
        if (firstStream != null)
            firstStream.reset();

		//return result;
		return result==0;
	}
}

/**
 * An OutputStream that parses javac output.
 *
 * @author  Michael Cahill
 */
class ErrorStream extends PrintStream
{
    private boolean haserror = false;
    private String filename, message;
    private int lineno;

    public ErrorStream()
    {
        // we do not actually intend to use an actual OutputStream from
        // within this class yet our superclass requires us to pass a
        // non-null OutputStream
        // we pass it the system error stream
        super(System.err);
    }

    public void reset()
    {
        haserror = false;
    }

    public boolean hasError()
    {
        return haserror;
    }

    public String getFilename()
    {
        return filename;
    }

    public int getLineNo()
    {
        return lineno;
    }

    public String getMessage()
    {
        return message;
    }

    /**
     * Note: this class "cheats" by assuming that all output will be written by
     * a call to print or println. It happens that this is true for the
     * current version of javac but this could change in the future.
     *
     * We assume a certain error message format here:
     *   filename:line-number:message
     *
     * We find the components by searching for the colons. Careful: MS Windows
     * systems might have a colon in the file name (if it is an absolute path
     * with a drive name included). In that case we have to ignore the first
     * colon.
     */
    public void print(String msg)
    {
        if (haserror)
            return;

        // Debug.message("Compiler message: " + msg);

        int first_colon = msg.indexOf(':', 0);
        if(first_colon == -1) {
            // cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        filename = msg.substring(0, first_colon);

        // Windows might have a colon after drive name. If so, ignore it
        if(! filename.endsWith(".java")) {
            first_colon = msg.indexOf(':', first_colon + 1);
            if(first_colon == -1) {
                    // cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
            }
            filename = msg.substring(0, first_colon);
        }
        int second_colon = msg.indexOf(':', first_colon + 1);
        if(second_colon == -1) {
            // cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        lineno = 0;
        try {
            lineno = Integer.parseInt(msg.substring(first_colon + 1, second_colon));
        } catch(NumberFormatException e) {
            // ignore it
        }

        message = msg.substring(second_colon + 1);

        haserror = true;
    }

    /**
     * Map println to print - we are not interested in the line break anyway.
     */
    public void println(String msg)
    {
        print(msg);
    }

    /**
     * JDK 1.4 seems to use write rather than print
     */
    public void write(byte[] buf, int off, int len)
    {
        print(new String(buf, off, len));
    }
}
