package edu.example.docxversioncontrol.storage.filesystem;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
@Data
public class StorageProperties {

    /**
     * Место, где размещаются загруженные файлы
     */
    private String sourcelocation;

    /**
     * Место, где размещаются итоговые файлы файлы
     */
    private String resultlocation;

    /**
     * Место, где размещаестя файл-разница
     */
    private String changeslocation;

    /**
     * Разделитель между именами файлов для интерфейса и параметров запроса
     */
    private String fileNamesSplitter = " --- ";

}