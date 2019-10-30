package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The controller class for the patient list on the left side.
 */
// TODO some form of pagination
public class ListController extends PaneController {
    
    @FXML private VBox patientList; // contains the graphical list of patients
    
    @FXML private TextField searchField;
    
    @FXML
    void initialize() {
        getAllUsersFromDatabase();
        
        // setup searching
        searchField.textProperty().addListener(((observable, oldValue, newValue) -> searchForUsers(newValue)));
    }
    
    private void searchForUsers(String search) { // null or empty or blank for all users
        // search for users with that name and set that as the list box
        if (search == null || search.isBlank()) {
            getAllUsersFromDatabase();
            return;
        }
        
        // Get the records
        UserDao dao = UserDao.newDao();
        List<UserRecord> records;
        try {
            records = dao.searchByName(search);
        } catch (IOException e) {
            System.err.println("[ListController] Could not search for users by string: " + search);
            e.printStackTrace();
            
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "Error: could not access database (failed to search for '" + search + "')");
            error.show();
            
            return;
        }
        
        // Clear the box and display them
        patientList.getChildren().clear();
        for (UserRecord record : records) {
            PatientCard card = new PatientCard(record);
            addPatient(card);
        }
    }
    
    private void getAllUsersFromDatabase() {
        // Get all the users with the annoying pagination thing
        UserDao dao = UserDao.newDao();
        
        int numPages;
        try {
            numPages = dao.getNumPages();
        } catch (IOException e) {
            System.err.println("[ListController] Could not get number of pages");
            e.printStackTrace();

            // Add an error message to the patient list box
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "Error: could not access database (failed to get number of pages)");
            error.show();

            return;
        }

        // Fetch all the records
        List<UserRecord> allRecords = new ArrayList<>();
        for (int p = 0; p < numPages; ++p) {
            List<UserRecord> records;
            try {
                records = dao.getAllSortedAlphabetically(p);
                allRecords.addAll(records);
            } catch (IOException e) {
                System.err.println("[ListController] Could not get records:");
                e.printStackTrace();

                // Another informative error message
                Alert error = new Alert(Alert.AlertType.ERROR,
                        "Error: could not access database (failed to get patient records)");
                error.show();
                
                break;
            }
        }
        
        // Clear the list and add all the new ones
        patientList.getChildren().clear();
        
        for (UserRecord record : allRecords) {
            PatientCard card = new PatientCard(record);
            addPatient(card);
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
