package test;

import controller.BancoController;
import model.Boleto;
import model.Boleto.Status;
import model.EmpresaEmissora;
import service.SegurancaService;

/**
 * Suite de testes de integração do Projeto A3 — Combate ao Golpe do Boleto Falso.
 *
 * Simula três cenários reais que o sistema bancário pode encontrar,
 * cobrindo tanto o caminho feliz quanto os dois vetores principais de fraude.
 *
 * COMO EXECUTAR:
 *   Compile todos os arquivos a partir do diretório /src:
 *     javac model/EmpresaEmissora.java model/Boleto.java \
 *           service/SegurancaService.java controller/BancoController.java \
 *           test/SegurancaServiceTest.java
 *
 *   Execute:
 *     java test.SegurancaServiceTest
 *
 * ============================================================
 * CENÁRIOS TESTADOS
 * ============================================================
 *
 * CENÁRIO 1 — Caminho Feliz:
 *   Boleto legítimo de concessionária de energia (CNPJ).
 *   Resultado esperado: Status = PAGO.
 *
 * CENÁRIO 2 — Fraude por Adulteração (Intercepção):
 *   Boleto legítimo interceptado; valor e/ou conta alterados.
 *   O hash original não bate com os dados adulterados.
 *   Resultado esperado: Status = SUSPEITO (barrado na Etapa 1).
 *
 * CENÁRIO 3 — Fraude por Escopo/Imediatismo (Modalidade):
 *   Boleto visualmente idêntico ao de uma concessionária (CNPJ),
 *   mas internamente a conta de destino real é de um golpista PF (CPF).
 *   O hash está válido (o golpista gerou um boleto falso do zero),
 *   mas a modalidade denuncia a fraude.
 *   Resultado esperado: Status = FRAUDE_MODALIDADE (barrado na Etapa 2).
 */
public class SegurancaServiceTest {

    public static void main(String[] args) {

        // -------------------------------------------------------
        // Setup: instanciação dos serviços e controlador
        // -------------------------------------------------------

        // Serviço de criptografia — núcleo do sistema antifrau
        SegurancaService segurancaService = new SegurancaService();

        // Controlador bancário — injeta o serviço de segurança
        BancoController bancoController = new BancoController(segurancaService);

        /*
         * Empresa emissora cadastrada no sistema bancário.
         * Representa "Energia Elétrica do Nordeste S.A.",
         * uma concessionária legítima com CNPJ e chave de autenticação.
         *
         * NOTA: a chavePublicaAutenticacao em produção seria uma chave
         * RSA/EC pública de 2048+ bits. Aqui usamos uma string
         * representativa para fins didáticos.
         */
        EmpresaEmissora empresaLegitima = new EmpresaEmissora(
                1,
                "Energia Elétrica do Nordeste S.A.",
                "12345678000195",                     // CNPJ da concessionária
                "CHAVE_PUBLICA_ENERGIA_NE_2024_XYZ"   // chave de autenticação
        );

        // Cabeçalho da suite de testes
        System.out.println("\n");
        System.out.println("*".repeat(70));
        System.out.println("*  PROJETO A3 — COMBATE AO GOLPE DO BOLETO FALSO                     *");
        System.out.println("*  Suite de Testes de Integração — SegurancaServiceTest               *");
        System.out.println("*".repeat(70));

        // Executa cada cenário e coleta os resultados
        boolean cenario1Ok = executarCenario1(segurancaService, bancoController, empresaLegitima);
        boolean cenario2Ok = executarCenario2(segurancaService, bancoController, empresaLegitima);
        boolean cenario3Ok = executarCenario3(segurancaService, bancoController, empresaLegitima);

        // Relatório final consolidado
        imprimirRelatorioFinal(cenario1Ok, cenario2Ok, cenario3Ok);
    }

    // ===========================================================
    // CENÁRIO 1 — Boleto Autêntico: Pagamento Deve Ser Aprovado
    // ===========================================================

