package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import ca.uwaterloo.arka.pharmacy.db.UserRecord;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.converter.IntegerStringConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.IplImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;

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
    @FXML private ImageView fingerprintVisual;
    
    @FXML private TextField nameField;
    @FXML private Node doctorsListContainer;
    @FXML private ListView<String> doctorsList;
    @FXML private Node prescriptionIdListContainer;
    @FXML private ListView<Integer> prescriptionIdList;
    
    @FXML private Button captureFaceFingerprintButton;
    @FXML private Node captureFaceFingerprintBox;
    @FXML private Label captureInstructions;
    @FXML private ImageView cameraView;
    
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
        }
        
        this.record = record;
        if (record == null) {
            // Null record = nothing, so invisible
            detailPaneRoot.setVisible(false);
            return;
        }
        
        // Bind all the fields
        nameText.textProperty().bind(record.nameProperty());
        fingerprintVisual.imageProperty().bind(Bindings.createObjectBinding(
                () -> generateFaceFingerprintImage(deserializeFingerprint(record.getFingerprint())),
                record.fingerprintProperty()));
        
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
        fingerprintVisual.setManaged(!edit);
        fingerprintVisual.setVisible(!edit);
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
        captureFaceFingerprintBox.setManaged(edit);
        captureFaceFingerprintBox.setVisible(edit);
        saveBtn.setManaged(edit);
        saveBtn.setVisible(edit);
        exitBtn.setManaged(edit);
        exitBtn.setVisible(edit);
        
        // clear the camera view - it's only for currently getting stuff
        cameraView.setImage(null);
        cameraView.setFitWidth(400);
    }
    
    @FXML
    private void delete() {
        if (record == null || editing) return;
        
        // are they sure?
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this user record?");
        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            // delete it - first do it in the database so we don't go out of sync
            UserDao dao = UserDao.newDao();
            dao.delete(record, () -> {
                // now delete it from everything else
                getListController().removePatientCard(record);
                displayRecord(null);
            }, errMsg -> {
                System.err.println("[DetailController] Could not delete record with id " + record.id);
                System.err.println(errMsg);
                
                // tell the user
                Alert error = new Alert(Alert.AlertType.ERROR, "Could not delete user record.");
                error.show();
            });
        });
    }
    
    @FXML
    private void save() {
        if (record == null || !editing) return;
        
        // update the record (and therefore the displaying stuff) with the editing data
        record.setName(nameField.getText());
        
        record.getDoctors().clear();
        for (String doctorName : doctorsList.getItems()) {
            record.getDoctors().add(doctorName);
        }
        
        record.getPrescriptionList().clear();
        for (int prescriptionId : prescriptionIdList.getItems()) {
            record.getPrescriptionList().add(new UserRecord.PrescriptionRecord(prescriptionId));
        }
        
        // Publish it
        UserDao dao = UserDao.newDao();
        dao.update(record, () -> {}, errMsg -> {
            System.err.println("[DetailController] Could not update record of patient: " + record.getName());
            System.err.println(errMsg);

            // let the user know with an alert
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Could not publish changes to database. Your changes are saved locally but will reset upon " +
                    "restarting the application, and will not appear in the pharmacy machine. Click 'save' again to " +
                    "retry publishing changes.");
            alert.show();
        });
    }
    
    @FXML
    private void captureFaceFingerprint() {
        // hide the button while we're capturing the fingerprint
        captureFaceFingerprintButton.setManaged(false);
        captureFaceFingerprintButton.setVisible(false);
        updateInstructions("Capturing face fingerprint: please hold still...");
        
        Thread imageThread = new Thread(() -> {
            // new thread so it doesn't block the UI thread
            FrameGrabber grabber = new OpenCVFrameGrabber(0);
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            try {
                grabber.start();
                List<double[]> fingerprints = new ArrayList<>();
                while (fingerprints.size() <= 2 || tooMuchUncertainty(fingerprints)) {
                    Frame frame = grabber.grab();
                    IplImage image = converter.convert(frame);
                    
                    // save to the temp directory for the python script to access
                    String imgFilename = Paths.get(System.getProperty("java.io.tmpdir"), "face-fingerprint.png")
                            .toString();
                    System.out.println("[DetailController] Successfully got an image: saving to " + imgFilename);
                    cvSaveImage(imgFilename, image);
                    
                    // display the user's pretty face
                    cameraView.setImage(new Image("file://" + imgFilename));
                    
                    // execute the python script
                    String pythonCommand = "python3 ./fingerprint.py " + imgFilename;
                    Process pythonProcess = Runtime.getRuntime().exec(pythonCommand);
                    
                    // pipe error logging from python script to System.out
                    InputStream pythonError = pythonProcess.getErrorStream();
                    InputStreamReader isReader = new InputStreamReader(pythonError);
                    BufferedReader reader = new BufferedReader(isReader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[fingerprint.py] " + line);
                    }
                    
                    int exit = pythonProcess.waitFor();
                    System.out.println("[DetailController] Python script exited with status " + exit);
                    if (exit != 0) {
                        // crap it failed
                        Platform.runLater(() -> {
                            cameraView.setImage(null);
                            updateInstructions("Failed to generate a fingerprint.");
                            Alert error = new Alert(Alert.AlertType.ERROR,
                                "Error: Failed to generate fingerprint data. Please ensure that a face is visible " +
                                "to the webcam, and that Python 3.7 or above is installed.");
                            error.show();
                        });
                        return;
                    }
                    
                    // get the fingerprint from the output
                    InputStream pythonOutput = pythonProcess.getInputStream();
                    Scanner scanner = new Scanner(pythonOutput);
                    double[] fingerprint = new double[128];
                    for (int i = 0; i < 128; i++) {
                        fingerprint[i] = scanner.nextDouble();
                    }
                    scanner.close();
                    
                    fingerprints.add(fingerprint);
                    
                    Platform.runLater(() -> updateInstructions("Captured " + fingerprints.size()
                            + " sample(s), please hold still..."));
                    System.out.println("number of fingerprints kept = " + fingerprints.size());
                }
                double[] fingerprint = meanFingerprint(fingerprints);
                
                // use it as the fingerprint
                Platform.runLater(() -> {
                    System.out.println(Arrays.toString(fingerprint));
                    System.out.println(Arrays.toString(deserializeFingerprint(serializeFingerprint(fingerprint))));
                    setFingerprint(serializeFingerprint(fingerprint));
                    cameraView.setImage(generateFaceFingerprintImage(fingerprint));
                    cameraView.setFitWidth(100);
                    updateInstructions("Successfully generated face fingerprint, be sure to save.");
                });
            } catch (Exception e) {
                System.err.println("[DetailController] Could not get image from webcam");
                e.printStackTrace();
                Platform.runLater(() -> updateInstructions("Error getting an image from webcam."));
                Alert error = new Alert(Alert.AlertType.ERROR,
                    "Could not get an image from a webcam. Please ensure that a webcam is plugged in and " +
                    "this application has access to it, then try again.");
                error.show();
            } finally {
                Platform.runLater(() -> {
                    // always make it visible again
                    captureFaceFingerprintButton.setManaged(true);
                    captureFaceFingerprintButton.setVisible(true);
                });
                try {
                    grabber.stop();
                } catch (Exception e) {
                    System.err.println("[DetailController] Exception in stopping grabber");
                    e.printStackTrace();
                }
            }
        });
        imageThread.setDaemon(true);
        imageThread.start();
    }
    
    private void updateInstructions(String instructions) {
        captureInstructions.setText(instructions);
    }
    
    private void setFingerprint(String fingerprint) {
        record.setFingerprint(fingerprint);
    }
    
    private String serializeFingerprint(double[] fingerprint) {
        // map from [-1, 1] to [-2^15, 2^15 - 1]
        byte[] packed = new byte[256];
        final double scale = 32767.999999999996;
        for (int i = 0; i < 128; i++) {
            int mapped = (int) Math.floor(scale * fingerprint[i]);
            packed[2*i  ] = (byte) (mapped);
            packed[2*i+1] = (byte) (mapped>>8);
        }
        return Base64.getEncoder().encodeToString(packed);
    }
    
    private double[] deserializeFingerprint(String serialized) {
        // map from [-2^15, 2^15 - 1] to [-1, 1]
        byte[] packed = Base64.getDecoder().decode(serialized);
        double[] res = new double[128];
        final double scale = 32767.999999999996;
        for (int i = 0; i < 128; i++) {
            short bit16 = (short) ((int) packed[2*i] + ((int) packed[2*i+1]<<8));
            res[i] = (double) bit16 / scale;
        }
        return res;
    }
    
    private double[] meanFingerprint(List<double[]> fingerprints) {
        double[] result = new double[128];
        int n = fingerprints.size();
        if(n==0)return result;
        for (double[] f : fingerprints) {
            for (int j = 0; j < 128; ++j) {
                result[j] += f[j];
            }
        }
        for(int j=0;j<128;++j){
            result[j] /= n;
        }
        return result;
    }
    
    private boolean tooMuchUncertainty(List<double[]> fingerprints) {
        if (okUncertaintyPartial(fingerprints)) return false;
        int n = fingerprints.size();
        for (int i=0;i<n;++i) {
            List<double[]> cut = new ArrayList<>(fingerprints);
            //noinspection SuspiciousListRemoveInLoop
            cut.remove(i);
            if (okUncertaintyPartial(cut)) return false;
        }
        return true;
    }
    
    private boolean okUncertaintyPartial(List<double[]> fingerprints) {
        double variance = 0;
        int n = fingerprints.size();
        for(int j=0;j<128;++j) {
            double sum = 0;
            double sumsq = 0;
            for (double[] fingerprint : fingerprints) {
                double v = fingerprint[j];
                sum += v;
                sumsq += v * v;
            }
            sum /= n;
            sumsq /= n;
            sum = sum * sum;
            double contrib = sumsq - sum;
            variance += contrib;
        }
        System.out.println("variance = " + variance);
        double bound = 0.01 + 0.001 * n;
        return variance <= bound;
    }
    
    private Image generateFaceFingerprintImage(double[] fingerprint) {
        int scale = 16;
        BufferedImage image = new BufferedImage(8*scale, 8*scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D)image.getGraphics();
        for (int i = 0; i < 64; i++) {
            int x = i % 8;
            int y = i / 8;
            double u = fingerprint[2*i  ];
            double v = fingerprint[2*i+1];
            u = Math.tanh(u*10); // exaggerate extreme values
            v = Math.tanh(v*10);
            final double R = 0.3, G = 0.15, B = 0.5;
            int r = (int) (127 + 127 * R * u);
            int g = (int) (127 + 127 * G * v);
            int b = (int) (127 + 127 * B * (-u-v));
            System.out.println("RGB for " + x + ", " + y + ": " + r + ", " + g + ", " + b);
            graphics.setColor(new Color(r, g, b));
            graphics.fillRect(x*scale, y*scale, scale, scale);
        }
        return SwingFXUtils.toFXImage(image, null);
    }
    
}
