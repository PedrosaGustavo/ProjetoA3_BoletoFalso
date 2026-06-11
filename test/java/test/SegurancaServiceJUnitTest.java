package test;

import controller.BancoController;
import model.Boleto;
import model.Boleto.Status;
import model.EmpresaEmissora;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import service.SegurancaService;

/**
 * Suite de testes unitários JUnit 4 para o Projeto A3.
 *
 * ============================================================
 * ARQUITETURA DE TESTES COM JUNIT
 * ============================================================
 *
 * Este arquivo demonstra as principais construções do JUnit:
 *
 *  @BeforeClass   — Executa UMA VEZ antes de todos os testes
 *                   (setup de recursos compartilhados — conexão BD, etc.)
 *
 *  @Before        — Executa ANTES de CADA teste
 *                   (setup individual — criar objetos, estado inicial)
 *
 *  @Test          — Marca um método como teste a executar
 *                   (cada @Test deve ser independente e idempotente)
 *
 *  @After         — Executa DEPOIS de CADA teste
 *                   (cleanup individual — fechar recursos, limpar estado)
 *
 * PADRÃO AAA (Arrange-Act-Assert):
 *  1. ARRANGE: configura os dados/objetos necessários.
 *  2. ACT:     executa a ação que se quer testar.
 *  3. ASSERT:  verifica que o resultado esperado foi obtido.
 *
 * HAMCREST MATCHERS (alternativa a assertEquals):
 *  - assertThat(valor, is(esperado))
 *  - assertThat(valor, equalTo(esperado))
 *  - assertThat(texto, containsString("part"))
 *  - assertThat(lista, hasItem(elemento))
 *  - assertThat(valor, not(outro_valor))
 */
public class SegurancaServiceJUnitTest {

    // -------------------------------------------------------
    // Atributos de classe — compartilhados entre testes
    // -------------------------------------------------------

    /**
     * Instância do serviço de segurança criada UMA VEZ antes
     * de todos os testes (setup compartilhado).
     * Isso otimiza recursos se SegurancaService fosse custoso de instanciar.
     */
    private static SegurancaService segurancaServiceStatic;

    // -------------------------------------------------------
    // Atributos de instância — criados antes de CADA teste
    // -------------------------------------------------------

    /**
     * Instância local do SegurancaService recriada antes de cada teste.
     * Garante isolamento completo entre testes.
     */
    private SegurancaService segurancaService;

    /**
     * Instância do BancoController que orquestra as verificações.
     * Recriada antes de cada teste para garantir estado limpo.
     */
    private BancoController bancoController;

    /**
     * Empresa emissora legítima usada como âncora de confiança.
     * Recriada antes de cada teste.
     */
    private EmpresaEmissora empresaLegitima;

    // -------------------------------------------------------
    // Setup Estático — Executa UMA VEZ no início
    // -------------------------------------------------------

    /**
     * Configuração estática executada uma única vez,
     * antes de todos os testes da classe rodarem.
     *
     * Útil para:
     *  - Inicializar resources caros (conexão DB, servidor)
     *  - Logs de cabeçalho
     *  - Dados que não mudam entre testes
     */
    @BeforeClass
    public static void setUpClass() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  INICIANDO SUITE DE TESTES JUNIT — PROJETO A3");
        System.out.println("  Framework: JUnit 4.13.2 + Hamcrest 1.3");
        System.out.println("=".repeat(70));

