package bluej.editor.moe.autocomplete;

/**
 * This listener interface defines the events that occur in a MoeDropDownList
 * that other classes will need to know about.  Other classes will only need
 * to know when an item has been clicked upon or double clicked upon.
 * <br>
 * An object constructed from a class that implements this interface can be
 * registered with a MoeDropDownList using the
 * addMoeDropDownListMouseListener method.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public interface MoeDropDownListMouseListener  {

    /**
     * Invoked when the mouse has been clicked on
     * a MoeDropDownItem in the MoeDropDownList
     */
  public void mouseClicked(MoeDropDownItem e);

    /**
     * Invoked when the mouse has been double clicked on
     * a MoeDropDownItem in the MoeDropDownList
     */
    public void mouseDoubleClicked(MoeDropDownItem e);

}