    /**
     * Simula o pagamento de uma conta de energia legítima.
     *
     * O boleto é emitido pela própria concessionária, a assinatura
     * é gerada com a chave correta, e todos os dados batem.
     * Ambas as etapas devem ser aprovadas → Status: PAGO.
     *
     * @return true se o teste passou (status == PAGO), false caso contrário.
     */
    private static boolean executarCenario1(SegurancaService segurancaService,
                                            BancoController bancoController,
                                            EmpresaEmissora empresa) {

        System.out.println("\n\n" + "#".repeat(70));
        System.out.println("# CENÁRIO 1: Boleto Autêntico — Concessionária PJ                     #");
        System.out.println("#".repeat(70));
        System.out.println("  Contexto: Cliente recebe conta de energia legítima e vai pagar.");

        // 1a. Cria o boleto COM dados consistentes (papel == conta real)
        Boleto boletoOriginal = new Boleto();
        boletoOriginal.setId(1001);
        boletoOriginal.setValor(157.43);
        boletoOriginal.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoOriginal.setDocumentoEmissorNoPapel("12345678000195"); // CNPJ — papel
        boletoOriginal.setTipoInscricaoPapel("CNPJ");
        boletoOriginal.setDocumentoDestinatarioReal("12345678000195"); // CNPJ — conta real (igual)
        boletoOriginal.setTipoDestinatarioReal("CNPJ");

        // 1b. Gera a assinatura digital legítima no momento da emissão
        String assinaturaLegitima = segurancaService.gerarAssinaturaDigital(boletoOriginal, empresa);
        boletoOriginal.setAssinaturaDigital(assinaturaLegitima);

        System.out.println("\n  Boleto emitido: " + boletoOriginal);
        System.out.println("  Assinatura gerada e gravada no boleto.");

        // 1c. Processa o pagamento
        String resultado = bancoController.processarPagamento(boletoOriginal, empresa);

        // 1d. Verificação do resultado esperado
        boolean passou = boletoOriginal.getStatus() == Status.PAGO;
        imprimirResultadoCenario(1, passou, Status.PAGO, boletoOriginal.getStatus(), resultado);
        return passou;
    }

    // ===========================================================
    // CENÁRIO 2 — Fraude por Adulteração: Etapa 1 Deve Barrar
    // ===========================================================

    /**
     * Simula um boleto legítimo interceptado por um fraudador que
     * altera o valor e a conta de destino, mas NÃO consegue re-gerar
     * um hash válido (pois não possui a chave da empresa).
     *
     * ETAPA 1 deve detectar a adulteração → Status: SUSPEITO.
     *
     * @return true se o teste passou (status == SUSPEITO), false caso contrário.
     */
    private static boolean executarCenario2(SegurancaService segurancaService,
                                            BancoController bancoController,
                                            EmpresaEmissora empresa) {

        System.out.println("\n\n" + "#".repeat(70));
        System.out.println("# CENÁRIO 2: Fraude por Adulteração — Arquivo Interceptado e Alterado  #");
        System.out.println("#".repeat(70));
        System.out.println("  Contexto: Fraudador intercept o boleto, altera valor e conta.");

        // 2a. Cria e assina o boleto ORIGINAL legítimo
        Boleto boletoOriginal = new Boleto();
        boletoOriginal.setId(1002);
        boletoOriginal.setValor(230.00);
        boletoOriginal.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoOriginal.setDocumentoEmissorNoPapel("12345678000195");
        boletoOriginal.setTipoInscricaoPapel("CNPJ");
        boletoOriginal.setDocumentoDestinatarioReal("12345678000195"); // conta correta
        boletoOriginal.setTipoDestinatarioReal("CNPJ");

        // Assina o boleto original ANTES da adulteração
        String assinaturaOriginal = segurancaService.gerarAssinaturaDigital(boletoOriginal, empresa);
        boletoOriginal.setAssinaturaDigital(assinaturaOriginal);

        System.out.println("\n  Boleto original (antes da adulteração): " + boletoOriginal);
        System.out.println("  Assinatura original gerada e gravada.");

        // 2b. Simula a ADULTERAÇÃO: fraudador altera valor e redireciona para outra conta
        System.out.println("\n  [SIMULANDO ATAQUE] Fraudador adultera o boleto...");
        boletoOriginal.setValor(1.00);                                 // valor original: R$230
        boletoOriginal.setDocumentoDestinatarioReal("99988877000166"); // conta do fraudador
        // IMPORTANTE: a assinatura NÃO é alterada (fraudador não tem a chave)

        System.out.println("  Boleto adulterado (enviado ao banco): " + boletoOriginal);
        System.out.println("  Assinatura no boleto: " + boletoOriginal.getAssinaturaDigital()
                           + " (hash do boleto ORIGINAL — inválido para os dados atuais)");

        // 2c. Processa o pagamento — deve ser barrado na ETAPA 1
        String resultado = bancoController.processarPagamento(boletoOriginal, empresa);

        // 2d. Verificação do resultado esperado
        boolean passou = boletoOriginal.getStatus() == Status.SUSPEITO;
        imprimirResultadoCenario(2, passou, Status.SUSPEITO, boletoOriginal.getStatus(), resultado);
        return passou;
    }

