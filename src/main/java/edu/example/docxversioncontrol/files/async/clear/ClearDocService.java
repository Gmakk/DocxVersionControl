package edu.example.docxversioncontrol.files.async.clear;

import edu.example.docxversioncontrol.files.async.extract.ExtractChangesTask;
import edu.example.docxversioncontrol.files.async.extract.HandleChangesService;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.docx4j.wml.ContentAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
public class ClearDocService {

    private final static Logger log = LoggerFactory.getLogger(ClearDocService.class);
    //принимать документ-сравнение и его тело в аргументы метода!!!!!!!!!!!

    //получаем от файловой системы первый файл
    //запускаем процесс, передаем список нужных изменений, список объектов из тела изменений, само тело(из которого удаляются ненужные)
    //ставим новое тело с изменениями старому файлу
    //сохраняем файл
    public static void clearDocument(WordprocessingMLPackage wordMLPackage, List<BigInteger> insertsIds, List<BigInteger> delsIds) {
        //первое задание, которому передается все тело документа
        Body documentBody =  wordMLPackage.getMainDocumentPart().getJaxbElement().getBody();
        ClearNodeTask clearNodeTask = new ClearNodeTask(documentBody, insertsIds, delsIds);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        //запуск первого задания
        forkJoinPool.execute(clearNodeTask);
        //ожидание, пока все не отработает
        if(forkJoinPool.awaitQuiescence(10, TimeUnit.SECONDS))
            log.info("ForkJoinPool has completed its work correctly");
        else
            log.error("ForkJoinPool failed to finish its work in 10 seconds");
        try {
            wordMLPackage.save(new File(System.getProperty("user.dir") + "/last_changes.docx"));
        }catch (Docx4JException e){
            log.error("Failed to save merged file");
            e.printStackTrace();
        }
    }
}
