package com.cofre;

import com.cofre.service.BackendService;
import com.cofre.service.HardwareMonitor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    private final String mensagemAviso;
    private ChangeListener<Boolean> listenerConexao;

    public MainApp() {
        this(null);
    }

    public MainApp(String mensagemAviso) {
        this.mensagemAviso = mensagemAviso;
    }

    @Override
    public void start(Stage palco) {
        palco.setTitle("Cofre de Senhas - Hardware 2FA");

        HardwareMonitor.iniciarMonitoramento();

        Button btnTema = ThemeManager.criarBotaoAlternarTema();

        HBox barraTopo = new HBox(new Region(), btnTema);
        HBox.setHgrow(barraTopo.getChildren().get(0), Priority.ALWAYS);
        barraTopo.setAlignment(Pos.CENTER_RIGHT);

        Label labelIcone = new Label("🔒");
        labelIcone.setStyle("-fx-font-size: 40px;");

        Label labelTitulo = new Label("Cofre de Senhas");
        labelTitulo.getStyleClass().add("title-label");

        Label labelSubtitulo = new Label("Autenticação Física de Dois Fatores (HSM)");
        labelSubtitulo.getStyleClass().add("subtitle-label");

        Label labelStatusPico = new Label();
        labelStatusPico.getStyleClass().add("status-pill");

        PasswordField campoSenhaOculta = new PasswordField();
        campoSenhaOculta.setPromptText("Digite a senha mestra");
        campoSenhaOculta.setMaxWidth(280);

        TextField campoSenhaVisivel = new TextField();
        campoSenhaVisivel.setPromptText("Digite a senha mestra");
        campoSenhaVisivel.setMaxWidth(280);
        campoSenhaVisivel.setManaged(false);
        campoSenhaVisivel.setVisible(false);

        campoSenhaOculta.textProperty().bindBidirectional(campoSenhaVisivel.textProperty());

        Button btnToggleVerSenha = new Button("👁️");
        btnToggleVerSenha.getStyleClass().add("btn-secondary");
        btnToggleVerSenha.setStyle("-fx-padding: 7px 10px;");

        btnToggleVerSenha.setOnAction(e -> {
            boolean estaVisivel = campoSenhaVisivel.isVisible();
            if (estaVisivel) {
                campoSenhaVisivel.setVisible(false);
                campoSenhaVisivel.setManaged(false);
                campoSenhaOculta.setVisible(true);
                campoSenhaOculta.setManaged(true);
                btnToggleVerSenha.setText("👁️");
                campoSenhaOculta.requestFocus();
                campoSenhaOculta.positionCaret(campoSenhaOculta.getText().length());
            } else {
                campoSenhaOculta.setVisible(false);
                campoSenhaOculta.setManaged(false);
                campoSenhaVisivel.setVisible(true);
                campoSenhaVisivel.setManaged(true);
                btnToggleVerSenha.setText("🙈");
                campoSenhaVisivel.requestFocus();
                campoSenhaVisivel.positionCaret(campoSenhaVisivel.getText().length());
            }
        });

        StackPane containerCampos = new StackPane(campoSenhaOculta, campoSenhaVisivel);
        HBox boxSenha = new HBox(6, containerCampos, btnToggleVerSenha);
        boxSenha.setAlignment(Pos.CENTER);

        Button botaoEntrar = new Button("Desbloquear Cofre");
        botaoEntrar.getStyleClass().add("btn-primary");
        botaoEntrar.setMaxWidth(320);

        botaoEntrar.disableProperty().bind(HardwareMonitor.conectadoProperty().not());

        Runnable atualizarStatusHardware = () -> {
            boolean conectado = HardwareMonitor.isConectado();
            labelStatusPico.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
            if (conectado) {
                labelStatusPico.setText("● Token conectado (/dev/ttyACM0)");
                labelStatusPico.getStyleClass().add("status-connected");
            } else {
                labelStatusPico.setText("○ Chave física desconectada (/dev/ttyACM0)");
                labelStatusPico.getStyleClass().add("status-disconnected");
            }
        };

        if (listenerConexao != null) {
            HardwareMonitor.conectadoProperty().removeListener(listenerConexao);
        }
        listenerConexao = (obs, antigo, conectado) -> atualizarStatusHardware.run();
        HardwareMonitor.conectadoProperty().addListener(listenerConexao);
        atualizarStatusHardware.run();

        Runnable acaoAutenticar = () -> {
            if (!HardwareMonitor.isConectado()) {
                labelStatusPico.setText("Conecte o Pico W antes de autenticar!");
                labelStatusPico.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
                labelStatusPico.getStyleClass().add("status-disconnected");
                return;
            }

            String senha = campoSenhaOculta.getText();
            if (senha.isEmpty()) return;

            botaoEntrar.disableProperty().unbind();
            botaoEntrar.setDisable(true);
            campoSenhaOculta.setDisable(true);
            campoSenhaVisivel.setDisable(true);
            btnToggleVerSenha.setDisable(true);

            labelStatusPico.setText("⏳ Aperte o botão no Pico W...");
            labelStatusPico.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
            labelStatusPico.getStyleClass().add("status-pending");

            new Thread(() -> {
                boolean sucesso = BackendService.autenticar(senha);

                Platform.runLater(() -> {
                    botaoEntrar.disableProperty().bind(HardwareMonitor.conectadoProperty().not());
                    campoSenhaOculta.setDisable(false);
                    campoSenhaVisivel.setDisable(false);
                    btnToggleVerSenha.setDisable(false);

                    if (sucesso) {
                        if (listenerConexao != null) {
                            HardwareMonitor.conectadoProperty().removeListener(listenerConexao);
                        }
                        new DashboardView(senha).exibir(palco);
                    } else {
                        labelStatusPico.setText("Falha na autenticação ou tempo esgotado!");
                        labelStatusPico.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
                        labelStatusPico.getStyleClass().add("status-disconnected");
                        campoSenhaOculta.clear();
                        if (campoSenhaOculta.isVisible()) {
                            campoSenhaOculta.requestFocus();
                        } else {
                            campoSenhaVisivel.requestFocus();
                        }
                    }
                });
            }).start();
        };

        botaoEntrar.setOnAction(e -> acaoAutenticar.run());
        campoSenhaOculta.setOnAction(e -> acaoAutenticar.run());
        campoSenhaVisivel.setOnAction(e -> acaoAutenticar.run());

        VBox cartaoCentral = new VBox(14, labelIcone, labelTitulo, labelSubtitulo, labelStatusPico, boxSenha, botaoEntrar);
        cartaoCentral.getStyleClass().add("card-pane");
        cartaoCentral.setAlignment(Pos.CENTER);
        cartaoCentral.setMaxWidth(380);

        if (mensagemAviso != null && !mensagemAviso.isBlank()) {
            Label labelAviso = new Label("⚠️ " + mensagemAviso);
            labelAviso.getStyleClass().add("alert-banner");
            labelAviso.setWrapText(true);
            labelAviso.setMaxWidth(320);
            cartaoCentral.getChildren().add(3, labelAviso);
        }

        VBox layoutGeral = new VBox(10, barraTopo, cartaoCentral);
        layoutGeral.setAlignment(Pos.CENTER);
        layoutGeral.setPadding(new Insets(20));

        Scene cena = new Scene(layoutGeral, 450, 480);
        ThemeManager.applyTheme(cena);

        palco.setScene(cena);
        palco.setOnCloseRequest(e -> HardwareMonitor.pararMonitoramento());
        palco.show();
    }

    public static void aplicarCss(Scene cena) {
        ThemeManager.applyTheme(cena);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
