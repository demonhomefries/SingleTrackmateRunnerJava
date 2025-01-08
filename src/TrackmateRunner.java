
import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlWriter;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory;
import fiji.plugin.trackmate.visualization.table.TrackTableView;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class TrackmateRunner {
    
    /**
     * Runs TrackMate processing on a list of TIFF files.
     *
     * @param tifFileList       A list of file paths to .tif images.
     * @param inputDir          The input directory containing the images.
     * @param autotrackedDir    The directory where processed results should be saved.
     * @param micronsPerPixel   The scale to set for image calibration.
     * @param detectorSettings  A map containing the detector settings (e.g. TARGET_CHANNEL, etc.).
     * @param featureFilterSettings A map for the feature filter (e.g. FEATURE, THRESHOLD, IS_ABOVE).
     * @param kalmanSettings    A map for the Kalman tracker settings.
     * @param saveXml           Whether to save the TrackMate model as XML.
     * @param csvMergerFilepath A path to the Python script that merges CSV files.
     */
    public static void runTrackMate(
            List<String> tifFileList,
            String inputDir,
            String autotrackedDir,
            double micronsPerPixel,
            java.util.Map<String, Object> detectorSettings,
            java.util.Map<String, Object> featureFilterSettings,
            java.util.Map<String, Object> kalmanSettings,
            boolean saveXml,
            String csvMergerFilepath
    ) throws Exception {
        
        List<String[]> tablesGenerated = new ArrayList<>();
        
        // Ensure no trailing slash
        if (inputDir.endsWith("\\") || inputDir.endsWith("/")) {
            inputDir = inputDir.substring(0, inputDir.length() - 1);
        }

        for (int index = 0; index < tifFileList.size(); index++) {
            String image = tifFileList.get(index);
            String imageName = imageNameWithoutExtension(image);
            
            java.io.File inputImageFile = new java.io.File(image);
            String inputDirName = inputImageFile.getParent();
            String finalOutputDir = inputDirName.replace(inputDir, autotrackedDir);

            java.io.File finalOutputDirFile = new java.io.File(finalOutputDir);
            if (!finalOutputDirFile.exists()) {
                finalOutputDirFile.mkdirs();
            }

            // Construct output paths
            String spotTableCsvFilepath = finalOutputDir + "/" + imageName + "_spottable_auto.csv";
            String trackTableCsvFilepath = finalOutputDir + "/" + imageName + "_tracktable_auto.csv";

            System.out.println("spot_table_csv_filepath: " + spotTableCsvFilepath);
            System.out.println("track_table_csv_filepath: " + trackTableCsvFilepath);

            String xmlFilepath = finalOutputDir + "/" + imageName + "_auto.xml";

            System.out.println("Starting to process image " + (index + 1) + "/" + tifFileList.size() + ": " + imageName);

            // Open image in ImageJ
            ImagePlus imp = IJ.openImage(image);

            // Set calibration
            Calibration cal = new Calibration();
            cal.pixelWidth = micronsPerPixel;
            cal.pixelHeight = micronsPerPixel;
            imp.setCalibration(cal);

            // Create TrackMate model
            Model model = new Model();
            model.setPhysicalUnits("microns", "seconds");
            Logger logger = Logger.IJ_LOGGER;
            model.setLogger(logger);

            // Settings
            Settings settings = new Settings(imp);

            // Detector
            settings.detectorFactory = new ThresholdDetectorFactory<UnsignedShortType>();
            settings.detectorSettings = new java.util.HashMap<>();
            settings.detectorSettings.put("TARGET_CHANNEL", detectorSettings.get("TARGET_CHANNEL"));
            settings.detectorSettings.put("SIMPLIFY_CONTOURS", detectorSettings.get("SIMPLIFY_CONTOURS"));
            settings.detectorSettings.put("INTENSITY_THRESHOLD", detectorSettings.get("INTENSITY_THRESHOLD"));

            // Feature filter
            FeatureFilter filter1 = new FeatureFilter(
                (String) featureFilterSettings.get("FEATURE"),
                ((Number) featureFilterSettings.get("THRESHOLD")).doubleValue(),
                (Boolean) featureFilterSettings.get("IS_ABOVE")
            );
            settings.addSpotFilter(filter1);

            // Kalman tracker
            settings.trackerFactory = new KalmanTrackerFactory();
            settings.trackerSettings = new java.util.HashMap<>();
            settings.trackerSettings.put("LINKING_MAX_DISTANCE", kalmanSettings.get("LINKING_MAX_DISTANCE"));
            settings.trackerSettings.put("KALMAN_SEARCH_RADIUS", kalmanSettings.get("KALMAN_SEARCH_RADIUS"));
            settings.trackerSettings.put("MAX_FRAME_GAP", kalmanSettings.get("MAX_FRAME_GAP"));

            // Add all analyzers
            settings.addAllAnalyzers();

            // Run TrackMate
            TrackMate trackmate = new TrackMate(model, settings);

            // Check input
            if (!trackmate.checkInput()) {
                throw new RuntimeException("Error in TrackMate input: " + trackmate.getErrorMessage());
            }

            System.out.println("\tStarting analysis");
            if (!trackmate.process()) {
                throw new RuntimeException("Error during TrackMate processing: " + trackmate.getErrorMessage());
            }

            SelectionModel sm = new SelectionModel(model);
            DisplaySettings ds = new DisplaySettings();

            // Export tables
            TrackTableView trackTableView = new TrackTableView(model, sm, ds);
            trackTableView.getSpotTable().exportToCsv(new File(spotTableCsvFilepath));
            System.out.println("\tSaved spot table to " + spotTableCsvFilepath);

            trackTableView.getTrackTable().exportToCsv(new File(trackTableCsvFilepath));
            System.out.println("\tSaved track table to " + trackTableCsvFilepath);

            tablesGenerated.add(new String[]{spotTableCsvFilepath, trackTableCsvFilepath});

            // Close the image to free memory
            imp.close();

            // Save XML if requested
            if (saveXml) {
                TmXmlWriter writer = new TmXmlWriter(new File(xmlFilepath));
                writer.appendLog(model.getLogger().toString());
                writer.appendModel(model);
                writer.appendSettings(settings);
                writer.appendDisplaySettings(ds);
                writer.writeToFile();
                System.out.println("Saved XML");
            }

            // Execute merge command (Python script)
            System.out.println("Executing merge with Python file at " + csvMergerFilepath);
            String csvListArgument = "\"" + spotTableCsvFilepath + "," + trackTableCsvFilepath + "\"";
            String command = "python \"" + csvMergerFilepath + "\" --csvlist " + csvListArgument;

            try {
                Process proc = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                int exitVal = proc.waitFor();
                if (exitVal != 0) {
                    System.err.println("Track-Spot_Merger_Auto failed with exit code " + exitVal);
                }
            } catch (Exception e) {
                System.err.println("ERROR Track-Spot_Merger_Auto failed: ");
                System.err.println("Command: " + command);
                e.printStackTrace();
            }
        }

        // tables_generated now contains the pairs of files generated for each TIFF.
    }

    /**
     * Helper method to get the image name without extension.
     */
    private static String imageNameWithoutExtension(String path) {
        java.io.File f = new java.io.File(path);
        String name = f.getName();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return name;
    }
}
