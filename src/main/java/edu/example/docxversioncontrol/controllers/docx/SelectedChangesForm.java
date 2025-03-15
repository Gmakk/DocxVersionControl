package edu.example.docxversioncontrol.controllers.docx;

import lombok.Data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Представляет собой выбранные пользователем изменения между двумя документами
 */
@Data
public class SelectedChangesForm {
    private List<BigInteger> selectedInserts = new ArrayList<>();
    private List<BigInteger> selectedDels = new ArrayList<>();
}
