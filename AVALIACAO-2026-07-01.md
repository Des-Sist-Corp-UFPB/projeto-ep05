# Avaliação — EQ05 (DSC)

**Data:** 2026-07-01  
**Avaliador:** Prof. Rodrigo  
**Método:** verificação automática cruzando o que o `README.md` declara com evidências no código-fonte (leitura de `origin/main`).

> Esta é uma avaliação automática preliminar. O que não estiver documentado no README e commitado no repositório é considerado não atendido.

---

## 1. Log de Auditoria

✅ **Atendido** — documentado no README e com 205 evidência(s) no código.

---

## 2. Integração com Serviço Externo

- ✅ **Mercado Pago** — declarado no README e comprovado no código (61 ocorrência(s)).
  - Evidência: `backend/docker/docker-compose.dev.yml:71:      MERCADOPAGO_ACCESS_TOKEN: ${MERCADOPAGO_ACCESS_TOKEN}`

_Detectado no código, mas **não documentado** no README (não pontua até ser descrito):_
- ℹ️ ViaCEP

---

## 3. Cobertura de Testes (≥ 85%)

✅ **Atendido** — backend linhas 99.4% (instruções 99.1% · ramos 91.3%) [JaCoCo]; frontend linhas 99.71% (JS) (relatório em `cobertura/`, 165 arquivo(s)).

> Critério: **cobertura de linhas** ≥ 85% (conforme a orientação). As demais métricas (instruções/ramos) são informativas.

> Observação: a cobertura é lida do relatório commitado pela equipe; não é recalculada nesta avaliação.

---

*Avaliação gerada automaticamente em 2026-07-01. Consulte `ORIENTACOES-AVALIACAO-2026-06-29.md` para os critérios.*