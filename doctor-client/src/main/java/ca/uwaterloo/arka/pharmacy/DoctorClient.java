package ca.uwaterloo.arka.pharmacy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The main class. Boots JavaFX and initializes the main scene. Performs other application-start setup.
 */
public class DoctorClient extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
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
