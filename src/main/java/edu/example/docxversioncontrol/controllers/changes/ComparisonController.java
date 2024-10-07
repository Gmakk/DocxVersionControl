package edu.example.docxversioncontrol.controllers.changes;

import edu.example.docxversioncontrol.files.async.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.async.HandleChanges;
import edu.example.docxversioncontrol.files.comparison.CompareDocuments;
import edu.example.docxversioncontrol.files.storage.FileSystemStorageService;
import edu.example.docxversioncontrol.files.storage.StorageException;
import edu.example.docxversioncontrol.files.storage.StorageProperties;
import org.docx4j.wml.Body;
import org.docx4j.wml.RunDel;
import org.docx4j.wml.RunIns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/changes")
@SessionAttributes("changes")
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

    @ModelAttribute
    public static void populateModelAttributes(Model model) {
        model.addAttribute("SelectedChangesForm", new SelectedChangesForm());//сюда записывается результат выбора
    }

    @GetMapping
    public String showChangesPage(@RequestParam(value = "pair") String pair, Model model) throws Exception {
        //TODO:Сохранять текущую пару в модель?

        String separator = System.getProperty("file.separator");
        String olderFilePath = System.getProperty("user.dir") + separator + rootLocation + separator + pair.split(splitter)[0];
        String newerFilePath = System.getProperty("user.dir") + separator + rootLocation + separator + pair.split(splitter)[1];

        //тело документа - результата сравнения двух документов
        Body newBody =  CompareDocuments.getComparisonResult(olderFilePath, newerFilePath);
        //получение разницы между двумя версиями
        DocInsertsAndDels changes = HandleChanges.getDocumentChanges(newBody);

        model.addAttribute("changes", changes);
//        System.out.println("Dels: " + changes.getDocDels().keySet());
//        System.out.println("Inserts: " + changes.getDocInserts().keySet());

        return "changes";
    }


    @PostMapping("/submit")
    public String saveChanges(Model model,SelectedChangesForm selectedchanges){
        DocInsertsAndDels docInsertsAndDels = (DocInsertsAndDels) model.getAttribute("changes");

        List<RunIns> selectedInserts = docInsertsAndDels.getDocInserts().entrySet().stream()
                .filter(pair -> selectedchanges.getSelectedInserts().contains(pair.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        List<RunDel> selectedDels = docInsertsAndDels.getDocDels().entrySet().stream()
                .filter(pair -> selectedchanges.getSelectedDels().contains(pair.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        System.out.println("Dels: " + selectedDels);
        System.out.println("Inserts: " + selectedInserts);
        return "redirect:/";
    }
}
