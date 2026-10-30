package com.cofre.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Credencial {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty servico;
    private final SimpleStringProperty usuario;
    private final SimpleStringProperty senha;

    public Credencial(int id, String servico, String usuario, String senha) {
        this.id = new SimpleIntegerProperty(id);
        this.servico = new SimpleStringProperty(servico);
        this.usuario = new SimpleStringProperty(usuario);
        this.senha = new SimpleStringProperty(senha);
    }

    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }

    public String getServico() { return servico.get(); }
    public SimpleStringProperty servicoProperty() { return servico; }

    public String getUsuario() { return usuario.get(); }
    public SimpleStringProperty usuarioProperty() { return usuario; }

    public String getSenha() { return senha.get(); }
    public SimpleStringProperty senhaProperty() { return senha; }
}
