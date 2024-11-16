package edu.example.docxversioncontrol.controllers.changes;

import edu.example.docxversioncontrol.files.async.clear.ClearDocService;
import edu.example.docxversioncontrol.files.async.extract.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.async.extract.HandleChangesService;
import edu.example.docxversioncontrol.files.comparison.CompareDocuments;
import edu.example.docxversioncontrol.files.storage.FileSystemStorageService;
import edu.example.docxversioncontrol.files.storage.StorageException;
import edu.example.docxversioncontrol.files.storage.StorageProperties;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/changes")
@SessionAttributes({"changes", "lastChanges"})
public class ComparisonController {

    private final FileSystemStorageService storageService;
    private final Path rootLocation;//место хранения файлов
    private final String splitter;//разграничитель между названиями файлов в паре
    private final ClearDocService clearDocService;

    @Autowired
    public ComparisonController(FileSystemStorageService storageService, StorageProperties storageProperties, ClearDocService clearDocService) {
        this.storageService = storageService;
        if(storageProperties.getLocation().trim().length() == 0){
            throw new StorageException("File upload location can not be Empty.");
        }
        this.splitter = storageProperties.getFileNamesSplitter();
        this.rootLocation = Paths.get(storageProperties.getLocation());
        this.clearDocService = clearDocService;
    }

    @ModelAttribute
    public static void populateModelAttributes(Model model) {
        model.addAttribute("SelectedChangesForm", new SelectedChangesForm());//сюда записывается результат выбора
    }

    @GetMapping
    public String showChangesPage(@RequestParam(value = "fileToCompare") String fileToCompare, Model model) throws Exception {
        //TODO:Сохранять текущую пару в модель?
        if(fileToCompare.equals("lastChanges")){
            return "redirect:/";
        }
        //начинаем сравнивать последние изменения с выбранным файлом
        String separator = System.getProperty("file.separator");
        String olderFilePath = ((Path) model.getAttribute("lastChanges")).toString();
        String newerFilePath = System.getProperty("user.dir") + separator + rootLocation + separator + fileToCompare;

        //тело документа - результата сравнения двух документов
        Body newBody =  CompareDocuments.getComparisonResult(olderFilePath, newerFilePath);
        //получение разницы между двумя версиями
        DocInsertsAndDels changes = HandleChangesService.getDocumentChanges(newBody);

        model.addAttribute("changes", changes);

        //обновляем Path файла с последними изменениями
        model.addAttribute("lastChanges", Path.of(System.getProperty("user.dir") +"/last_changes.docx"));
        return "changes";
    }

    @PostMapping("/submit")
    public String saveChanges(Model model,SelectedChangesForm selectedchanges) throws Docx4JException {
        DocInsertsAndDels docInsertsAndDels = (DocInsertsAndDels) model.getAttribute("changes");

        //Список выбранных изменений в виде их id
        List<BigInteger> selectedInserts = docInsertsAndDels.getDocInserts().keySet().stream()
                .filter(runIns -> selectedchanges.getSelectedInserts().contains(runIns))
                .toList();

        List<BigInteger> selectedDels = docInsertsAndDels.getDocDels().keySet().stream()
                .filter(runDel -> selectedchanges.getSelectedDels().contains(runDel))
                .toList();


        //Временно выбираются все удаления автоматически
        //List<BigInteger> selectedDels = ((DocInsertsAndDels)model.getAttribute("changes")).getDocDels().keySet().stream().toList();


        //загружаем результат сравнения если это первый файл, то его
        WordprocessingMLPackage changesPackage = WordprocessingMLPackage.load(new java.io.File(String.valueOf(model.getAttribute("lastChanges"))));
        //передаем в метод для очистки результата сравнения от ненужных изменений
        ClearDocService.clearDocument(changesPackage, selectedInserts, selectedDels);

        try {
            changesPackage.save(new File(System.getProperty("user.dir") + "/last_changes.docx"));
        }catch (Docx4JException e){
            e.printStackTrace();
        }
        return "redirect:/";
    }
}
