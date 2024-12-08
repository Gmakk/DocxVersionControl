package edu.example.docxversioncontrol.files.storage.filesystem;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

    void init();

    void storeUpload(MultipartFile file);

    void storeResult(MultipartFile file) throws IOException;

    void storeChanges(WordprocessingMLPackage mlPackage);

    void storeResult(WordprocessingMLPackage mlPackage) throws IOException;

    Stream<Path> loadAllSourceFiles();

    Path loadSourceFile(String filename);

    Path loadLastResult();

    Path loadLastChanges();

    Boolean isLastResultEmpty();

    Boolean isLastChangesEmpty();

    Resource loadSourceAsResource(String filename);

    Resource loadLastResultAsResource();

    void deleteAll();

}