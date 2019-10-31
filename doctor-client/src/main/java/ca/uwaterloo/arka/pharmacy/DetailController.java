package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.text.Text;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * The controller class for the "detail" pane on the right side. Controls editing and saving records.
 * Note: when we switch to editing mode, the old data is copied into the editing components, which are not bound
 * to anything, unlike the displaying components, which are bound to the UserRecords' fields. The editing components
 * then are modified and the old ones, along with the record, are saved upon hitting the save button and discarded
 * upon hitting the exit button.
 */
// TODO - a selection of doctors, also prescriptions rather than raw ID
// TODO - some save status indicator
// TODO - prevent having multiple prescriptions of same ID
public class DetailController extends PaneController {
    
    @FXML private Node detailPaneRoot;
    
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;
    @FXML private Button exitBtn;
    
    @FXML private Text nameText;
    @FXML private Text doctorsText;
    @FXML private Text prescriptionIdText;
    @FXML private Text faceFingerprintDataText;
    
    @FXML private TextField nameField;
    @FXML private Node doctorsListContainer;
    @FXML private ListView<String> doctorsList;
    @FXML private Node prescriptionIdListContainer;
    @FXML private ListView<Integer> prescriptionIdList;
    @FXML private TextField faceFingerprintDataField;
    
    private boolean editing = false;
    
    private UserRecord record = null;
    
    @FXML
    private void initialize() {
        // the ListViews need editable cell factories
        doctorsList.setCellFactory(TextFieldListCell.forListView());
        // TODO catch the NumberFormatException when the user tries to input a non-integer
        prescriptionIdList.setCellFactory(TextFieldListCell.forListView(new IntegerStringConverter()));
        
        // do a stupid thing so that the list views have the right height
        final int ITEM_HEIGHT = 26;
        doctorsList.prefHeightProperty().bind(Bindings.size(doctorsList.getItems())
                .multiply(ITEM_HEIGHT).add(ITEM_HEIGHT));
        prescriptionIdList.prefHeightProperty().bind(Bindings.size(prescriptionIdList.getItems())
                .multiply(ITEM_HEIGHT).add(ITEM_HEIGHT));
    }
    
    /**
     * Display and allow editing of the given record. Pass null in order to clear the display pane.
     */
    void displayRecord(UserRecord record) {
        if (this.record != null) {
            if (editing) {
                // can't be in edit mode now
                exit();
            }
            
            // Unbind everything from the last one
            nameText.textProperty().unbind();
            doctorsText.textProperty().unbind();
            prescriptionIdText.textProperty().unbind();
            faceFingerprintDataText.textProperty().unbind();
        }
        
        this.record = record;
        if (record == null) {
            // Null record = nothing, so invisible
            detailPaneRoot.setVisible(false);
            return;
        }
        
        // Bind all the fields
        nameText.textProperty().bind(record.nameProperty());
        faceFingerprintDataText.textProperty().bind(record.fingerprintProperty());
        
        doctorsText.textProperty().bind(Bindings.createStringBinding(
                () -> String.join(", ", record.getDoctors()), record.doctorsProperty()));
        prescriptionIdText.textProperty().bind(Bindings.createStringBinding(
                () -> record.getPrescriptionList().stream()
                        .map(prescription -> Integer.toString(prescription.getDin()))
                        .collect(Collectors.joining(", ")), record.prescriptionsProperty()));
        
        detailPaneRoot.setVisible(true);
    }
    
    @FXML
    public void edit() {
        if (record == null || editing) return;
        
        // copy the record stuff into the editing stuff
        nameField.setText(record.getName());
        faceFingerprintDataField.setText(record.getFingerprint());
        doctorsList.getItems().clear();
        // TODO have a better doctor choosing UI
        doctorsList.getItems().addAll(record.getDoctors());
        prescriptionIdList.getItems().clear();
        // TODO have a list of prescriptions to choose from
        prescriptionIdList.getItems().addAll(record.getPrescriptionList().stream()
                .map(UserRecord.PrescriptionRecord::getDin)
                .collect(Collectors.toList()));
        
        changeMode(true);
    }
    
