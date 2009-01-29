import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * This is a small program to get the size of the jar file which is extracted during the
 * BlueJ installation (unix installer), and write the size to a property in a properties
 * file.
 * 
 * @author Davin McCall
 */
public class InstallerProps
{
    public static void main(String [] args)
    {
        if (args.length != 1) {
            System.err.println("Must include properties template file on command line.");
            System.exit(1);
        }
        
        Properties props = new Properties();
        String propsTemplate = args[0];
        
        try {
            FileInputStream is = new FileInputStream(new File(propsTemplate));
            props.load(is);
            
            File installTmp = new File("install_tmp");
            File distJar = new File(props.getProperty("install.pkgJar"));
            long length = distJar.length();
            
            props.put("install.pkgJarSize", Long.toString(length));
            
            File newProps = new File(installTmp, "installer.props");
            FileOutputStream os = new FileOutputStream(newProps);
            props.store(os, "Installer properties");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
