
package bluej.parser.symtab;

/*******************************************************************************
 * A stub symbol that is used to temporarily hold the name of a class
 *  until it can be properly resolved
 ******************************************************************************/
public class DummyClass extends ClassDef {
    //==========================================================================
    //==  Class Variables
    //==========================================================================

    /** The name of the package containing the class */
    private String pkg;


    //==========================================================================
    //==  Methods
    //==========================================================================


    /** Constructor to create a placeholder class object
     *  This version provides a means to set the package containing the class
     */
    public DummyClass() {
        super();
        this.pkg = "java.lang";
    }


    /** Constructor to create a placeholder class object */
    public DummyClass(String name, Occurrence occ) {
        super(name, false, false, false, occ, null, null, null);
    }


    /** Constructor to create a placeholder class object
     *  This version provides a means to set the package containing the class
     */
    public DummyClass(String name, Occurrence occ, String pkg) {
        super(name, false, false, false, occ, null, null, null);
        this.pkg = pkg;
    }


    /** Get the name of the package in which this class is defined */
    public String getPackage() {
        return pkg;
    }

}
