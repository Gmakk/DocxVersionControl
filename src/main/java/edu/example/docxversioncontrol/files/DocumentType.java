package edu.example.docxversioncontrol.files;

import lombok.Getter;

@Getter
public enum DocumentType {
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),;


    private final String contentType;
    private final String fileExtension;

    DocumentType(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }
}
