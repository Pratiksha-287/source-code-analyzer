
package org.example.sourcecodeanalyser.model;

public class FileAnalysisMetadata {

    private String fileName;
    private String relativePath;
    private String fileType;

    public FileAnalysisMetadata(
            String fileName,
            String relativePath,
            String fileType) {

        this.fileName = fileName;
        this.relativePath = relativePath;
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getFileType() {
        return fileType;
    }
}
