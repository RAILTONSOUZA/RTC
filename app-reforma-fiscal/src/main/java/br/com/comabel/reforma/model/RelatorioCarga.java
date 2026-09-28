package br.com.comabel.reforma.model;

import java.util.ArrayList;
import java.util.List;

public class RelatorioCarga {
    private String origem; // Ex: "Diretório: c:/agente/agente" ou "Upload de Arquivos"
    private int totalArquivosExaminados;
    private int totalXmlsLidos;
    private int totalZipsLidos;
    private int notasFiscaisProcessadas; // NF-e (55) + NFC-e (65)
    private int totalNfeMod55;
    private int totalNfceMod65;
    private int eventosSefaz; // Confirmacoes, cancelamentos, CC-e
    private int notasCanceladasAplicadas;
    private int notasDuplicadasIgnoradas;
    private int totalErros;
    private List<String> erros = new ArrayList<>();

    public RelatorioCarga() {}

    public RelatorioCarga(String origem) {
        this.origem = origem;
    }

    public void incrementarExaminados() { this.totalArquivosExaminados++; }
    public void incrementarXmlsLidos() { this.totalXmlsLidos++; }
    public void incrementarZipsLidos() { this.totalZipsLidos++; }
    
    public void registrarNota(String modelo) {
        this.notasFiscaisProcessadas++;
        if ("65".equals(modelo)) {
            this.totalNfceMod65++;
        } else {
            this.totalNfeMod55++;
        }
    }

    public void registrarEvento() { this.eventosSefaz++; }
    public void registrarCancelada() { this.notasCanceladasAplicadas++; }
    public void registrarDuplicada() { this.notasDuplicadasIgnoradas++; }
    
    public void registrarErro(String erro) {
        this.totalErros++;
        if (this.erros.size() < 20) {
            this.erros.add(erro);
        }
    }

    public String getMensagemFormatada() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d nota(s) fiscal(is) auditada(s) com sucesso", notasFiscaisProcessadas));
        if (totalNfeMod55 > 0 || totalNfceMod65 > 0) {
            sb.append(" (");
            if (totalNfeMod55 > 0) sb.append(totalNfeMod55).append(" NF-e");
            if (totalNfeMod55 > 0 && totalNfceMod65 > 0) sb.append(", ");
            if (totalNfceMod65 > 0) sb.append(totalNfceMod65).append(" NFC-e");
            sb.append(")");
        }
        if (eventosSefaz > 0) {
            sb.append(String.format(" | %d arquivo(s) de eventos SEFAZ identificados (confirmações/cancelamentos)", eventosSefaz));
        }
        if (notasCanceladasAplicadas > 0) {
            sb.append(String.format(" [%d cancelamento(s) aplicado(s)]", notasCanceladasAplicadas));
        }
        if (notasDuplicadasIgnoradas > 0) {
            sb.append(String.format(" | %d nota(s) repetida(s) desconsiderada(s)", notasDuplicadasIgnoradas));
        }
        if (totalErros > 0) {
            sb.append(String.format(" | %d erro(s) de leitura", totalErros));
        }
        return sb.toString();
    }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public int getTotalArquivosExaminados() { return totalArquivosExaminados; }
    public void setTotalArquivosExaminados(int totalArquivosExaminados) { this.totalArquivosExaminados = totalArquivosExaminados; }

    public int getTotalXmlsLidos() { return totalXmlsLidos; }
    public void setTotalXmlsLidos(int totalXmlsLidos) { this.totalXmlsLidos = totalXmlsLidos; }

    public int getTotalZipsLidos() { return totalZipsLidos; }
    public void setTotalZipsLidos(int totalZipsLidos) { this.totalZipsLidos = totalZipsLidos; }

    public int getNotasFiscaisProcessadas() { return notasFiscaisProcessadas; }
    public void setNotasFiscaisProcessadas(int notasFiscaisProcessadas) { this.notasFiscaisProcessadas = notasFiscaisProcessadas; }

    public int getTotalNfeMod55() { return totalNfeMod55; }
    public void setTotalNfeMod55(int totalNfeMod55) { this.totalNfeMod55 = totalNfeMod55; }

    public int getTotalNfceMod65() { return totalNfceMod65; }
    public void setTotalNfceMod65(int totalNfceMod65) { this.totalNfceMod65 = totalNfceMod65; }

    public int getEventosSefaz() { return eventosSefaz; }
    public void setEventosSefaz(int eventosSefaz) { this.eventosSefaz = eventosSefaz; }

    public int getNotasCanceladasAplicadas() { return notasCanceladasAplicadas; }
    public void setNotasCanceladasAplicadas(int notasCanceladasAplicadas) { this.notasCanceladasAplicadas = notasCanceladasAplicadas; }

    public int getNotasDuplicadasIgnoradas() { return notasDuplicadasIgnoradas; }
    public void setNotasDuplicadasIgnoradas(int notasDuplicadasIgnoradas) { this.notasDuplicadasIgnoradas = notasDuplicadasIgnoradas; }

    public int getTotalErros() { return totalErros; }
    public void setTotalErros(int totalErros) { this.totalErros = totalErros; }

    public List<String> getErros() { return erros; }
    public void setErros(List<String> erros) { this.erros = erros; }
}
