package bluej.guibuilder;


/**
 * A class used to store a listener pair. A listener pair consists of a name
 * and a type, both strings, ie. "MyButtonHandlerr" and "ActionListener".
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ListenerPair
{
    /**
     * The name of the listener.
     */
    public String name;

    /**
     * The type of the listener.
     */
    public String type;


    /**
     * Constructs a new ListenerPair.
     */
    ListenerPair ()
    {
	name = new String();
	type = new String();
    }


    /**
     * Constructs a new ListenerPair with the specified name and type.
     *
     * @param name The name of the listener.
     * @param type The type of the listener.
     */
    ListenerPair (String name, String type)
    {
	this.name = name;
	this.type = type;
    }


    /**
     * Gets the Java code used to add this particular listener to the component.
     * This function is used by the generateCode() function in the component
     * classes.
     *
     * @return The Java code.
     */
    public String getAddFunction()
    {
	StringBuffer code = new StringBuffer();
	code.append("add");
	code.append(type);
	code.append("(");
	code.append(name);
	code.append(")");
	return code.toString();
    }
}
