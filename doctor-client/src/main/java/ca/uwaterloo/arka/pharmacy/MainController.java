package ca.uwaterloo.arka.pharmacy;

import javafx.fxml.FXML;

/**
 * The controller for the main scene. UI is defined in MainScene.fxml.
 */
public class MainController {
    
    // Controller classes for the two panes
    @FXML private ListController listPaneController;
    @FXML private DetailController detailPaneController;
    
    public ListController getListPaneController() {
        return listPaneController;
    }
    
    public DetailController getDetailPaneController() {
        return detailPaneController;
    }
    
}
