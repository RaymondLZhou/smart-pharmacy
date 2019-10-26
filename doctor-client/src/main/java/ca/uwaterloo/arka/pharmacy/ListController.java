package ca.uwaterloo.arka.pharmacy;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * The controller class for the patient list on the left side.
 */
public class ListController {
    
    @FXML private VBox patientList; // contains the graphical list of patients
    
    @FXML
    void addNewPatient() {
        // TODO
        System.out.println("[ListController] Adding new patient");
    }
    
}
