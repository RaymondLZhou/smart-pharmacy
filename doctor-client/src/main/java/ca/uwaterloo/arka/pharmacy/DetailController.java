package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.stream.Collectors;

/**
 * The controller class for the "detail" pane on the right side.
 */
public class DetailController extends PaneController {
    
    @FXML private Node detailPaneRoot;
    
    @FXML private Text nameText;
    @FXML private Text doctorsText;
    @FXML private Text prescriptionIdText;
    @FXML private Text faceFingerprintDataText;
    
    private UserRecord record = null;
    
    /**
     * Display and allow editing of the given record. Pass null in order to clear the display pane.
     */
    public void displayRecord(UserRecord record) {
        if (this.record != null) {
            // Unbind everything from the last one
            nameText.textProperty().unbindBidirectional(this.record.nameProperty());
            faceFingerprintDataText.textProperty().unbindBidirectional(this.record.getFingerprint().dataProperty());
            doctorsText.textProperty().unbind();
            prescriptionIdText.textProperty().unbind();
        }
        
        this.record = record;
        if (record == null) {
            // Null record = nothing, so invisible
            detailPaneRoot.setVisible(false);
            return;
        }
        
        // Bind all the fields
        nameText.textProperty().bindBidirectional(record.nameProperty());
        faceFingerprintDataText.textProperty().bindBidirectional(record.getFingerprint().dataProperty());
        
        doctorsText.textProperty().bind(Bindings.createStringBinding(
                () -> record.getDoctors().stream()
                        .map(UserRecord.DoctorRecord::getName)
                        .collect(Collectors.joining(", "))));
        prescriptionIdText.textProperty().bind(Bindings.createStringBinding(
                () -> record.getPrescriptions().stream()
                        .map(prescription -> Integer.toString(prescription.id))
                        .collect(Collectors.joining(", "))));

        detailPaneRoot.setVisible(true);
    }
    
    @FXML
    private void edit() {
        if (record == null) return;
        // TODO
    }
    
    @FXML
    private void delete() {
        if (record == null) return;
        // TODO
    }
    
}
