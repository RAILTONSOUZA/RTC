# -*- coding: utf-8 -*-
import collections
from decimal import Decimal, InvalidOperation

p = r"c:\Concretize\Spool\SpedEFD-07068224000184-120829118-Remessa de arquivo original-ago.2026.txt"


def dec(x):
    x = (x or "").strip().replace(".", "").replace(",", ".")
    if not x:
        return Decimal("0")
    try:
        return Decimal(x)
    except InvalidOperation:
        return Decimal("0")


def money(x):
    return f"{x:,.2f}".replace(",", "_").replace(".", ",").replace("_", ".")


c190_by_key = collections.defaultdict(
    lambda: {
        "n": 0,
        "vl_opr": Decimal(0),
        "vl_bc": Decimal(0),
        "vl_icms": Decimal(0),
        "vl_st": Decimal(0),
    }
)
mod_sit = collections.Counter()
oper_emit = collections.Counter()
cancel = collections.Counter()
cfop_tot = collections.defaultdict(
    lambda: {"n": 0, "vl_opr": Decimal(0), "vl_icms": Decimal(0), "vl_st": Decimal(0)}
)
e110 = e210 = e250 = k010 = r1010 = r1601 = None
e111s, e116s, d100s, natops, unids, c100 = [], [], [], [], [], []
parts = {}
part_saida = collections.defaultdict(lambda: Decimal(0))
part_entrada = collections.defaultdict(lambda: Decimal(0))
mods_val = collections.defaultdict(lambda: {"n": 0, "vl": Decimal(0)})
sit_skip = {"02", "03", "04", "05"}
c170_n = 0
c170_val = Decimal(0)
k200_n = 0
k220_n = 0
prods = 0

with open(p, "r", encoding="latin-1") as f:
    for line in f:
        if not line.startswith("|"):
            continue
        fset = line.rstrip("\n").split("|")
        rec = fset[1]
        if rec == "0150":
            parts[fset[2]] = {"nome": fset[3], "cnpj": fset[5], "cpf": fset[6]}
        elif rec == "0190":
            unids.append((fset[2], fset[3] if len(fset) > 3 else ""))
        elif rec == "0200":
            prods += 1
        elif rec == "0400":
            natops.append((fset[2], fset[3] if len(fset) > 3 else ""))
        elif rec == "C100":
            current = {
                "ind_oper": fset[2],
                "ind_emit": fset[3],
                "cod_part": fset[4],
                "mod": fset[5],
                "sit": fset[6],
                "vl_doc": dec(fset[12]),
            }
            oper_emit[f"{fset[2]}|{fset[3]}|{fset[5]}"] += 1
            mod_sit[f"mod{fset[5]} sit{fset[6]}"] += 1
            if fset[6] not in sit_skip:
                mods_val[fset[5]]["n"] += 1
                mods_val[fset[5]]["vl"] += current["vl_doc"]
                if fset[2] == "1" and fset[4]:
                    part_saida[fset[4]] += current["vl_doc"]
                if fset[2] == "0" and fset[4]:
                    part_entrada[fset[4]] += current["vl_doc"]
            else:
                cancel[fset[6]] += 1
            c100.append(current)
        elif rec == "C170":
            c170_n += 1
            if len(fset) > 7:
                c170_val += dec(fset[7])
        elif rec == "C190":
            cst, cfop, aliq = fset[2], fset[3], fset[4]
            vo, vbc, vicms, vst = dec(fset[5]), dec(fset[6]), dec(fset[7]), dec(fset[9])
            k = (cst, cfop, aliq)
            c190_by_key[k]["n"] += 1
            c190_by_key[k]["vl_opr"] += vo
            c190_by_key[k]["vl_bc"] += vbc
            c190_by_key[k]["vl_icms"] += vicms
            c190_by_key[k]["vl_st"] += vst
            cfop_tot[cfop]["n"] += 1
            cfop_tot[cfop]["vl_opr"] += vo
            cfop_tot[cfop]["vl_icms"] += vicms
            cfop_tot[cfop]["vl_st"] += vst
        elif rec == "E110":
            e110 = fset
        elif rec == "E111":
            e111s.append(fset)
        elif rec == "E116":
            e116s.append(fset)
        elif rec == "E210":
            e210 = fset
        elif rec == "E250":
            e250 = fset
        elif rec == "K010":
            k010 = fset
        elif rec == "K200":
            k200_n += 1
        elif rec == "K220":
            k220_n += 1
        elif rec == "D100":
            d100s.append(fset)
        elif rec == "1010":
            r1010 = fset
        elif rec == "1601":
            r1601 = fset

print("=== NATOPS ===")
for a, b in natops:
    print(a, "|", b)
