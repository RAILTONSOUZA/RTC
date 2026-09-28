package br.com.comabel.reforma.service;

import br.com.comabel.reforma.model.EmpresaAuditada;
import br.com.comabel.reforma.model.ItemAuditoria;
import br.com.comabel.reforma.model.KpiDashboard;
import br.com.comabel.reforma.model.NotaFiscalResumo;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class PdfExportService {

    private static final DecimalFormat MOEDA = new DecimalFormat("R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));
    private static final DecimalFormat PERCENT = new DecimalFormat("#,##0.0'%'", new DecimalFormatSymbols(new Locale("pt", "BR")));

    // Paleta de Cores Executiva
    private static final Color PRIMARY_NAVY = new Color(15, 23, 42);      // #0f172a
    private static final Color HEADER_BLUE  = new Color(30, 58, 138);     // #1e3a8a
    private static final Color ACCENT_BLUE  = new Color(2, 132, 199);     // #0284c7
    private static final Color SUCCESS_GREEN = new Color(22, 101, 52);    // #166534
    private static final Color SUCCESS_BG   = new Color(240, 253, 244);   // #f0fdf4
    private static final Color WARNING_AMBER = new Color(180, 83, 9);     // #b45309
    private static final Color WARNING_BG   = new Color(254, 243, 199);   // #fef3c7
    private static final Color DANGER_RED   = new Color(185, 28, 28);     // #b91c1c
    private static final Color DANGER_BG    = new Color(254, 242, 242);   // #fef2f2
    private static final Color BG_LIGHT     = new Color(248, 250, 252);   // #f8fafc
    private static final Color BORDER_GRAY  = new Color(226, 232, 240);   // #e2e8f0
    private static final Color TEXT_DARK    = new Color(30, 41, 59);      // #1e293b
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);   // #64748b

    public byte[] gerarRelatorioExecutivoPdf(EmpresaAuditada empresa,
                                            KpiDashboard kpi,
                                            List<NotaFiscalResumo> notas,
                                            List<ItemAuditoria> itensDivergentes) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 32, 32, 32, 32);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new RodapeNumerado());

            document.open();

            // Fontes
            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.WHITE);
            Font fSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(224, 231, 255));
            Font fSecao = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_NAVY);
            Font fTexto = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_DARK);
            Font fTextoBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_DARK);
            Font fMuted = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);

            // 1. Cabeçalho Principal (Banner)
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(10);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(HEADER_BLUE);
            headerCell.setPadding(12);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph pBadge = new Paragraph("PARECER EXECUTIVO DE CONFORMIDADE FISCAL • LC 214/2025 & EC 132/2023",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(147, 197, 253)));
            pBadge.setSpacingAfter(4);
            headerCell.addElement(pBadge);

            Paragraph pTitulo = new Paragraph("RELATÓRIO DE AUDITORIA FISCAL - REFORMA TRIBUTÁRIA", fTitulo);
            headerCell.addElement(pTitulo);

            Paragraph pSub = new Paragraph("Diagnóstico Estratégico de Transição IBS / CBS e Adequação Tributária", fSubtitulo);
            headerCell.addElement(pSub);

            headerTable.addCell(headerCell);
            document.add(headerTable);

            // 2. Quadro de Identificação da Empresa Auditada
            PdfPTable empresaTable = new PdfPTable(2);
            empresaTable.setWidthPercentage(100);
            empresaTable.setWidths(new float[]{60f, 40f});
            empresaTable.setSpacingAfter(10);

            // Coluna Esquerda: Dados da Empresa
            PdfPCell cEmp1 = new PdfPCell();
            cEmp1.setBackgroundColor(BG_LIGHT);
            cEmp1.setBorderColor(BORDER_GRAY);
            cEmp1.setPadding(8);

            String razao = (empresa != null && empresa.getRazaoSocial() != null) ? empresa.getRazaoSocial() : "Empresa Auditada";
            String cnpj = (empresa != null && empresa.getCnpjPrincipal() != null) ? empresa.getCnpjPrincipal() : "Não informado";
            String ufMun = (empresa != null ? (empresa.getMunicipio() != null ? empresa.getMunicipio() : "") + " / " + (empresa.getUf() != null ? empresa.getUf() : "") : "");

            cEmp1.addElement(new Paragraph("EMPRESA AUDITADA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, ACCENT_BLUE)));
            cEmp1.addElement(new Paragraph(razao, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, PRIMARY_NAVY)));
            cEmp1.addElement(new Paragraph("CNPJ Matriz: " + cnpj + " • Município/UF: " + ufMun, fTexto));
            empresaTable.addCell(cEmp1);

            // Coluna Direita: Período e Data de Emissão
            PdfPCell cEmp2 = new PdfPCell();
            cEmp2.setBackgroundColor(BG_LIGHT);
            cEmp2.setBorderColor(BORDER_GRAY);
            cEmp2.setPadding(8);

            String periodo = (empresa != null && empresa.getPeriodoAuditado() != null) ? empresa.getPeriodoAuditado() : "Agosto / 2026";
            String emissao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            cEmp2.addElement(new Paragraph("PARÂMETROS DA AUDITORIA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, ACCENT_BLUE)));
            cEmp2.addElement(new Paragraph("Período Auditado: " + periodo, fTextoBold));
            cEmp2.addElement(new Paragraph("Data de Emissão: " + emissao, fTexto));
            cEmp2.addElement(new Paragraph("Regime: Lucro Real/Presumido (Transição 2026)", fMuted));
            empresaTable.addCell(cEmp2);

            document.add(empresaTable);

            // 3. Quadro Executivo de Indicadores (Big Numbers)
            document.add(new Paragraph("1. INDICADORES GERAIS & EXPOSIÇÃO TRIBUTÁRIA", fSecao));
            document.add(new Paragraph("Visão consolidada do volume auditado e índice global de conformidade da empresa com a Reforma Tributária:", fMuted));

            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{25f, 25f, 25f, 25f});
            kpiTable.setSpacingBefore(6);
            kpiTable.setSpacingAfter(12);

            BigDecimal vTotal = kpi != null ? kpi.getValorTotalNotas() : BigDecimal.ZERO;
            BigDecimal taxaConf = kpi != null ? kpi.getTaxaConformidade() : BigDecimal.ZERO;
            BigDecimal vIbs = kpi != null ? kpi.getValorTotalIbs() : BigDecimal.ZERO;
            BigDecimal vCbs = kpi != null ? kpi.getValorTotalCbs() : BigDecimal.ZERO;
            int totalNf = kpi != null ? kpi.getTotalNotas() : 0;
            int conf = kpi != null ? kpi.getNotasConformes() : 0;
            int div = kpi != null ? kpi.getNotasDivergentes() : 0;

            adicionarCardKpi(kpiTable, "VOLUME FATURADO", MOEDA.format(vTotal), totalNf + " notas auditadas", ACCENT_BLUE, BG_LIGHT);

            Color corConf = taxaConf.compareTo(BigDecimal.valueOf(80)) >= 0 ? SUCCESS_GREEN : (taxaConf.compareTo(BigDecimal.valueOf(50)) >= 0 ? WARNING_AMBER : DANGER_RED);
            Color bgConf = taxaConf.compareTo(BigDecimal.valueOf(80)) >= 0 ? SUCCESS_BG : (taxaConf.compareTo(BigDecimal.valueOf(50)) >= 0 ? WARNING_BG : DANGER_BG);
            adicionarCardKpi(kpiTable, "CONFORMIDADE", PERCENT.format(taxaConf), conf + " conf. / " + div + " diverg.", corConf, bgConf);

            adicionarCardKpi(kpiTable, "TOTAL IBS APURADO", MOEDA.format(vIbs), "Alíquota estadual/mun.", SUCCESS_GREEN, BG_LIGHT);
            adicionarCardKpi(kpiTable, "TOTAL CBS APURADA", MOEDA.format(vCbs), "Alíquota federal", ACCENT_BLUE, BG_LIGHT);

            document.add(kpiTable);

            // 4. Detalhamento de Documentos e Fluxos
            PdfPTable fluxosTable = new PdfPTable(3);
            fluxosTable.setWidthPercentage(100);
            fluxosTable.setWidths(new float[]{34f, 33f, 33f});
            fluxosTable.setSpacingAfter(12);

            int nfe = 0, nfce = 0, canc = 0;
            if (notas != null) {
                for (NotaFiscalResumo n : notas) {
                    if (n.isCancelada()) canc++;
                    else if ("65".equals(n.getModelo())) nfce++;
                    else nfe++;
                }
            }

            adicionarLinhaFluxo(fluxosTable, "🛒 Vendas & Saídas", (nfe + nfce) + " notas emitidas", MOEDA.format(kpi != null ? kpi.getValorVendasComabel() : BigDecimal.ZERO));
            adicionarLinhaFluxo(fluxosTable, "🏢 Compras Fornecedores", (kpi != null ? kpi.getTotalEntradasFornecedor() : 0) + " notas recebidas", MOEDA.format(kpi != null ? kpi.getValorComprasFornecedor() : BigDecimal.ZERO));
            adicionarLinhaFluxo(fluxosTable, "🔄 Transferências / Matriz", (kpi != null ? kpi.getTotalTransferencias() : 0) + " notas internas", MOEDA.format(kpi != null ? kpi.getValorTransferencias() : BigDecimal.ZERO));

            document.add(fluxosTable);

            // 5. Diagnóstico de Riscos & Top Produtos com Divergência
            document.add(new Paragraph("2. PRINCIPAIS DIVERGÊNCIAS DETECTADAS & CORREÇÃO INDICADA", fSecao));
            document.add(new Paragraph("Relação dos principais itens com divergência de classificação tributária (cClassTrib) ou CST frente à LC 214/2025:", fMuted));

            PdfPTable divTable = new PdfPTable(6);
            divTable.setWidthPercentage(100);
            divTable.setWidths(new float[]{12f, 26f, 12f, 13f, 15f, 22f});
            divTable.setSpacingBefore(6);
            divTable.setSpacingAfter(14);

            adicionarHeaderTabela(divTable, "Código");
            adicionarHeaderTabela(divTable, "Produto / Descrição");
            adicionarHeaderTabela(divTable, "NCM");
            adicionarHeaderTabela(divTable, "CST Atual / Sug.");
            adicionarHeaderTabela(divTable, "cClass Atual / Sug.");
            adicionarHeaderTabela(divTable, "Correção Recomendada");

            if (itensDivergentes != null && !itensDivergentes.isEmpty()) {
                int limit = Math.min(itensDivergentes.size(), 12); // Top 12 itens no resumo executivo
                for (int i = 0; i < limit; i++) {
                    ItemAuditoria item = itensDivergentes.get(i);
                    Color bg = (i % 2 == 0) ? Color.WHITE : BG_LIGHT;

                    divTable.addCell(celulaTabela(item.getcProd() != null ? item.getcProd() : "-", bg, fTexto, Element.ALIGN_CENTER));
                    String prod = item.getxProd() != null ? (item.getxProd().length() > 32 ? item.getxProd().substring(0, 30) + "..." : item.getxProd()) : "-";
                    divTable.addCell(celulaTabela(prod, bg, fTextoBold, Element.ALIGN_LEFT));
                    divTable.addCell(celulaTabela(item.getNcm() != null ? item.getNcm() : "-", bg, fTexto, Element.ALIGN_CENTER));

                    String cstAtual = item.getCstIbsCbs() != null ? item.getCstIbsCbs() : "Sem CST";
                    String cstSug = item.getCstIbsCbsSugerido() != null ? item.getCstIbsCbsSugerido() : "-";
                    divTable.addCell(celulaTabela(cstAtual + " ➔ " + cstSug, bg, fTexto, Element.ALIGN_CENTER));

                    String clAtual = item.getcClassTrib() != null ? item.getcClassTrib() : "Sem cClass";
                    String clSug = item.getcClassTribSugerido() != null ? item.getcClassTribSugerido() : "-";
                    divTable.addCell(celulaTabela(clAtual + " ➔ " + clSug, bg, fTexto, Element.ALIGN_CENTER));

                    String corr = item.getCorrecaoSugerida() != null ? (item.getCorrecaoSugerida().length() > 50 ? item.getCorrecaoSugerida().substring(0, 48) + "..." : item.getCorrecaoSugerida()) : "Revisar enquadramento SVRS";
                    divTable.addCell(celulaTabela(corr, bg, fTexto, Element.ALIGN_LEFT));
                }
            } else {
                PdfPCell cVazio = new PdfPCell(new Paragraph("Nenhuma inconsistência fiscal identificada no lote auditado. Documentos 100% conformes.", fTextoBold));
                cVazio.setColspan(6);
                cVazio.setPadding(10);
                cVazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                divTable.addCell(cVazio);
            }

            document.add(divTable);

            // 6. Recomendações Estratégicas para o Empresário
            document.add(new Paragraph("3. RECOMENDAÇÕES PRÁTICAS PARA A DIRETORIA", fSecao));

            PdfPTable recTable = new PdfPTable(1);
            recTable.setWidthPercentage(100);
            recTable.setSpacingBefore(4);
            recTable.setSpacingAfter(14);

            PdfPCell recCell = new PdfPCell();
            recCell.setBackgroundColor(BG_LIGHT);
            recCell.setBorderColor(BORDER_GRAY);
            recCell.setPadding(8);

            Font fRecItem = FontFactory.getFont(FontFactory.HELVETICA, 8.2f, TEXT_DARK);
            recCell.addElement(new Paragraph("• ATUALIZAÇÃO DO CADASTRO DE PRODUTOS NO ERP: Parametrizar a correlação de NCM com os novos códigos de classificação tributária (cClassTrib) estabelecidos pela SVRS e LC 214/2025.", fRecItem));
            recCell.addElement(new Paragraph("• PRODUTOS DE CESTA BÁSICA & ALIMENTAÇÃO: Aplicar redução de 100% (Anexo I) e 60% (Anexo VII) exclusivamente nos NCMs elegíveis com CST 200 para evitar autuação fiscal e cobrança indevida.", fRecItem));
            recCell.addElement(new Paragraph("• CONCILIAÇÃO DE COMPRAS X VENDAS: Garantir a uniformidade entre os códigos emitidos pelos seus fornecedores e os códigos adotados nas suas saídas, prevenindo glosa de créditos compensatórios.", fRecItem));
            recCell.addElement(new Paragraph("• SISTEMA EMISSOR DE NOTAS: Verificar com a software-house do ERP a atualização dos grupos de totalizadores <IBSCBSTot> e preenchimento dos nós de detalhamento por item <IBSCBS>.", fRecItem));

            recTable.addCell(recCell);
            document.add(recTable);

            // 7. Termo de Encerramento e Assinaturas
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50f, 50f});
            signTable.setSpacingBefore(12);

            PdfPCell s1 = new PdfPCell();
            s1.setBorder(Rectangle.NO_BORDER);
            s1.setHorizontalAlignment(Element.ALIGN_CENTER);
            s1.addElement(new Paragraph("____________________________________________", fMuted));
            s1.addElement(new Paragraph("AUDITORIA / CONSULTORIA FISCAL", fTextoBold));
            s1.addElement(new Paragraph("Responsável Técnico pela Avaliação", fMuted));
            signTable.addCell(s1);

            PdfPCell s2 = new PdfPCell();
            s2.setBorder(Rectangle.NO_BORDER);
            s2.setHorizontalAlignment(Element.ALIGN_CENTER);
            s2.addElement(new Paragraph("____________________________________________", fMuted));
            s2.addElement(new Paragraph("DIRETORIA / REPRESENTANTE LEGAL", fTextoBold));
            s2.addElement(new Paragraph((empresa != null && empresa.getRazaoSocial() != null) ? empresa.getNomeExibicao() : "Empresa Auditada", fMuted));
            signTable.addCell(s2);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório executivo em PDF: " + e.getMessage(), e);
        }
    }

    private void adicionarCardKpi(PdfPTable table, String titulo, String valor, String sub, Color corValor, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(7);

        c.addElement(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, TEXT_MUTED)));
        Paragraph pVal = new Paragraph(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, corValor));
        pVal.setSpacingBefore(2);
        pVal.setSpacingAfter(2);
        c.addElement(pVal);
        c.addElement(new Paragraph(sub, FontFactory.getFont(FontFactory.HELVETICA, 7f, TEXT_MUTED)));

        table.addCell(c);
    }

    private void adicionarLinhaFluxo(PdfPTable table, String titulo, String notas, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(Color.WHITE);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(6);

        c.addElement(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PRIMARY_NAVY)));
        c.addElement(new Paragraph(notas, FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXT_MUTED)));
        c.addElement(new Paragraph(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, ACCENT_BLUE)));

        table.addCell(c);
    }

    private void adicionarHeaderTabela(PdfPTable table, String titulo) {
        PdfPCell c = new PdfPCell(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
        c.setBackgroundColor(HEADER_BLUE);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(c);
    }

    private PdfPCell celulaTabela(String texto, Color bg, Font font, int align) {
        PdfPCell c = new PdfPCell(new Paragraph(texto, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    // Evento de Rodapé com Número de Página e Aviso Legal
    private static class RodapeNumerado extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font fFoot = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(148, 163, 184));

            Phrase pLeft = new Phrase("Portal Reforma Tributária 2026 • Auditoria IBS / CBS • Conforme LC 214/2025", fFoot);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, pLeft, document.leftMargin(), 16, 0);

            Phrase pRight = new Phrase("Página " + writer.getPageNumber(), fFoot);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pRight, document.right() - document.rightMargin() + 32, 16, 0);
        }
    }
}
