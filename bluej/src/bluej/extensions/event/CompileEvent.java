package bluej.extensions.event;

import java.io.File;

/**
 * This class encapsulates compiler events.
 * It allows an extension writer to know when a compilation fails and
 * when a compilation is successful.
 * 
 * @version $Id: CompileEvent.java 1848 2003-04-14 10:24:47Z damiano $
 */

/*
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class CompileEvent implements BlueJExtensionEvent 
{
  /**
   * Event generated when compilation begins
   */
  public static final int COMPILE_START_EVENT=1;

  /**
   * Event generated when a compilation warning is generated.
   * A warining event is one that will not invalidate the compilation.
   */
  public static final int COMPILE_WARNING_EVENT=2;

  /**
   * Event generated when a compilation Error is generated.
   * An error event is one that will invalidate the compilation
   */
  public static final int COMPILE_ERROR_EVENT=3;

  /**
   * Event generated when a compilation is finished.
   * This is a successful compilation.
   */
  public static final int COMPILE_DONE_EVENT=4;

  /**
   * Event generated when a compilation is finished.
   * This is a failed compilation.
   */
  public static final int COMPILE_FAILED_EVENT=5;

  private int    eventId;
  private File[] fileNames;   // An array of names this event belong to
  private int    errorLineNumber;
  private String errorMessage;

  /**
   * Constructor for a CompileEvent.
   */
  public CompileEvent(int anEventId, File[] aFileNames)
    {
    eventId   = anEventId;
    fileNames = aFileNames;
    }

  /**
   * Return the eventId, one of the values defined.
   */
  public int getEvent ()
    {
    return eventId;
    }


  /**
   * Returns an array of zero, one or more files related to this event.
   * In case of COMPILE_ERROR_EVENT or COMPILE_WARNING_EVENT it is normally a one element array.
   * In all other cases it depends on the number of files being compiled.
   */
  public File[] getFiles ()
    {
    return fileNames;
    }

  /**
   * Sets the line number where an error or warning occourred.
   */
  public void setErrorLineNumber ( int aLineNumber )
    {
    errorLineNumber = aLineNumber;
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
   * Sets the error message for an error or warning event.
   */
  public void setErrorMessage ( String anErrorMessage )
    {
    errorMessage = anErrorMessage;
    }
  
  /**
   * Return the error message where the compilation error occours.
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