package controller;

import model.Boleto;
import model.Boleto.Status;
import model.EmpresaEmissora;
import service.SegurancaService;

/**
 * Controlador principal do fluxo de pagamento bancário.
 *
 * Orquestra as verificações de segurança antes de autorizar
 * qualquer transação, funcionando como a "linha de defesa" do banco.
 *
 * ============================================================
 * ARQUITETURA DE SEGURANÇA EM CAMADAS (Defense in Depth)
 * ============================================================
 * O processamento é deliberadamente sequencial e fail-fast:
 * um boleto DEVE passar na Etapa 1 antes de chegar à Etapa 2.
 * Isso garante que boletos adulterados (Etapa 1) nunca
 * consumam processamento da verificação de modalidade (Etapa 2),
 * e que fraudes mais sofisticadas (hash re-gerado pelo fraudador
 * mas com modalidade errada) sejam capturadas na Etapa 2.
 *
 * ETAPA 1 — Integridade do Arquivo (Anti-Adulteração):
 *   Garante que o boleto não foi modificado após sua emissão.
 *   Detecta alterações de valor, conta de destino, etc.
 *
 * ETAPA 2 — Correspondência de Modalidade e Identidade (Anti-Desvio):
 *   Garante que a conta que receberá o dinheiro corresponde
 *   exatamente ao tipo e documento do emissor visível no papel.
 *   Detecta o golpe de "boleto falso" que troca CNPJ por CPF.
 */
public class BancoController {

    // -------------------------------------------------------
    // Dependência injetada (composição OOP)
    // -------------------------------------------------------

    /**
     * Serviço de segurança responsável pelos cálculos criptográficos.
     * Injeção via construtor — boa prática de OOP que facilita
     * testes e substituição da implementação.
     */
    private final SegurancaService segurancaService;

    // -------------------------------------------------------
    // Construtor
    // -------------------------------------------------------

    /**
     * Constrói o BancoController injetando o SegurancaService.
     *
     * @param segurancaService Serviço de criptografia e validação de integridade.
     */
    public BancoController(SegurancaService segurancaService) {
        this.segurancaService = segurancaService;
    }

    // -------------------------------------------------------
    // Método principal: processarPagamento
    // -------------------------------------------------------

