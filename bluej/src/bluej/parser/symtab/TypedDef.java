
package bluej.parser.symtab;

/*******************************************************************************
 * This interface represents definitions that have a "type" associated with
 *  them.  It provides a getType method that can be used to retrieve the
 *  symbol for that type.
 ******************************************************************************/
interface TypedDef {

    /** returns the symbol representing the type associated with a definition */
    Definition getType();
}
