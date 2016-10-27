package client;

import common.ClientCredentials;
import common.Server;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ResourceBundle;

public class LoginController extends Controller {
    private Client client;
    private Server server;

    private Stage loginDialogStage;

    @FXML public TextField usernameTextField;
    @FXML public PasswordField passwordField;
    @FXML public Button loginButton;
    @FXML public Button registerButton;
    @FXML public Button cancelButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        //Login and register buttons are locked when username form is empty.
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        usernameTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                loginButton.setDisable(newValue.trim().isEmpty());
                registerButton.setDisable(newValue.trim().isEmpty());
            }
        });

        usernameTextField.requestFocus();
    }

    public void showLoginDialog() {
        loginDialogStage.showAndWait();
    }

    public void closeLoginDialog(ActionEvent actionEvent) {
        loginDialogStage.close();
    }

    public void loginClient(ActionEvent actionEvent) throws RemoteException, NotBoundException {
        ClientCredentials clientCredentials = collectClientCredentials();
        client.setClientCredentials(clientCredentials);
        boolean response = server.login(client.getClientCredentials());

        new InfoAlert(processLoginResponse(response));

        if (response)
            setUsernameInTitle();
            loginDialogStage.close();
    }

    private String processLoginResponse(boolean response) {
        String communicate;
        if (response)
            communicate = "Logged in.";
        else
            communicate = "Invalid username/password.";

        return communicate;
    }

    public void setUsernameInTitle() {
        if (client != null)
            Main.primaryStage.setTitle("CopyCat (" + client.getClientCredentials().getUsername() + ")");
        else
            Main.primaryStage.setTitle("CopyCat");
    }

    public void registerClient(ActionEvent actionEvent) throws RemoteException, NotBoundException {
        ClientCredentials clientCredentials = collectClientCredentials();
        client.setClientCredentials(clientCredentials);
        String response = server.register(client.getClientCredentials());

        new InfoAlert(response);
    }

    private ClientCredentials collectClientCredentials(){
        String username = usernameTextField.getText(),
               password = passwordField.getText();

        return new ClientCredentials(username, password);
    }

    public void setLoginDialogStage(Stage loginDialogStage) {
        this.loginDialogStage = loginDialogStage;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
