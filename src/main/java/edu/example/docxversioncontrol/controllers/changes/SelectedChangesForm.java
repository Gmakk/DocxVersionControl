package edu.example.docxversioncontrol.controllers.changes;

import lombok.Data;
import org.docx4j.wml.RunDel;
import org.docx4j.wml.RunIns;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Представляет собой выбранные пользователем изменения между двумя документами
 */
@Data
public class SelectedChangesForm {
    private List<BigInteger> selectedInserts = new ArrayList<>();
    private List<BigInteger> selectedDels = new ArrayList<>();
}
