package bluej.editor.red;	// This file forms part of the red package

/**
 ** @version $Id: ActionStack.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 ** This class implements the Editors Stack of Actions
 **/

public final class ActionStack
{
  // public constant variables
  public final static int ACT_STACK_SIZE = 80;

  //private variables
  private Action stack[];	// the Action stack
  private int newest;		// pointer to last element inserted
  private int current;		// pointer to next element that will be returned
  private boolean thats_all;	// True, if no more action records are available

/**
 * CONSTRUCTOR: ActionStack()
 * Initialise the stack to hold pointers to valid actions
 * (undefined ones), and set the stack pointers to represent an empty
 * stack.
 */

public ActionStack()
{
  int i;

  stack = new Action[ACT_STACK_SIZE];

  for (i=0; i<ACT_STACK_SIZE; i++)
    stack[i] = new Action();

  newest = current = 0;
  thats_all = true;
}

/**
 * FUNCTION: new_action()  
 * Enter an action to the top of the action stack.  Note: if the 
 * stack is full, the oldest element is overwritten.  So it's really
 * a ring, not a stack...
 * The action is not really entered here.  This function merely selects
 * and returns the next action record to be used.  It is filled 
 * directly from outside then.
 */

public Action new_action ()
{
  newest = next (newest);
  if (newest == current)		// caught up with current pointer
    thats_all = true;			// can't go back further then!
  return stack[newest];
}

/**
 * FUNCTION: get_action()
 * Return the last action (pointed to by current) and set
 * current to the action before that.  Return NULL, if no further
 * action is available.
 */

public Action get_action ()
{
  Action act;

  if (thats_all)			// at end of stack
    return null;
  else {
    act = stack[current];
    current = prev(current);
    if (current == newest)
      thats_all = true;
    if (act.get_type() == Action.UndefAction) {
      thats_all = true;
      return null;
    }
    else
      return act;
  }
}

/**
 * FUNCTION: reset()
 * Reset the scan pointer to the action last entered.
 */

public void reset ()
{
  current = newest;
  thats_all = false;
}

/**
 * FUNCTION: clear()
 * Remove all actions from stack
 */

public void clear ()
{
  int i = 0;

  while ((i < ACT_STACK_SIZE) && (stack[i].get_type() != Action.UndefAction))
    stack[i].set_undef ();

  newest = current = 0;
  thats_all = true;
}

/**
 * FUNCTION: next(int)
 * Return the next index in the array, including wrap around.
 */
private int next (int idx)
{ 
  return (idx+1) % ACT_STACK_SIZE; 
}

/**
 * FUNCTION: prev(int)
 * Return the previous index in the array, including wrap around.
 */
private int prev (int idx)
{ 
  return idx==0 ? ACT_STACK_SIZE-1 : idx-1; 
}

} // end class ActionStack
