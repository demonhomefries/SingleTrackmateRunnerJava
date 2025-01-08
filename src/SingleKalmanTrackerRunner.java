// Generic imports
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// import org.jline.reader.impl.completer.SystemCompleter;

// import org.jline.reader.impl.completer.SystemCompleter;
import java.io.*;

// ImageJ imports
import ij.IJ;
import ij.measure.Calibration;
import ij.ImagePlus;

// Trackmate Imports
// import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.visualization.table.TrackTableView;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;


// import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;





public class SingleKalmanTrackerRunner {
    static {
        net.imagej.patcher.LegacyInjector.preinit();
    }
    // The output returns the same list of pairs as the original runKalmanTracking function
    public static List<Pair> runSingleKalmanTracking(
        String tifFile,
        Map<String, Object> trackmateConfigMap,
        String inputDirectory,
        String outputDirectory) {

            // Initialize the output file path list
            List<Pair> outputFilePathList = new ArrayList<>();

            @SuppressWarnings("unchecked")
            Map<String, Object> detectorSettingsMap = (Map<String, Object>) trackmateConfigMap.get("detectorSettings");
            @SuppressWarnings("unchecked")
            Map<String, Object> featureFilterSettingsMap = (Map<String, Object>) trackmateConfigMap.get("featureFilterSettings");
            @SuppressWarnings("unchecked")
            Map<String, Object> kalmanSettingsMap = (Map<String, Object>) trackmateConfigMap.get("kalmanSettings");

            System.out.println("SingleKalmanTrackerRunner Arguments:");
            System.out.println(trackmateConfigMap);
            System.out.println(inputDirectory);
            System.out.println(outputDirectory);

            String spotsFilePath = FilePathGenerator.generateNewFilePath(outputDirectory,inputDirectory,tifFile,"_spottable_auto", ".csv");
            String tracksFilePath = FilePathGenerator.generateNewFilePath(outputDirectory,inputDirectory,tifFile,"_tracktable_auto", ".csv");
            

            System.out.println("SingleKalmanTrackerRunner processing: " + tifFile);


            // Open the image and set the calibration
            ImagePlus imp = IJ.openImage(tifFile);
            System.out.println("\tImage opened");
            Calibration cal = new Calibration();
            double micronsPerPixel = (double) trackmateConfigMap.get("microns_per_pixel");
            cal.pixelHeight = micronsPerPixel;
            cal.pixelWidth = micronsPerPixel;
            imp.setCalibration(cal);   
            System.out.println("\tCalibration Complete");

            IJ.run(imp, "Re-order Hyperstack ...", "channels=[Channels (c)] slices=[Frames (t)] frames=[Slices (z)]");
            // System.out.println("Original dimensions: " + imp.getDimensions());

            // // Reorder axes to switch Z and T
            // ReorderHyperstack reorder = new ReorderHyperstack();
            // reorder.run("xyztc"); // Adjust to "xytzc" or another order if needed
            // ImagePlus imp = IJ.getImage(); // Get the reordered image

            // // Verify the new stack dimensions
            // System.out.println("Reordered dimensions: " + imp.getDimensions());

            // imp.show();
            


            // Initialize the model
            Model model = new Model();
            model.setPhysicalUnits("microns", "seconds");
            System.out.println("\tModel Initialized");



            // Initialize the logger
            // Logger logger = Logger.IJ_LOGGER;
            // System.out.println("Logger Initialized");



            // Initialize the settings (get them from the image itself)
            Settings settings = new Settings(imp);
            System.out.println("\tSettings Initialized");
            


            // Set up the threshold detector factory and pass arguments trackmateconfig
            settings.detectorFactory = new ThresholdDetectorFactory<>();
            settings.detectorSettings.put("TARGET_CHANNEL", detectorSettingsMap.get("TARGET_CHANNEL"));
            settings.detectorSettings.put("SIMPLIFY_CONTOURS", detectorSettingsMap.get("SIMPLIFY_CONTOURS"));
            settings.detectorSettings.put("INTENSITY_THRESHOLD", detectorSettingsMap.get("INTENSITY_THRESHOLD"));
            System.out.println("\tThresholdDetectorFactory Initialized");



            // Set up feature filter and pass arguments
            String feature = (String) featureFilterSettingsMap.get("FEATURE");
            Double threshold = (Double) featureFilterSettingsMap.get("THRESHOLD");
            Boolean isabove = (Boolean) featureFilterSettingsMap.get("IS_ABOVE");
            FeatureFilter filter1 = new FeatureFilter(feature, threshold, isabove);

            settings.addSpotFilter(filter1);
            System.out.println("\tSpot filter added");


            // Set up the kalman tracker
            settings.trackerFactory = new KalmanTrackerFactory();
            System.out.println("\tCreated KalmanTrackerFactory");

            Double linkingMaxDistance = (Double) kalmanSettingsMap.get("LINKING_MAX_DISTANCE");
            Double kalmanSearchRadius = (Double) kalmanSettingsMap.get("KALMAN_SEARCH_RADIUS");
            Integer maxFrameGap = (Integer) kalmanSettingsMap.get("MAX_FRAME_GAP");
            settings.trackerSettings.put("LINKING_MAX_DISTANCE", linkingMaxDistance);
            settings.trackerSettings.put("KALMAN_SEARCH_RADIUS", kalmanSearchRadius);
            settings.trackerSettings.put("MAX_FRAME_GAP", maxFrameGap);
            System.out.println("\tAdded trackerSettings Parameters");


            // Add all the analyzers to the settings because we want all the information
            settings.addAllAnalyzers();
            System.out.println("\tAnalyzers added");

            // Instantiate trackmate
            TrackMate trackmate = new TrackMate(model, settings);
            System.out.println("\tTrackmate Instantiated");

            if (trackmate.checkInput() != true){
                String errormessage = trackmate.getErrorMessage();
                System.out.println(errormessage);
                System.exit(0);
            }
            System.out.println("\tTrackmate validated successfully");

            // Start trackmate and print errors 
            Boolean trackmateProcessStatusBoolean = trackmate.process();
            if (trackmateProcessStatusBoolean != true){
                String errormessage = trackmate.getErrorMessage();
                System.out.println(errormessage);
                System.exit(0);
            }
            System.out.println("\tTrackmate processed successfully");


            SelectionModel sm = new SelectionModel(model);
            DisplaySettings ds = new DisplaySettings();

            TrackTableView trackTableView = new TrackTableView(model, sm, ds);
            
            System.out.println("\tCreated new TrackTableView");

            // Export the spots
            File spotsFile = new File(spotsFilePath);
            try {
            trackTableView.getSpotTable().exportToCsv(spotsFile);
            System.out.println("\tSpot CSV exported");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            // Export the tracks
            File tracksFile = new File(tracksFilePath);
            try {
            trackTableView.getTrackTable().exportToCsv(tracksFile);
            System.out.println("\tTracks CSV exported");
        }
            catch (IOException e) {
                e.printStackTrace();
            }


            // Create a Pair object and add it to the list
            Pair pair = new Pair(spotsFilePath, tracksFilePath);
            outputFilePathList.add(pair);
            
            String csvArgumentString = "\"" + spotsFilePath + ";" + tracksFilePath + "\"";
            System.out.println(csvArgumentString);




            System.out.println("Starting CSV Merger using Python...");
            try{
            // Build the process
            ProcessBuilder pb = new ProcessBuilder("python", "src\\Track-Spot_Merger_Auto.py", "--csvlist", csvArgumentString);

            // Redirect error stream to capture all outputs
            pb.redirectErrorStream(true);

            // Start the process
            Process process = pb.start();

            // Capture output from the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            System.out.println("Python script exited with code: " + exitCode);
            
            if (exitCode == 0){
            System.out.println("Successfully finished merging CSVs");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    
    IJ.run("Collect Garbage");

    return outputFilePathList;

    }
}
