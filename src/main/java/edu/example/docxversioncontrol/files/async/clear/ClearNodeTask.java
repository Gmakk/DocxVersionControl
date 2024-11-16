package edu.example.docxversioncontrol.files.async.clear;

import edu.example.docxversioncontrol.files.async.extract.DocInsertsAndDels;
import edu.example.docxversioncontrol.files.async.extract.ExtractChangesTask;
import org.docx4j.wml.*;

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
        System.out.println("ClearNodeTask start: " + this);
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
            //System.out.println(object);
            //ЛОМАЕТСЯ, ЕСЛИ УКАЗЫВАТЬ НЕ ВСЕ УДАЛЕНИЯ КАК НУЖНЫЕ

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
                    System.out.println("Нужное добавление: " + index);
                    //содержимое RunIns
                    List<Object> replacements = ((RunIns) object).getCustomXmlOrSmartTagOrSdt();
                    //кладем в map для последующего замещения
                    replaceMap.put(index,replacements);
                }else { //если добавление ненужное
                    listToDelete.add(object);
                    System.out.println("Ненужное добавление: " + object);

                }
            }else if (object instanceof RunDel) {//если объект содержит удаление
                //если это ненужное удаление
                if(!delsIds.contains(((RunDel) object).getId())){//убираем пометку об удалении, чтобы она не отображалась в документе
                    //содержание родительского массива элементов
                    List<Object> content = ((ContentAccessor)((RunDel) object).getParent()).getContent();
                    //индекс удаления в родительском массиве элементов
                    Integer index = content.indexOf(object);
                    System.out.println("Ненужное удаление: " + index);
                    //содержимое RunDel
                    List<Object> replacements = ((RunDel) object).getCustomXmlOrSmartTagOrSdt();//TODO: ломается здесь
                    //кладем в map для последующего замещения
                    replaceMap.put(index,replacements);
                }else {//если удаление нужное
                    listToDelete.add(object);
                    System.out.println("Нужное удаление: " + object);

                }
            }
        }


        Integer offset = 0; //тк, при замене элементов их содержимым, эти индексы смещаются, то это нужно учитывать при подстановке последующих
        for (Integer index : replaceMap.keySet()) {
            //избавляемся от артефактов удаления в виде DelText
//            replaceMap.get(index).replaceAll(object -> {
//                if(object instanceof DelText) {
//                    Text text = ObjectFactory.get().createText();
//                    text.setValue(((DelText) object).getValue());
//                    return text;
//                }
//                return object;
//                //else if(object instanceof InsText) {}   ??????????
//            });



            currentContentAccessor.getContent().remove(currentContentAccessor.getContent().get(index + offset));
            currentContentAccessor.getContent().addAll(index + offset, replaceMap.get(index));//TODO: Распаковка происходит на один элемент в перед
            offset += replaceMap.get(index).size() - 1;//-1 тк сам элемент убираем
        }
        if(!listToDelete.isEmpty()) {
            currentContentAccessor.getContent().removeAll(listToDelete);
        }
        //Для того, что бы вызывающий процесс дождался выполнения всех задач. В том числе созданных через .fork()
        RecursiveAction.helpQuiesce();
    }
}
