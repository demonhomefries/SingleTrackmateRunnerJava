import java.io.File;

public class ReplicateFolderStructure {

    public static String replicateFolders(String stringInputDir, String stringOutputDir) {
        File fileInputDir = new File(stringInputDir);
        String stringInputBaseName = fileInputDir.getName();

        // Create the base output directory
        File outputDirectory = new File(stringOutputDir, stringInputBaseName + "_autotracked");
        if (!outputDirectory.exists()) {
            boolean isCreated = outputDirectory.mkdirs();
            if (!isCreated) {
                throw new RuntimeException("Failed to create the output directory: " + outputDirectory.getAbsolutePath());
            }
        }

        // Recursively replicate folder structure
        replicateDirectoryStructure(fileInputDir, outputDirectory);

        return outputDirectory.getAbsolutePath();
    }

    private static void replicateDirectoryStructure(File sourceDir, File targetDir) {
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Create the corresponding subdirectory in the target
                    File newDir = new File(targetDir, file.getName());
                    if (!newDir.exists()) {
                        boolean isCreated = newDir.mkdirs();
                        if (!isCreated) {
                            throw new RuntimeException("Failed to create directory: " + newDir.getAbsolutePath());
                        }
                    }
                    // Recursively handle subdirectories
                    replicateDirectoryStructure(file, newDir);
                }
            }
        }
    }
}
