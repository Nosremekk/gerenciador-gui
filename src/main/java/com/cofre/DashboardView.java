package com.cofre;

import com.cofre.model.Credencial;
import com.cofre.model.ResultadoAuditoria;
import com.cofre.service.BackendService;
import com.cofre.service.HardwareMonitor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;

public class DashboardView {

    private final ObservableList<Credencial> dados = FXCollections.observableArrayList();
    private String senhaMestra;
    private final SecureRandom random = new SecureRandom();

    private final BooleanProperty mostrarSenhas = new SimpleBooleanProperty(false);

    private static final int TEMPO_INATIVIDADE_SEG = 120;
    private Timeline timerInatividade;

    private static final int TEMPO_CLIPBOARD_SEG = 30;
    private Timeline timerClipboard;
    private String ultimaSenhaCopiada = null;

    private ChangeListener<Boolean> listenerHardware;

    public DashboardView(String senhaMestra) {
        this.senhaMestra = senhaMestra;
    }

    public void exibir(Stage palco) {
        palco.setTitle("Cofre de Senhas - Painel");

        TextField campoBusca = new TextField();
        campoBusca.setPromptText("🔍 Buscar serviço ou usuário...");

        Button btnToggleVisibilidade = new Button("👁️ Mostrar Senhas");
        btnToggleVisibilidade.getStyleClass().add("btn-secondary");

        Button btnTema = ThemeManager.criarBotaoAlternarTema();

        TableView<Credencial> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhuma credencial cadastrada"));

        TableColumn<Credencial, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cell -> cell.getValue().idProperty().asObject());
        colId.setMinWidth(45);
        colId.setPrefWidth(55);
        colId.setMaxWidth(70);

        TableColumn<Credencial, String> colServico = new TableColumn<>("Serviço");
        colServico.setCellValueFactory(cell -> cell.getValue().servicoProperty());

        TableColumn<Credencial, String> colUsuario = new TableColumn<>("Usuário");
        colUsuario.setCellValueFactory(cell -> cell.getValue().usuarioProperty());

        TableColumn<Credencial, String> colSenha = new TableColumn<>("Senha");
        colSenha.setCellValueFactory(cell -> cell.getValue().senhaProperty());

