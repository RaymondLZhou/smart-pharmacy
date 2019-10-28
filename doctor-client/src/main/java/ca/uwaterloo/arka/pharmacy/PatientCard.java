package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

/**
 * A custom component representing the patient cards displayed in the list box. Also the controller of that component.
 */
public class PatientCard extends Pane {
    
    private UserRecord record;
    
    @FXML private Text patientNameText;
    @FXML private Text doctorsNamesText;
    
    public PatientCard(UserRecord record) {
        if (record == null) throw new NullPointerException("cannot have null record");
        this.record = record;
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PatientCard.fxml"));
        fxmlLoader.setController(this);
        try {
            Node view = fxmlLoader.load(); // triggers us as the controller, initialize() will be called when it's ready
            getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }
    
    @FXML
    public void initialize() {
        patientNameText.textProperty().bind(record.nameProperty());
        doctorsNamesText.textProperty().bind(Bindings.createStringBinding(
                () -> record.getDoctors().stream()
                        .map(UserRecord.DoctorRecord::getName)
                        .collect(Collectors.joining(", "))));
    }
    
    public UserRecord getRecord() {
        return record;
    }
    
}
