package bluej.groupwork;

import java.io.*;
import java.util.*;

import bluej.Config;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.filefilter.DirectoryFilter;


/**
 * This class is responsible for reading and writing the configuration files
 * regarding teamwork settings. The files are team.defs, which is located in
 * the top-level folder of a team project, and the bluej.properties
 *
 * @author fisker
 * @version $Id: TeamSettingsController.java 4779 2006-12-22 04:06:21Z bquig $
 */
public class TeamSettingsController
{
    private Project project;
    private File projectDir;
    private Properties teamProperties;
    private TeamSettingsDialog teamSettingsDialog;
    private boolean includeLayout;
    
    //general
    //private String user;
    private String password;

    // cvs data
    //private String protocol;
    //private String server;
    //private String groupname;
    //private String module;
    //private String repositoryPrefix;
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
            String cvsroot = getCvsRoot();
            if (cvsroot != null) {
                repository = Repository.getInstance(projectDir, cvsroot);
            }
        }
        
        return repository;
    }
    
    /**
     * Get a list of files in the project which should be under version
     * control management.
     * 
     * @param includeLayout  indicates whether to include the layout (bluej.pkg) files.
     */
    public Set getProjectFiles(boolean includeLayout)
    {
        // Get a list of files to commit
        Set files = project.getFilesInProject(includeLayout);
        
        if (repository != null) {
            // Include files which did exist locally and were under version control,
            // but have been locally deleted (without the delete being committed).
            LinkedList stack = new LinkedList();
            stack.add(project.getProjectDir());
            Set tempSet = new HashSet();
            // We filter these files, but don't filter the layout files; if they've
            // been deleted, the delete should always be committed.
            FilenameFilter filter = getFileFilter(true);
            while (! stack.isEmpty()) {
                File dir = (File) stack.remove(0);
                File [] subDirs = dir.listFiles(new DirectoryFilter());
                for (int i = 0; i < subDirs.length; i++) {
                    stack.add(subDirs[i]);
                }
                try {
                    repository.getLocallyDeletedFiles(tempSet, dir);
                    for (Iterator i = tempSet.iterator(); i.hasNext(); ) {
                        File file = (File) i.next();
                        if (filter.accept(dir, file.getName())) {
                            files.add(file);
                        }
                    }
                    tempSet.clear();
                }
                catch (IOException ioe) { }
            }
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

    public String getCvsRoot()
    {
        if (password == null) {
            // If we don't yet know the password, prompt the user
            getTeamSettingsDialog().doTeamSettings();
            
            // If we still don't know it, user cancelled
            if (password == null) {
                return null;
            }
        }

        String protocol = getPropString("bluej.teamsettings.cvs.protocol");
        String user = getPropString("bluej.teamsettings.user");

        String server = getPropString("bluej.teamsettings.cvs.server");
        String repositoryPrefix = getPropString(
                "bluej.teamsettings.cvs.repositoryPrefix");
        
        String groupname = getPropString("bluej.teamsettings.groupname");

        if (! repositoryPrefix.endsWith("/") && groupname.length() != 0) {
            repositoryPrefix = repositoryPrefix + "/";
        }
        
        String cvsRoot = ":" + protocol + ":" + user + ":" + password + "@" +
            server + ":" + repositoryPrefix + groupname;

        return cvsRoot;
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
        teamProperties.put("bluej.teamsettings.cvs.ignore1", ".*\\.class");
        teamProperties.put("bluej.teamsettings.cvs.ignore2", "bluej\\.pkh");
        teamProperties.put("bluej.teamsettings.cvs.ignore3", "team\\.defs");
        teamProperties.put("bluej.teamsettings.cvs.ignore4", ".*\\.ctxt");
        teamProperties.put("bluej.teamsettings.cvs.ignore5", ".*\\~");
        teamProperties.put("bluej.teamsettings.cvs.ignore6", ".*\\#");
        teamProperties.put("bluej.teamsettings.cvs.ignore7", ".*\\#backup");
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
     * gets the regular expressions in string form for the files we shoule ignore
     * @return List containing Strings
     */
    public List getIgnoreFiles()
    {
        Enumeration keys = teamProperties.keys();
        List patterns = new LinkedList();

        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();

            if (key.startsWith("bluej.teamsettings.cvs.ignore")) {
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
    
    public boolean includeLayout()
    {
        return includeLayout;
    }
    
    public void setIncludeLayout(boolean state)
    {
        includeLayout = state;
    }
}