        colSenha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                if (vazio || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(mostrarSenhas.get() ? item : "••••••••••••");
                }
            }
        });

        mostrarSenhas.addListener((obs, oldVal, newVal) -> tabela.refresh());

        btnToggleVisibilidade.setOnAction(e -> {
            boolean novoEstado = !mostrarSenhas.get();
            mostrarSenhas.set(novoEstado);
            btnToggleVisibilidade.setText(novoEstado ? "🙈 Ocultar Senhas" : "👁️ Mostrar Senhas");
        });

        tabela.getColumns().addAll(colId, colServico, colUsuario, colSenha);

        FilteredList<Credencial> dadosFiltrados = new FilteredList<>(dados, p -> true);
        campoBusca.textProperty().addListener((obs, antigo, novo) -> {
            dadosFiltrados.setPredicate(cred -> {
                if (novo == null || novo.isBlank()) return true;
                String filtro = novo.toLowerCase();
                return cred.getServico().toLowerCase().contains(filtro) ||
                        cred.getUsuario().toLowerCase().contains(filtro);
            });
        });
        tabela.setItems(dadosFiltrados);

        Label labelStatus = new Label("Carregando...");
        labelStatus.getStyleClass().add("status-info");

        Label labelPicoStatus = new Label(HardwareMonitor.isConectado() ? "● HSM Conectado" : "○ HSM Desconectado");
        labelPicoStatus.getStyleClass().addAll("status-pill", HardwareMonitor.isConectado() ? "status-connected" : "status-disconnected");

        listenerHardware = (obs, antigo, conectado) -> {
            labelPicoStatus.setText(conectado ? "● HSM Conectado" : "○ HSM Desconectado");
            labelPicoStatus.getStyleClass().removeAll("status-connected", "status-disconnected");
            labelPicoStatus.getStyleClass().add(conectado ? "status-connected" : "status-disconnected");

            if (!conectado) {
                Platform.runLater(() -> bloquearCofre(palco, "Chave física desconectada! Cofre bloqueado por segurança."));
            }
        };
        HardwareMonitor.conectadoProperty().addListener(listenerHardware);

        Button btnCopiar = new Button("Copiar");
        btnCopiar.getStyleClass().add("btn-primary");
        btnCopiar.setOnAction(e -> {
            Credencial selecionada = tabela.getSelectionModel().getSelectedItem();
            if (selecionada != null) {
                copiarComLimpezaAutomatica(selecionada.getSenha(), labelStatus);
            }
        });

        Button btnEditar = new Button("Editar");
        btnEditar.getStyleClass().add("btn-warning");
        btnEditar.setOnAction(e -> {
            Credencial selecionada = tabela.getSelectionModel().getSelectedItem();
            if (selecionada != null) {
                abrirModalFormulario(palco, labelStatus, selecionada);
            }
        });

        Button btnExcluir = new Button("Excluir");
        btnExcluir.getStyleClass().add("btn-danger");
        btnExcluir.setOnAction(e -> {
            Credencial selecionada = tabela.getSelectionModel().getSelectedItem();
            if (selecionada != null) {
                Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacao.setTitle("Excluir Credencial");
                confirmacao.setHeaderText("Excluir credencial de \"" + selecionada.getServico() + "\"?");
                confirmacao.setContentText("Esta ação é irreversível e exigirá confirmação no Pico W.");
                ThemeManager.applyTheme(confirmacao.getDialogPane().getScene());

                confirmacao.showAndWait().ifPresent(resposta -> {
                    if (resposta == ButtonType.OK) {
                        Stage dialogFeedback = exibirFeedbackHardware(palco, "Aguardando confirmação no Pico W...");
                        new Thread(() -> {
                            boolean ok = BackendService.deletarCredencial(senhaMestra, selecionada.getId());
                            Platform.runLater(() -> {
                                dialogFeedback.close();
                                if (ok) {
                                    recarregarTabela(labelStatus);
                                } else {
                                    labelStatus.setText("Falha ao excluir credencial.");
                                    labelStatus.getStyleClass().removeAll("status-connected", "status-disconnected");
                                    labelStatus.getStyleClass().add("status-disconnected");
                                }
                            });
                        }).start();
                    }
                });
            }
        });

        btnCopiar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        btnEditar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        btnExcluir.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());

        tabela.setRowFactory(tv -> {
            TableRow<Credencial> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    abrirModalFormulario(palco, labelStatus, row.getItem());
                }
            });

            ContextMenu contextMenu = new ContextMenu();
            MenuItem itemCopiar = new MenuItem("📋 Copiar Senha");
            itemCopiar.setOnAction(ev -> {
                if (!row.isEmpty()) {
                    copiarComLimpezaAutomatica(row.getItem().getSenha(), labelStatus);
                }
            });
            MenuItem itemEditar = new MenuItem("✏️ Editar");
            itemEditar.setOnAction(ev -> {
                if (!row.isEmpty()) {
                    abrirModalFormulario(palco, labelStatus, row.getItem());
                }
            });
            MenuItem itemExcluir = new MenuItem("🗑️ Excluir");
            itemExcluir.setOnAction(ev -> {
                if (!row.isEmpty()) {
                    tabela.getSelectionModel().select(row.getItem());
                    btnExcluir.fire();
                }
            });
            contextMenu.getItems().addAll(itemCopiar, itemEditar, new SeparatorMenuItem(), itemExcluir);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            return row;
        });

        Button btnAdicionar = new Button("+ Nova Senha");
        btnAdicionar.getStyleClass().add("btn-success");
        btnAdicionar.setOnAction(e -> abrirModalFormulario(palco, labelStatus, null));

        Button btnAuditoria = new Button("🛡️ Auditoria");
        btnAuditoria.getStyleClass().add("btn-warning");
        btnAuditoria.setOnAction(e -> abrirModalAuditoria(palco));

        MenuButton btnConfiguracoes = new MenuButton("⚙️ Opções");
        btnConfiguracoes.getStyleClass().add("btn-secondary");

        MenuItem itemTrocarSenha = new MenuItem("🔑 Alterar Senha Mestra...");
        itemTrocarSenha.setOnAction(e -> abrirModalAlterarSenha(palco, labelStatus));

        MenuItem itemAlternarTema = new MenuItem("🌓 Alternar Tema Claro / Escuro");
        itemAlternarTema.setOnAction(e -> ThemeManager.toggleTheme());

        MenuItem itemCsv = new MenuItem("📄 Exportar para CSV...");
        itemCsv.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Salvar exportação CSV");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo CSV (*.csv)", "*.csv"));
            fc.setInitialFileName("senhas_exportadas.csv");
            File file = fc.showSaveDialog(palco);
            if (file != null) {
                Stage dialogFeedback = exibirFeedbackHardware(palco, "Validando token no Pico W...");
                new Thread(() -> {
                    boolean ok = BackendService.exportarCsv(senhaMestra, file.getAbsolutePath());
                    Platform.runLater(() -> {
                        dialogFeedback.close();
                        labelStatus.setText(ok ? "CSV gerado com sucesso!" : "Erro ao exportar CSV.");
                        labelStatus.getStyleClass().removeAll("status-connected", "status-disconnected");
                        labelStatus.getStyleClass().add(ok ? "status-connected" : "status-disconnected");
                    });
                }).start();
            }
        });

        MenuItem itemBackup = new MenuItem("💾 Criar Backup Criptografado...");
        itemBackup.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Selecionar pasta para backup seguro");
            File dir = dc.showDialog(palco);
            if (dir != null) {
                Stage dialogFeedback = exibirFeedbackHardware(palco, "Validando token no Pico W...");
                new Thread(() -> {
                    boolean ok = BackendService.exportarBackup(senhaMestra, dir.getAbsolutePath());
                    Platform.runLater(() -> {
                        dialogFeedback.close();
                        labelStatus.setText(ok ? "Backup (.dat, .txt, .bin) concluído!" : "Falha no backup.");
                        labelStatus.getStyleClass().removeAll("status-connected", "status-disconnected");
                        labelStatus.getStyleClass().add(ok ? "status-connected" : "status-disconnected");
                    });
                }).start();
            }
        });

        btnConfiguracoes.getItems().addAll(itemTrocarSenha, itemAlternarTema, new SeparatorMenuItem(), itemCsv, itemBackup);

        Button btnBloquearManual = new Button("🔒 Bloquear");
        btnBloquearManual.getStyleClass().add("btn-danger");
        btnBloquearManual.setOnAction(e -> bloquearCofre(palco, null));

        HBox barraTopo = new HBox(10, campoBusca, btnToggleVisibilidade, btnTema);
        HBox.setHgrow(campoBusca, Priority.ALWAYS);
        barraTopo.setAlignment(Pos.CENTER_LEFT);

        HBox barraAcoes = new HBox(8, labelStatus, new Region(), labelPicoStatus, btnAuditoria, btnConfiguracoes, btnCopiar, btnEditar, btnExcluir, btnAdicionar, btnBloquearManual);
        HBox.setHgrow(barraAcoes.getChildren().get(1), Priority.ALWAYS);
        barraAcoes.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(14, barraTopo, tabela, barraAcoes);
        layout.setPadding(new Insets(18));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        Scene cena = new Scene(layout, 940, 540);
        ThemeManager.applyTheme(cena);

        iniciarTimerInatividade(palco, cena);

        palco.setScene(cena);
        palco.setOnCloseRequest(e -> {
            if (timerInatividade != null) timerInatividade.stop();
            if (timerClipboard != null) timerClipboard.stop();
            HardwareMonitor.pararMonitoramento();
        });
        palco.show();

        recarregarTabela(labelStatus);
    }

    private Stage exibirFeedbackHardware(Stage palcoPai, String mensagem) {
        Stage stageFeedback = new Stage(StageStyle.TRANSPARENT);
        stageFeedback.initModality(Modality.APPLICATION_MODAL);
        stageFeedback.initOwner(palcoPai);

        Label icone = new Label("📟");
        icone.setStyle("-fx-font-size: 26px;");

        Label label = new Label(mensagem);
        label.getStyleClass().add("status-info");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(24, 24);

        HBox box = new HBox(14, icone, progress, label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 26, 20, 26));
        box.getStyleClass().add("hardware-modal");

        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);

        stageFeedback.setScene(scene);
        stageFeedback.show();
        return stageFeedback;
    }

    private void abrirModalAlterarSenha(Stage palcoPai, Label statusDashboard) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(palcoPai);
        modal.setTitle("Alterar Senha Mestra");

        PasswordField txtAtual = new PasswordField();
        txtAtual.setPromptText("Senha Mestra Atual");

        PasswordField txtNova = new PasswordField();
        txtNova.setPromptText("Nova Senha Mestra");

        PasswordField txtConfirma = new PasswordField();
        txtConfirma.setPromptText("Confirme a Nova Senha Mestra");

        Label lblErro = new Label();
        lblErro.getStyleClass().add("status-disconnected");

        Button btnConfirmar = new Button("Re-encriptar Cofre");
        btnConfirmar.getStyleClass().add("btn-danger");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);

        btnConfirmar.setOnAction(e -> {
            String atual = txtAtual.getText();
            String nova = txtNova.getText();
            String confirma = txtConfirma.getText();

            if (atual.isEmpty() || nova.isEmpty() || confirma.isEmpty()) {
                lblErro.setText("Preencha todos os campos.");
                return;
            }

            if (!nova.equals(confirma)) {
                lblErro.setText("A nova senha e a confirmação não coincidem.");
                return;
            }

            if (nova.length() < 8) {
                lblErro.setText("A nova senha deve ter pelo menos 8 caracteres.");
                return;
            }

            Stage feedback = exibirFeedbackHardware(modal, "Recalculando chaves no Pico W...");
            btnConfirmar.setDisable(true);

            new Thread(() -> {
                boolean sucesso = BackendService.alterarSenhaMestra(atual, nova);
                Platform.runLater(() -> {
                    feedback.close();
                    if (sucesso) {
                        this.senhaMestra = nova;
                        modal.close();
                        statusDashboard.setText("Senha mestra alterada com sucesso!");
                        statusDashboard.getStyleClass().removeAll("status-connected", "status-disconnected");
                        statusDashboard.getStyleClass().add("status-connected");
                    } else {
                        btnConfirmar.setDisable(false);
                        lblErro.setText("Falha: senha atual incorreta ou erro no token.");
                    }
                });
            }).start();
        });

        Label titulo = new Label("🔑 Alteração de Senha Mestra");
        titulo.getStyleClass().add("title-label");

        VBox layout = new VBox(12, titulo, txtAtual, txtNova, txtConfirma, lblErro, btnConfirmar);
        layout.getStyleClass().add("card-pane");
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Scene cena = new Scene(layout, 400, 300);
        ThemeManager.applyTheme(cena);
        modal.setScene(cena);
        modal.show();
    }

    private void copiarComLimpezaAutomatica(String senha, Label labelStatus) {
        ClipboardContent content = new ClipboardContent();
        content.putString(senha);
        Clipboard.getSystemClipboard().setContent(content);
        ultimaSenhaCopiada = senha;

        if (timerClipboard != null) {
            timerClipboard.stop();
        }

        final int[] contagem = {TEMPO_CLIPBOARD_SEG};
        labelStatus.setText(String.format("Senha copiada! Limpando clipboard em %ds...", TEMPO_CLIPBOARD_SEG));
        labelStatus.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
        labelStatus.getStyleClass().add("status-connected");

        timerClipboard = new Timeline(new KeyFrame(Duration.seconds(1), evt -> {
            contagem[0]--;
            if (contagem[0] > 0) {
                labelStatus.setText(String.format("Senha copiada! Limpando clipboard em %ds...", contagem[0]));
            } else {
                Clipboard clip = Clipboard.getSystemClipboard();
                if (clip.hasString() && ultimaSenhaCopiada != null && ultimaSenhaCopiada.equals(clip.getString())) {
                    clip.clear();
                }
                ultimaSenhaCopiada = null;
                labelStatus.setText("Área de transferência limpa por segurança.");
                labelStatus.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
                labelStatus.getStyleClass().add("status-pending");
            }
        }));
        timerClipboard.setCycleCount(TEMPO_CLIPBOARD_SEG);
        timerClipboard.play();
    }

    private void iniciarTimerInatividade(Stage palco, Scene cena) {
        timerInatividade = new Timeline(new KeyFrame(Duration.seconds(TEMPO_INATIVIDADE_SEG), evt -> {
            bloquearCofre(palco, "Sessão bloqueada por inatividade.");
        }));
        timerInatividade.setCycleCount(1);
        timerInatividade.play();

        Runnable resetarInatividade = () -> {
            if (timerInatividade != null) {
                timerInatividade.stop();
                timerInatividade.play();
            }
        };

        cena.addEventFilter(MouseEvent.ANY, e -> resetarInatividade.run());
        cena.addEventFilter(KeyEvent.ANY, e -> resetarInatividade.run());
    }

    private void bloquearCofre(Stage palco, String motivo) {
        if (timerInatividade != null) timerInatividade.stop();
        if (timerClipboard != null) timerClipboard.stop();

        if (ultimaSenhaCopiada != null) {
            Clipboard clip = Clipboard.getSystemClipboard();
            if (clip.hasString() && ultimaSenhaCopiada.equals(clip.getString())) {
                clip.clear();
            }
            ultimaSenhaCopiada = null;
        }

        if (listenerHardware != null) {
            HardwareMonitor.conectadoProperty().removeListener(listenerHardware);
        }

        dados.clear();
        new MainApp(motivo).start(palco);
    }

    private void recarregarTabela(Label labelStatus) {
        new Thread(() -> {
            List<Credencial> lista = BackendService.carregarCredenciais(senhaMestra);
            Platform.runLater(() -> {
                dados.setAll(lista);
                labelStatus.setText(lista.size() + " credenciais no cofre");
                labelStatus.getStyleClass().removeAll("status-connected", "status-disconnected", "status-pending");
                labelStatus.getStyleClass().add("status-info");
            });
        }).start();
    }

    private void abrirModalFormulario(Stage palcoPai, Label statusDashboard, Credencial credencialExistente) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(palcoPai);
        boolean isEdicao = (credencialExistente != null);
        modal.setTitle(isEdicao ? "Editar Credencial" : "Nova Credencial");

        Label lblTituloModal = new Label(isEdicao ? "✏️ Editar Credencial" : "➕ Nova Credencial");
        lblTituloModal.getStyleClass().add("title-label");

        TextField txtServico = new TextField(isEdicao ? credencialExistente.getServico() : "");
        txtServico.setPromptText("Serviço (ex: GitHub, Google)");

        TextField txtUsuario = new TextField(isEdicao ? credencialExistente.getUsuario() : "");
        txtUsuario.setPromptText("Usuário / E-mail");

        PasswordField txtSenhaOculta = new PasswordField();
        txtSenhaOculta.setPromptText("Senha");

        TextField txtSenhaVisivel = new TextField();
        txtSenhaVisivel.setPromptText("Senha");
        txtSenhaVisivel.setManaged(false);
        txtSenhaVisivel.setVisible(false);

        txtSenhaOculta.textProperty().bindBidirectional(txtSenhaVisivel.textProperty());

        if (isEdicao) {
            txtSenhaOculta.setText(credencialExistente.getSenha());
        }

        Button btnToggleOlho = new Button("👁️");
        btnToggleOlho.getStyleClass().add("btn-secondary");
        btnToggleOlho.setStyle("-fx-padding: 7px 10px;");
        btnToggleOlho.setOnAction(e -> {
            boolean visivel = txtSenhaVisivel.isVisible();
            if (visivel) {
                txtSenhaVisivel.setVisible(false);
                txtSenhaVisivel.setManaged(false);
                txtSenhaOculta.setVisible(true);
                txtSenhaOculta.setManaged(true);
                btnToggleOlho.setText("👁️");
            } else {
                txtSenhaOculta.setVisible(false);
                txtSenhaOculta.setManaged(false);
                txtSenhaVisivel.setVisible(true);
                txtSenhaVisivel.setManaged(true);
                btnToggleOlho.setText("🙈");
            }
        });

        StackPane stackSenha = new StackPane(txtSenhaOculta, txtSenhaVisivel);
        HBox.setHgrow(stackSenha, Priority.ALWAYS);
        HBox boxLinhaSenha = new HBox(6, stackSenha, btnToggleOlho);
        boxLinhaSenha.setAlignment(Pos.CENTER);

        Label lblTamanho = new Label("Comprimento: 16");
        lblTamanho.getStyleClass().add("subtitle-label");

        Slider sliderTamanho = new Slider(8, 64, 16);
        sliderTamanho.setBlockIncrement(1);
        sliderTamanho.valueProperty().addListener((obs, antigo, novo) ->
                lblTamanho.setText("Comprimento: " + novo.intValue())
        );

        Button btnGerar = new Button("🎲 Gerar");
        btnGerar.getStyleClass().add("btn-primary");
        btnGerar.setOnAction(e -> {
            String gerada = gerarSenhaAleatoria((int) sliderTamanho.getValue());
            txtSenhaOculta.setText(gerada);
            if (!txtSenhaVisivel.isVisible()) {
                btnToggleOlho.fire();
            }
        });

        HBox boxSliderLinha = new HBox(8, lblTamanho, sliderTamanho, btnGerar);
        boxSliderLinha.setAlignment(Pos.CENTER);

        VBox boxGerador = new VBox(8, boxSliderLinha);
        boxGerador.getStyleClass().add("box-elevated");
        boxGerador.setAlignment(Pos.CENTER);

        Label lblErroValidacao = new Label();
        lblErroValidacao.getStyleClass().add("status-disconnected");
        lblErroValidacao.setVisible(false);

        Button btnSalvar = new Button(isEdicao ? "Atualizar Registro" : "Salvar Registro");
        btnSalvar.getStyleClass().add("btn-success");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);

        btnSalvar.setOnAction(e -> {
            String s = txtServico.getText().trim();
            String u = txtUsuario.getText().trim();
            String p = txtSenhaOculta.getText();

            if (s.isEmpty() || u.isEmpty() || p.isEmpty()) {
                lblErroValidacao.setText("Todos os campos devem ser preenchidos.");
                lblErroValidacao.setVisible(true);
                return;
            }

            lblErroValidacao.setVisible(false);
            btnSalvar.setDisable(true);
            Stage feedback = exibirFeedbackHardware(modal, "Gravando com chave do Pico W...");

            new Thread(() -> {
                boolean ok = isEdicao
                        ? BackendService.editarCredencial(senhaMestra, credencialExistente.getId(), s, u, p)
                        : BackendService.adicionarCredencial(senhaMestra, s, u, p);

                Platform.runLater(() -> {
                    feedback.close();
                    if (ok) {
                        modal.close();
                        recarregarTabela(statusDashboard);
                    } else {
                        btnSalvar.setDisable(false);
                        lblErroValidacao.setText("Erro ao salvar no cofre (verifique o token).");
                        lblErroValidacao.setVisible(true);
                    }
                });
            }).start();
        });

        VBox layoutModal = new VBox(12, lblTituloModal, txtServico, txtUsuario, boxLinhaSenha, boxGerador, lblErroValidacao, btnSalvar);
        layoutModal.getStyleClass().add("card-pane");
        layoutModal.setPadding(new Insets(20));
        layoutModal.setAlignment(Pos.CENTER);

        Scene cenaModal = new Scene(layoutModal, 420, 390);
        ThemeManager.applyTheme(cenaModal);

        modal.setScene(cenaModal);
        modal.show();
    }

    private void abrirModalAuditoria(Stage palcoPai) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(palcoPai);
        modal.setTitle("Auditoria de Segurança do Cofre");

        Label lblTitulo = new Label("🛡️ Auditoria de Segurança");
        lblTitulo.getStyleClass().add("title-label");

        Label lblStatus = new Label("Processando registros...");
        lblStatus.getStyleClass().add("status-info");

        ProgressBar barraScore = new ProgressBar(0);
        barraScore.setMaxWidth(Double.MAX_VALUE);

        VBox relatorio = new VBox(8);
        relatorio.getStyleClass().add("box-elevated");

        Button btnFechar = new Button("Fechar");
        btnFechar.getStyleClass().add("btn-secondary");
        btnFechar.setOnAction(e -> modal.close());

        VBox layout = new VBox(14, lblTitulo, barraScore, lblStatus, relatorio, btnFechar);
        layout.getStyleClass().add("card-pane");
        layout.setPadding(new Insets(22));
        layout.setAlignment(Pos.CENTER);

        Stage feedback = exibirFeedbackHardware(palcoPai, "Validando token no Pico W...");
        new Thread(() -> {
            ResultadoAuditoria res = BackendService.executarAuditoria(senhaMestra);
            Platform.runLater(() -> {
                feedback.close();
                if (res == null || res.total == 0) {
                    lblStatus.setText("Nenhum registro disponível para auditar.");
                    return;
                }

                double score = res.getScoreSeguranca();
                barraScore.setProgress(score);
                lblStatus.setText(String.format("Índice Geral de Segurança: %.0f%%", score * 100));

                Label lblTotal = new Label("• Total de credenciais analisadas: " + res.total);
                Label lblMF = new Label("• Senhas muito fortes: " + res.muitoFortes);
                lblMF.getStyleClass().add("status-connected");
                Label lblF = new Label("• Senhas fortes: " + res.fortes);
                Label lblM = new Label("• Senhas médias: " + res.medias);
                lblM.getStyleClass().add("status-pending");
                Label lblFracas = new Label("• Senhas fracas: " + res.fracas);
                lblFracas.getStyleClass().add("status-disconnected");
                Label lblRepetidas = new Label("• Senhas duplicadas / repetidas: " + res.repetidas);
                lblRepetidas.getStyleClass().add("status-disconnected");

                relatorio.getChildren().addAll(lblTotal, lblMF, lblF, lblM, lblFracas, lblRepetidas);
            });
        }).start();

        Scene cena = new Scene(layout, 420, 360);
        ThemeManager.applyTheme(cena);
        modal.setScene(cena);
        modal.show();
    }

    private String gerarSenhaAleatoria(int tamanho) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}";
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
