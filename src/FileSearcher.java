import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearcher {

    public static List<String> findFilesWithExtensions(String[] fileExtensions, String inputPath, boolean searchSubdirs) {
        List<String> filePaths = new ArrayList<>();
        File inputDir = new File(inputPath);

        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new IllegalArgumentException("Input path must be a valid directory.");
        }

        searchDirectory(inputDir, fileExtensions, searchSubdirs, filePaths);
        return filePaths;
    }

    private static void searchDirectory(File dir, String[] fileExtensions, boolean searchSubdirs, List<String> filePaths) {
        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory() && searchSubdirs) {
                searchDirectory(file, fileExtensions, true, filePaths);
            } else if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                for (String ext : fileExtensions) {
                    ext = ext.startsWith(".") ? ext.substring(1) : ext;
                    if (fileName.endsWith("." + ext.toLowerCase())) {
                        filePaths.add(file.getAbsolutePath());
                        break;
                    }
                }
            }
        }
    }

}
