package com.cofre.service;

import com.cofre.model.Credencial;
import com.cofre.model.ResultadoAuditoria;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BackendService {

    private static final String BIN = System.getProperty("user.home") + "/.local/bin/gerenciador_senhas";

    private static boolean verificarPicoConectado() {
        return HardwareMonitor.isConectado();
    }

    private static void escreverStdin(Process p, String... linhas) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
            for (String linha : linhas) {
                writer.write(linha);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private static int executarComStdin(ProcessBuilder pb, String... linhas) {
        try {
            pb.redirectErrorStream(true);
            Process p = pb.start();
            escreverStdin(p, linhas);
            try (InputStream in = p.getInputStream()) {
                in.readAllBytes();
            }
            return p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean autenticar(String senhaMestra) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--auth");
        return executarComStdin(pb, senhaMestra) == 0;
    }

    public static List<Credencial> carregarCredenciais(String senhaMestra) {
        List<Credencial> lista = new ArrayList<>();
        if (!verificarPicoConectado()) return lista;

        try {
            ProcessBuilder pb = new ProcessBuilder(BIN, "--list");
            Process p = pb.start();
            escreverStdin(p, senhaMestra);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    String[] partes = linha.split("\\|", 4);
                    if (partes.length == 4) {
                        try {
                            int id = Integer.parseInt(partes[0].trim());
                            lista.add(new Credencial(id, partes[1], partes[2], partes[3]));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static boolean adicionarCredencial(String senhaMestra, String servico, String usuario, String senha) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--add", servico, usuario, senha);
        return executarComStdin(pb, senhaMestra) == 0;
    }

    public static boolean deletarCredencial(String senhaMestra, int id) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--delete", String.valueOf(id));
        return executarComStdin(pb, senhaMestra) == 0;
    }

    public static boolean editarCredencial(String senhaMestra, int id, String servico, String usuario, String senha) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--edit", String.valueOf(id), servico, usuario, senha);
        return executarComStdin(pb, senhaMestra) == 0;
    }

    public static ResultadoAuditoria executarAuditoria(String senhaMestra) {
        if (!verificarPicoConectado()) return null;
        try {
            ProcessBuilder pb = new ProcessBuilder(BIN, "--audit");
            Process p = pb.start();
            escreverStdin(p, senhaMestra);

            ResultadoAuditoria res = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    if (linha.startsWith("AUDIT|")) {
                        String[] f = linha.split("\\|");
                        if (f.length >= 7) {
                            try {
                                res = new ResultadoAuditoria(
                                        Integer.parseInt(f[1].trim()),
                                        Integer.parseInt(f[2].trim()),
                                        Integer.parseInt(f[3].trim()),
                                        Integer.parseInt(f[4].trim()),
                                        Integer.parseInt(f[5].trim()),
                                        Integer.parseInt(f[6].trim())
                                );
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
            p.waitFor();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean exportarCsv(String senhaMestra, String caminhoArquivo) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--export-csv", caminhoArquivo);
        return executarComStdin(pb, senhaMestra) == 0;
    }

    public static boolean exportarBackup(String senhaMestra, String pastaDestino) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--backup", pastaDestino);
        return executarComStdin(pb, senhaMestra) == 0;
    }

    public static boolean alterarSenhaMestra(String senhaAtual, String novaSenha) {
        if (!verificarPicoConectado()) return false;
        ProcessBuilder pb = new ProcessBuilder(BIN, "--change-master");
        return executarComStdin(pb, senhaAtual, novaSenha) == 0;
    }
}
