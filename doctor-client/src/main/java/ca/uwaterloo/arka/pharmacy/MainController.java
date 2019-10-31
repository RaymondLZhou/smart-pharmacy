package ca.uwaterloo.arka.pharmacy;

import javafx.fxml.FXML;

/**
 * The controller for the main scene. UI is defined in MainScene.fxml. Exists mostly to connect the list and detail
 * pane controllers.
 */
public class MainController {
    
    // Controller classes for the two panes
    @FXML private ListController listPaneController;
    @FXML private DetailController detailPaneController;
    
    @FXML
    private void initialize() {
        listPaneController.setControllers(listPaneController, detailPaneController);
        detailPaneController.setControllers(listPaneController, detailPaneController);
    }
    
}
