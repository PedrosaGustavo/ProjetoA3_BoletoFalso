# Projeto A3 — Combate ao Golpe do Boleto Falso

Um sistema bancário robusto em Java Puro com proteção criptográfica contra fraudes de boletos falsos, utiliza SHA-256 e validação em duas camadas de segurança.

## 📋 Visão Geral

O **Golpe do Boleto Falso** é uma fraude sofisticada onde:
1. Um criminoso intercepta ou falsifica um boleto que visualmente pertence a uma empresa legítima
2. Altera a conta bancária de destino (inserindo sua própria conta CPF)
3. O boleto permanece visualmente idêntico — mesma empresa, mesmo valor
4. O pagador transfere dinheiro para a conta do criminoso

Este projeto implementa **duas travas de segurança criptográfica** que detectam essas fraudes:

### 🔒 Trava 1 — Integridade do Arquivo (SHA-256)
- Valida que o boleto não foi adulterado após emissão
- Usa hash SHA-256 combinando: ID, Valor, Conta de Destino, Chave da Empresa
- Se um golpista alterar qualquer dado, o hash não corresponde mais

### 🔒 Trava 2 — Correspondência Modalidade/Identidade
- Verifica que modalidade do papel (CNPJ) == modalidade da conta real
- Detecta o caso onde visual diz "Empresa PJ" mas conta real é "Pessoa Física"

---

## 📁 Estrutura do Projeto

```
ProjetoA3/
├── src/
│   ├── model/
│   │   ├── EmpresaEmissora.java          # Âncora de confiança (chave pública)
│   │   └── Boleto.java                   # Entidade principal + enum Status
│   ├── service/
│   │   └── SegurancaService.java         # SHA-256, gerar/validar hash
│   ├── controller/
│   │   └── BancoController.java          # Orquestrador das 2 travas
│   └── test/
│       ├── SegurancaServiceTest.java     # Testes executáveis (método main)
│       └── SegurancaServiceJUnitTest.java # Suite completa de testes JUnit
├── pom.xml                                # Configuração Maven
├── build.gradle                           # Configuração Gradle
└── README.md                              # Este arquivo
```
---

## 🧪 Testes Incluídos

### Testes Simples (método main em SegurancaServiceTest.java)

Executa 3 cenários simulando ataques reais:

| # | Cenário | O que testa | Esperado |
|---|---------|-------------|----------|
| 1 | **Caminho Feliz** | Boleto autêntico PJ → PJ | Status `PAGO` ✅ |
| 2 | **Adulteração** | Valor/conta alterados | Status `SUSPEITO` (Etapa 1 barra) |
| 3 | **Imediatismo** | Visual CNPJ, conta real CPF | Status `FRAUDE_MODALIDADE` (Etapa 2 barra) |

**Executar:**
```bash
cd src && java test.SegurancaServiceTest
```

### Suite JUnit (SegurancaServiceJUnitTest.java)

13 testes unitários cobrindo:

- ✅ Determinismo de hash (mesmo boleto = mesmo hash sempre)
- ✅ Efeito avalanche (1 centavo de diferença = hash radicalmente diferente)
- ✅ Validação de integridade com hash correto
- ✅ Detecção de adulteração (hash inválido)
- ✅ Processamento de boleto legítimo → PAGO
- ✅ Detecção de adulteração → SUSPEITO
- ✅ Detecção de divergência modalidade (CNPJ↔CPF) → FRAUDE_MODALIDADE
- ✅ Detecção de documentos diferentes (CNPJ 1 vs CNPJ 2)
- ✅ Preservação de propriedades após processamento
- ✅ Formato do hash SHA-256 (64 caracteres hexadecimais)
- ✅ Edge case: valor zero
- ✅ Edge case: valor muito grande
- ✅ Segurança: validação com empresa diferente

**Executar com Maven:**
```bash
mvn test
# Saída no console mostrará cada teste com ✓ ou ✗
```

