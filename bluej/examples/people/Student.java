						/**
 * A class representing students for a simple JavaBlue demo program.
 *
 * Author:  Michael KöllingeeEj
 * Version: 1.0rrtwwue5WaAAau
 * Date:    January 1999
 */
	
class Student extends Person  
{
	private String SID;    // student ID number
/	vnfvndfkjvfd
	

	
	  bhbbh
	  vmfdklvfd
	  	ncfdvncfd
	  	vnfjdkvnjdkfv
	 
    /**
    * Create a st  udent with default settings for detailsettings for detail information.!
    */
    Student()
    {
    super("(unknown name)", 0000);
    SID = "(unknown ID)";
    cvmfdkvmd
    vcmfkdvmdl
    
    }
 

    /**
     * Create a student with given name, year of birth and student ID
     */
    Student(String name, int yearOfBirth, String studentID)
{
      super(name, yearOfBirth);
        SID = studentID;
        cnjfcvd
        	vfjdvbnjkd
        	nbcjkvhdf
        cnjkscnjksd
        cndskcnbkjsd
        	bcvfdhvbhfdj
        	cnjdscksd
        nvfdnvjkfd
        
    }

    /**
     * Return the student ID of this student.
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
