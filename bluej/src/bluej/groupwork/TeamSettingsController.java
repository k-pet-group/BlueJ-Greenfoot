package bluej.groupwork;

import java.io.*;
import java.util.*;

import bluej.Config;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;


/**
 * This class is responsible for reading and writing the configuration files
 * regarding teamwork settings. The files are team.defs, which is located in
 * the top-level folder of a team project, and the bluej.properties
 *
 * @author fisker
 * @version $Id: TeamSettingsController.java 5472 2008-01-22 03:48:01Z davmac $
 */
public class TeamSettingsController
{
    private Project project;
    private File projectDir;
    private Properties teamProperties;
    private TeamSettingsDialog teamSettingsDialog;
    
    //general
    private String password;

    private File teamdefs;
    
    // repository
    private Repository repository;

    /**
     * Construct a team settings controller for the given project.
     */
    public TeamSettingsController(Project project)
    {
        this.project = project;
        this.projectDir = project.getProjectDir();
        teamProperties = new Properties();
        readSetupFile();
    }

    /**
     * Construct a team settings controller, not associated with
     * any project initially. The supplied projectDir need not be the
     * final project directory - it is just used as a working location
     * until the project is set.
     */
    public TeamSettingsController(File projectDir)
    {
        this.projectDir = projectDir;
        teamProperties = new Properties();
    }

    /**
     * Assign this team settings controller to a particular project.
     * Once this is done, the repository settings can no longer be
     * changed.
     */
    public void setProject(Project proj)
    {
        project = proj;
        projectDir = proj.getProjectDir();
        repository = null;
        checkTeamSettingsDialog();
    }
    
    /**
     * Get the repository. Can return null if user credentials are required
     * but the user chooses to cancel.
     */
    public Repository getRepository()
    {
        if (repository == null) {
            
            if (password == null) {
                // If we don't yet know the password, prompt the user
                getTeamSettingsDialog().doTeamSettings();
                
                // If we still don't know it, user cancelled
                if (password == null) {
                    return null;
                }
            }
            
            TeamSettings settings = teamSettingsDialog.getSettings();
            repository = settings.getProvider().getRepository(projectDir, settings);
        }
        
        return repository;
    }
    
    /**
     * Get a list of files in the project which should be under version
     * control management. This includes files which have been locally deleted
     * since the last commit.
     * 
     * @param includeLayout  indicates whether to include the layout (bluej.pkg) files.
     * (Note that locally deleted bluej.pkg files are always included).
     */
    public Set getProjectFiles(boolean includeLayout)
    {
        // Get a list of files to commit
        Set files = project.getFilesInProject(includeLayout);
        
        if (repository != null) {
            repository.getAllLocallyDeletedFiles(files);
        }
        
        return files;
    }
    
    /**
     * Get a filename filter suitable for filtering out files which we don't want
     * to be under version control.
     */
    public FilenameFilter getFileFilter(boolean includeLayout)
    {
        return new CodeFileFilter(getIgnoreFiles(), includeLayout);
    }
    
    /**
     * Read the team setup file in the top level folder of the project and
     * configure the cvsroot and set the globalOptions
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void readSetupFile()
    {
        teamdefs = new File(projectDir + "/team.defs");

        try {
            teamProperties.load(new FileInputStream(teamdefs));
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            // This is allowed to happen - if a non-shared project becomes
            // shared
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepare for the deletion of a directory. For CVS, this involves moving
     * the metadata elsewhere. Returns true if the directory should actually
     * be deleted, or false if the version control system will delete it either
     * immediately or at commit time.
     */
    public boolean  prepareDeleteDir(File dir)
    {
        return getRepository().prepareDeleteDir(dir);
    }
    
    /**
     * Prepare a newly created directory for version control.
     */
    public void prepareCreateDir(File dir)
    {
        getRepository().prepareCreateDir(dir);
    }

    /**
     * Get the team settings dialog to edit these team settings.
     */
    public TeamSettingsDialog getTeamSettingsDialog()
    {
        if (teamSettingsDialog == null) {
            teamSettingsDialog = new TeamSettingsDialog(this);
            teamSettingsDialog.setLocationRelativeTo(PkgMgrFrame.getMostRecent());
            checkTeamSettingsDialog();
        }
        
        return teamSettingsDialog;
    }
    
    /**
     * Disable the repository fields in the team settings dialog if
     * we have a project attached.
     */
    private void checkTeamSettingsDialog()
    {
        if (teamSettingsDialog != null && project != null) {
            // We have a project, which means we have an established
            // repository. It shouldn't be changed now.
            teamSettingsDialog.disableRepositorySettings();
        }
    }
    
    /**
     * Write the settings to team.defs in the project. It no project is known,
     * nothing happens. Note that nothing is written to bluej.properties. That
     * is handled by the Config class.
     *
     */
    public void writeToProject()
    {
        if (projectDir == null) {
            return;
        }

        File cfgFile = new File(projectDir + "/team.defs");

        if (!cfgFile.exists()) {
            addIgnoreFilePatterns(teamProperties);
        }

        try {
            teamProperties.store(new FileOutputStream(cfgFile), null);
            repository = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add to the team properties the files we wish to ignore, like class files
     * and ctxt files
     * @param teamProperties
     */
    private void addIgnoreFilePatterns(Properties teamProperties)
    {
        teamProperties.put("bluej.teamsettings.ignore1", ".*\\.class");
        teamProperties.put("bluej.teamsettings.ignore2", "bluej\\.pkh");
        teamProperties.put("bluej.teamsettings.ignore3", "team\\.defs");
        teamProperties.put("bluej.teamsettings.ignore4", ".*\\.ctxt");
        teamProperties.put("bluej.teamsettings.ignore5", ".*\\~");
        teamProperties.put("bluej.teamsettings.ignore6", ".*\\#");
        teamProperties.put("bluej.teamsettings.ignore7", ".*\\#backup");
        teamProperties.put("bluej.teamsettings.ignore8", "\\.DS_Store");
    }

    /**
     * get the property by the name strname. If the property is present in
     * the project, that value is returned. If not, bluej.properties and then
     * bluej.defs are searched. If not found, null is returned.
     * @param strname
     * @return
     */
    public String getPropString(String strname)
    {
        String result = teamProperties.getProperty(strname);

        if (result != null) {
            return result;
        }

        result = Config.getPropString(strname, null);

        return result;
    }

    public void setPropString(String key, String value)
    {
        teamProperties.setProperty(key, value);
    }

    /**
     * In the first instance we don't want to store password.
     * We want to ask the first time they want to try and perform operation
     * We then store for the rest of the session. Over time we may want to provide
     * some way of storing with relative security.
     */
    public String getPasswordString()
    {
        return password;
    }

    public void setPasswordString(String password)
    {
        this.password = password;
    }

    public boolean hasPasswordString()
    {
        return password != null;
    }

    /**
     * gets the regular expressions in string form for the files we should ignore
     * @return List containing Strings
     */
    public List getIgnoreFiles()
    {
        Enumeration keys = teamProperties.keys();
        List patterns = new LinkedList();

        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();

            // legacy settings
            if (key.startsWith("bluej.teamsettings.cvs.ignore")) {
                patterns.add(teamProperties.getProperty(key));
            }
            
            // new settings
            if (key.startsWith("bluej.teamsettings.ignore")) {
                patterns.add(teamProperties.getProperty(key));
            }
        }

        return patterns;
    }

    public boolean hasProject()
    {
        return project != null;
    }
    
    public Project getProject()
    {
        return project;
    }
}
