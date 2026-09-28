# Sistema de Auditoria Fiscal - Reforma Tributária (IBS / CBS)

Plataforma completa para **Auditoria Fiscal Preventiva e Conformidade** com a nova **Reforma Tributária (EC 132/2023 - PLP 68/2024)**, focada no cruzamento de documentos fiscais eletrônicos (**NF-e / NFC-e**) e registros da **EFD ICMS/IPI (SPED Fiscal)** com validação de regras de classificação (**cClass / cBenef / CST IBS/CBS**).

---

## 🚀 Principais Funcionalidades

1. **Definição Dinâmica de Empresa Auditada por XML**:
   - Identificação automática do CNPJ e Razão Social da empresa através da emissão de NF-e (`<emit>`).
   - Suporte a múltiplas empresas auditadas com seletor no cabeçalho e isolamento de relatórios.

2. **Auditoria de XMLs (NF-e / NFC-e)**:
   - Importação via upload de múltiplos arquivos `.xml` ou arquivos compactados `.zip`.
   - Validação da chave de acesso, destinatário, itens/produtos, NCM, CFOP, CST e alíquotas.
   - Detecção de divergências para o modelo IBS/CBS (ex: itens sem cClass obrigatório, cBenef incompatível, regimes diferenciados).

3. **Auditoria e Cruzamento com SPED Fiscal**:
   - Leitura de registros do SPED Fiscal (Blocos 0, C100, C170, C190, E110, etc.).
   - Cruzamento entre o que foi emitido/recebido em XML e o que foi escriturado no SPED.
   - Detecção de itens e notas faltantes, divergências de valores e tributação.

4. **Painel Executivo e Relatórios**:
   - Dashboard com indicadores consolidados de notas auditadas, divergências e conformidade.
   - **Exportação de Relatório Executivo em PDF** detalhado e diagramado.
   - **Exportação de Relatório Analítico em Excel (.xlsx)** para conciliação contábil/fiscal.

5. **Scripts Analíticos Auxiliares (Python)**:
   - Scripts para processamento em lote e testes de validação com base nas tabelas da SVRS.

---

## 🛠️ Tecnologias Utilizadas

- **Backend**: Java 17+, Spring Boot 3.2.x, Spring Data JPA
- **Banco de Dados**: H2 Database (modo arquivo persistente)
- **Frontend**: Thymeleaf, Bootstrap 5, FontAwesome, Vanilla CSS responsivo
- **Geração de Documentos**: OpenPDF (PDF executivo), Apache POI (Excel)
- **Scripts Auxiliares**: Python 3 (pandas, openpyxl, xml.etree)

---

## 📦 Como Executar

### Pré-requisitos
- **Java JDK 17** ou superior
- **Apache Maven 3.8+** (ou wrapper)

### Executando a Aplicação Web
```bash
cd app-reforma-fiscal
mvn clean spring-boot:run
```
Acesse no navegador:
👉 **`http://localhost:8080`**

---

## 🔒 Segurança e Dados Sensíveis
Arquivos de documentos fiscais reais (`.xml`, `.zip`, `.txt` do SPED) e base de dados local (`data/`) são ignorados pelo `.gitignore` para proteção do sigilo fiscal e conformidade com a LGPD.
