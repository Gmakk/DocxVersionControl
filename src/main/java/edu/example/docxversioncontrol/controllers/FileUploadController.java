package edu.example.docxversioncontrol.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import edu.example.docxversioncontrol.storage.filesystem.StorageFileNotFoundException;
import edu.example.docxversioncontrol.storage.filesystem.StorageService;
import edu.example.docxversioncontrol.storage.minio.MinioService;
import edu.example.docxversioncontrol.files.DocumentType;
import edu.example.docxversioncontrol.messaging.NoticeMessagingServiceKafka;
import edu.example.docxversioncontrol.messaging.NotificationMessage;
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
@RequestMapping("/uploadForm")
@SessionAttributes({"lastChanges", "docType"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileUploadController {

    StorageService storageService;
    MinioService minioService;
    NoticeMessagingServiceKafka messagingService;

    @GetMapping
    public String listUploadedFiles(@RequestParam(name = "docType", required = false) String docTypeString,
                                    Model model) {
        checkDocType(model, docTypeString);

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

    @PostMapping
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes, Model model) throws IOException {
        if(file.isEmpty()){
            redirectAttributes.addFlashAttribute("message",
                    "Select a file to upload.");
            return "redirect:/uploadForm";
        }
        storageService.storeUpload(file);
        //задаем первый загруженный файл, относительно которого будем отсчитывать изменения
        if(storageService.isLastResultEmpty()){
            storageService.storeResult(file);
        }
        redirectAttributes.addFlashAttribute("message",
                "Вы успешно загрузили " + file.getOriginalFilename() + "!");

        return "redirect:/uploadForm";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/files/lastResult")
    public ResponseEntity<Object> getLastResultFile(Model model) throws MalformedURLException {
        Resource resource = new UrlResource(storageService.loadLastResult().toUri());
        return ResponseEntity.ok().contentType(getMediaType(model)).body(resource);
    }

    @GetMapping("/notify")
    public String sendFileURL() {
        String fileName = storageService.loadLastResult().getFileName().toString();
        String url = minioService.getFileURL(fileName);
        NotificationMessage message = new NotificationMessage(url, DocumentType.DOCX);
        messagingService.sendFileURL(message);
        return "redirect:/uploadForm";
    }

    /**
     * В случае, если ранее пользователь не работал с файлами или сменил тип документа, очищает хранилище
     *
     * @param model model
     * @param docTypeString новый тип документа
     */
    private void checkDocType(Model model, String docTypeString) {
        if (docTypeString == null) {
            return;
        }

        DocumentType oldDocumentType = (DocumentType) model.getAttribute("docType");
        DocumentType newDocumentType = DocumentType.valueOf(docTypeString);
        if(oldDocumentType == null || oldDocumentType != newDocumentType){
            storageService.deleteAll();
            storageService.init();
            model.asMap().clear();
        }
        model.addAttribute("docType", newDocumentType);
    }

    /**
     * Получить MediaType для актуального выбранного типа файлов
     *
     * @param model model
     * @return MediaType
     */
    private MediaType getMediaType(Model model) {
        DocumentType docType = (DocumentType) model.getAttribute("docType");
        String contentType = docType != null ? docType.getContentType() : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return MediaType.parseMediaType(contentType);
    }
}