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

public class SegurancaServiceJUnitTest {


    private static SegurancaService segurancaServiceStatic;

    
    

    private SegurancaService segurancaService;

    private BancoController bancoController;

    private EmpresaEmissora empresaLegitima;

    @BeforeClass
    public static void setUpClass() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  INICIANDO SUITE DE TESTES JUNIT — PROJETO A3");
        System.out.println("  Framework: JUnit 4.13.2 + Hamcrest 1.3");
        System.out.println("=".repeat(70));

        
        segurancaServiceStatic = new SegurancaService();
    }

    
    @Before
    public void setUp() {
        
        this.segurancaService = new SegurancaService();
        this.bancoController = new BancoController(segurancaService);

        
        this.empresaLegitima = new EmpresaEmissora(
                1,
                "Energia Elétrica do Nordeste S.A.",
                "12345678000195",
                "CHAVE_PUBLICA_ENERGIA_NE_2024_XYZ"
        );
    }

    @After
    public void tearDown() {
        
        
    }

    
    @Test
    public void testGerarAssinaturaDigitalDeterministico() {
        
        Boleto boleto = new Boleto();
        boleto.setId(1);
        boleto.setValor(100.00);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String hash1 = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        String hash2 = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);

        
        assertThat("Hashes devem ser determinísticos para o mesmo input",
                   hash1, equalTo(hash2));

        
        assertThat("Hash SHA-256 deve ter 64 caracteres hexadecimais",
                   hash1.length(), equalTo(64));

        System.out.println("  ✓ Teste determinismo de hash: PASSOU");
    }

    
    @Test
    public void testEfeitoAvalanchemudancaValor() {
        
        Boleto boleto1 = new Boleto();
        boleto1.setId(2);
        boleto1.setValor(100.00); 
        boleto1.setDocumentoDestinatarioReal("12345678000195");
        boleto1.setTipoDestinatarioReal("CNPJ");

        Boleto boleto2 = new Boleto();
        boleto2.setId(2);
        boleto2.setValor(100.01); 
        boleto2.setDocumentoDestinatarioReal("12345678000195");
        boleto2.setTipoDestinatarioReal("CNPJ");

        
        String hash1 = segurancaService.gerarAssinaturaDigital(boleto1, empresaLegitima);
        String hash2 = segurancaService.gerarAssinaturaDigital(boleto2, empresaLegitima);

        
        assertThat("Uma alteração de 1 centavo deve gerar hash completamente diferente",
                   hash1, not(equalTo(hash2)));

        
        int bitsDiferentes = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                bitsDiferentes += 4; 
            }
        }

        System.out.println("  ✓ Teste efeito avalanche (valor): PASSOU");
        System.out.println("    Bits diferentes: " + bitsDiferentes + "/256");
        assertTrue("Avalanche effect esperado (muitos bits devem diferir)",
                   bitsDiferentes > 50); 
    }

    @Test
    public void testValidarIntegridadeBoleto_BoletoIntegro() {
        
        Boleto boleto = new Boleto();
        boleto.setId(3);
        boleto.setValor(250.50);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        
        boolean integro = segurancaService.validarIntegridadeBoleto(boleto, empresaLegitima);

        
        assertTrue("Boleto sem alterações deve passar na validação de integridade", integro);

        System.out.println("  ✓ Teste validação integridade (válido): PASSOU");
    }

    @Test
    public void testValidarIntegridadeBoleto_BoletoAdulterado() {
        
        Boleto boleto = new Boleto();
        boleto.setId(4);
        boleto.setValor(300.00);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        
        boleto.setValor(1.00); 

        
        boolean integro = segurancaService.validarIntegridadeBoleto(boleto, empresaLegitima);

        
        assertFalse("Boleto adulterado deve falhar na validação de integridade", integro);

        System.out.println("  ✓ Teste validação integridade (adulterado): PASSOU");
    }

    
    @Test
    public void testProcessarPagamento_BoletoLegitimo_Pago() {
        
        Boleto boleto = new Boleto();
        boleto.setId(5);
        boleto.setValor(157.43);
        boleto.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boleto.setDocumentoEmissorNoPapel("12345678000195"); 
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("12345678000195"); 
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        
        assertThat("Status deve ser PAGO após validações bem-sucedidas",
                   boleto.getStatus(), equalTo(Status.PAGO));

        assertThat("Mensagem de retorno deve conter [APROVADO]",
                   resultado, containsString("[APROVADO]"));

        System.out.println("  ✓ Teste processamento legítimo: PASSOU");
    }

    @Test
    public void testProcessarPagamento_BoletoAdulterado_Suspeito() {
        
        Boleto boleto = new Boleto();
        boleto.setId(6);
        boleto.setValor(200.00);
        boleto.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boleto.setDocumentoEmissorNoPapel("12345678000195");
        boleto.setTipoInscricaoPapel("CNPJ");
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        
        boleto.setValor(1.00);

        
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        
        assertThat("Status deve ser SUSPEITO após detecção de adulteração",
                   boleto.getStatus(), equalTo(Status.SUSPEITO));

        assertThat("Mensagem deve conter [BLOQUEADO - ETAPA 1]",
                   resultado, containsString("[BLOQUEADO - ETAPA 1]"));

        assertThat("Mensagem deve alertar sobre adulteração",
                   resultado, containsString("ADULTERADO"));

        System.out.println("  ✓ Teste detecção adulteração (ETAPA 1): PASSOU");
    }

    @Test
    public void testProcessarPagamento_FraudemodalidadeCNPJparaCPF_FraudeDetectada() {
        
        Boleto boletoFalso = new Boleto();
        boletoFalso.setId(7);
        boletoFalso.setValor(157.43);

        
        boletoFalso.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoFalso.setDocumentoEmissorNoPapel("12345678000195"); 
        boletoFalso.setTipoInscricaoPapel("CNPJ");

        
        boletoFalso.setDocumentoDestinatarioReal("98765432100"); 
        boletoFalso.setTipoDestinatarioReal("CPF"); 

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boletoFalso, empresaLegitima);
        boletoFalso.setAssinaturaDigital(assinatura);

        
        String resultado = bancoController.processarPagamento(boletoFalso, empresaLegitima);

        
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

    public void testProcessarPagamento_FraudedocumentosDivergentes_FraudeDetectada() {
        
        Boleto boletoFalso = new Boleto();
        boletoFalso.setId(8);
        boletoFalso.setValor(100.00);

        boletoFalso.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoFalso.setDocumentoEmissorNoPapel("12345678000195"); 
        boletoFalso.setTipoInscricaoPapel("CNPJ");

        
        boletoFalso.setDocumentoDestinatarioReal("98765432000100"); 
        boletoFalso.setTipoDestinatarioReal("CNPJ");

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boletoFalso, empresaLegitima);
        boletoFalso.setAssinaturaDigital(assinatura);

        
        String resultado = bancoController.processarPagamento(boletoFalso, empresaLegitima);

        
        assertThat("Status deve ser FRAUDE_MODALIDADE",
                   boletoFalso.getStatus(), equalTo(Status.FRAUDE_MODALIDADE));

        assertThat("Mensagem deve citar divergência de documento",
                   resultado, containsString("Documento divergente"));

        System.out.println("  ✓ Teste detecção fraude documentos diferentes: PASSOU");
    }

    @Test
    public void testProcessarPagamento_PropriedadesBoletoPago() {
        
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

        
        bancoController.processarPagamento(boleto, empresaLegitima);

        
        assertThat("Valor não deve ser alterado durante processamento",
                   boleto.getValor(), equalTo(valorEsperado));

        assertThat("Nome do emissor não deve ser alterado",
                   boleto.getNomeEmissorNoPapel(), is("Concessionária X"));

        System.out.println("  ✓ Teste integridade de propriedades: PASSOU");
    }

    @Test
    public void testFormatoHashSHA256() {
        
        Boleto boleto = new Boleto();
        boleto.setId(10);
        boleto.setValor(500.00);
        boleto.setDocumentoDestinatarioReal("11111111000000");
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String hash = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);

        
        assertThat("Hash SHA-256 deve ter exatamente 64 caracteres",
                   hash.length(), is(64));

        assertTrue("Hash deve conter apenas dígitos hexadecimais [0-9a-f]",
                   hash.matches("[0-9a-f]{64}"));

        System.out.println("  ✓ Teste formato hash SHA-256: PASSOU");
    }

    @Test
    public void testProcessarPagamento_BoletoComValorZero() {
        
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

        
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        
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

        
        String resultado = bancoController.processarPagamento(boleto, empresaLegitima);

        
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
        
        Boleto boleto = new Boleto();
        boleto.setId(13);
        boleto.setValor(100.00);
        boleto.setDocumentoDestinatarioReal("12345678000195");
        boleto.setTipoDestinatarioReal("CNPJ");

        
        String assinatura = segurancaService.gerarAssinaturaDigital(boleto, empresaLegitima);
        boleto.setAssinaturaDigital(assinatura);

        
        EmpresaEmissora outraEmpresa = new EmpresaEmissora(
                2,
                "Outra Concessionária",
                "98765432000100",
                "CHAVE_PUBLICA_OUTRA_EMPRESA" 
        );

        
        boolean integro = segurancaService.validarIntegridadeBoleto(boleto, outraEmpresa);

        
        assertFalse("Boleto assinado com chave A não deve validar com chave B",
                    integro);

        System.out.println("  ✓ Teste validação com empresa diferente: PASSOU");
    }

}
