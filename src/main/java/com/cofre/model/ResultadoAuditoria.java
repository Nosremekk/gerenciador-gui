package com.cofre.model;

public class ResultadoAuditoria {
    public final int total;
    public final int fracas;
    public final int medias;
    public final int fortes;
    public final int muitoFortes;
    public final int repetidas;

    public ResultadoAuditoria(int total, int fracas, int medias, int fortes, int muitoFortes, int repetidas) {
        this.total = total;
        this.fracas = fracas;
        this.medias = medias;
        this.fortes = fortes;
        this.muitoFortes = muitoFortes;
        this.repetidas = repetidas;
    }

    public double getScoreSeguranca() {
        if (total == 0) return 1.0;
        double pontos = (fracas * 0.1) + (medias * 0.4) + (fortes * 0.8) + (muitoFortes * 1.0);
        double base = pontos / total;
        if (repetidas > 0) base = Math.max(0.1, base - (repetidas * 0.15));
        return Math.min(1.0, Math.max(0.0, base));
    }
}