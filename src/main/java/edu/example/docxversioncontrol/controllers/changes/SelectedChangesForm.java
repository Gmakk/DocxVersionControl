package edu.example.docxversioncontrol.controllers.changes;

import lombok.Data;
import org.docx4j.wml.RunDel;
import org.docx4j.wml.RunIns;

import java.math.BigInteger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Представляет собой выбранные пользователем изменения между двумя документами
 */
@Data
public class SelectedChangesForm {
    private CopyOnWriteArrayList<BigInteger> selectedInserts = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<BigInteger> selectedDels = new CopyOnWriteArrayList<>();
}
