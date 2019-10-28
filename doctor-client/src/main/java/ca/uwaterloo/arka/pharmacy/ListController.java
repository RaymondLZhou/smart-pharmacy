package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.List;

/**
 * The controller class for the patient list on the left side.
 */
public class ListController extends PaneController {
    
    @FXML private VBox patientList; // contains the graphical list of patients
    
    @FXML
    void initialize() {
        // Add all the users from the database
        UserDao dao = UserDao.newDao();
        
        int numPages;
        try {
            numPages = dao.getNumPages();
        } catch (IOException e) {
            System.err.println("[ListController] Could not get number of pages:");
            e.printStackTrace();
            
            // Add an error message to the patient list box
            Text error = new Text("Error: could not access database (failed to get number of pages)");
            patientList.getChildren().add(error);
            
            return;
        }
        
        // Fetch all the records and add them as patient cards to the list
        for (int p = 0; p < numPages; ++p) {
            List<UserRecord> records;
            try {
                records = dao.getAllSortedAlphabetically(p);
            } catch (IOException e) {
                System.err.println("[ListController] Could not get records:");
                e.printStackTrace();
                // Another informative error message
                Text error = new Text("Error: could not access database (failed to get patient records)");
                patientList.getChildren().add(error);
                continue;
            }
            
            for (UserRecord record : records) {
                PatientCard card = new PatientCard(record);
                addPatient(card);
            }
        }
    }
    
    @FXML
    private void addNewPatient() {
        // TODO
        System.out.println("[ListController] Adding new patient");
    }
    
    private void addPatient(PatientCard card) {
        card.getView().setOnMouseClicked(e -> getDetailController().displayRecord(card.getRecord()));
        patientList.getChildren().add(card);
    }
    
}
