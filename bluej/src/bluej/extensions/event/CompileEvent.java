package bluej.extensions.event;

import java.io.File;

/**
 * This class encapsulates compiler events
 * 
 * @version $Id: CompileEvent.java 1767 2003-04-09 08:43:42Z damiano $
 */
public class CompileEvent extends BluejEvent 
{
  /**
   * Event generated when compilation begins
   */
  public static final int COMPILE_START_EVENT=1;

  /**
   * Event generated when a compilation WARNING is generated.
   * A Warining event is one that will NOT invalidate the compilation
   */
  public static final int COMPILE_WARNING_EVENT=2;

  /**
   * Event generated when a compilation Error is generated.
   * A Warining event is one that WILL invalidate the compilation
   */
  public static final int COMPILE_ERROR_EVENT=3;

  /**
   * Event generated when a compilation is finished.
   * This is a successful compilation.
   */
  public static final int COMPILE_DONE_EVENT=4;

  /**
   * Event generated when a compilation is finished.
   * This is a FAILED compilation.
   */
  public static final int COMPILE_FAILED_EVENT=5;

  private int    eventId;
  private File[] fileNames;   // An array of names this event belong to
  private int    errorLineNumber;
  private String errorMessage;

  /**
   * NOT to be used by Extension writer.
   */
  public CompileEvent(int i_eventId, File[] i_fileNames)
    {
    eventId   = i_eventId;
    fileNames = i_fileNames;
    }

  /**
   * Returns the event type, one of the values defined.
   */
  public int getEvent ()
    {
    return eventId;
    }


  /**
   * Returns an array of zero, one or more files related to this event.
   * In case of COMPILE_ERROR_EVENT or COMPILE_WARNING_EVENT it is normally a one element array.
   */
  public File[] getFiles ()
    {
    return fileNames;
    }

  /**
   * NOT to be used by Extension writer.
   */
  public void setErrorLineNumber ( int i_lineNumber )
    {
    errorLineNumber = i_lineNumber;
    }

  /**
   * Returns the line number where the compilation error occours.
   * To be used only for COMPILE_ERROR_EVENT or COMPILE_WARNING_EVENT
   */
  public int getErrorLineNumber ( )
    {
    return errorLineNumber;
    }

  /**
   * NOT to be used by Extension writer.
   */
  public void setErrorMessage ( String i_errorMessage )
    {
    errorMessage = i_errorMessage;
    }
  
  /**
   * Returns the error message where the compilation error occours.
   * To be used only for COMPILE_ERROR_EVENT or COMPILE_WARNING_EVENT
   */
  public String getErrorMessage ( )
    {
    return errorMessage;
    }

  /**
   * Returns a meaningful description of this event.
   */
  public String toString()
    {
    StringBuffer aRisul = new StringBuffer (500);

    aRisul.append("CompileEvent:");

    if ( eventId == COMPILE_START_EVENT ) aRisul.append(" COMPILE_START_EVENT");
    if ( eventId == COMPILE_WARNING_EVENT ) aRisul.append(" COMPILE_WARNING_EVENT");
    if ( eventId == COMPILE_ERROR_EVENT ) aRisul.append(" COMPILE_ERROR_EVENT");
    if ( eventId == COMPILE_DONE_EVENT ) aRisul.append(" COMPILE_DONE_EVENT");
    if ( eventId == COMPILE_FAILED_EVENT ) aRisul.append(" COMPILE_FAILED_EVENT");

    aRisul.append(" getFiles()[0]=");
    aRisul.append(fileNames[0]);

    if ( eventId == COMPILE_WARNING_EVENT || eventId == COMPILE_ERROR_EVENT )
      {
      aRisul.append(" errorLineNumber="+errorLineNumber);
      aRisul.append(" errorMessage="+errorMessage);
      }

    return aRisul.toString();
    }
}