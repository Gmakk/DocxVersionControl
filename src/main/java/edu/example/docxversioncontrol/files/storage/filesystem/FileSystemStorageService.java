package edu.example.docxversioncontrol.files.storage.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import edu.example.docxversioncontrol.files.storage.minio.MinioService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FileUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileSystemStorageService implements StorageService {

    Path sourceLocation;
    Path resultLocation;
    Path changesLocation;
    MinioService minioService;

    @Autowired
    public FileSystemStorageService(StorageProperties properties, MinioService minioService) {

        if(properties.getResultlocation().trim().length() == 0 || properties.getSourcelocation().trim().length() == 0
                || properties.getChangeslocation().trim().length() == 0){
            throw new StorageException("File upload location can not be Empty.");
        }

        this.sourceLocation = Paths.get(properties.getSourcelocation());
        this.resultLocation = Paths.get(properties.getResultlocation());
        this.changesLocation = Paths.get(properties.getChangeslocation());
        this.minioService = minioService;
    }

    @Override
    public void storeUpload(MultipartFile file) {
        storeFile(file, sourceLocation);
    }

    @Override
    public void storeResult(MultipartFile file) throws IOException {
        FileUtils.cleanDirectory(resultLocation.toFile());
        storeFile(file, resultLocation);
        minioService.deleteAll();
        minioService.saveFile(file);
    }

    @Override
    public void storeResult(WordprocessingMLPackage mlPackage) throws IOException {
        FileUtils.cleanDirectory(resultLocation.toFile());
        storeMLPackage(mlPackage, resultLocation, "last_result.docx");
        minioService.deleteAll();
        minioService.saveFile(loadLastResultAsResource());
    }

    @Override
    public void storeChanges(WordprocessingMLPackage mlPackage) {
        storeMLPackage(mlPackage, changesLocation, "last_changes.docx");
    }

    private void storeMLPackage(WordprocessingMLPackage mlPackage, Path destination, String fileName) {
        try {
            mlPackage.save(destination.resolve(fileName).toFile());
        }
        catch (Docx4JException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    private void storeFile(MultipartFile file, Path destination) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }
            Path destinationFile = destination.resolve(
                            Paths.get(file.getOriginalFilename()))
                    .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(destination.toAbsolutePath())) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public Stream<Path> loadAllSourceFiles() {
        try {
            return Files.walk(this.sourceLocation, 1)
                    .filter(path -> !path.equals(this.sourceLocation))
                    .map(this.sourceLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path loadSourceFile(String filename) {
        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(FileUtils.listFiles(this.sourceLocation.toFile(), null, true));
        File result = allFiles.stream().filter(file -> file.getName().equals(filename)).findFirst().get();
        return Paths.get(result.getPath());
    }

    @Override
    public Path loadLastResult(){
        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(FileUtils.listFiles(this.resultLocation.toFile(), null, true));
        File result = allFiles.stream().findAny().get();
//        File result = allFiles.stream().filter(file -> file.getName().equals("lastChanges")).findFirst().get();
        return Paths.get(result.getPath());
    }

    @Override
    public Path loadLastChanges(){
        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(FileUtils.listFiles(this.changesLocation.toFile(), null, true));
        File result = allFiles.stream().findAny().get();
//        File result = allFiles.stream().filter(file -> file.getName().equals("lastChanges")).findFirst().get();
        return Paths.get(result.getPath());
    }

    @Override
    public Boolean isLastResultEmpty(){
        return FileUtils.listFiles(this.resultLocation.toFile(), null, true).isEmpty();
    }

    @Override
    public Boolean isLastChangesEmpty(){
        return FileUtils.listFiles(this.changesLocation.toFile(), null, true).isEmpty();
    }

    @Override
    public Resource loadSourceAsResource(String filename) {
        try {
            Path file = loadSourceFile(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);
            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public Resource loadLastResultAsResource() {
        try {
            Path file = loadLastResult();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + file.getFileName());
            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file", e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(sourceLocation.toFile());
        FileSystemUtils.deleteRecursively(resultLocation.toFile());
        FileSystemUtils.deleteRecursively(changesLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(resultLocation);
            Files.createDirectories(sourceLocation);
            Files.createDirectories(changesLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
