package com.cofre;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {

    public enum Theme {
        DARK("Escuro", "/dark-theme.css", "🌙"),
        LIGHT("Claro", "/light-theme.css", "☀️");

        private final String nome;
        private final String cssPath;
        private final String icone;

        Theme(String nome, String cssPath, String icone) {
            this.nome = nome;
            this.cssPath = cssPath;
            this.icone = icone;
        }

        public String getNome() {
            return nome;
        }

        public String getCssPath() {
            return cssPath;
        }

        public String getIcone() {
            return icone;
        }
    }

    private static final String PREF_KEY_THEME = "app_theme_mode";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static Theme currentTheme;
    private static final List<WeakReference<Scene>> activeScenes = new ArrayList<>();
    private static final List<WeakReference<Button>> registeredToggleButtons = new ArrayList<>();

    static {
        String saved = prefs.get(PREF_KEY_THEME, Theme.DARK.name());
        try {
            currentTheme = Theme.valueOf(saved);
        } catch (Exception e) {
            currentTheme = Theme.DARK;
        }
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public static void toggleTheme() {
        setTheme(currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    public static void setTheme(Theme theme) {
        if (theme == null) return;
        currentTheme = theme;
        try {
            prefs.put(PREF_KEY_THEME, theme.name());
        } catch (Exception ignored) {
        }

        Platform.runLater(() -> {
            Iterator<WeakReference<Scene>> itScenes = activeScenes.iterator();
            while (itScenes.hasNext()) {
                Scene scene = itScenes.next().get();
                if (scene != null) {
                    aplicarCssNaCena(scene);
                } else {
                    itScenes.remove();
                }
            }

            Iterator<WeakReference<Button>> itBtns = registeredToggleButtons.iterator();
            while (itBtns.hasNext()) {
                Button btn = itBtns.next().get();
                if (btn != null) {
                    atualizarTextoBotao(btn);
                } else {
                    itBtns.remove();
                }
            }
        });
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        boolean exists = false;
        for (WeakReference<Scene> ref : activeScenes) {
            if (ref.get() == scene) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            activeScenes.add(new WeakReference<>(scene));
        }

        aplicarCssNaCena(scene);
    }

    private static void aplicarCssNaCena(Scene scene) {
        try {
            String darkUrl = ThemeManager.class.getResource(Theme.DARK.getCssPath()) != null
                    ? ThemeManager.class.getResource(Theme.DARK.getCssPath()).toExternalForm() : null;
            String lightUrl = ThemeManager.class.getResource(Theme.LIGHT.getCssPath()) != null
                    ? ThemeManager.class.getResource(Theme.LIGHT.getCssPath()).toExternalForm() : null;

            if (darkUrl != null) scene.getStylesheets().remove(darkUrl);
            if (lightUrl != null) scene.getStylesheets().remove(lightUrl);

            String targetUrl = ThemeManager.class.getResource(currentTheme.getCssPath()) != null
                    ? ThemeManager.class.getResource(currentTheme.getCssPath()).toExternalForm() : null;

            if (targetUrl != null && !scene.getStylesheets().contains(targetUrl)) {
                scene.getStylesheets().add(targetUrl);
            }
        } catch (Exception e) {
            System.err.println("Erro ao aplicar tema: " + e.getMessage());
        }
    }

    public static Button criarBotaoAlternarTema() {
        Button btn = new Button();
        btn.getStyleClass().add("btn-theme-toggle");
        atualizarTextoBotao(btn);
        btn.setOnAction(e -> toggleTheme());
        registeredToggleButtons.add(new WeakReference<>(btn));
        return btn;
    }

    private static void atualizarTextoBotao(Button btn) {
        if (currentTheme == Theme.DARK) {
            btn.setText("☀️ Claro");
            btn.setTooltip(new Tooltip("Mudar para Modo Claro"));
        } else {
            btn.setText("🌙 Escuro");
            btn.setTooltip(new Tooltip("Mudar para Modo Escuro"));
        }
    }
}
