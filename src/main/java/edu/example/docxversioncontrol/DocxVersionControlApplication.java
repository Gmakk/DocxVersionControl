package edu.example.docxversioncontrol;

import edu.example.docxversioncontrol.files.async.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.async.HandleChanges;
import edu.example.docxversioncontrol.files.comparison.CompareDocuments;
import edu.example.docxversioncontrol.files.storage.StorageProperties;
import org.docx4j.wml.Body;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class DocxVersionControlApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DocxVersionControlApplication.class, args);
        String newerfilepath = System.getProperty("user.dir") + "/upload-dir/test2.docx";
        String olderfilepath = System.getProperty("user.dir") + "/upload-dir/test.docx";
        Body newBody =  CompareDocuments.getComparisonResult(olderfilepath, newerfilepath);
        DocInsertsAndDels changes = HandleChanges.getDocumentChanges(newBody);
        System.out.println("Dels: " + changes.getDocDels().keySet());
        System.out.println("Inserts: " + changes.getDocInserts().keySet());
    }

    //TODO раскоментить
//    @Bean
//    CommandLineRunner init(StorageService storageService) {
//        return (args) -> {
//            storageService.deleteAll();
//            storageService.init();
//        };
//    }
}
