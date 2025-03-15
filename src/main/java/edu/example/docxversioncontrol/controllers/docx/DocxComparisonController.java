package edu.example.docxversioncontrol.controllers.docx;

import edu.example.docxversioncontrol.files.docx.async.clear.ClearDocService;
import edu.example.docxversioncontrol.files.docx.async.extract.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.docx.async.extract.HandleChangesService;
import edu.example.docxversioncontrol.files.docx.comparison.CompareDocuments;
import edu.example.docxversioncontrol.storage.filesystem.StorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/docxChanges")
@SessionAttributes({"docxChanges", "lastChanges"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocxComparisonController {

    StorageService storageService;
    ClearDocService clearDocService;
    CompareDocuments compareDocuments;
    HandleChangesService handleChangesService;


    @ModelAttribute
    public void populateModelAttributes(Model model) {
        model.addAttribute("SelectedChangesForm", new SelectedChangesForm());//сюда записывается результат выбора
    }

    @GetMapping
    public String showChangesPage(@RequestParam(value = "fileToCompare") String fileToCompare, Model model) throws Exception {
        Path oldResult = storageService.loadLastResult();
        Path newFile = storageService.loadSourceFile(fileToCompare);

        //тело документа - результата сравнения двух документов
        Body newBody =  compareDocuments.getComparisonResult(oldResult, newFile);
        //получение разницы между двумя версиями
        DocInsertsAndDels docxChanges = HandleChangesService.getDocumentChanges(newBody);

        model.addAttribute("docxChanges", docxChanges);
        return "docxChanges";
    }

    @PostMapping("/submit")
    public String saveChanges(Model model,SelectedChangesForm selectedchanges) throws IOException, Docx4JException {
        DocInsertsAndDels docInsertsAndDels = (DocInsertsAndDels) model.getAttribute("docxChanges");

        //Список выбранных изменений в виде их id
        List<BigInteger> selectedInserts = docInsertsAndDels.getDocInserts().keySet().stream()
                .filter(runIns -> selectedchanges.getSelectedInserts().contains(runIns))
                .toList();

        List<BigInteger> selectedDels = docInsertsAndDels.getDocDels().keySet().stream()
                .filter(runDel -> selectedchanges.getSelectedDels().contains(runDel))
                .toList();

        //Временно выбираются все удаления автоматически
        //List<BigInteger> selectedDels = ((DocInsertsAndDels)model.getAttribute("changes")).getDocDels().keySet().stream().toList();

        //получаем файл-разницу
        WordprocessingMLPackage changesPackage = WordprocessingMLPackage.load(storageService.loadLastChanges().toFile());
        //передаем в метод для очистки результата сравнения от ненужных изменений
        clearDocService.clearDocument(changesPackage, selectedInserts, selectedDels);

        storageService.storeResult(changesPackage);

        return "redirect:/uploadForm";
    }
}
