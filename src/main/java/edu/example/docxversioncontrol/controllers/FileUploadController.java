package edu.example.docxversioncontrol.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.example.docxversioncontrol.files.storage.StorageFileNotFoundException;
import edu.example.docxversioncontrol.files.storage.StorageProperties;
import edu.example.docxversioncontrol.files.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
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
        List<String> pairs = new ArrayList<>();
        if(filenames.size() > 1){//если есть хотя бы одна пара для сравнения
            for(int i = 0; i < filenames.size() - 1; i++){//-1, потому что последний уже не с кем сравнивать
                pairs.add(filenames.get(i) + splitter + filenames.get(i + 1));
            }
        }
        model.addAttribute("pairs", pairs);
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
                                   RedirectAttributes redirectAttributes) {

        storageService.store(file);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
