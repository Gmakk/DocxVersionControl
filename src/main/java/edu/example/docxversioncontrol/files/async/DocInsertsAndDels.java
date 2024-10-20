package edu.example.docxversioncontrol.files.async;

import lombok.Data;
import org.docx4j.wml.CTTrackChange;
import org.docx4j.wml.RunDel;
import org.docx4j.wml.RunIns;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Представляет собой набор элементов добавленных и удаленных в новой версии документа
 */
@Data
public class DocInsertsAndDels {
    //ConcurrentSkipListMap, так как элементы будут добавляться из нескольких потоков и должны сохранять порядок как в документе
    private Map<BigInteger, RunIns> docInserts = new ConcurrentSkipListMap<>();    //Id - document tag
    private Map<BigInteger, RunDel> docDels = new ConcurrentSkipListMap<>();
}
