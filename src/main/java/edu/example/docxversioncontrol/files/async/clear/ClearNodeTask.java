package edu.example.docxversioncontrol.files.async.clear;

import edu.example.docxversioncontrol.files.async.extract.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.async.extract.ExtractChangesTask;
import org.docx4j.wml.Body;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.RunDel;
import org.docx4j.wml.RunIns;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveAction;

public class ClearNodeTask extends RecursiveAction {

    //текущий набор элементов, с которыми ведется работа
    ContentAccessor currentContentAccessor;
    //Список добавлений, которые нужно оставить в файле
    List<BigInteger> insertsIds;
    //Список добавлений, которые нужно оставить в файле
    List<BigInteger> delsIds;


    public ClearNodeTask(ContentAccessor currentContentAccessor, List<BigInteger> insertsIds, List<BigInteger> delsIds) {
        this.currentContentAccessor = currentContentAccessor;
        this.insertsIds = insertsIds;
        this.delsIds = delsIds;
    }

    /**
     * Редактирует CTTrackChange теги на основании их выбора пользователем
     * Представляет собой задание для выполнения в одном потоке в ForkJoinPool.
     */
    @Override
    protected void compute() {
//        выбранные:
//          инсерты:
//              убираем оболочку инсерта
//          делиты:
//              удаляем целиком

//        ненужные:
//          инсерты:
//              удаляем целиком
//          делиты:
//              убираем оболочку из делита
        if (currentContentAccessor.getContent().isEmpty()) {
            return;
        }
        List<Object> listToDelete = new ArrayList<>();
        Map<Integer,List<Object>> replaceMap = new HashMap<>();
        for (Object object : currentContentAccessor.getContent()) {
            //System.out.println("Extracting " + object);
            if(object instanceof ContentAccessor) {//если объект является хранилищем других объектов
                //запускаем отдельный процесс
                ClearNodeTask newClear = new ClearNodeTask((ContentAccessor) object, insertsIds, delsIds);
                newClear.fork();
            } else if (object instanceof RunIns) {//если объект содержит добавление
                //если это нужное добавление
                if(insertsIds.contains(((RunIns) object).getId())){//убираем пометку о добавлении, чтобы она не отображалась в документе
                    //содержание родительского массива элементов
                    List<Object> content = ((ContentAccessor)((RunIns) object).getParent()).getContent();
                    //индекс добавления в родительском массиве элементов
                    Integer index = content.indexOf(object);
                    //содержимое RunIns
                    List<Object> replacements = ((RunIns) object).getCustomXmlOrSmartTagOrSdt();
                    //кладем в map для последующего замещения
                    replaceMap.put(index,replacements);
                }else //если добавление ненужное
                    listToDelete.add(object);
            }else if (object instanceof RunDel) {//если объект содержит удаление
                //если это ненужное удаление
                if(!delsIds.contains(((RunDel) object).getId())){//убираем пометку об удалении, чтобы она не отображалась в документе
                    //содержание родительского массива элементов
                    List<Object> content = ((ContentAccessor)((RunDel) object).getParent()).getContent();
                    //индекс удаления в родительском массиве элементов
                    Integer index = content.indexOf(object);
                    //содержимое RunDel
                    List<Object> replacements = ((RunIns) object).getCustomXmlOrSmartTagOrSdt();
                    //кладем в map для последующего замещения
                    replaceMap.put(index,replacements);
                }else//если удаление нужное
                    listToDelete.add(object);
            }
        }

        System.out.println(replaceMap.keySet().size());
        for (Integer index : replaceMap.keySet()) {
            currentContentAccessor.getContent().remove(index);
            currentContentAccessor.getContent().addAll(index, replaceMap.get(index));
        }
        System.out.println(listToDelete.size());
        if(!listToDelete.isEmpty()) {
            currentContentAccessor.getContent().removeAll(listToDelete);
        }
        //Для того, что бы вызывающий процесс дождался выполнения всех задач. В том числе созданных через .fork()
        RecursiveAction.helpQuiesce();
    }
}