    // ===========================================================
    // CENÁRIO 3 — Fraude de Escopo/Imediatismo: Etapa 2 Deve Barrar
    // ===========================================================

    /**
     * Simula o cenário mais sofisticado: o golpista não adultera um
     * boleto existente, mas cria um boleto FALSO do zero.
     *
     * O boleto falso:
     *  - Tem visual idêntico ao da concessionária (nome, logo, CNPJ impresso).
     *  - Tem hash válido (gerado pelo próprio golpista com sua chave de "empresa-fantasma").
     *  - MAS a conta bancária real é de uma pessoa física (CPF do golpista).
     *
     * Neste cenário, imagine que o sistema bancário, ao receber o boleto,
     * consulta a empresa emissora declarada e descobre que o CNPJ declarado
     * pertence à concessionária legítima — cujos dados batem com a empresa
     * cadastrada. A trava 1 passaria SE o fraudador tivesse a chave da empresa
     * real, o que não é o caso aqui.
     *
     * PORÉM, para demonstrar EXCLUSIVAMENTE a Etapa 2, simulamos um cenário
     * onde o hash foi re-gerado com a chave legítima (ex: vazamento interno,
     * ou o fraudador fez parceria com um funcionário desonesto da empresa).
     * O importante é que a Etapa 2 detecta a divergência de modalidade.
     *
     * ETAPA 1: APROVADA (hash válido para os dados do boleto falso)
     * ETAPA 2: BARRADA (CPF vs CNPJ — desvio de fundos detectado)
     * Resultado esperado → Status: FRAUDE_MODALIDADE.
     *
     * @return true se o teste passou (status == FRAUDE_MODALIDADE).
     */
    private static boolean executarCenario3(SegurancaService segurancaService,
                                            BancoController bancoController,
                                            EmpresaEmissora empresa) {

        System.out.println("\n\n" + "#".repeat(70));
        System.out.println("# CENÁRIO 3: Fraude de Imediatismo — CNPJ no Papel, CPF na Conta Real #");
        System.out.println("#".repeat(70));
        System.out.println("  Contexto: Golpista cria boleto falso com visual de concessionária,");
        System.out.println("  mas direciona o dinheiro para sua conta pessoal (CPF).");
        System.out.println("  Hash re-gerado internamente (simula vazamento da chave ou insider).");

        // 3a. Cria o boleto FALSO: visual de empresa, mas conta real é CPF de golpista
        Boleto boletoFalso = new Boleto();
        boletoFalso.setId(1003);
        boletoFalso.setValor(157.43);

        // Dados visuais: idênticos ao boleto da concessionária legítima
        boletoFalso.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");  // visual correto
        boletoFalso.setDocumentoEmissorNoPapel("12345678000195");                // CNPJ no papel
        boletoFalso.setTipoInscricaoPapel("CNPJ");                              // tipo no papel: PJ

        // Metadados reais: conta do GOLPISTA (pessoa física — CPF)
        boletoFalso.setDocumentoDestinatarioReal("98765432100");  // CPF do golpista!
        boletoFalso.setTipoDestinatarioReal("CPF");               // AQUI está a divergência fatal

        // 3b. Gera hash VÁLIDO sobre os dados do boleto falso
        //     (simulando que o golpista obteve a chave — cenário insider)
        String assinaturaFalsa = segurancaService.gerarAssinaturaDigital(boletoFalso, empresa);
        boletoFalso.setAssinaturaDigital(assinaturaFalsa);

        System.out.println("\n  Boleto falso (criado pelo golpista): " + boletoFalso);
        System.out.println("  Assinatura gerada com chave vazada — hash válido para os dados.");
        System.out.println("\n  O que o pagador VÊ:  CNPJ 12345678000195 (Concessionária)");
        System.out.println("  Para onde VAI o $$$:  CPF  98765432100     (Golpista!)");

        // 3c. Processa o pagamento — Etapa 1 passa, Etapa 2 barra
        String resultado = bancoController.processarPagamento(boletoFalso, empresa);

        // 3d. Verificação do resultado esperado
        boolean passou = boletoFalso.getStatus() == Status.FRAUDE_MODALIDADE;
        imprimirResultadoCenario(3, passou, Status.FRAUDE_MODALIDADE, boletoFalso.getStatus(), resultado);
        return passou;
    }

