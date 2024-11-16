package edu.example.docxversioncontrol.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.example.docxversioncontrol.files.storage.StorageFileNotFoundException;
import edu.example.docxversioncontrol.files.storage.StorageProperties;
import edu.example.docxversioncontrol.files.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@SessionAttributes("lastChanges")
public class FileUploadController {

    private final StorageService storageService;
    private final String splitter;

    @Autowired
    public FileUploadController(StorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
        this.splitter = storageProperties.getFileNamesSplitter();
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {
        //получаем список файлов
        List<String> filenames = storageService.loadAll().map(
                        path -> path.getFileName().toString()).toList();
        //делаем список ссылок
        List<String> filesLinkslist = filenames.stream().map(
                        filename -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                                "serveFile", filename).build().toUri().toString())
                .toList();
        //добавляем список для отображения в виде ссылок
        model.addAttribute("files", filesLinkslist);
        //добавляем пары для выбора к сравнению
        List<String> filesToCompare = new ArrayList<>();
        if(filenames.size() > 1){//если есть хотя бы одна пара для сравнения
            //все кроме первого, тк сравнивать начинаем с ним
            filesToCompare.addAll(filenames);
            filesToCompare.remove(((Path)model.getAttribute("lastChanges")).getFileName().toString());

        }
        model.addAttribute("filesToCompare", filesToCompare);
        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);

        if (file == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes, Model model){
        if(file.isEmpty()){
            redirectAttributes.addFlashAttribute("message",
                    "Select a file to upload.");
            return "redirect:/";
        }
        storageService.store(file);
        //задаем первый загруженный файл, относительно которого будем отсчитывать изменения
        if(!model.containsAttribute("lastChanges")){
            model.addAttribute("lastChanges", storageService.load(file.getOriginalFilename()));
        }
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/files/lastChanges")
    public ResponseEntity<Object> getLastResultFile(Model model) throws MalformedURLException {
        //byte[] с типом
        Path file = (Path)model.getAttribute("lastChanges");
        if(file == null)
            return ResponseEntity.notFound().build();
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).body(resource);
    }
}
