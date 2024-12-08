package edu.example.docxversioncontrol.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import edu.example.docxversioncontrol.files.storage.filesystem.StorageFileNotFoundException;
import edu.example.docxversioncontrol.files.storage.filesystem.StorageService;
import edu.example.docxversioncontrol.files.storage.minio.MinioService;
import edu.example.docxversioncontrol.messaging.NoticeMessagingServiceKafka;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileUploadController {

    StorageService storageService;
    MinioService minioService;
    NoticeMessagingServiceKafka messagingService;

    @GetMapping("/")
    public String listUploadedFiles(Model model) {
        //получаем список файлов
        List<String> filenames = storageService.loadAllSourceFiles().map(
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
            String fileNameToRemove = storageService.loadLastResult().getFileName().toString();
            filesToCompare.remove(fileNameToRemove);
        }
        model.addAttribute("filesToCompare", filesToCompare);
        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadSourceAsResource(filename);

        if (file == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes, Model model) throws IOException {
        if(file.isEmpty()){
            redirectAttributes.addFlashAttribute("message",
                    "Select a file to upload.");
            return "redirect:/";
        }
        storageService.storeUpload(file);
        //задаем первый загруженный файл, относительно которого будем отсчитывать изменения
        if(storageService.isLastResultEmpty()){
            storageService.storeResult(file);
        }
        redirectAttributes.addFlashAttribute("message",
                "Вы успешно загрузили " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/files/lastResult")
    public ResponseEntity<Object> getLastResultFile(Model model) throws MalformedURLException {
        Resource resource = new UrlResource(storageService.loadLastResult().toUri());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).body(resource);
    }

    @GetMapping("/notify")
    public String sendFileURL() {
        String fileName = storageService.loadLastResult().getFileName().toString();
        String URL = minioService.getFileURL(fileName);
        messagingService.sendFileURL(URL);
        return "redirect:/";
    }
}