print("=== UNIDS ===")
for a, b in unids:
    print(a, "|", b)
print("prods 0200", prods)
print("=== MODELOS validos ===")
for m, v in sorted(mods_val.items()):
    print(m, v["n"], money(v["vl"]))
print("=== OPER|EMIT|MOD counts ===")
for k, v in oper_emit.most_common():
    print(k, v)
print("=== SIT ===")
for k, v in mod_sit.most_common():
    print(k, v)
print("cancel", dict(cancel))
print("C100", len(c100), "C170", c170_n, "c170_val", money(c170_val))
print("K010", k010)
print("K200 n", k200_n, "K220 n", k220_n)
print("E110 fields", e110)
print("E111")
for x in e111s:
    print(x)
print("E116")
for x in e116s:
    print(x)
print("E210", e210)
print("E250", e250)
print("D100 n", len(d100s))
for x in d100s:
    print("D100", x[2:14])
print("1010", r1010)
print("1601", r1601)

print("\n=== TOP CFOP por VL_OPR ===")
for cfop, v in sorted(cfop_tot.items(), key=lambda kv: kv[1]["vl_opr"], reverse=True):
    print(
        cfop,
        v["n"],
        "opr",
        money(v["vl_opr"]),
        "icms",
        money(v["vl_icms"]),
        "st",
        money(v["vl_st"]),
    )

print("\n=== C190 CST/CFOP/ALIQ top 50 ===")
items = sorted(c190_by_key.items(), key=lambda kv: kv[1]["vl_opr"], reverse=True)
for (cst, cfop, aliq), v in items[:50]:
    print(
        cst,
        cfop,
        aliq + "%",
        v["n"],
        "opr",
        money(v["vl_opr"]),
        "bc",
        money(v["vl_bc"]),
        "icms",
        money(v["vl_icms"]),
        "st",
        money(v["vl_st"]),
    )

print("\n=== TOP SAIDAS ===")
for cod, vl in sorted(part_saida.items(), key=lambda kv: -kv[1])[:25]:
    inf = parts.get(cod, {})
    tipo = "CNPJ" if inf.get("cnpj") else ("CPF" if inf.get("cpf") else "?")
    print(money(vl), tipo, inf.get("cnpj") or inf.get("cpf"), inf.get("nome", "")[:55])

print("\n=== TOP ENTRADAS ===")
for cod, vl in sorted(part_entrada.items(), key=lambda kv: -kv[1])[:20]:
    inf = parts.get(cod, {})
    tipo = "CNPJ" if inf.get("cnpj") else ("CPF" if inf.get("cpf") else "?")
    print(money(vl), tipo, inf.get("cnpj") or inf.get("cpf"), inf.get("nome", "")[:55])

pj = pf = z = Decimal(0)
npj = npf = nz = 0
for cod, vl in part_saida.items():
    inf = parts.get(cod, {})
    if inf.get("cnpj"):
        pj += vl
        npj += 1
    elif inf.get("cpf"):
        pf += vl
        npf += 1
    else:
        z += vl
        nz += 1
print("\nSAIDA PJ", npj, money(pj), "PF", npf, money(pf), "sem", nz, money(z))

sai = ent = Decimal(0)
ns = ne = 0
for x in c100:
    if x["sit"] in sit_skip:
        continue
    if x["ind_oper"] == "1":
        sai += x["vl_doc"]
        ns += 1
    else:
        ent += x["vl_doc"]
        ne += 1
print("C100 validos saida", ns, money(sai))
print("C100 validos entrada", ne, money(ent))

# CFOP groups
def grp(cfop):
    c = cfop[:1]
    n = cfop[1:]
    if cfop.startswith(("5102", "6102", "5405", "6405", "5403", "6403", "5101", "6101")):
        return "saida_revenda"
    if cfop.startswith(("5104", "6104", "5106", "6106")):
        return "saida_revenda"
    if cfop.startswith(("1202", "2202", "1411", "2411")):
        return "devolucao_compra"
    if cfop.startswith(("1102", "2102", "1403", "2403", "1405", "2405")):
        return "entrada_compra"
    if cfop.startswith(("1556", "2556", "1551", "2551")):
        return "uso_consumo_ativo"
    if cfop.startswith(("5929", "6929", "5922", "6922")):
        return "ajuste_lancamento"
    return f"outros_{c}"

g = collections.defaultdict(lambda: Decimal(0))
for cfop, v in cfop_tot.items():
    g[grp(cfop)] += v["vl_opr"]
print("\n=== GRUPOS CFOP ===")
for k, v in sorted(g.items(), key=lambda kv: -kv[1]):
    print(k, money(v))
