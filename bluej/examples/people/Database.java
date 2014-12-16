import java.util.ArrayList;
import java.util.Iterator;

/**
 * A very simple database of people in a university. This class simply stores
 * persons and, at request, lists them on standard output.
 *
 * Written as a first demo program for BlueJ.
 *
 * @author  Michael Kölling
 * @version 1.1, March 2002
 */

public class Database {

    private ArrayList<Person> persons;

    /**
     * Create a new, empty person database.
     */
    public Database() 
    {
        persons = new ArrayList<Person>();
    }

    /**
     * Add a person to the database.
     */
    public void addPerson(Person p) 
    {
        persons.add(p);
    }

    /**
     * List all the persons currently in the database on standard out.
     */
    public void listAll () 
    {
        for (Iterator i = persons.iterator(); i.hasNext();) {
            System.out.println(i.next());
        }
    }
}
