package edu.example.docxversioncontrol.controllers.changes;

import jakarta.xml.bind.JAXBElement;
import org.docx4j.wml.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChangeFormatterService {
    public String extractTextFromChange(CTTrackChange change) {
        List<Object> content;
        if (change instanceof RunIns) {
            content = ((RunIns) change).getCustomXmlOrSmartTagOrSdt();
        } else {
            content = ((RunDel) change).getCustomXmlOrSmartTagOrSdt();
        }

        for (Object object : content) {
            if (object instanceof R) {
                Object runContent = ((R) object).getContent().get(0);
                String innerText;
                if(runContent instanceof JAXBElement<?>) {
                    innerText = ((Text)((JAXBElement<?>) runContent).getValue()).getValue();
                }else if(runContent instanceof Text)
                    innerText = ((Text)runContent).getValue();
                else //DelText
                    innerText = ((DelText)runContent).getValue();
                return innerText;
            }
        }
        return "";
    }
}
