import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 ** The main class.
 ** Contains a PersonVector and methods for adding and showing the vector.
 ** This class is part of a simple Database program, made to demonstrate
 ** JavaBlue by Michael Kolling.
 ** @author  Morten Knudsen & Kent Hansen
 ** @version 1.0, July 1998
 **/
public class Database {
	public PersonList theList;

	public static void main (String[] args) {
		Database theDatabase = new Database ();
		theDatabase.menu ();
	}

	public void menu () {
		int choice = 0;
		String buffer;
		BufferedReader in = new BufferedReader
			(new InputStreamReader (System.in));

		while (choice != '5') {
			System.out.println ("Menu\n----------");
			System.out.println ("1. Add Student");
			System.out.println ("2. Add Staff member");
			System.out.println ("3. Not implemented");
			System.out.println ("4. Show database");
			System.out.println ("5. Exit");
			System.out.print ("Select a number from the menu: ");
			try {
				buffer = in.readLine ();
				choice = buffer.charAt (0);
			}
			catch (IOException e) {
				System.out.println ("Exception: ");
			}
			catch (StringIndexOutOfBoundsException e) {
				System.out.println ("Exception: ");
			}

			switch (choice) {
				case '1':
					addStudent ();
					break;
				case '2':
					addStaff ();
					break;
				case '4':
					showAll ();
					break;
				case 'X':
				case 'x':
				case '5':
					break;
				default:
					System.out.println ("Not valid. Try again...");
					break;
			}
		}
	}

	public Database () {
		theList = new PersonList ();
	}

	public void addPerson (Person p) {
		theList.addPerson (p);
	}

	public void addStudent () {
		System.out.println ("Adding a new student to the database.");
		String fName = getStringInput ("Enter students first name   : ");
		String lName = getStringInput ("Enter students last name    : ");
		Long yob     = getLongInput   ("Enter students year of birth: ");
		String sID   = getStringInput ("Enter students ID           : ");
		String login = getStringInput ("Enter students login        : ");

		Student tmp = new Student (fName, lName, yob.longValue (), sID, login);
		theList.addPerson (tmp);
	}

	public void addStaff () {
		System.out.println ("Adding a new staff member to the database.");
		String fName = getStringInput ("Enter staff members first name   : ");
		String lName = getStringInput ("Enter staff members last name    : ");
		Long yob     = getLongInput   ("Enter staff members year of birth: ");
		String room  = getStringInput ("Enter staff members ID           : ");
		String pos   = getStringInput ("Enter staff members login        : ");

		Staff tmp = new Staff (fName, lName, yob.longValue (), room, pos);
		theList.addPerson (tmp);
	}

	public void showAll () {
		Person p;
		theList.start();
		while ( (p = theList.getNext()) != null) 
		{
			System.out.println(p);
		}
	}

	private String getStringInput (String prompt) {
		BufferedReader in = new BufferedReader (new InputStreamReader (System.in));
		String buffer = new String ();
		try {
			System.out.print (prompt);
			buffer = in.readLine();
		}
		catch (IOException e) {
			System.out.println ("Exception: "+e.getMessage ());
		}
		return buffer;
	}


	private Long getLongInput (String prompt) {
		String temp = getStringInput (prompt);
		Long value = new Long (0);
		try {
			value = new Long (temp);
		}
		catch (NumberFormatException e) {
			System.out.println ("Exception: "+e.getMessage ());
		}
		return value;
	}
}
