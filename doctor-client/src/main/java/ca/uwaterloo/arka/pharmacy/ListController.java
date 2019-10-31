package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
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
        
        // Get a DAO
        UserDao dao;
        try {
            dao = UserDao.newDao();
        } catch (IOException e) {
            System.err.println("[ListController] Could not access database: " + search);
            e.printStackTrace();
            
            Alert error = new Alert(Alert.AlertType.ERROR, "Error: could not access database");
            error.show();
            
            return;
        }
        
        // Search for the records
        patientList.getChildren().clear(); // straight up assuming it'll work
        dao.searchByName(search, record -> {
            PatientCard card = new PatientCard(record);
            addPatient(card);
        }, error -> {
            System.err.println("[ListController] Could not search for string '" + search + "'");
            System.err.println("Error: " + error);
            Alert err = new Alert(Alert.AlertType.ERROR, "Error: could not retrieve search for '" + search + '"');
            err.show();
        });
    }
    
    private void getAllUsersFromDatabase() { // TODO combine this with the method above it
        // Get all the users
        UserDao dao;
        try {
            dao = UserDao.newDao();
        } catch (IOException e) {
            System.err.println("[ListController] Could not initialize DAO");
            e.printStackTrace();
            
            Alert error = new Alert(Alert.AlertType.ERROR, "Error: could not connect to database");
            error.show();
            
            return;
        }
        
        // Get them
        patientList.getChildren().clear(); // straight up assuming it'll work
        dao.getAllSortedAlphabetically(record -> {
            PatientCard card = new PatientCard(record);
            addPatient(card);
        }, error -> {
            System.err.println("[ListController] Could not retrieve user records");
            System.err.println("Error: " + error);
            Alert err = new Alert(Alert.AlertType.ERROR, "Error: could not retrieve patients from database");
            err.show();
        });
    }
    
    @FXML
    private void addNewPatient() {
        // strategy: make new record, save it immediately, send it to detail pane already open to edit
        UUID uuid = UUID.randomUUID();
        UserRecord newRecord = new UserRecord(uuid.hashCode(), "New User", new ArrayList<>(), new ArrayList<>(), "");
        
        // gotta save it before we can edit it
        try {
            UserDao dao = UserDao.newDao();
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
