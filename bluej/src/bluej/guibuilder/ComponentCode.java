package javablue.GUIBuilder;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;

/**
 * A class used to handle component code. This class handles code at three
 * levels: class level, method level and code used in the 'add' functions.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ComponentCode
{
    /**
     * Ease-of-use constant for the state of the GUIComponents.
     * Specifies that the variable created for the component will be placed
     * at class level, so it is accessible from other method in the same class.
     */
    public static final int CLASSLEVEL = 0;

    /**
     * Ease-of-use constant for the state of the GUIComponents.
     * Specifies that the variable created for the component will be placed
     * at in the createInterface() method. This means the component can only
     * be referenced during the the build-up-phase of the interface.
     */
    public static final int METHODLEVEL = 1;

    /**
     * Ease-of-use constant for the state of the GUIComponents.
     * Specifies that no variable will be created for the component. The 
     * created component will be passed directly as a parameter in the
     * appropriate add-function.
     */
    public static final int UNREFERENCEABLE = 2;

    // The 'tab' to be used:
    private static final String INDENT = new String("    ");

    private StringBuffer globalCode = new StringBuffer();
    private StringBuffer creationCode = new StringBuffer();
    private StringBuffer addCode = new StringBuffer();


    /**
     * Constructs a new ComponentCode.
     */
    public ComponentCode()
    {
    }


    /**
     * Returns the code marked as global.
     *
     * @return The stored global code.
     */
    public String getGlobalCode()
    {
	return globalCode.toString();
    }


    /**
     * Returns the code marked as method.
     *
     * @return The stored method code.
     */
    public String getCreationCode()
    {
	return creationCode.toString();
    }


    /**
     * Returns the code marked as unreferenceable.
     *
     * @return The stored unreferenceable code.
     */
    public String getUnreferenceableCode()
    {
	return addCode.toString();
    }


    /**
     * Adds code to the code marked as global.
     *
     * @param code The code to be added.
     */
    public void addCreation (String code)
    {
	creationCode.append(code);
    }


    /**
     * Adds code to the code marked as method.
     *
     * @param code The code to be added.
     */
    public void addGlobal (String code)
    {
	globalCode.append(code);
    }


    /**
     * Adds code to the code marked as unreferenceable.
     *
     * @param code The code to be added.
     */
    public void addUnreferenceable (String code)
    {
	addCode.append(code);
    }


    /**
     * Do indentation of the specified code based on the curly-brackets in that
     * code. All brackets must be on a separate line.
     *
     * @param code The code to indent.
     * @return The indented code.
     */
    public static String doIndent (String code)
    {
	int tabs = 0;
	boolean incTab = false;
	String buffer = null;
	StringBuffer tmpCode = new StringBuffer();	

	BufferedReader br = new BufferedReader(new StringReader(code));

	try
	{
	    while ((buffer = br.readLine()) != null)
	    {
		buffer.trim();
		if (buffer.startsWith("{"))
		    incTab = true;
		else if (buffer.startsWith("}"))
		    tabs--;

		for (int i=0; i<tabs; i++)
		    tmpCode.append(ComponentCode.INDENT);
		tmpCode.append(buffer+"\n");

		if (incTab)
		{
		    tabs++;
		    incTab=false;
		}
	    }
	}
	catch (IOException e)
	{
	    System.out.println("Exception: "+e.getMessage());
	}
	return tmpCode.toString();
    }
}
