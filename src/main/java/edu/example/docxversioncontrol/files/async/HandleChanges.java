package edu.example.docxversioncontrol.files.async;

import org.docx4j.wml.ContentAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
public class HandleChanges {

    private final static Logger log = LoggerFactory.getLogger(HandleChanges.class);

    /**
     * Асинхронно получает изменения из документа, возвращает результат только после конца работы всех потоков
     * @param documentBody Тело документа или любой другой {@link ContentAccessor}
     * @return объект с 2 map, в которых находится информация о добавлениях и удалениях из документа
     */
    public static DocInsertsAndDels getDocumentChanges(Object documentBody){
        //хранилище изменений
        DocInsertsAndDels docInsertsAndDels = new DocInsertsAndDels();
        //первое задание, которому передается все тело документа
        ExtractChanges extractChanges = new ExtractChanges((ContentAccessor)documentBody, docInsertsAndDels);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        //запуск первого задания
        forkJoinPool.execute(extractChanges);
        //ожидание, пока все не отработает
        if(forkJoinPool.awaitQuiescence(10, TimeUnit.SECONDS))
            log.info("ForkJoinPool has completed its work correctly");
        else
            log.error("ForkJoinPool failed to finish its work in 10 seconds");
        return docInsertsAndDels;
    }
}
