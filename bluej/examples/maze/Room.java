/**
 * Author:  Morten Knudsen & Kent Hansen
 *         Ported from the Blue program "Maze" by Michael Kolling.
 * Version: 1.0
 * Date:    July 1998
 * Short:   A room in an adventure game.
 *
 * This class is part of Maze. Maze is a simple, text based adventure game.
 *
 * "Room" represents one location in the scenery of the game.  It is 
 * connected to at most four other rooms via exits.  Each Room can hold
 * one object.
 */

class Room {
    private String name;
    private Room north_exit, south_exit, west_exit, east_exit;
    private Item object_present;
    
    /**
     *  Create a room named "name".
     */
    public Room (String name) {
        this.name = name;
        north_exit = null;
        south_exit = null;
        west_exit  = null;
        east_exit  = null;
        object_present = null;
    }

    /**
     *    Return the name of the room.
     */
    public String getName () {
        return name;
    }

    /**
     *  Define the exits of this room.  Every direction either leads
     *  to another room or is nil (no exit there).
     */
    public void setExits (Room north, Room east, Room south, Room west) {
        north_exit = north;
        south_exit = south;
        west_exit  = west;
        east_exit  = east;
    }

    /**
     *  Return a string with  the room description (that is its name and
     *  information about exits) and, if an object is present, the name of
     *  the object.
     */
    public String toString () {
        StringBuffer buffer = new StringBuffer ();
        buffer.append ("You are in "+name+"\n");
        buffer.append ("Exists: ");
        if (north_exit != null)
            buffer.append ("north ");
        if (south_exit != null)
            buffer.append ("south ");
        if (east_exit != null)
            buffer.append ("east ");
        if (west_exit != null)
            buffer.append ("west ");
        buffer.append ("\n");
        if (object_present != null)
            buffer.append ("You see "+object_present+".\n");
        return new String(buffer);
    }

    /**
     *    Return the room that is reached if we go from this room in direction
     *    'dir'. If there is no room in that direction, 'next_room' returns null
     */
    public Room nextRoom (Command direction) {
        if (direction.equals ("north"))
            return north_exit;
        else if (direction.equals ("south"))
            return south_exit;
        else if (direction.equals ("east"))
            return east_exit;
        else
            return west_exit;
    }

    /**
     *    Try to take "object" from this room. If it is here, remove it and
     *    return true. If it is not here, return false.
     */
    public boolean takeObject (Item object) {
        if (object_present.equals (object)) {
            // Take object.
            object_present = null;
            return true;
        }
        else
            // Object is not here.
            return false;
    }

    /**
     *   Try to put an object into this room. If there is no other object 
     *   here, put the object and return true. Otherwise return false (cannot
     *    put object)
     */
    public boolean putObject (Item object) {
        if (object_present == null) {
            object_present = object;
            return true;
        }
        else
            return false;
    }

    /**
     *    Return the object currently present in this room (null is returned
     *    if there is no object present)
     */
    public Item getObject () {
        return object_present;
    }
}
