package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        // strategy: make new record, save it immediately, send it to detail pane already open to edit
        UUID uuid = UUID.randomUUID();
        UserRecord newRecord = new UserRecord(uuid.hashCode(), "New User", new ArrayList<>(), new ArrayList<>(),
                new UserRecord.FaceFingerprintRecord(""));
        
        // gotta save it before we can edit it
        UserDao dao = UserDao.newDao();
        try {
            dao.create(newRecord);
        } catch (IOException e) {
            System.err.println("[ListController] Could not create new user record");
            e.printStackTrace();
            
            Alert error = new Alert(Alert.AlertType.ERROR, "Could not register a new user with the database.");
            error.show();
            
            return;
        }
        
        // we're good
        PatientCard newCard = new PatientCard(newRecord);
        addPatient(newCard);
        
        getDetailController().displayRecord(newRecord);
        getDetailController().edit();
    }
    
    /** Remove the card with the following record */
    void removePatientCard(UserRecord recordToRemove) {
        if (recordToRemove == null) throw new NullPointerException("cannot remove null card");
        for (int i = 0; i < patientList.getChildren().size(); ++i) {
            PatientCard card = (PatientCard) patientList.getChildren().get(i);
            if (card.getRecord().id == recordToRemove.id) {
                patientList.getChildren().remove(i);
                break;
            }
        }
    }
    
    private void addPatient(PatientCard card) {
        card.getView().setOnMouseClicked(e -> getDetailController().displayRecord(card.getRecord()));
        patientList.getChildren().add(card);
    }
    
}
