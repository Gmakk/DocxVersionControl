package edu.example.docxversioncontrol.files.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
@Data
public class StorageProperties {

    /**
     * Место, где размещаются загруженные файлы
     */
    private String location = "upload-dir";

    /**
     * Разделитель между именами файлов для интерфейса и параметров запроса
     */
    private String fileNamesSplitter = " --- ";

}