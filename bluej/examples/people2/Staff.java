/**
 * A class representing staff members for a simple BlueJ demo program.
 *
 * @author  Michael Kölling
 * @version 1.0, January 1999
 */

class Staff extends Person
{
    private String room;

    /**
     * Create a staff member with default settings for detail information.
     */
    Staff()
    {
        super("(unknown name)", 0000);
        room = "(unknown room)";
    }

    /**
     * Create a staff member with given name, year of birth and room
     * number.
     */
    Staff(String name, int yearOfBirth, String roomNumber)
    {
        super(name, yearOfBirth);
        room = roomNumber;
    }

    /**
     * Set a new room number for this person.
     */
    public void setRoom(String newRoom)
    {
        room = newRoom;
    }

    /**
     * Return the room number of this person.
     */
    public String getRoom()
    {
        return room;
    }

    /**
     * Return a string representation of this object.
     */
    public String toString()    // redefined from "Person"
    {
        return super.toString() +
               "Staff member\n" +
               "Room: " + room + "\n";
    }

}

