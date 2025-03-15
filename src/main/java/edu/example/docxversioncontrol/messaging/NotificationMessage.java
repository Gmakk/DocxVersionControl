package edu.example.docxversioncontrol.messaging;

import edu.example.docxversioncontrol.files.DocumentType;
import lombok.Value;

@Value
public class NotificationMessage {
    String url;
    String contentType;
    String fileExtension;

    public NotificationMessage(String url, DocumentType documentType) {
        this.url = url;
        this.contentType = documentType.getContentType();
        this.fileExtension = documentType.getFileExtension();
    }
}
