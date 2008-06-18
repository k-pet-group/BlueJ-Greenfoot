package greenfoot.export.mygame;

import greenfoot.export.WebPublisher;

/**
 * Class that can be used to check whether a scenario already exists on the publish site.
 * 
 * @author Poul Henriksen
 *
 */
public abstract class ExistingScenarioChecker
{
    
    /**
     * Will start a thread that checks whether a scenario with the given name exists for the given user. When a result is ready the method scenarioExistenceChecked will be called.
     * 
     * @param scenarioName
     */
    public void startScenarioExistenceCheck(String userName, String scenarioName) {
        // start new thread that checks existence
        WebPublisher client = new WebPublisher();
        //client.checkExistingScenario(scenarioName, userName, scenarioName, info)
        
    }
    
    /**
     * Will abort the checking.
     * 
     * @return True if successful abort, false if we didn't manage to abort (because it already finished the check)
     */
    public boolean abort() {
        // pre: is checking 
        // should sync with calling the hook methods, so that they do not get called after this method has been called, or if they do, maybe we should return FALSE from this method.
        return true;
    }
    
    /**
     * Method that will be called when the check has finished.
     * @param exists True if the scenario exists.
     */
    public abstract void scenarioExistenceChecked(boolean exists);
    
    /**
     * Method that will be called if a check fails. 
     * This can be because of a network error or other things.
     * @param reason
     */
    public abstract void scenarioExistenceCheckFailed(String reason);
    
}
