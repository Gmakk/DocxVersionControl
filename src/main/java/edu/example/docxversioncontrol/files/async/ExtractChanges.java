package edu.example.docxversioncontrol.files.async;

import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.RunDel;
import org.docx4j.wml.RunIns;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.RecursiveAction;


public class ExtractChanges extends RecursiveAction {
    //место для записи результата
    DocInsertsAndDels docInsertsAndDels;
    //текущий набор элементов, с которыми ведется работа
    List<Object> currentObjects;

    public ExtractChanges(ContentAccessor currentContentAccessor, DocInsertsAndDels docInsertsAndDels) {
        this.currentObjects = currentContentAccessor.getContent();
        this.docInsertsAndDels = docInsertsAndDels;
    }

    /**
     * Представляет собой задание для выполнения в одном потоке в ForkJoinPool
     */
    @Override
    protected void compute() {
        //проходим по телу, если изменение - записываем в map
        //если не изменение - запускаем процесс для этого дочерний
        if (currentObjects.isEmpty()) {
            return;
        }
        for (Object object : currentObjects) {
            if(object instanceof ContentAccessor) {//если объект является хранилищем других объектов
                //запускаем отдельный процесс
                ExtractChanges newExtract = new ExtractChanges((ContentAccessor) object, docInsertsAndDels);
                newExtract.fork();
            } else if (object instanceof RunIns) {//если объект содержит добавления
                //берем id объекта и кладем в map с изменениями
                docInsertsAndDels.getDocInserts().put(((RunIns)object).getId(),(RunIns)object);
            }else if (object instanceof RunDel) {//если объект содержит удаления
                //берем id объекта и кладем в map с изменениями
                docInsertsAndDels.getDocDels().put(((RunDel)object).getId(),(RunDel)object);
            }

        }
        //Для того, что бы вызывающий процесс дождался выполнения всех задач. В том числе созданных через .fork()
        RecursiveAction.helpQuiesce();
    }
}
