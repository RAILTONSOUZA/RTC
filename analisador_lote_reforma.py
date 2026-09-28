# -*- coding: utf-8 -*-
"""
Analisador e Auditor de Lote de XMLs de NF-e na Reforma Tributária (IBS / CBS)
Compatível com a LC 214/2025, EC 132/2023 e Regras de Validação SVRS / CGIBS.
"""

import os
import sys
import glob
import json
import argparse
from decimal import Decimal, ROUND_HALF_UP
import xml.etree.ElementTree as ET
import pandas as pd
from datetime import datetime

def dec(val, default="0.00"):
    if val is None or val == "":
        return Decimal(default)
    try:
        cleaned = str(val).strip().replace(",", ".")
        return Decimal(cleaned)
    except:
        return Decimal(default)

def format_currency(val):
    if val is None:
        return "R$ 0,00"
    return f"R$ {val:,.2f}".replace(",", "_").replace(".", ",").replace("_", ".")

def format_percent(val):
    if val is None:
        return "0,00%"
    return f"{val:.2f}%".replace(".", ",")

class AuditorLoteReforma:
    def __init__(self, regras_json_path=None):
        self.regras_path = regras_json_path or os.path.join(os.path.dirname(__file__), "regras_cclass_svrs.json")
        self.regras = self.carregar_regras()
        
    def carregar_regras(self):
        if os.path.exists(self.regras_path):
            with open(self.regras_path, "r", encoding="utf-8") as f:
                return json.load(f)
        return {"classificacoes": {}, "ncm_map": {}}

    def processar_pasta(self, pasta_xml, recursivo=True):
        padrao = os.path.join(pasta_xml, "**", "*.xml") if recursivo else os.path.join(pasta_xml, "*.xml")
        todos_arquivos = glob.glob(padrao, recursive=recursivo)
        # Ignorar pastas ocultas como .git
        arquivos = [a for a in todos_arquivos if ".git" not in a.split(os.sep)]
        print(f"\n[INFO] Encontrados {len(arquivos)} arquivos XML para processamento na pasta: {pasta_xml}")
        
        notas_resumo = []
        itens_detalhados = []
        
        for idx, arq in enumerate(arquivos, 1):
            try:
                resumo_nf, itens_nf = self.auditar_arquivo_xml(arq)
                if resumo_nf:
                    notas_resumo.append(resumo_nf)
                    itens_detalhados.extend(itens_nf)
            except Exception as e:
                print(f"[ERRO] Falha ao processar {os.path.basename(arq)}: {e}")
                
        return notas_resumo, itens_detalhados

    def auditar_arquivo_xml(self, caminho_arquivo):
        tree = ET.parse(caminho_arquivo)
        root = tree.getroot()
        
        # Helper para remover namespace
        def tag_name(elem):
            return elem.tag.split("}")[-1] if "}" in elem.tag else elem.tag
        
        def find_el(elem, path):
            # busca ignorando namespaces
            parts = path.split("/")
            curr = [elem]
            for part in parts:
                next_curr = []
                for c in curr:
                    for child in c:
                        if tag_name(child) == part:
                            next_curr.append(child)
                curr = next_curr
                if not curr:
                    return None
            return curr[0] if curr else None
        
        def text_el(elem, path, default=""):
            el = find_el(elem, path)
            return el.text if el is not None and el.text is not None else default

        # Cabeçalho
        ide = find_el(root, "NFe/infNFe/ide")
        if ide is None:
            ide = find_el(root, "infNFe/ide")
        if ide is None:
            return None, []
        
        infNFe = find_el(root, "NFe/infNFe")
        if infNFe is None:
            infNFe = find_el(root, "infNFe")
        chNFe = infNFe.attrib.get("Id", "").replace("NFe", "") if infNFe is not None else ""
        nNF = text_el(ide, "nNF")
        serie = text_el(ide, "serie")
        dhEmi = text_el(ide, "dhEmi")[:10]
        tpNF = "SAIDA" if text_el(ide, "tpNF") == "1" else "ENTRADA"
        
        emit = find_el(root, "NFe/infNFe/emit")
        if emit is None:
            emit = find_el(root, "infNFe/emit")
        emit_cnpj = text_el(emit, "CNPJ") or text_el(emit, "CPF")
        emit_nome = text_el(emit, "xNome")
        emit_uf = text_el(emit, "enderEmit/UF")
        
        dest = find_el(root, "NFe/infNFe/dest")
        if dest is None:
            dest = find_el(root, "infNFe/dest")
        dest_cnpj = text_el(dest, "CNPJ") or text_el(dest, "CPF")
        dest_nome = text_el(dest, "xNome")
        dest_uf = text_el(dest, "enderDest/UF")
        
        total = find_el(root, "NFe/infNFe/total")
        if total is None:
            total = find_el(root, "infNFe/total")
        vNF = dec(text_el(total, "ICMSTot/vNF"))
        vICMS_tot = dec(text_el(total, "ICMSTot/vICMS"))
        
        ibscbs_tot = find_el(total, "IBSCBSTot")
        has_ibscbs_tot = ibscbs_tot is not None
        vBC_ibscbs_tot = dec(text_el(ibscbs_tot, "vBCIBSCBS")) if has_ibscbs_tot else Decimal(0)
        vIBS_tot = dec(text_el(ibscbs_tot, "gIBS/vIBS")) if has_ibscbs_tot else Decimal(0)
        vCBS_tot = dec(text_el(ibscbs_tot, "gCBS/vCBS")) if has_ibscbs_tot else Decimal(0)
        
        # Itens
        dets = root.findall(".//{*}det")
        itens = []
        total_divergencias_nf = 0
        soma_bc_itens = Decimal(0)
        soma_ibs_itens = Decimal(0)
        soma_cbs_itens = Decimal(0)
        
        for det in dets:
            nItem = det.attrib.get("nItem", "")
            prod = find_el(det, "prod")
            cProd = text_el(prod, "cProd")
            xProd = text_el(prod, "xProd")
            ncm = text_el(prod, "NCM")
            cfop = text_el(prod, "CFOP")
            qCom = dec(text_el(prod, "qCom"))
            vUnCom = dec(text_el(prod, "vUnCom"))
            vProd = dec(text_el(prod, "vProd"))
            
            imposto = find_el(det, "imposto")
            
            # ICMS Atual
            icms_node = find_el(imposto, "ICMS")
            cst_icms = ""
            vbc_icms = Decimal(0)
            vicms = Decimal(0)
            if icms_node is not None and len(icms_node) > 0:
                sub_icms = icms_node[0]
                cst_icms = text_el(sub_icms, "CST") or text_el(sub_icms, "CSOSN")
                vbc_icms = dec(text_el(sub_icms, "vBC"))
                vicms = dec(text_el(sub_icms, "vICMS"))
            
            # Reforma Tributária (IBS/CBS)
            ibscbs_node = find_el(imposto, "IBSCBS")
            has_ibscbs = ibscbs_node is not None
            
            cst_ibscbs = text_el(ibscbs_node, "CST") if has_ibscbs else ""
            if cst_ibscbs and len(cst_ibscbs) < 3:
                cst_ibscbs = cst_ibscbs.zfill(3)
                
            cClassTrib = text_el(ibscbs_node, "cClassTrib") if has_ibscbs else ""
            if cClassTrib and len(cClassTrib) < 6:
                cClassTrib = cClassTrib.zfill(6)
            
            gIBSCBS = find_el(ibscbs_node, "gIBSCBS") if has_ibscbs else None
            vBC_item = dec(text_el(gIBSCBS, "vBC")) if gIBSCBS is not None else Decimal(0)
            
            pIBSUF = dec(text_el(gIBSCBS, "gIBSUF/pIBSUF")) if gIBSCBS is not None else Decimal(0)
            pRedAliqIBS = dec(text_el(gIBSCBS, "gIBSUF/gRed/pRedAliq")) if gIBSCBS is not None else Decimal(0)
            vIBSUF = dec(text_el(gIBSCBS, "gIBSUF/vIBSUF")) if gIBSCBS is not None else Decimal(0)
            
            pIBSMun = dec(text_el(gIBSCBS, "gIBSMun/pIBSMun")) if gIBSCBS is not None else Decimal(0)
            vIBSMun = dec(text_el(gIBSCBS, "gIBSMun/vIBSMun")) if gIBSCBS is not None else Decimal(0)
            vIBS_item = dec(text_el(gIBSCBS, "vIBS")) if gIBSCBS is not None else Decimal(0)
            
            pCBS = dec(text_el(gIBSCBS, "gCBS/pCBS")) if gIBSCBS is not None else Decimal(0)
            pRedAliqCBS = dec(text_el(gIBSCBS, "gCBS/gRed/pRedAliq")) if gIBSCBS is not None else Decimal(0)
            vCBS_item = dec(text_el(gIBSCBS, "gCBS/vCBS")) if gIBSCBS is not None else Decimal(0)
            
            soma_bc_itens += vBC_item
            soma_ibs_itens += vIBS_item
            soma_cbs_itens += vCBS_item
            
            # --- MOTOR DE AUDITORIA DE CONFORMIDADE ---
            apontamentos = []
            
            if not has_ibscbs:
                apontamentos.append("XML sem grupo IBSCBS (Regime anterior)")
            else:
                regra_cclass = self.regras.get("classificacoes", {}).get(cClassTrib)
                if not regra_cclass:
                    apontamentos.append(f"cClassTrib '{cClassTrib}' não reconhecido na base SVRS")
                else:
                    cst_esperado = regra_cclass.get("cst_esperado")
                    if cst_ibscbs != cst_esperado:
                        apontamentos.append(f"CST incorreto: informado {cst_ibscbs}, esperado {cst_esperado} para cClass {cClassTrib}")
                    
                    # Checagem de NCM se a classificacao tiver restrição de anexos
                    ncms_permitidos = regra_cclass.get("ncms_permitidos", [])
                    if ncms_permitidos and ncm not in ncms_permitidos:
                        # verificar se prefixo de 4 digitos confere
                        prefix_match = any(n.startswith(ncm[:4]) for n in ncms_permitidos)
                        if not prefix_match:
                            apontamentos.append(f"NCM {ncm} não permitido no Anexo da classificação {cClassTrib} ({regra_cclass.get('nome')})")
                    
                    # Checagem de redução
                    red_esperada_ibs = dec(regra_cclass.get("perc_red_ibs", 0.0))
                    red_esperada_cbs = dec(regra_cclass.get("perc_red_cbs", 0.0))
                    if red_esperada_ibs > 0 and abs(pRedAliqIBS - red_esperada_ibs) > Decimal("0.01"):
                        apontamentos.append(f"Redução IBS divergente: informada {pRedAliqIBS}%, esperada {red_esperada_ibs}%")
                    if red_esperada_cbs > 0 and abs(pRedAliqCBS - red_esperada_cbs) > Decimal("0.01"):
                        apontamentos.append(f"Redução CBS divergente: informada {pRedAliqCBS}%, esperada {red_esperada_cbs}%")
            
            is_conforme = len(apontamentos) == 0
            if not is_conforme:
                total_divergencias_nf += 1
                
            item_dict = {
                "chNFe": chNFe,
                "nNF": nNF,
                "dhEmi": dhEmi,
                "nItem": nItem,
                "cProd": cProd,
                "xProd": xProd,
                "NCM": ncm,
                "CFOP": cfop,
                "vProd": float(vProd),
                "CST_ICMS": cst_icms,
                "vICMS": float(vicms),
                "has_IBSCBS": has_ibscbs,
                "CST_IBSCBS": cst_ibscbs,
                "cClassTrib": cClassTrib,
                "vBC_IBSCBS": float(vBC_item),
                "pRedAliq_IBS": float(pRedAliqIBS),
                "vIBS": float(vIBS_item),
                "pRedAliq_CBS": float(pRedAliqCBS),
                "vCBS": float(vCBS_item),
                "Status": "CONFORME" if is_conforme else "DIVERGENTE",
                "Ocorrencias": "; ".join(apontamentos) if apontamentos else "Nenhuma divergência identificada"
            }
            itens.append(item_dict)

        # Checagem de totalizador da NF
        apontamentos_nf = []
        if has_ibscbs_tot:
            if abs(soma_bc_itens - vBC_ibscbs_tot) > Decimal("0.05"):
                apontamentos_nf.append(f"Totalizador vBCIBSCBS ({vBC_ibscbs_tot}) diverge da soma dos itens ({soma_bc_itens})")
            if abs(soma_ibs_itens - vIBS_tot) > Decimal("0.05"):
                apontamentos_nf.append(f"Totalizador vIBS ({vIBS_tot}) diverge da soma dos itens ({soma_ibs_itens})")
            if abs(soma_cbs_itens - vCBS_tot) > Decimal("0.05"):
                apontamentos_nf.append(f"Totalizador vCBS ({vCBS_tot}) diverge da soma dos itens ({soma_cbs_itens})")
        
        status_nf = "CONFORME" if (total_divergencias_nf == 0 and len(apontamentos_nf) == 0) else "DIVERGENTE"
        
        resumo_nf = {
            "chNFe": chNFe,
            "nNF": nNF,
            "serie": serie,
            "dhEmi": dhEmi,
            "tpNF": tpNF,
            "emit_cnpj": emit_cnpj,
            "emit_nome": emit_nome,
            "emit_uf": emit_uf,
            "dest_cnpj": dest_cnpj,
            "dest_nome": dest_nome,
            "dest_uf": dest_uf,
            "vNF": float(vNF),
            "vICMS": float(vICMS_tot),
            "vBC_IBSCBS": float(vBC_ibscbs_tot),
            "vIBS": float(vIBS_tot),
            "vCBS": float(vCBS_tot),
            "total_itens": len(itens),
            "itens_divergentes": total_divergencias_nf,
            "Status": status_nf,
            "Obs_Totais": "; ".join(apontamentos_nf) if apontamentos_nf else "Totais conciliados"
        }
        
        return resumo_nf, itens

    def exportar_excel(self, notas, itens, caminho_saida):
        with pd.ExcelWriter(caminho_saida, engine="openpyxl") as writer:
            df_notas = pd.DataFrame(notas)
            df_itens = pd.DataFrame(itens)
            
            # Aba 1: Resumo
            total_nfs = len(df_notas)
            total_conforme = len(df_notas[df_notas["Status"] == "CONFORME"])
            total_div = len(df_notas[df_notas["Status"] == "DIVERGENTE"])
            perc_conf = (total_conforme / total_nfs * 100) if total_nfs > 0 else 0
            
            resumo_kpis = pd.DataFrame([
                {"Indicador": "Total de Notas Processadas", "Valor": total_nfs},
                {"Indicador": "Notas 100% Conformes", "Valor": total_conforme},
                {"Indicador": "Notas com Divergências", "Valor": total_div},
                {"Indicador": "Taxa Geral de Conformidade", "Valor": f"{perc_conf:.1f}%"},
                {"Indicador": "Valor Total das Notas (R$)", "Valor": f"R$ {df_notas['vNF'].sum():,.2f}"},
                {"Indicador": "Total ICMS Tradicional (R$)", "Valor": f"R$ {df_notas['vICMS'].sum():,.2f}"},
                {"Indicador": "Base de Cálculo IBS/CBS (R$)", "Valor": f"R$ {df_notas['vBC_IBSCBS'].sum():,.2f}"},
                {"Indicador": "Total IBS Calculado (R$)", "Valor": f"R$ {df_notas['vIBS'].sum():,.2f}"},
                {"Indicador": "Total CBS Calculada (R$)", "Valor": f"R$ {df_notas['vCBS'].sum():,.2f}"},
            ])
            resumo_kpis.to_excel(writer, sheet_name="Resumo_Executivo", index=False)
            df_notas.to_excel(writer, sheet_name="Auditoria_Notas", index=False)
            df_itens.to_excel(writer, sheet_name="Auditoria_Itens", index=False)
        print(f"[SUCESSO] Relatório Excel gerado: {caminho_saida}")

    def exportar_html(self, notas, itens, caminho_saida):
        df_notas = pd.DataFrame(notas)
        df_itens = pd.DataFrame(itens)
        
        total_nfs = len(df_notas)
        total_conforme = len(df_notas[df_notas["Status"] == "CONFORME"])
        total_div = len(df_notas[df_notas["Status"] == "DIVERGENTE"])
        perc_conf = (total_conforme / total_nfs * 100) if total_nfs > 0 else 0
        
        v_total = df_notas['vNF'].sum() if not df_notas.empty else 0
        v_icms = df_notas['vICMS'].sum() if not df_notas.empty else 0
        v_bc_ibs = df_notas['vBC_IBSCBS'].sum() if not df_notas.empty else 0
        v_ibs = df_notas['vIBS'].sum() if not df_notas.empty else 0
        v_cbs = df_notas['vCBS'].sum() if not df_notas.empty else 0
        
        # Filtra apenas itens com divergência para a tabela de alertas
        itens_alertas = [it for it in itens if it["Status"] == "DIVERGENTE"]
        
        html_content = f"""<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Auditoria em Lote - Reforma Tributária (IBS/CBS)</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {{
            --bg: #0f172a;
            --surface: #1e293b;
            --card: #334155;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --accent-green: #10b981;
            --accent-red: #ef4444;
            --accent-blue: #38bdf8;
            --accent-purple: #a855f7;
            --border: #475569;
        }}
        * {{ box-sizing: border-box; margin: 0; padding: 0; }}
        body {{
            font-family: 'Inter', sans-serif;
            background-color: var(--bg);
            color: var(--text-main);
            padding: 30px;
            line-height: 1.5;
        }}
        .header {{
            margin-bottom: 30px;
            padding-bottom: 20px;
            border-bottom: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }}
        .header h1 {{ font-size: 24px; font-weight: 700; color: var(--accent-blue); }}
        .header p {{ color: var(--text-muted); font-size: 14px; margin-top: 5px; }}
        .badge-date {{ background: var(--card); padding: 6px 14px; border-radius: 20px; font-size: 13px; color: var(--text-muted); }}
        
        .kpi-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 20px;
            margin-bottom: 35px;
        }}
        .kpi-card {{
            background: var(--surface);
            padding: 20px;
            border-radius: 12px;
            border: 1px solid var(--border);
            position: relative;
        }}
        .kpi-title {{ font-size: 13px; color: var(--text-muted); text-transform: uppercase; font-weight: 600; letter-spacing: 0.5px; }}
        .kpi-value {{ font-size: 26px; font-weight: 700; margin-top: 8px; }}
        .kpi-sub {{ font-size: 12px; margin-top: 5px; color: var(--text-muted); }}
        
        .section-title {{
            font-size: 18px;
            font-weight: 600;
            margin: 30px 0 15px 0;
            display: flex;
            align-items: center;
            gap: 10px;
        }}
        .table-container {{
            background: var(--surface);
            border-radius: 12px;
            border: 1px solid var(--border);
            overflow-x: auto;
            margin-bottom: 30px;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
            text-align: left;
        }}
        th {{
            background: #111827;
            padding: 12px 16px;
            font-weight: 600;
            color: var(--text-muted);
            border-bottom: 1px solid var(--border);
        }}
        td {{
            padding: 12px 16px;
            border-bottom: 1px solid rgba(71, 85, 105, 0.4);
        }}
        tr:hover {{ background: rgba(51, 65, 85, 0.4); }}
        
        .tag {{
            display: inline-block;
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 600;
        }}
        .tag-success {{ background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }}
        .tag-danger {{ background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3); }}
        .tag-cclass {{ background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.3); }}
    </style>
</head>
<body>

    <div class="header">
        <div>
            <h1>Auditoria de Conformidade - Reforma Tributária (IBS / CBS)</h1>
            <p>Validação em Lote de XMLs de NF-e conforme LC 214/2025 e Portal CFF SVRS</p>
        </div>
        <div class="badge-date">Gerado em: {datetime.now().strftime("%d/%m/%Y %H:%M")}</div>
    </div>

    <div class="kpi-grid">
        <div class="kpi-card">
            <div class="kpi-title">Total de Notas</div>
            <div class="kpi-value">{total_nfs}</div>
            <div class="kpi-sub">Faturamento: {format_currency(v_total)}</div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">Conformidade</div>
            <div class="kpi-value" style="color: {'var(--accent-green)' if perc_conf >= 90 else 'var(--accent-red)'}">{perc_conf:.1f}%</div>
            <div class="kpi-sub">{total_conforme} conformes / {total_div} divergentes</div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">Base IBS / CBS</div>
            <div class="kpi-value" style="color: var(--accent-blue)">{format_currency(v_bc_ibs)}</div>
            <div class="kpi-sub">ICMS Anterior: {format_currency(v_icms)}</div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">IBS + CBS Apurados</div>
            <div class="kpi-value" style="color: var(--accent-purple)">{format_currency(v_ibs + v_cbs)}</div>
            <div class="kpi-sub">IBS: {format_currency(v_ibs)} | CBS: {format_currency(v_cbs)}</div>
        </div>
    </div>

    <div class="section-title">📄 Relação de Notas Fiscais Processadas</div>
    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <th>NF-e</th>
                    <th>Emissão</th>
                    <th>Tipo</th>
                    <th>Emitente</th>
                    <th>Destinatário</th>
                    <th>Valor Total</th>
                    <th>Base IBS/CBS</th>
                    <th>Status</th>
                    <th>Observações</th>
                </tr>
            </thead>
            <tbody>"""
        
        for n in notas:
            status_tag = f'<span class="tag tag-success">CONFORME</span>' if n["Status"] == "CONFORME" else f'<span class="tag tag-danger">DIVERGENTE</span>'
            html_content += f"""
                <tr>
                    <td><strong>{n['nNF']}</strong><br><small style="color: var(--text-muted)">{n['chNFe'][:22]}...</small></td>
                    <td>{n['dhEmi']}</td>
                    <td>{n['tpNF']}</td>
                    <td>{n['emit_nome'][:30]}<br><small style="color: var(--text-muted)">{n['emit_cnpj']}</small></td>
                    <td>{n['dest_nome'][:30]}<br><small style="color: var(--text-muted)">{n['dest_cnpj']}</small></td>
                    <td>{format_currency(n['vNF'])}</td>
                    <td>{format_currency(n['vBC_IBSCBS'])}</td>
                    <td>{status_tag}</td>
                    <td>{n['Obs_Totais']}</td>
                </tr>"""

        html_content += """
            </tbody>
        </table>
    </div>

    <div class="section-title">🔍 Detalhamento de Itens com Apontamentos de Divergência</div>
    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <th>NF-e</th>
                    <th>Item</th>
                    <th>Descrição</th>
                    <th>NCM</th>
                    <th>CST IBS/CBS</th>
                    <th>cClassTrib</th>
                    <th>Valor</th>
                    <th>Apontamento / Divergência</th>
                </tr>
            </thead>
            <tbody>"""
            
        if not itens_alertas:
            html_content += """
                <tr>
                    <td colspan="8" style="text-align: center; color: var(--accent-green); padding: 25px;">
                        ✓ Excelente! Nenhum item com divergência fiscal ou tributária foi identificado no lote.
                    </td>
                </tr>"""
        else:
            for it in itens_alertas:
                html_content += f"""
                    <tr>
                        <td><strong>{it['nNF']}</strong></td>
                        <td>#{it['nItem']}</td>
                        <td>{it['xProd']}</td>
                        <td><code>{it['NCM']}</code></td>
                        <td>{it['CST_IBSCBS']}</td>
                        <td><span class="tag tag-cclass">{it['cClassTrib']}</span></td>
                        <td>{format_currency(it['vProd'])}</td>
                        <td style="color: #f87171;">{it['Ocorrencias']}</td>
                    </tr>"""
                    
        html_content += """
            </tbody>
        </table>
    </div>

</body>
</html>"""
        
        with open(caminho_saida, "w", encoding="utf-8") as f:
            f.write(html_content)
        print(f"[SUCESSO] Relatório HTML gerado: {caminho_saida}")

