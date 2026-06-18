package test;

import controller.BancoController;
import model.Boleto;
import model.Boleto.Status;
import model.EmpresaEmissora;
import service.SegurancaService;

public class SegurancaServiceTest {

    public static void main(String[] args) {

        SegurancaService segurancaService = new SegurancaService();

        BancoController bancoController = new BancoController(segurancaService);

        EmpresaEmissora empresaLegitima = new EmpresaEmissora(
                1,
                "Energia Elétrica do Nordeste S.A.",
                "12345678000195",                    
                "CHAVE_PUBLICA_ENERGIA_NE_2024_XYZ"  
        );

        System.out.println("\n");
        System.out.println("*".repeat(70));
        System.out.println("*  PROJETO A3 — COMBATE AO GOLPE DO BOLETO FALSO                     *");
        System.out.println("*  Suite de Testes de Integração — SegurancaServiceTest               *");
        System.out.println("*".repeat(70));

        boolean cenario1Ok = executarCenario1(segurancaService, bancoController, empresaLegitima);
        boolean cenario2Ok = executarCenario2(segurancaService, bancoController, empresaLegitima);
        boolean cenario3Ok = executarCenario3(segurancaService, bancoController, empresaLegitima);

        imprimirRelatorioFinal(cenario1Ok, cenario2Ok, cenario3Ok);
    }

    private static boolean executarCenario1(SegurancaService segurancaService,
                                            BancoController bancoController,
                                            EmpresaEmissora empresa) {

        System.out.println("\n\n" + "#".repeat(70));
        System.out.println("# CENÁRIO 1: Boleto Autêntico — Concessionária PJ                     #");
        System.out.println("#".repeat(70));
        System.out.println("  Contexto: Cliente recebe conta de energia legítima e vai pagar.");

        Boleto boletoOriginal = new Boleto();
        boletoOriginal.setId(1001);
        boletoOriginal.setValor(157.43);
        boletoOriginal.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoOriginal.setDocumentoEmissorNoPapel("12345678000195"); 
        boletoOriginal.setTipoInscricaoPapel("CNPJ");
        boletoOriginal.setDocumentoDestinatarioReal("12345678000195"); 
        boletoOriginal.setTipoDestinatarioReal("CNPJ");

        String assinaturaLegitima = segurancaService.gerarAssinaturaDigital(boletoOriginal, empresa);
        boletoOriginal.setAssinaturaDigital(assinaturaLegitima);

        System.out.println("\n  Boleto emitido: " + boletoOriginal);
        System.out.println("  Assinatura gerada e gravada no boleto.");

        String resultado = bancoController.processarPagamento(boletoOriginal, empresa);

        boolean passou = boletoOriginal.getStatus() == Status.PAGO;
        imprimirResultadoCenario(1, passou, Status.PAGO, boletoOriginal.getStatus(), resultado);
        return passou;
    }

    private static boolean executarCenario2(SegurancaService segurancaService,
                                            BancoController bancoController,
                                            EmpresaEmissora empresa) {

        System.out.println("\n\n" + "#".repeat(70));
        System.out.println("# CENÁRIO 2: Fraude por Adulteração — Arquivo Interceptado e Alterado  #");
        System.out.println("#".repeat(70));
        System.out.println("  Contexto: Fraudador intercept o boleto, altera valor e conta.");

        Boleto boletoOriginal = new Boleto();
        boletoOriginal.setId(1002);
        boletoOriginal.setValor(230.00);
        boletoOriginal.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");
        boletoOriginal.setDocumentoEmissorNoPapel("12345678000195");
        boletoOriginal.setTipoInscricaoPapel("CNPJ");
        boletoOriginal.setDocumentoDestinatarioReal("12345678000195"); 
        boletoOriginal.setTipoDestinatarioReal("CNPJ");

        String assinaturaOriginal = segurancaService.gerarAssinaturaDigital(boletoOriginal, empresa);
        boletoOriginal.setAssinaturaDigital(assinaturaOriginal);

        System.out.println("\n  Boleto original (antes da adulteração): " + boletoOriginal);
        System.out.println("  Assinatura original gerada e gravada.");

        System.out.println("\n  [SIMULANDO ATAQUE] Fraudador adultera o boleto...");
        boletoOriginal.setValor(1.00);                                 
        boletoOriginal.setDocumentoDestinatarioReal("99988877000166"); 

        System.out.println("  Boleto adulterado (enviado ao banco): " + boletoOriginal);
        System.out.println("  Assinatura no boleto: " + boletoOriginal.getAssinaturaDigital()
                           + " (hash do boleto ORIGINAL — inválido para os dados atuais)");

        String resultado = bancoController.processarPagamento(boletoOriginal, empresa);

        boolean passou = boletoOriginal.getStatus() == Status.SUSPEITO;
        imprimirResultadoCenario(2, passou, Status.SUSPEITO, boletoOriginal.getStatus(), resultado);
        return passou;
    }

    private static boolean executarCenario3(SegurancaService segurancaService,
                                            BancoController bancoController,
                                            EmpresaEmissora empresa) {

        System.out.println("\n\n" + "#".repeat(70));
        System.out.println("# CENÁRIO 3: Fraude de Imediatismo — CNPJ no Papel, CPF na Conta Real #");
        System.out.println("#".repeat(70));
        System.out.println("  Contexto: Golpista cria boleto falso com visual de concessionária,");
        System.out.println("  mas direciona o dinheiro para sua conta pessoal (CPF).");
        System.out.println("  Hash re-gerado internamente (simula vazamento da chave ou insider).");

        Boleto boletoFalso = new Boleto();
        boletoFalso.setId(1003);
        boletoFalso.setValor(157.43);

        boletoFalso.setNomeEmissorNoPapel("Energia Elétrica do Nordeste S.A.");  
        boletoFalso.setDocumentoEmissorNoPapel("12345678000195");                
        boletoFalso.setTipoInscricaoPapel("CNPJ");                              

        boletoFalso.setDocumentoDestinatarioReal("98765432100");  
        boletoFalso.setTipoDestinatarioReal("CPF");               

        String assinaturaFalsa = segurancaService.gerarAssinaturaDigital(boletoFalso, empresa);
        boletoFalso.setAssinaturaDigital(assinaturaFalsa);

        System.out.println("\n  Boleto falso (criado pelo golpista): " + boletoFalso);
        System.out.println("  Assinatura gerada com chave vazada — hash válido para os dados.");
        System.out.println("\n  O que o pagador VÊ:  CNPJ 12345678000195 (Concessionária)");
        System.out.println("  Para onde VAI o $$$:  CPF  98765432100     (Golpista!)");

        String resultado = bancoController.processarPagamento(boletoFalso, empresa);

        boolean passou = boletoFalso.getStatus() == Status.FRAUDE_MODALIDADE;
        imprimirResultadoCenario(3, passou, Status.FRAUDE_MODALIDADE, boletoFalso.getStatus(), resultado);
        return passou;
    }

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
