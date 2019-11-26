package ca.uwaterloo.arka.pharmacy;

/**
 * The superclass of DetailController and ListController. Exists to facilitate interactions between the two.
 */
class PaneController {
    
    private ListController listController = null;
    private DetailController detailController = null;
    
    void setControllers(ListController listController, DetailController detailController) {
        this.listController = listController;
        this.detailController = detailController;
    }
    
    ListController getListController() {
        return listController;
    }
    
    DetailController getDetailController() {
        return detailController;
    }
    
}