def main():
    parser = argparse.ArgumentParser(description="Auditor e Analisador de Lote de XMLs para Reforma Tributária")
    parser.add_argument("--pasta", default=r"c:\agente\agente", help="Diretório contendo os arquivos XML")
    parser.add_argument("--regras", default=r"c:\agente\agente\regras_cclass_svrs.json", help="Arquivo JSON de regras da SVRS")
    parser.add_argument("--excel", default=r"c:\agente\agente\relatorio_auditoria_reforma.xlsx", help="Caminho do arquivo Excel de saída")
    parser.add_argument("--html", default=r"c:\agente\agente\relatorio_auditoria_reforma.html", help="Caminho do relatório HTML de saída")
    
    args = parser.parse_args()
    
    auditor = AuditorLoteReforma(regras_json_path=args.regras)
    notas, itens = auditor.processar_pasta(pasta_xml=args.pasta)
    
    if not notas:
        print("[AVISO] Nenhuma nota fiscal elegível encontrada para processamento.")
        return

    auditor.exportar_excel(notas, itens, args.excel)
    auditor.exportar_html(notas, itens, args.html)
    
    print("\n=======================================================")
    print("           RESUMO DA AUDITORIA DO LOTE                 ")
    print("=======================================================")
    print(f"Total de Notas Analisadas : {len(notas)}")
    total_conforme = sum(1 for n in notas if n['Status'] == 'CONFORME')
    print(f"Notas Conformes           : {total_conforme} ({total_conforme / len(notas) * 100:.1f}%)")
    print(f"Notas com Divergências    : {len(notas) - total_conforme}")
    print(f"Relatório Excel Salvo em  : {args.excel}")
    print(f"Relatório HTML Salvo em   : {args.html}")
    print("=======================================================\n")

if __name__ == "__main__":
    main()
