package bluej.groupwork;

import bluej.Config;
import java.io.File;

/**
 * Team status information for a file
 * 
 * @author Davin McCall
 * @version $Id: TeamStatusInfo.java 5159 2007-08-17 03:27:38Z davmac $
 */
public class TeamStatusInfo
{
    private File file;
    private String localVersion;
    private String remoteVersion;
    private int status;
    
    /** The file is up-to-date, the local revision is the same as in the repository */
    public final static int STATUS_UPTODATE = 0;
    /** The file doesn't exist locally, but is in the repository */
    public final static int STATUS_NEEDSCHECKOUT = 1;
    /** The file has been deleted locally, but the deletion hasn't been committed yet */ 
    public final static int STATUS_DELETED = 2;
    /** The repository version is newer */
    public final static int STATUS_NEEDSUPDATE = 3;
    /** The local version has been modified */
    public final static int STATUS_NEEDSCOMMIT = 4;
    /** The repository version is newer, but the file is also locally modified */
    public final static int STATUS_NEEDSMERGE = 5;
    /** The file exists locally, but not in the repository */
    public final static int STATUS_NEEDSADD = 6;
    /** The file exists locally, but has been removed in the repository */
    public final static int STATUS_REMOVED = 7;
    /**
     * An unresolved conflict. This can happen when either:<ul>
     * <li>the file has been removed from the repository, but local changes have been made
     * <li>the file has been locally removed, but modified in the repository
     * <li>a file has been locally added, but has been added to the repository by someone
     *   else
     * <li>when two binary files have been modified maybe?
     * </ul><p> 
     * 
     * The only way out is to either delete the file locally, or do a forced
     * commit or a forced update.
     */
    public final static int STATUS_UNRESOLVED = 8;
    /** 
     * There are conflicts now present in the local file. The local file
     * needs to be edited to resolve the conflicts. (This state occurs
     * after an update which merged changes from the repository).
     */
    public final static int STATUS_HASCONFLICTS = 9;
    /** Unknown */
    public final static int STATUS_WEIRD = 10;
    /**
     * The file has been created locally, but a file with the same name has been
     * added in the repository. This is a conflict.
     */
    public final static int STATUS_CONFLICT_ADD = 12;
    /**
     * Locally modified, but deleted in repository (conflict)
     */
    public final static int STATUS_CONFLICT_LMRD = 13;
    /**
     * Locally deleted, but modified in repository (conflict)
     */
    public final static int STATUS_CONFLICT_LDRM = 14;
    
    
    /* It has no status, only used for default constructor while waiting for cvs */
    public final static int STATUS_BLANK = 11;
    
    public final static String [] statusStrings = {
        "team.statusinfo.upToDate",
        "team.statusinfo.needsCheckout",
        "team.statusinfo.deleted",
        "team.statusinfo.needsUpdate",
        "team.statusinfo.needsCommit",
        "team.statusinfo.needsMerge",
        "team.statusinfo.needsAdd",    
        "team.statusinfo.removed",
        "team.statusinfo.unresolved",
        "team.statusinfo.hasConflicts",
        "team.statusinfo.weird",
        "",
        "team.statusinfo.conflictAdd",
        "team.statusinfo.conflictLMRD",
        "team.statusinfo.conflictLDRM"
    };
    
    /**
     * Default "blank" status info. Used to prepopulate status table whilst 
     * awaiting repository response.
     */
    public TeamStatusInfo()
    {
        this(new File(""), "", "", STATUS_BLANK);
    }
    
    public TeamStatusInfo(File file, String localVersion, String remoteVersion, int status)
    {
        this.file = file;
        this.localVersion = localVersion;
        this.remoteVersion = remoteVersion;
        this.status = status;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public String getLocalVersion()
    {
        return localVersion;
    }
    
    public String getRepositoryVersion()
    {
        return remoteVersion;
    }
    
    
    public int getStatus()
    {
        return status;
    }
    
    public String toString()
    {
        return getFile().getName();
    }
    
    public static String getStatusString(int status)
    {
        if(status == STATUS_BLANK)
            return "";
        return Config.getString(statusStrings[status]);
    }
    
}