        // Cria instância estática do serviço
        segurancaServiceStatic = new SegurancaService();
    }

    // -------------------------------------------------------
    // Setup de Instância — Executa ANTES de CADA teste
    // -------------------------------------------------------

    /**
     * Configuração executada antes de cada método @Test.
     *
     * Garante que cada teste começa com:
     *  - Um SegurancaService limpo e isolado
     *  - Um BancoController novo
     *  - Uma EmpresaEmissora reconfigurada
     *
     * Isso evita contaminação entre testes (estado compartilhado).
     */
    @Before
    public void setUp() {
        // Instancia componentes que serão usados nos testes
        this.segurancaService = new SegurancaService();
        this.bancoController = new BancoController(segurancaService);

        // Configura a empresa emissora (âncora de confiança)
        this.empresaLegitima = new EmpresaEmissora(
                1,
                "Energia Elétrica do Nordeste S.A.",
                "12345678000195",
                "CHAVE_PUBLICA_ENERGIA_NE_2024_XYZ"
        );
    }

    // -------------------------------------------------------
    // Cleanup de Instância — Executa DEPOIS de CADA teste
    // -------------------------------------------------------

    /**
     * Executado após cada teste para liberar recursos
     * ou fazer limpeza específica.
     *
     * Neste projeto simples, é principalmente documentacional.
     * Em projetos reais, seria usado para fechar conexões DB,
     * arquivos, sockets, etc.
     */
    @After
    public void tearDown() {
        // Limpar estado se necessário
        // Neste caso, objetos serão garbage-collected automaticamente
    }

    // ============================================================
    // TESTES DE INTEGRIDADE (SegurancaService)
    // ============================================================

    /**
     * TESTE 1: Geração de hash SHA-256 deve ser determinístico.
     *
     * OBJETIVO: Validar que o mesmo boleto sempre produz o mesmo hash.
     *
     * PADRÃO AAA:
     *  - Arrange: cria um boleto com dados conhecidos.
     *  - Act:     gera o hash duas vezes.
     *  - Assert:  ambos os hashes devem ser idênticos.
     */
    @Test
    public void testGerarAssinaturaDigitalDeterministico() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(1);
        boleto.setValor(100.00);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        // ACT — gera o hash duas vezes
        String hash1 = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        String hash2 = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);

        // ASSERT — ambos devem ser idênticos (propriedade de hash determinístico)
        assertThat("Hashes devem ser determinísticos para o mesmo input",
                   hash1, equalTo(hash2));

        // Assertions adicionais para qualidade
        assertThat("Hash SHA-256 deve ter 64 caracteres hexadecimais",
                   hash1.length(), equalTo(64));

        System.out.println("  ✓ Teste determinismo de hash: PASSOU");
    }

    /**
     * TESTE 2: Efeito avalanche — pequena alteração causa hash completamente diferente.
     *
     * OBJETIVO: Validar que mudança de um centavo quebra o hash (segurança criptográfica).
     *
     * SHA-256 tem propriedade de "avalanche effect": qualquer alteração mínima
     * no input produz hash radicalmente diferente no output.
     */
    @Test
    public void testEfeitoAvalanchemudancaValor() {
        // ARRANGE
        Boleto boleto1 = new Boleto();
        boleto1.setId(2);
        boleto1.setValor(100.00); // valor original
        boleto1.setDocumentoDestinatarioReal("12345678000195");
        boleto1.setTipoDestinatarioReal("CNPJ");

        Boleto boleto2 = new Boleto();
        boleto2.setId(2);
        boleto2.setValor(100.01); // valor alterado em 1 centavo apenas
        boleto2.setDocumentoDestinatarioReal("12345678000195");
        boleto2.setTipoDestinatarioReal("CNPJ");

        // ACT
        String hash1 = segurancaService.gerarAssinaturaDigital(boleto1, empresaLegitima);
        String hash2 = segurancaService.gerarAssinaturaDigital(boleto2, empresaLegitima);

        // ASSERT — hashes completamente diferentes apesar de apenas 1 centavo de diferença
        assertThat("Uma alteração de 1 centavo deve gerar hash completamente diferente",
                   hash1, not(equalTo(hash2)));

        // Calcula quantos bits diferem (Hamming distance)
        int bitsDiferentes = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                bitsDiferentes += 4; // cada caractere hex = 4 bits
            }
        }

        System.out.println("  ✓ Teste efeito avalanche (valor): PASSOU");
        System.out.println("    Bits diferentes: " + bitsDiferentes + "/256");
        assertTrue("Avalanche effect esperado (muitos bits devem diferir)",
                   bitsDiferentes > 50); // Heurística: esperamos > 50 bits diferentes
    }

    /**
     * TESTE 3: Validação de integridade com hash correto (boleto íntegro).
     *
     * OBJETIVO: Um boleto com hash válido deve ser considerado íntegro.
     *
     * Cenário: o banco recebe um boleto cuja assinatura bate com o hash recalculado.
     */
    @Test
    public void testValidarIntegridadeBoleto_BoletoIntegro() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(3);
        boleto.setValor(250.50);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        // Gera e grava a assinatura original
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // ACT — valida o boleto sem alterações
        boolean integro = segurancaService.validarIntegridadeBoleto(boleto, empresaLegitima);

        // ASSERT
        assertTrue("Boleto sem alterações deve passar na validação de integridade", integro);

        System.out.println("  ✓ Teste validação integridade (válido): PASSOU");
    }

    /**
     * TESTE 4: Validação de integridade com hash inválido (boleto adulterado).
     *
     * OBJETIVO: Um boleto cujo valor foi alterado deve falhar na validação.
     */
    @Test
    public void testValidarIntegridadeBoleto_BoletoAdulterado() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(4);
        boleto.setValor(300.00);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        // Gera e grava a assinatura com o valor original
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // SIMULA ADULTERAÇÃO: altera o valor mas mantém a assinatura original
        boleto.setValor(1.00); // fraudador tenta roubar a diferença

        // ACT
        boolean integro = segurancaService.validarIntegridadeBoleto(boleto, empresaLegitima);

        // ASSERT
        assertFalse("Boleto adulterado deve falhar na validação de integridade", integro);

        System.out.println("  ✓ Teste validação integridade (adulterado): PASSOU");
    }

    // ============================================================
    // TESTES DE PROCESSAMENTO BANCÁRIO (BancoController)
    // ============================================================

    /**
     * TESTE 5: Cenário Feliz — Boleto legítimo deve ser PAGO.
     *
     * OBJETIVO: Validar que um boleto autêntico (PJ → PJ) passa em ambas
     * as etapas e resulta em Status PAGO.
     */
    @Test
    public void testProcessarPagamento_BoletoLegitimo_Pago() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(5);
        boleto.setValor(157.43);
        boleto.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boleto.setDocumentoEmissorNoPapel("12345678000195"); // CNPJ no papel
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("12345678000195"); // conta real igual
        boleto.setTipoDestinatarioReal("CNPJ");

        // Assina o boleto legitimamente
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // ACT
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        // ASSERT — Etapa 1 e 2 devem passar
        assertThat("Status deve ser PAGO após validações bem-sucedidas",
                   boleto.getStatus(), equalTo(Status.PAGO));

        assertThat("Mensagem de retorno deve conter [APROVADO]",
                   resultado, containsString("[APROVADO]"));

        System.out.println("  ✓ Teste processamento legítimo: PASSOU");
    }

    /**
     * TESTE 6: Etapa 1 falha — Boleto adulterado deve ser SUSPEITO.
     *
     * OBJETIVO: Validar que alteração no valor ativa a TRAVA 1 (integridade).
     */
    @Test
    public void testProcessarPagamento_BoletoAdulterado_Suspeito() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(6);
        boleto.setValor(200.00);
        boleto.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boleto.setDocumentoEmissorNoPapel("12345678000195");
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        // Assina com valor original
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // SIMULA ADULTERAÇÃO
        boleto.setValor(1.00);

        // ACT
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        // ASSERT — Etapa 1 deve falhar
        assertThat("Status deve ser SUSPEITO após detecção de adulteração",
                   boleto.getStatus(), equalTo(Status.SUSPEITO));

        assertThat("Mensagem deve conter [BLOQUEADO - ETAPA 1]",
                   resultado, containsString("[BLOQUEADO - ETAPA 1]"));

        assertThat("Mensagem deve alertar sobre adulteração",
                   resultado, containsString("ADULTERADO"));

        System.out.println("  ✓ Teste detecção adulteração (ETAPA 1): PASSOU");
    }

    /**
     * TESTE 7: Etapa 2 falha — Divergência de modalidade (CNPJ → CPF) detectada.
     *
     * OBJETIVO: Validar que a TRAVA 2 detecta o "Golpe do Boleto Falso por Imediatismo":
     * visual diz CNPJ (empresa) mas conta real é CPF (golpista).
     *
     * Este é o cenário mais sofisticado, onde o hash está válido
     * (boleto falso re-gerado com chave vazada ou insider),
     * mas a modalidade denuncia a fraude.
     */
    @Test
    public void testProcessarPagamento_FraudemodalidadeCNPJparaCPF_FraudeDetectada() {
        // ARRANGE
        Boleto boletoFalso = new Boleto();
        boletoFalso.setId(7);
        boletoFalso.setValor(157.43);

        // Visual: idêntico ao da concessionária legítima
        boletoFalso.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoFalso.setDocumentoEmissorNoPapel("12345678000195"); // CNPJ no papel
        boletoFalso.setTipoInscricaoPapel("CNPJ");

        // Metadata real: conta de golpista (CPF)
        boletoFalso.setDocumentoDestinatarioReal("98765432100"); // CPF do golpista
        boletoFalso.setTipoDestinatarioReal("CPF"); // DIVERGÊNCIA AQUI

        // Gera hash válido sobre o boleto falso (simula chave vazada/insider)
        String assinatura = segurancaService.gerarAssinaturaDigital(boletoFalso, empresaLegitima);
        boletoFalso.setAssinaturaDigital(assinatura);

        // ACT
        String resultado = bancoController.processarPagamento(boletoFalso, empresaLegitima);

        // ASSERT — Etapa 1 passa, Etapa 2 falha
        assertThat("Status deve ser FRAUDE_MODALIDADE",
                   boletoFalso.getStatus(), equalTo(Status.FRAUDE_MODALIDADE));

        assertThat("Mensagem deve conter [BLOQUEADO - ETAPA 2]",
                   resultado, containsString("[BLOQUEADO - ETAPA 2]"));

        assertThat("Mensagem deve indicar desvio de fundos",
                   resultado, containsString("DESVIO DE FUNDOS"));

        assertThat("Mensagem deve citar a divergência de modalidade",
                   resultado, containsString("Modalidade divergente"));

        System.out.println("  ✓ Teste detecção fraude modalidade (ETAPA 2): PASSOU");
    }

    /**
     * TESTE 8: Etapa 2 falha — Divergência de documento (mesmo tipo, números diferentes).
     *
     * OBJETIVO: Validar que dois CNPJs diferentes também são detectados.
     */
    @Test
    public void testProcessarPagamento_FraudedocumentosDivergentes_FraudeDetectada() {
        // ARRANGE
        Boleto boletoFalso = new Boleto();
        boletoFalso.setId(8);
        boletoFalso.setValor(100.00);

        boletoFalso.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoFalso.setDocumentoEmissorNoPapel("12345678000195"); // CNPJ legítimo no papel
        boletoFalso.setTipoInscricaoPapel("CNPJ");

        // Conta real: CNPJ diferente (outra empresa que o fraudador controla)
        boletoFalso.setDocumentoDestinatarioReal("98765432000100"); // CNPJ fraudulento
        boletoFalso.setTipoDestinatarioReal("CNPJ");

        // Gera hash válido
        String assinatura = segurancaService.gerarAssinaturaDigital(boletoFalso, empresaLegitima);
        boletoFalso.setAssinaturaDigital(assinatura);

        // ACT
        String resultado = bancoController.processarPagamento(boletoFalso, empresaLegitima);

        // ASSERT
        assertThat("Status deve ser FRAUDE_MODALIDADE",
                   boletoFalso.getStatus(), equalTo(Status.FRAUDE_MODALIDADE));

        assertThat("Mensagem deve citar divergência de documento",
                   resultado, containsString("Documento divergente"));

        System.out.println("  ✓ Teste detecção fraude documentos diferentes: PASSOU");
    }

    /**
     * TESTE 9: Propriedades de um boleto pago devem estar íntegras.
     *
     * OBJETIVO: Validar que após aprovação, o boleto mantém seus dados.
     */
    @Test
    public void testProcessarPagamento_PropriedadesBoletoPago() {
        // ARRANGE
        double valorEsperado = 999.99;
        Boleto boleto = new Boleto();
        boleto.setId(9);
        boleto.setValor(valorEsperado);
        boleto.setNomeEmissorNoPapel("Concessionária X");
        boleto.setDocumentoEmissorNoPapel("99999999000100");
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("99999999000100");
        boleto.setTipoDestinatarioReal("CNPJ");

        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // ACT
        bancoController.processarPagamento(boleto, empresaLegitima);

        // ASSERT — propriedades devem ser preservadas
        assertThat("Valor não deve ser alterado durante processamento",
                   boleto.getValor(), equalTo(valorEsperado));

        assertThat("Nome do emissor não deve ser alterado",
                   boleto.getNomeEmissorNoPapel(), is("Concessionária X"));

        System.out.println("  ✓ Teste integridade de propriedades: PASSOU");
    }

    /**
     * TESTE 10: Hash SHA-256 sempre gera 64 caracteres hexadecimais.
     *
     * OBJETIVO: Validar invariante de formato do hash.
     */
    @Test
    public void testFormatoHashSHA256() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(10);
        boleto.setValor(500.00);
        boleto.setDocumentoDestinatarioReal("11111111000000");
        boleto.setTipoDestinatarioReal("CNPJ");

        // ACT
        String hash = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);

        // ASSERT
        assertThat("Hash SHA-256 deve ter exatamente 64 caracteres",
                   hash.length(), is(64));

        assertTrue("Hash deve conter apenas dígitos hexadecimais [0-9a-f]",
                   hash.matches("[0-9a-f]{64}"));

        System.out.println("  ✓ Teste formato hash SHA-256: PASSOU");
    }

    // ============================================================
    // TESTES DE CASOS EXTREMOS (Edge Cases)
    // ============================================================

    /**
     * TESTE 11: Boleto com valor zero (edge case).
     *
     * OBJETIVO: Validar comportamento com valor mínimo.
     */
    @Test
    public void testProcessarPagamento_BoletoComValorZero() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(11);
        boleto.setValor(0.00);
        boleto.setNomeEmissorNoPapel("Empresa");
        boleto.setDocumentoEmissorNoPapel("11111111000000");
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("11111111000000");
        boleto.setTipoDestinatarioReal("CNPJ");

        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // ACT
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        // ASSERT — ainda assim deve ser processado normalmente
        assertThat("Boleto com valor zero deve ser tratado como válido (negócio define regras)",
                   boleto.getStatus(), equalTo(Status.PAGO));

        System.out.println("  ✓ Teste valor zero: PASSOU");
    }

    /**
     * TESTE 12: Boleto com valor muito grande (edge case).
     *
     * OBJETIVO: Validar com valores extremos de magnitude.
     */
    @Test
    public void testProcessarPagamento_BoletoComValorMuitoGrande() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(12);
        boleto.setValor(999999999.99);
        boleto.setNomeEmissorNoPapel("Empresa");
        boleto.setDocumentoEmissorNoPapel("22222222000000");
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("22222222000000");
        boleto.setTipoDestinatarioReal("CNPJ");

        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // ACT
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        // ASSERT
        assertThat("Boleto com valor grande deve ser processado",
                   boleto.getStatus(), equalTo(Status.PAGO));

        System.out.println("  ✓ Teste valor muito grande: PASSOU");
    }

    /**
     * TESTE 13: Validação com empresa emissora diferente (segurança).
     *
     * OBJETIVO: Validar que usar chave de empresa errada invalida o boleto.
     *
     * Cenário: banco B tenta validar um boleto emitido por banco A,
     * mas usa a chave do banco B. Deve falhar.
     */
    @Test
    public void testValidarIntegridade_EmpresaDiferente_Invalido() {
        // ARRANGE
        Boleto boleto = new Boleto();
        boleto.setId(13);
        boleto.setValor(100.00);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        // Assina com empresaLegitima
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        // Cria OUTRA empresa com chave diferente
        EmpresaEmissora outraEmpresa = new EmpresaEmissora(
                2,
                "Outra Concessionária",
                "98765432000100",
                "CHAVE_PUBLICA_OUTRA_EMPRESA" // chave diferente
        );

        // ACT — tenta validar com chave errada
        boolean integro = segurancaService.validarIntegridadeBoleto(boleto, outraEmpresa);

        // ASSERT
        assertFalse("Boleto assinado com chave A não deve validar com chave B",
                    integro);

        System.out.println("  ✓ Teste validação com empresa diferente: PASSOU");
    }

}