    // -------------------------------------------------------
    // Utilitários de exibição
    // -------------------------------------------------------

    /**
     * Imprime o resultado de um cenário de teste de forma padronizada.
     */
    private static void imprimirResultadoCenario(int numero,
                                                  boolean passou,
                                                  Status statusEsperado,
                                                  Status statusObtido,
                                                  String mensagemRetornada) {
        System.out.println("\n  ─────────────────────────────────────");
        System.out.println("  RESULTADO DO CENÁRIO " + numero + ":");
        System.out.println("  Status esperado : " + statusEsperado);
        System.out.println("  Status obtido   : " + statusObtido);
        System.out.println("  Veredicto       : " + (passou ? "✓ PASSOU" : "✗ FALHOU"));
        System.out.println("  ─────────────────────────────────────");
    }

    /**
     * Imprime o relatório consolidado de todos os cenários.
     */
    private static void imprimirRelatorioFinal(boolean c1, boolean c2, boolean c3) {
        int total    = 3;
        int aprovados = (c1 ? 1 : 0) + (c2 ? 1 : 0) + (c3 ? 1 : 0);

        System.out.println("\n\n" + "=".repeat(70));
        System.out.println("  RELATÓRIO FINAL — SUITE DE TESTES ANTIFRAU A3");
        System.out.println("=".repeat(70));
        System.out.printf("  Cenário 1 (Caminho Feliz)       : %s%n", c1 ? "✓ PASSOU" : "✗ FALHOU");
        System.out.printf("  Cenário 2 (Fraude Adulteração)  : %s%n", c2 ? "✓ PASSOU" : "✗ FALHOU");
        System.out.printf("  Cenário 3 (Fraude Modalidade)   : %s%n", c3 ? "✓ PASSOU" : "✗ FALHOU");
        System.out.println("  " + "-".repeat(46));
        System.out.printf("  Total: %d/%d testes aprovados%n", aprovados, total);
        System.out.println("=".repeat(70));

        if (aprovados == total) {
            System.out.println("\n  ✓ SISTEMA ANTIFRAU FUNCIONANDO CORRETAMENTE.");
            System.out.println("  Todas as travas de segurança operam como esperado.\n");
        } else {
            System.out.println("\n  ✗ ATENÇÃO: " + (total - aprovados) + " teste(s) FALHARAM.");
            System.out.println("  Revisar implementação antes de colocar em produção.\n");
        }
    }
}
