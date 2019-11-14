package ca.uwaterloo.arka.pharmacy;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;

/**
 * The controller for the main scene. UI is defined in MainScene.fxml. Exists mostly to connect the list and detail
 * pane controllers.
 */
public class MainController {
    
    @FXML private Node rootNode;
    
    // Controller classes for the two panes
    @FXML private ListController listPaneController;
    @FXML private DetailController detailPaneController;
    
    @FXML
    private void initialize() {
        listPaneController.setControllers(listPaneController, detailPaneController);
        detailPaneController.setControllers(listPaneController, detailPaneController);
        
        // set everything unfocused by default
        Platform.runLater(() -> rootNode.requestFocus());
    }
    
}
