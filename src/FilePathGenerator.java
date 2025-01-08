import java.nio.file.Path;
import java.nio.file.Paths;


public class FilePathGenerator {
    public static String generateNewFilePath(String autoTrackedDir, String inputDir, String tifPath, String suffix, String newExtension) {
        // Convert strings to Path objects
        Path inputDirPath = Paths.get(inputDir);
        Path tifPathPath = Paths.get(tifPath);

        // Subtract inputDir from tifPath to get the relative path
        Path relativePath = inputDirPath.relativize(tifPathPath);

        // Extract the file name and parent directory from the relative path
        String fileName = relativePath.getFileName().toString(); // e.g., "myfile.tif"
        String parentDir = relativePath.getParent() != null ? relativePath.getParent().toString() : ""; // e.g., "subfolder"

        // Modify the file name with suffix and new extension
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName; // Remove the extension
        String newFileName = baseName + suffix + newExtension; // Add suffix and new extension

        // Build the final output path
        Path outputPath = Paths.get(autoTrackedDir, parentDir, newFileName);

        return outputPath.toString();
    }
}