    /**
     * Processa uma solicitação de pagamento de boleto aplicando
     * as duas camadas de verificação de segurança anti-fraude.
     *
     * O método modifica o status do boleto diretamente como efeito
     * colateral (registro auditável) e retorna uma mensagem descritiva
     * do resultado para o sistema chamador.
     *
     * @param boleto  O boleto apresentado para pagamento.
     * @param empresa A empresa emissora cadastrada no sistema bancário,
     *                usada como âncora de confiança para as verificações.
     * @return        Mensagem de texto descrevendo o resultado do processamento.
     */
    public String processarPagamento(Boleto boleto, EmpresaEmissora empresa) {

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  BANCO — Iniciando processamento do Boleto #" + boleto.getId());
        System.out.println("  Emissor cadastrado: " + empresa.getNomeEmpresa());
        System.out.println("  Valor declarado: R$" + String.format("%.2f", boleto.getValor()));
        System.out.println("=".repeat(60));

        // ==================================================
        // ETAPA 1: Verificação de Integridade do Arquivo
        // ==================================================
        System.out.println("\n  >> ETAPA 1: Verificando integridade do arquivo (SHA-256)...");

        boolean arquivoIntegro = segurancaService.validarIntegridadeBoleto(boleto, empresa);

        if (!arquivoIntegro) {
            /*
             * TRAVA 1 ATIVADA — O hash recalculado é diferente da assinatura
             * gravada no boleto. Isso prova que algum dado foi modificado
             * após a emissão legítima.
             *
             * AÇÃO: marcar o boleto como SUSPEITO, barrar o pagamento e
             * alertar o operador/sistema. O boleto NÃO é processado.
             *
             * MOTIVO DE SEGURANÇA: prosseguir com um arquivo adulterado
             * implicaria em aceitar dados que não foram aprovados pela
             * empresa emissora — risco imediato de desvio financeiro.
             */
            boleto.setStatus(Status.SUSPEITO);

            String mensagem = "[BLOQUEADO - ETAPA 1] "
                    + "ALERTA DE SEGURANÇA: O arquivo do boleto #" + boleto.getId()
                    + " foi ADULTERADO. "
                    + "A assinatura digital não confere com os dados recebidos. "
                    + "Possível intercepção e modificação maliciosa do documento. "
                    + "Status atualizado para: " + boleto.getStatus()
                    + ". Pagamento BARRADO.";

            System.out.println("\n  *** " + mensagem);
            return mensagem;
        }

        System.out.println("  >> ETAPA 1: APROVADA. Arquivo íntegro — dados não foram adulterados.");

        // ==================================================
        // ETAPA 2: Verificação de Modalidade e Identidade
        // ==================================================
        System.out.println("\n  >> ETAPA 2: Verificando correspondência de modalidade e identidade...");
        System.out.println("     Tipo no papel     : " + boleto.getTipoInscricaoPapel()
                           + " [" + boleto.getDocumentoEmissorNoPapel() + "]");
        System.out.println("     Tipo conta real    : " + boleto.getTipoDestinatarioReal()
                           + " [" + boleto.getDocumentoDestinatarioReal() + "]");

        /*
         * Verificação dupla de correspondência:
         *
         * CONDIÇÃO A — Divergência de TIPO/MODALIDADE:
         *   Ex: papel diz "CNPJ" (empresa), mas conta real é "CPF" (pessoa física).
         *   Este é o mecanismo central do "Golpe do Boleto Falso por Imediatismo":
         *   o pagador vê uma empresa, mas paga para um golpista PF.
         *
         * CONDIÇÃO B — Divergência de NÚMERO DE DOCUMENTO:
         *   Ex: papel diz CNPJ "12345678000195", mas conta real tem CNPJ "98765432000100".
         *   Mesmo sendo ambos CNPJ, o dinheiro vai para outra empresa — desvio de fundos.
         *
         * O operador OR (||) garante que QUALQUER uma das duas divergências
         * seja suficiente para barrar o pagamento.
         */
        boolean tiposDivergentes   = !boleto.getTipoInscricaoPapel()
                                           .equalsIgnoreCase(boleto.getTipoDestinatarioReal());

        boolean documentosDivergentes = !boleto.getDocumentoEmissorNoPapel()
                                               .equals(boleto.getDocumentoDestinatarioReal());

        if (tiposDivergentes || documentosDivergentes) {
            /*
             * TRAVA 2 ATIVADA — Os dados de identidade/modalidade não conferem.
             *
             * AÇÃO: marcar como FRAUDE_MODALIDADE e barrar o pagamento com
             * mensagem explícita indicando desvio de fundos detectado.
             *
             * DIFERENÇA DA ETAPA 1: aqui o hash pode estar válido — o golpista
             * pode ter recriado um hash sobre um boleto falsificado do zero.
             * Por isso esta etapa é independente e complementar à Etapa 1.
             */
            boleto.setStatus(Status.FRAUDE_MODALIDADE);

            // Constrói diagnóstico detalhado para o alerta
            StringBuilder diagnostico = new StringBuilder();
            if (tiposDivergentes) {
                diagnostico.append("Modalidade divergente: papel indica [")
                           .append(boleto.getTipoInscricaoPapel())
                           .append("] mas conta real é [")
                           .append(boleto.getTipoDestinatarioReal())
                           .append("]. ");
            }
            if (documentosDivergentes) {
                diagnostico.append("Documento divergente: papel indica [")
                           .append(boleto.getDocumentoEmissorNoPapel())
                           .append("] mas conta real registrada em [")
                           .append(boleto.getDocumentoDestinatarioReal())
                           .append("]. ");
            }

            String mensagem = "[BLOQUEADO - ETAPA 2] "
                    + "FRAUDE DETECTADA no boleto #" + boleto.getId()
                    + ": DESVIO DE FUNDOS CONFIRMADO. "
                    + diagnostico
                    + "O pagamento seria destinado a uma entidade diferente "
                    + "da exibida no documento. "
                    + "Status atualizado para: " + boleto.getStatus()
                    + ". Pagamento BARRADO.";

            System.out.println("\n  *** " + mensagem);
            return mensagem;
        }

        System.out.println("  >> ETAPA 2: APROVADA. Modalidade e identidade conferem.");

        // ==================================================
        // APROVAÇÃO FINAL — Boleto legítimo
        // ==================================================

        /*
         * O boleto passou em AMBAS as verificações:
         *  ✓ Hash íntegro (não adulterado)
         *  ✓ Modalidade e documento do destinatário real conferem com o papel
         *
         * Somente agora o pagamento é autorizado e o status atualizado para PAGO.
         */
        boleto.setStatus(Status.PAGO);

        String mensagem = "[APROVADO] "
                + "Boleto #" + boleto.getId()
                + " processado com sucesso. "
                + "Pagamento de R$" + String.format("%.2f", boleto.getValor())
                + " autorizado para " + boleto.getNomeEmissorNoPapel()
                + " [" + boleto.getTipoDestinatarioReal() + ": "
                + boleto.getDocumentoDestinatarioReal() + "]. "
                + "Status: " + boleto.getStatus() + ".";

        System.out.println("\n  *** " + mensagem);
        return mensagem;
    }
}
