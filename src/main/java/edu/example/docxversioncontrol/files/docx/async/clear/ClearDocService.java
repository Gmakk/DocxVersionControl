package edu.example.docxversioncontrol.files.docx.async.clear;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ClearDocService {

    /**
     * Метод для очистки передаваемого файла от тегов-пометок об изменениях с учетом выбранных изменений
     * @param wordMLPackage Редактируемый файл
     * @param insertsIds Список id нужных добавлений
     * @param delsIds Список id нужных удалений
     */
    public void clearDocument(WordprocessingMLPackage wordMLPackage, List<BigInteger> insertsIds, List<BigInteger> delsIds) {
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
    }
}
