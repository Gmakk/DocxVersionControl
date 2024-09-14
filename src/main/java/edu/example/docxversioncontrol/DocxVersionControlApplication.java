package edu.example.docxversioncontrol;

import edu.example.docxversioncontrol.files.StorageProperties;
import edu.example.docxversioncontrol.files.StorageService;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class DocxVersionControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocxVersionControlApplication.class, args);
    }

    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            storageService.deleteAll();
            storageService.init();
        };
    }
}