**Executar com Gradle:**
```bash
gradle test
```

---

## 🔐 Segurança Criptográfica

### SHA-256 (Secure Hash Algorithm 256-bit)

Implementado via `java.security.MessageDigest` — nativa da JVM.

**Propriedades utilizadas:**

1. **Determinismo**: `f(x) = y` sempre produz o mesmo `y`
   - Permite recomputar e comparar o hash na validação

2. **Efeito Avalanche**: Pequena mudança em `x` gera `y` completamente diferente
   - Alteração de 1 centavo no valor → hash muda radicalmente

3. **One-Way**: Computacionalmente impossível recuperar `x` a partir de `y`
   - Impossível fabricar um boleto fake com hash válido sem a chave

4. **Resistência a Colisões**: Impossível encontrar `x1 ≠ x2` tal que `f(x1) = f(x2)`
   - Não há como "imitar" o hash de um boleto legítimo

### Componentes do Hash

```java
dadosParaAssinar = boleto.getId() 
                 + "|" + boleto.getValor()
                 + "|" + boleto.getDocumentoDestinatarioReal()
                 + "|" + boleto.getTipoDestinatarioReal()
                 + "|" + empresa.getChavePublicaAutenticacao()
```

**Por que estes dados?**
- **ID**: Identificação do boleto
- **Valor**: Fundamental — alteração principal dos fraudadores
- **Documento Destinatário Real**: A conta para onde vai o dinheiro (roubada por fraudadores)
- **Tipo Destinatário Real**: Modalidade da conta (PJ/PF)
- **Chave da Empresa**: Componente secreto — torna o hash específico de cada empresa

**Por que NÃO incluir dados visuais?**
- Dados visuais (nome da empresa, logo) são o alvo do golpista — ele os mantém intactos
- O importante selar é o que é FINANCEIRAMENTE CRÍTICO: valor e conta real

---

## 📊 Fluxo de Segurança do BancoController

```
Boleto recebido
      ↓
[ETAPA 1] Validar Integridade (SHA-256)
      ↓
   Hash OK?  ← NÃO → Status = SUSPEITO → 🚫 BARRADO
      ↓ SIM
[ETAPA 2] Validar Modalidade/Identidade
      ↓
 Tipo papel   ← NÃO OU Documento papel   → Status = FRAUDE_MODALIDADE → 🚫 BARRADO
   == tipo                    == documento
   real?                       real?
      ↓ SIM
Status = PAGO
Pagamento autorizado ✅
```

---

## 💻 Exemplos de Uso

### Exemplo 1: Criar e processar um boleto legítimo

```java
// Instanciar serviços
SegurancaService segurancaService = new SegurancaService();
BancoController bancoController = new BancoController(segurancaService);

// Empresa emissora legítima
EmpresaEmissora empresa = new EmpresaEmissora(
    1,
    "Energia Elétrica do Nordeste S.A.",
    "12345678000195",
    "CHAVE_PUBLICA_ENERGIA_NE_2024_XYZ"
);

// Criar boleto (dados visuais = dados reais)
Boleto boleto = new Boleto();
boleto.setId(1001);
boleto.setValor(157.43);
boleto.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
boleto.setDocumentoEmissorNoPapel("12345678000195");  // CNPJ
boleto.setTipoInscricaoPapel("CNPJ");
boleto.setDocumentoDestinatarioReal("12345678000195"); // conta real
boleto.setTipoDestinatarioReal("CNPJ");

// Gerar assinatura digital legítima
String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresa);
boleto.setAssinaturaDigital(assinatura);

// Processar pagamento
String resultado = bancoController.processarPagamento(boleto, empresa);
System.out.println(resultado);
// Saída: [APROVADO] Boleto #1001 processado com sucesso...
// Status: PAGO ✅
```

### Exemplo 2: Detectar adulteração (Etapa 1)

