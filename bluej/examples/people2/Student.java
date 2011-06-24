/**
 * A class representing students for a simple BlueJ demo program.
 *
 * @author  Michael Kölling
 * @version 1.0, January 1999
 */
class Student extends Person
{
    private String SID;    // student ID number

    /**
     * Create a student with default settings for detail information.
     */
    public Student()
    {
        super("(unknown name)", 0000);
        SID = "(unknown ID)";
    }

    /**
     * Create a student with given name, year of birth and student ID
     */
    public Student(String name, int yearOfBirth, String studentID)
    {
        super(name, yearOfBirth);
        SID = studentID;
    }

    /**
     * Return the stident ID of this student.
     */
    public String getStudentID()
    {
        return SID;
    }

    /**
     * Return a string representation of this object.
     */
    public String toString()    // redefined from "Person"
    {
        return super.toString() +
               "Student\n" +
               "Student ID: " + SID + "\n";
    }
}