    @FXML
    void addDoctor() {
        // TODO preserve doctor IDs
        doctorsList.getItems().add("New Doctor");
    }
    
    @FXML
    void removeSelectedDoctor() {
        if (!doctorsList.getSelectionModel().isEmpty()) {
            doctorsList.getItems().remove(doctorsList.getSelectionModel().getSelectedIndex());
        }
    }
    
    @FXML
    void addPrescription() {
        prescriptionIdList.getItems().add(0); // 0 is default prescription id
    }
    
    @FXML
    void removeSelectedPrescription() {
        if (!prescriptionIdList.getSelectionModel().isEmpty()) {
            prescriptionIdList.getItems().remove(prescriptionIdList.getSelectionModel().getSelectedIndex());
        }
    }
    
    @FXML
    private void exit() {
        if (record == null || !editing) return;
        changeMode(false);
    }
    
    private void changeMode(boolean edit) {
        if (edit == editing) return;
        editing = edit;
        
        // set all the displaying stuff to invisible if editing, visible if displaying
        nameText.setManaged(!edit);
        nameText.setVisible(!edit);
        doctorsText.setManaged(!edit);
        doctorsText.setVisible(!edit);
        prescriptionIdText.setManaged(!edit);
        prescriptionIdText.setVisible(!edit);
        faceFingerprintDataText.setManaged(!edit);
        faceFingerprintDataText.setVisible(!edit);
        editBtn.setManaged(!edit);
        editBtn.setVisible(!edit);
        deleteBtn.setManaged(!edit);
        deleteBtn.setVisible(!edit);
        
        // set all the editing stuff to visible if editing, invisible if displaying
        nameField.setManaged(edit);
        nameField.setVisible(edit);
        doctorsListContainer.setManaged(edit);
        doctorsListContainer.setVisible(edit);
        prescriptionIdListContainer.setManaged(edit);
        prescriptionIdListContainer.setVisible(edit);
        faceFingerprintDataField.setManaged(edit);
        faceFingerprintDataField.setVisible(edit);
        saveBtn.setManaged(edit);
        saveBtn.setVisible(edit);
        exitBtn.setManaged(edit);
        exitBtn.setVisible(edit);
    }
    
    @FXML
    private void delete() {
        if (record == null || editing) return;
        
        // are they sure?
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this user record?");
        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            // delete it - first do it in the database so we don't go out of sync
            try {
                UserDao dao = UserDao.newDao();
                dao.delete(record);
            } catch (IOException e) {
                System.err.println("[DetailController] Could not delete record with id " + record.id);
                e.printStackTrace();
                
                // tell the user
                Alert error = new Alert(Alert.AlertType.ERROR, "Could not delete user record.");
                error.show();
                
                return;
            }
            
            // now delete it from everything else
            getListController().removePatientCard(record);
            displayRecord(null);
        });
    }
    
    @FXML
    private void save() {
        if (record == null || !editing) return;
        
        // update the record (and therefore the displaying stuff) with the editing data
        record.setName(nameField.getText());
        record.setFingerprint(faceFingerprintDataField.getText());
        
        record.getDoctors().clear();
        for (String doctorName : doctorsList.getItems()) {
            record.getDoctors().add(doctorName);
        }
        
        record.getPrescriptionList().clear();
        for (int prescriptionId : prescriptionIdList.getItems()) {
            record.getPrescriptionList().add(new UserRecord.PrescriptionRecord(prescriptionId));
        }
        
        // Publish it
        try {
            UserDao dao = UserDao.newDao();
            dao.update(record);
        } catch (IOException e) {
            System.err.println("[DetailController] Could not update record of patient: " + record.getName());
            e.printStackTrace();
            
            // let the user know with an alert
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Could not publish changes to database. Your changes are saved locally but will reset upon " +
                    "restarting the application, and will not appear in the pharmacy machine. Click 'save' again to " +
                    "retry publishing changes.");
            alert.show();
        }
    }
    
}
