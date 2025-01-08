import ij.IJ;
import ij.gui.GenericDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

public class TrackmateParameters {
    static {
        net.imagej.patcher.LegacyInjector.preinit();
    }
    static {
        // Increase the default font size
        Font font = new Font("Dialog", Font.PLAIN, 30); // Adjust size as needed
        UIManager.put("Label.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("CheckBox.font", font);
        UIManager.put("TitledBorder.font", font);
        UIManager.put("ComboBox.font", font);
    }
    public static class Settings {
        public String inputDir;
        public String outputDir;
        public boolean searchSubdirs;
        public boolean saveXML;
        public String trackmateConfigJSONString;
        public List<String> tifFileList;
    }

    public static Settings getSettings() {
        String inputDir = "";
        String outputDir = "";
        boolean searchSubdirs = false;
        boolean saveXML = false;
        String trackmateConfigJSONString = "";

        while (true) {
            // Create the dialog
            GenericDialog gd = new GenericDialog("Get Trackmate Parameters");
            gd.addDirectoryField("Input Directory: ", inputDir);
            gd.addCheckbox("Search Subdirectories", searchSubdirs);
            gd.addMessage("");
            gd.addDirectoryField("Output Directory: ", outputDir);
            gd.addCheckbox("Save XML", saveXML);
            gd.addFileField("Trackmate Config JSON: ", trackmateConfigJSONString);


            // Set font and size for all components
            Font largerFont = new Font("Dialog", Font.PLAIN, 20);
            Dimension largerSize = new Dimension(250, 40);

            for (Component comp : gd.getComponents()) {
                comp.setFont(largerFont);
    
                // Check for child components (e.g., buttons, checkboxes)
                if (comp instanceof Container) {
                    for (Component subComp : ((Container) comp).getComponents()) {
                        subComp.setFont(largerFont);
                        if (subComp instanceof java.awt.Checkbox || subComp instanceof java.awt.Button) {
                            subComp.setPreferredSize(largerSize);
                        }
                    }
                }
    
                // Adjust size directly for top-level components
                if (comp instanceof java.awt.Checkbox || comp instanceof java.awt.Button) {
                    comp.setPreferredSize(largerSize);
                }
            }
    
            gd.setSize(600, 500); // Optionally adjust dialog size
            gd.showDialog();

            if (gd.wasCanceled()) {
                IJ.log("getSettings was cancelled, restoring default values...");
                return null;
            }

            // Retrieve values
            inputDir = gd.getNextString();
            outputDir = gd.getNextString();
            searchSubdirs = gd.getNextBoolean();
            saveXML = gd.getNextBoolean();
            trackmateConfigJSONString = gd.getNextString();

            // Validate input directory
            File inputDirFile = new File(inputDir);
            if (!inputDirFile.isDirectory()) {
                warningDialog("Please choose a valid input directory.");
                continue;
            }

            // Find .tif files
            List<String> tifFileList;
            if (searchSubdirs) {
                tifFileList = findTifFiles(inputDir);
            } else {
                tifFileList = findTifFilesSurfaceDir(inputDir);
            }

            if (tifFileList.isEmpty()) {
                warningDialog(inputDir + " contains 0 .tif files. Please choose a directory with 1 or more .tif files.");
                continue;
            }

            // Validate output directory
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.isDirectory()) {
                warningDialog("Please choose a valid output directory.");
                continue;
            }

            File jsonFile = new File(trackmateConfigJSONString);
            if (!jsonFile.isFile()) {
                warningDialog("Please choose a valid JSON file.");
                continue;
            }

            // Return settings
            Settings settings = new Settings();
            settings.inputDir = inputDir;
            settings.outputDir = outputDir;
            settings.searchSubdirs = searchSubdirs;
            settings.saveXML = saveXML;
            settings.tifFileList = tifFileList;
            settings.trackmateConfigJSONString = trackmateConfigJSONString;
            return settings;
        }
    }

    public static List<String> findTifFiles(String directory) {
        List<String> tifFiles = new ArrayList<>();
        File dir = new File(directory);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".tif")) {
                    tifFiles.add(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    tifFiles.addAll(findTifFiles(file.getAbsolutePath()));
                }
            }
        }
        return tifFiles;
    }

    public static List<String> findTifFilesSurfaceDir(String directory) {
        List<String> tifFiles = new ArrayList<>();
        File dir = new File(directory);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".tif")) {
                    tifFiles.add(file.getAbsolutePath());
                }
            }
        }
        return tifFiles;
    }

    public static void warningDialog(String message) {
        GenericDialog gd = new GenericDialog("Warning");
        gd.addMessage(message);
        gd.showDialog();
    }

}
