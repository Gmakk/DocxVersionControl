package edu.example.docxversioncontrol.controllers;

import edu.example.docxversioncontrol.files.async.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.async.HandleChanges;
import edu.example.docxversioncontrol.files.comparison.CompareDocuments;
import edu.example.docxversioncontrol.files.storage.FileSystemStorageService;
import edu.example.docxversioncontrol.files.storage.StorageException;
import edu.example.docxversioncontrol.files.storage.StorageProperties;
import org.docx4j.wml.Body;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/changes")
public class ComparisonController {

    private final FileSystemStorageService storageService;
    private final Path rootLocation;//место хранения файлов
    private final String splitter;//разграничитель между названиями файлов в паре

    @Autowired
    public ComparisonController(FileSystemStorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
        if(storageProperties.getLocation().trim().length() == 0){
            throw new StorageException("File upload location can not be Empty.");
        }
        this.splitter = storageProperties.getFileNamesSplitter();
        this.rootLocation = Paths.get(storageProperties.getLocation());
    }

    @GetMapping
    public String index(@RequestParam(value = "pair") String pair) throws Exception {
        String separator = System.getProperty("file.separator");
        String olderFilePath = System.getProperty("user.dir") + separator + rootLocation + separator + pair.split(splitter)[0];
        String newerFilePath = System.getProperty("user.dir") + separator + rootLocation + separator + pair.split(splitter)[1];

        //тело документа - результата сравнения двух документов
        Body newBody =  CompareDocuments.getComparisonResult(olderFilePath, newerFilePath);
        //получение разницы между двумя версиями
        DocInsertsAndDels changes = HandleChanges.getDocumentChanges(newBody);
        System.out.println("Dels: " + changes.getDocDels().keySet());
        System.out.println("Inserts: " + changes.getDocInserts().keySet());

        return "changes";
    }
}
