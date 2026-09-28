package br.com.comabel.reforma.service;

import br.com.comabel.reforma.model.ItemAuditoria;
import br.com.comabel.reforma.model.KpiDashboard;
import br.com.comabel.reforma.model.NotaFiscalResumo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] gerarExcelAuditoria(KpiDashboard kpis, List<NotaFiscalResumo> notas, List<ItemAuditoria> itens) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            // Aba 1: Resumo
            Sheet sheetResumo = workbook.createSheet("Resumo_Executivo");
            Row r0 = sheetResumo.createRow(0);
            r0.createCell(0).setCellValue("Indicador");
            r0.createCell(1).setCellValue("Valor");
            r0.getCell(0).setCellStyle(headerStyle);
            r0.getCell(1).setCellStyle(headerStyle);

            Object[][] kpiData = {
                {"Total de Notas Processadas", kpis.getTotalNotas()},
                {"Notas 100% Conformes", kpis.getNotasConformes()},
                {"Notas com Divergências", kpis.getNotasDivergentes()},
                {"Taxa Geral de Conformidade", kpis.getTaxaConformidade() + "%"},
                {"Valor Total das Notas (R$)", kpis.getValorTotalNotas().doubleValue()},
                {"Total ICMS Tradicional (R$)", kpis.getValorTotalIcms().doubleValue()},
                {"Base de Cálculo IBS/CBS (R$)", kpis.getBaseCalculoIbsCbs().doubleValue()},
                {"Total IBS Calculado (R$)", kpis.getValorTotalIbs().doubleValue()},
                {"Total CBS Calculada (R$)", kpis.getValorTotalCbs().doubleValue()}
            };

            for (int i = 0; i < kpiData.length; i++) {
                Row row = sheetResumo.createRow(i + 1);
                row.createCell(0).setCellValue(kpiData[i][0].toString());
                if (kpiData[i][1] instanceof Number) {
                    row.createCell(1).setCellValue(((Number) kpiData[i][1]).doubleValue());
                } else {
                    row.createCell(1).setCellValue(kpiData[i][1].toString());
                }
            }
            sheetResumo.autoSizeColumn(0);
            sheetResumo.autoSizeColumn(1);

            // Aba 2: Notas
            Sheet sheetNotas = workbook.createSheet("Auditoria_Notas");
            Row rNfHead = sheetNotas.createRow(0);
            String[] headsNf = {"Modelo", "Chave de Acesso", "Número", "Série", "Data Emissão", "Tipo", "CNPJ Emitente", "Emitente", "UF", "CNPJ Destinatário", "Destinatário", "Valor NF", "Base IBS/CBS", "Valor IBS", "Valor CBS", "Itens", "Divergências", "Status", "Observações", "Correção Indicada"};
            for (int i = 0; i < headsNf.length; i++) {
                Cell c = rNfHead.createCell(i);
                c.setCellValue(headsNf[i]);
                c.setCellStyle(headerStyle);
            }

            for (int i = 0; i < notas.size(); i++) {
                NotaFiscalResumo n = notas.get(i);
                Row row = sheetNotas.createRow(i + 1);
                row.createCell(0).setCellValue("65".equals(n.getModelo()) ? "NFC-e" : "NF-e");
                row.createCell(1).setCellValue(n.getChNFe() != null ? n.getChNFe() : "");
                row.createCell(2).setCellValue(n.getnNF() != null ? n.getnNF() : "");
                row.createCell(3).setCellValue(n.getSerie() != null ? n.getSerie() : "");
                row.createCell(4).setCellValue(n.getDhEmi() != null ? n.getDhEmi() : "");
                row.createCell(5).setCellValue(n.getTpNF() != null ? n.getTpNF() : "");
                row.createCell(6).setCellValue(n.getEmitCnpj() != null ? n.getEmitCnpj() : "");
                row.createCell(7).setCellValue(n.getEmitNome() != null ? n.getEmitNome() : "");
                row.createCell(8).setCellValue(n.getEmitUf() != null ? n.getEmitUf() : "");
                row.createCell(9).setCellValue(n.getDestCnpj() != null ? n.getDestCnpj() : "");
                row.createCell(10).setCellValue(n.getDestNome() != null ? n.getDestNome() : "");
                row.createCell(11).setCellValue(n.getvNF() != null ? n.getvNF().doubleValue() : 0.0);
                row.createCell(12).setCellValue(n.getvBcIbsCbs() != null ? n.getvBcIbsCbs().doubleValue() : 0.0);
                row.createCell(13).setCellValue(n.getvIbs() != null ? n.getvIbs().doubleValue() : 0.0);
                row.createCell(14).setCellValue(n.getvCbs() != null ? n.getvCbs().doubleValue() : 0.0);
                row.createCell(15).setCellValue(n.getTotalItens());
                row.createCell(16).setCellValue(n.getItensDivergentes());
                row.createCell(17).setCellValue(n.isCancelada() ? "CANCELADA" : (n.getStatus() != null ? n.getStatus() : ""));
                row.createCell(18).setCellValue(n.getObsTotais() != null ? n.getObsTotais() : "");
                row.createCell(19).setCellValue(n.getCorrecaoSugerida() != null ? n.getCorrecaoSugerida() : "");
            }

            // Aba 3: Itens
            Sheet sheetItens = workbook.createSheet("Auditoria_Itens");
            Row rItHead = sheetItens.createRow(0);
            String[] headsIt = {"Chave NF", "NF-e", "Item", "Cód. Produto", "Descrição", "NCM", "CFOP", "Valor Item", "CST ICMS", "Valor ICMS", "Grupo IBS/CBS", "CST IBS/CBS", "cClassTrib", "Base IBS/CBS", "Redução IBS %", "Valor IBS", "Redução CBS %", "Valor CBS", "Status", "Apontamentos / Divergências", "Correção Indicada (Ação Recomendada)", "cClass Sugerido", "CST Sugerido", "Base Legal"};
            for (int i = 0; i < headsIt.length; i++) {
                Cell c = rItHead.createCell(i);
                c.setCellValue(headsIt[i]);
                c.setCellStyle(headerStyle);
            }

            for (int i = 0; i < itens.size(); i++) {
                ItemAuditoria it = itens.get(i);
                Row row = sheetItens.createRow(i + 1);
                row.createCell(0).setCellValue(it.getChNFe());
                row.createCell(1).setCellValue(it.getnNF());
                row.createCell(2).setCellValue(it.getnItem());
                row.createCell(3).setCellValue(it.getcProd());
                row.createCell(4).setCellValue(it.getxProd());
                row.createCell(5).setCellValue(it.getNcm());
                row.createCell(6).setCellValue(it.getCfop());
                row.createCell(7).setCellValue(it.getvProd().doubleValue());
                row.createCell(8).setCellValue(it.getCstIcms());
                row.createCell(9).setCellValue(it.getvIcms().doubleValue());
                row.createCell(10).setCellValue(it.isHasIbsCbs() ? "SIM" : "NÃO");
                row.createCell(11).setCellValue(it.getCstIbsCbs());
                row.createCell(12).setCellValue(it.getcClassTrib());
                row.createCell(13).setCellValue(it.getvBcIbsCbs().doubleValue());
                row.createCell(14).setCellValue(it.getpRedAliqIbs().doubleValue());
                row.createCell(15).setCellValue(it.getvIbs().doubleValue());
                row.createCell(16).setCellValue(it.getpRedAliqCbs().doubleValue());
                row.createCell(17).setCellValue(it.getvCbs().doubleValue());
                row.createCell(18).setCellValue(it.getStatus());
                row.createCell(19).setCellValue(it.getOcorrenciasFormatadas());
                row.createCell(20).setCellValue(it.getCorrecaoSugerida() != null ? it.getCorrecaoSugerida() : "");
                row.createCell(21).setCellValue(it.getcClassTribSugerido() != null ? it.getcClassTribSugerido() : "");
                row.createCell(22).setCellValue(it.getCstIbsCbsSugerido() != null ? it.getCstIbsCbsSugerido() : "");
                row.createCell(23).setCellValue(it.getFundamentacaoSugerida() != null ? it.getFundamentacaoSugerida() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar planilha Excel: " + e.getMessage(), e);
        }
    }

    public byte[] gerarExcelProdutosDivergentes(List<ItemAuditoria> itensDivergentes, br.com.comabel.reforma.model.EmpresaAuditada empresa) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Produtos_Divergentes");

            // Estilos
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle metaStyle = workbook.createCellStyle();
            Font metaFont = workbook.createFont();
            metaFont.setBold(true);
            metaStyle.setFont(metaFont);
            metaStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            metaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            CreationHelper ch = workbook.getCreationHelper();
            currencyStyle.setDataFormat(ch.createDataFormat().getFormat("R$ #,##0.00"));

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(ch.createDataFormat().getFormat("0.00%"));

            // Linha 0: Cabeçalho Título
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("RELATÓRIO DE PRODUTOS COM DIVERGÊNCIA DE CONFORMIDADE - REFORMA TRIBUTÁRIA 2026 (IBS / CBS)");
            c0.setCellStyle(titleStyle);

            // Linha 1: Metadados da Empresa
            Row r1 = sheet.createRow(1);
            String empresaNome = (empresa != null && empresa.getRazaoSocial() != null) ? empresa.getRazaoSocial() : "Empresa Auditada";
            String cnpj = (empresa != null && empresa.getCnpjPrincipal() != null) ? empresa.getCnpjPrincipal() : "-";
            String periodo = (empresa != null && empresa.getPeriodoAuditado() != null) ? empresa.getPeriodoAuditado() : "-";
            r1.createCell(0).setCellValue("Empresa: " + empresaNome + "  |  CNPJ Principal: " + cnpj + "  |  Período: " + periodo);
            r1.getCell(0).setCellStyle(metaStyle);

            // Linha 2: Resumo
            Row r2 = sheet.createRow(2);
            double totalValor = itensDivergentes.stream().mapToDouble(i -> i.getvProd() != null ? i.getvProd().doubleValue() : 0.0).sum();
            r2.createCell(0).setCellValue("Total de Itens com Divergência: " + itensDivergentes.size() + " produto(s)  |  Impacto Financeiro Total: R$ " + String.format("%.2f", totalValor));
            r2.getCell(0).setCellStyle(metaStyle);

            // Linha 4: Cabeçalhos da Tabela
            Row rHead = sheet.createRow(4);
            String[] heads = {
                "Chave NF-e", "NF-e", "Data Emissão", "Item", "Cód. Produto", "Descrição do Produto", "NCM", "CFOP",
                "Valor Item (R$)", "CST ICMS", "Grupo IBS/CBS", "cClass Informado", "CST IBS Informado", "Base IBS/CBS (R$)",
                "Redução Informada %", "Inconsistências Identificadas", "💡 Correção Indicada (Ação Recomendada)",
                "cClass Sugerido", "CST Sugerido", "Redução Sugerida %", "Fundamentação Legal (LC 214/2025)"
            };

            for (int i = 0; i < heads.length; i++) {
                Cell c = rHead.createCell(i);
                c.setCellValue(heads[i]);
                c.setCellStyle(headerStyle);
            }

            // Linhas de Dados
            int rowIdx = 5;
            for (ItemAuditoria it : itensDivergentes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(it.getChNFe() != null ? it.getChNFe() : "");
                row.createCell(1).setCellValue(it.getnNF() != null ? it.getnNF() : "");
                row.createCell(2).setCellValue(it.getDhEmi() != null ? it.getDhEmi() : "");
                row.createCell(3).setCellValue(it.getnItem() != null ? it.getnItem() : "");
                row.createCell(4).setCellValue(it.getcProd() != null ? it.getcProd() : "");
                row.createCell(5).setCellValue(it.getxProd() != null ? it.getxProd() : "");
                row.createCell(6).setCellValue(it.getNcm() != null ? it.getNcm() : "");
                row.createCell(7).setCellValue(it.getCfop() != null ? it.getCfop() : "");
                
                Cell cV = row.createCell(8);
                cV.setCellValue(it.getvProd() != null ? it.getvProd().doubleValue() : 0.0);
                cV.setCellStyle(currencyStyle);

                row.createCell(9).setCellValue(it.getCstIcms() != null ? it.getCstIcms() : "");
                row.createCell(10).setCellValue(it.isHasIbsCbs() ? "SIM" : "NÃO");
                row.createCell(11).setCellValue(it.getcClassTrib() != null ? it.getcClassTrib() : "");
                row.createCell(12).setCellValue(it.getCstIbsCbs() != null ? it.getCstIbsCbs() : "");

                Cell cBc = row.createCell(13);
                cBc.setCellValue(it.getvBcIbsCbs() != null ? it.getvBcIbsCbs().doubleValue() : 0.0);
                cBc.setCellStyle(currencyStyle);

                row.createCell(14).setCellValue(it.getpRedAliqIbs() != null ? it.getpRedAliqIbs().doubleValue() : 0.0);
                row.createCell(15).setCellValue(it.getOcorrenciasFormatadas());
                row.createCell(16).setCellValue(it.getCorrecaoSugerida() != null ? it.getCorrecaoSugerida() : "");
                row.createCell(17).setCellValue(it.getcClassTribSugerido() != null ? it.getcClassTribSugerido() : "");
                row.createCell(18).setCellValue(it.getCstIbsCbsSugerido() != null ? it.getCstIbsCbsSugerido() : "");
                row.createCell(19).setCellValue(it.getpRedSugerido() != null ? it.getpRedSugerido().doubleValue() : 0.0);
                row.createCell(20).setCellValue(it.getFundamentacaoSugerida() != null ? it.getFundamentacaoSugerida() : "");
            }

            // Auto-size das primeiras colunas principais
            for (int i = 0; i < 10; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar planilha de divergências: " + e.getMessage(), e);
        }
    }
}
