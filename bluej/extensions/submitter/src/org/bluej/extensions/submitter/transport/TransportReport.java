package org.bluej.extensions.submitter.transport;

/**
 * This interface, if implemented, allows an object to get some reporting
 * on what is happening to the Transport. Of course you have to set the reporting
 * opject by the means of setTransportReport()
 */
public interface TransportReport 
  {

  /**
   * Reports an Event.
   */
  public void reportEvent(String message);

  /**
   * Reports something that is below an event, a log of what is happening
   */
  public void reportLog(String message);
  }