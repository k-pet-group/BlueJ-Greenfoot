package bluej.editor.red;            // This file forms part of the red package
  
/**
 ** @version $Id: Question.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Giuseppe Speranza
 **/

public class Question 
{

  // public variables
  public String text;
  public String yes_label;
  public String no_label;
  public String cancel_label;

/**
 * CONSTRUCTOR: Question(String, String, String, String)
 */

public Question(String text, String yes_label, String no_label, 
                String cancel_label)
{
  // initialise variables
  this.text = text;
  this.yes_label = yes_label;
  this.no_label = no_label;
  this.cancel_label = cancel_label;
}

}  // end class Question
