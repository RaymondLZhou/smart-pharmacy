package ca.uwaterloo.arka.pharmacy;

import ca.uwaterloo.arka.pharmacy.db.UserDao;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The main class. Boots JavaFX, initializes the main scene, and initializes database connection.
 */
public class DoctorClient extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize the database connection
        UserDao dao = UserDao.newDao();
        try {
            dao.initialize();
        } catch (IOException e) {
            // this is bad - can't access database which is the whole point!
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "Error: could not access database. Check your internet connection and relaunch the application. " +
                    "This application will shut down.");
            error.showAndWait();
            throw e; // don't do anything
        }
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScene.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Doctor's cabinet - Smart Pharmacy");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
}