```java
// ... setup igual ao exemplo 1 ...

// Fraudador altera o valor
boleto.setValor(1.00); // original era 157.43
// MAS não consegue regenerar o hash válido

String resultado = bancoController.processarPagamento(boleto, empresa);
System.out.println(resultado);
// Saída: [BLOQUEADO - ETAPA 1] ALERTA DE SEGURANÇA...
// Status: SUSPEITO 🚫
```

### Exemplo 3: Detectar fraude de modalidade (Etapa 2)

```java
// ... setup igual ao exemplo 1 ...

// Golpista cria boleto falso: visual de empresa, conta de pessoa física
boleto.setDocumentoDestinatarioReal("98765432100");    // CPF do golpista
boleto.setTipoDestinatarioReal("CPF");                 // DIVERGÊNCIA

// Gera hash válido (com chave vazada ou insider)
String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresa);
boleto.setAssinaturaDigital(assinatura);

String resultado = bancoController.processarPagamento(boleto, empresa);
System.out.println(resultado);
// Saída: [BLOQUEADO - ETAPA 2] FRAUDE DETECTADA...
// Status: FRAUDE_MODALIDADE 🚫
```

---

## 📚 Tecnologias Utilizadas

| Componente | Versão | Descrição |
|------------|--------|-----------|
| **Java** | 11+ | Linguagem base — POO, Generics, Streams |
| **JUnit** | 4.13.2 | Framework de testes unitários |
| **Hamcrest** | 1.3 | Matchers para assertions fluentes |
| **Maven** | 3.6+ | Build system (pom.xml) |
| **Gradle** | 7.0+ | Build system alternativo (build.gradle) |
| **SHA-256** | FIPS 180-4 | Algoritmo de hash nativo da JVM |

---

## 🎓 Conceitos de Engenharia Demonstrados

✅ **Programação Orientada a Objetos**
- Encapsulamento (getters/setters)
- Herança e Polimorfismo (enum Status)
- Composição (BancoController depende de SegurancaService)

✅ **Arquitetura em Camadas**
- Model: Entidades do domínio
- Service: Lógica de negócio (criptografia)
- Controller: Orquestração e processamento

✅ **Segurança Criptográfica**
- Hash SHA-256 determinístico
- Validação de integridade
- Proteção contra alteração de dados

✅ **Testes Unitários**
- Padrão AAA (Arrange-Act-Assert)
- Isolamento entre testes (@Before/@After)
- Cobertura de happy path e edge cases

✅ **Build Automation**
- Maven POM com dependências gerenciadas
- Gradle com tasks customizadas
- CI/CD ready

---

## 🚀 Próximos Passos (Extensões Futuras)

1. **Integração com BD Real**: Persistir boletos em PostgreSQL/MySQL
2. **API REST**: Expor endpoints `/boleto/processar` com Spring Boot
3. **Criptografia RSA**: Substituir SHA-256 por assinatura digital RSA
4. **Logging Estruturado**: SLF4J + Logback para auditoria
5. **Containerização**: Dockerfile para deploy em Docker/Kubernetes
6. **Cobertura de Testes**: JaCoCo para medir cobertura >80%
7. **Documentação API**: Swagger/OpenAPI

---

## 📖 Referências

- [NIST FIPS 180-4](https://csrc.nist.gov/publications/detail/fips/180/4) — Definição oficial do SHA-256
- [Java Cryptography Architecture](https://docs.oracle.com/javase/11/docs/api/java.base/java/security/MessageDigest.html)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Maven Official Guide](https://maven.apache.org/guides/)
- [Gradle Official Guide](https://docs.gradle.org/)

---

## 📝 Licença

Este projeto é fornecido como material educacional. Sinta-se livre para usar, modificar e distribuir.

---

## 👨‍💼 Lucas Matheus

**Desenvolvido como análise de engenharia de software** para demonstração de boas práticas em:
- Design seguro de sistemas bancários
- Implementação de criptografia em Java
- Testes unitários com JUnit
- Automação de build

**Data**: Junho de 2026
**Versão**: 1.0.0
