package pollinatorconservation.views;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import pollinatorconservation.model.dao.CladeDAO;
import pollinatorconservation.model.dao.FamilyDAO;
import pollinatorconservation.model.dao.OrderDAO;
import pollinatorconservation.model.dao.PollinatorDAO;
import pollinatorconservation.model.pojo.Family;
import pollinatorconservation.model.pojo.Order;
import pollinatorconservation.model.pojo.Pollinator;
import pollinatorconservation.util.Constants;
import pollinatorconservation.util.Utilities;

public class FXMLPollinatorController implements Initializable {

    @FXML
    private TextField scientificNameTextField;
    @FXML
    private TextField genericNameTextField;
    @FXML
    private TextArea descriptionTextArea;
    @FXML
    private ComboBox<Order> orderComboBox;
    @FXML
    private ComboBox<Family> familyComboBox;
    @FXML
    private ImageView imageView;

    private int typeOfViewToConfigure;

    private ObservableList<Order> orders;
    private ObservableList<Family> families;

    private File pollinatortImageFile;
    private Image pollinatorImage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            loadClades();
        } catch (SQLException exception) {
            Utilities.showAlert("No hay conexión con la base de datos.\n\nPor favor, inténtelo más tarde.\n",
                    Alert.AlertType.ERROR);
        }
    }

    public void configureView(int typeOfViewToConfigure, Pollinator pollinator) {
        this.typeOfViewToConfigure = typeOfViewToConfigure;
        if (typeOfViewToConfigure == Constants.QUERY_WINDOW_CODE) {
            pollinatortImageFile = new File("src/pollinatorconservation/images/" + getPollinatorImageName(pollinator) + ".jpg");
            imageView.setOnMouseClicked(null);
        } else {
            pollinatortImageFile = new File("src/pollinatorconservation/images/default.png");
        }
        pollinatorImage = new Image(pollinatortImageFile.toURI().toString());
        imageView.setImage(pollinatorImage);
    }

    @FXML
    private void addPollinatorImageClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen de referencia de la especie polinizadora.");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Formato de intercambio de archivos JPEG (*.jpg, *.jpeg)",
                "*.jpg", "*.jpeg");
        fileChooser.getExtensionFilters().add(extensionFilter);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        pollinatortImageFile = fileChooser.showOpenDialog(stage);
        loadPollinatorImage();
    }

    @FXML
    private void acceptButtonClick(ActionEvent event) throws IOException, SQLException {
        if (!validateEmptyFields()) {
            Pollinator pollinator = new Pollinator();
            pollinator.setScientificName(scientificNameTextField.getText());
            pollinator.setGenericName(genericNameTextField.getText());
            pollinator.setDescription(descriptionTextArea.getText());
            pollinator.setFamily(familyComboBox.getValue());
            if (typeOfViewToConfigure == Constants.REGISTRATION_WINDOW_CODE) {
                registerPollinator(pollinator);
            } else {
                // PENDING.
            }
        } else {
            Utilities.showAlert("No se puede dejar ningún campo vacío.\n\n"
                    + "Por favor, compruebe la información ingresada e inténtelo nuevamente.\n",
                    Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void cancelButtonClick(ActionEvent event) {
        closePopUpWindow();
    }

    private void registerPollinator(Pollinator pollinator) throws SQLException, IOException {
        int responseCode = PollinatorDAO.registerPollinator(pollinator);
        switch (responseCode) {
            case Constants.CORRECT_OPERATION_CODE:
                Utilities.showAlert("La información se registró correctamente en el sistema.\n",
                        Alert.AlertType.INFORMATION);
                registerPollinatorImage(pollinator);
                closePopUpWindow();
                break;
            case Constants.SPECIES_ALREADY_REGISTERED:
                Utilities.showAlert("La información ingresada corresponde a una especie polinizadora que ya se encuentra registrada en el sistema.\n\n"
                        + "Por favor, compruebe la información ingresada e inténtelo nuevamente.\n",
                        Alert.AlertType.WARNING);
                break;
            default:
                Utilities.showAlert("No hay conexión con la base de datos.\n\n"
                        + "Por favor, inténtelo más tarde.\n",
                        Alert.AlertType.ERROR);
                break;
        }
    }

    private void registerPollinatorImage(Pollinator pollinator) throws IOException {
        File file = new File("src/pollinatorconservation/images/pollinator/"
                + getPollinatorImageName(pollinator) + ".jpg");
        file.delete();
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(pollinatorImage, null);
        ImageIO.write(bufferedImage, "jpg", file);
    }

    private void loadPollinatorImage() {
        if (pollinatortImageFile != null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(pollinatortImageFile);
                pollinatorImage = SwingFXUtils.toFXImage(bufferedImage, null);
                imageView.setImage(pollinatorImage);
            } catch (IOException exception) {
                System.err.println("Error loading flowering plant image...");
            }
        }
    }

    private void loadClades() throws SQLException {
        orders = FXCollections.observableArrayList();
        ArrayList<Order> query = OrderDAO.getOrders();
        orders.addAll(query);
        orderComboBox.setItems(orders);
        orderComboBox.valueProperty().addListener(new ChangeListener<Order>() {
            @Override
            public void changed(ObservableValue<? extends Order> observable, Order oldValue, Order newValue) {
                if (newValue != null) {
                    try {
                        loadFamiliesByOrder(orderComboBox.getValue());
                    } catch (SQLException ex) {
                        Utilities.showAlert("No hay conexión con la base de datos.\n\n"
                                + "Por favor, inténtelo más tarde.\n",
                                Alert.AlertType.ERROR);
                    }
                }
            }
        });
    }

    private void loadFamiliesByOrder(Order order) throws SQLException {
        families = FXCollections.observableArrayList();
        ArrayList<Family> query = FamilyDAO.getFamiliesByOrder(order);
        families.addAll(query);
        familyComboBox.setItems(families);
    }

    private boolean validateEmptyFields() {
        return (scientificNameTextField.getText().isEmpty()
                || genericNameTextField.getText().isEmpty()
                || descriptionTextArea.getText().isEmpty()
                || orderComboBox.getSelectionModel().isEmpty()
                || familyComboBox.getSelectionModel().isEmpty())
                || pollinatortImageFile.getName().equals("default.png");
    }

    private String getPollinatorImageName(Pollinator pollinator) {
        return pollinator.getScientificName().toLowerCase().replaceAll("\\s", "");
    }

    private void closePopUpWindow() {
        Stage stage = (Stage) scientificNameTextField.getScene().getWindow();
        stage.close();
    }

}