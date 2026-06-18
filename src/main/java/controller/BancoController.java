package controller;

import model.Boleto;
import model.EmpresaEmissora;
import model.Boleto.Status;
import service.SegurancaService;

public class BancoController {

    private SegurancaService segurancaService;

    public BancoController(SegurancaService segurancaService) {
        this.segurancaService = segurancaService;
    }

    public String processarPagamento(Boleto boleto, EmpresaEmissora empresa) {

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  BANCO — Iniciando processamento do Boleto #" + boleto.getId());
        System.out.println("  Emissor cadastrado: " + empresa.getNomeEmpresa());
        System.out.println("  Valor declarado: R$" + String.format("%.2f", boleto.getValor()));
        System.out.println("=".repeat(60));

        System.out.println("\n  >> ETAPA 1: Verificando integridade do arquivo (SHA-256)...");

        boolean arquivoIntegro = segurancaService.validarIntegridadeBoleto(boleto, empresa);

        if (!arquivoIntegro) {
            
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

        System.out.println("\n  >> ETAPA 2: Verificando correspondência de modalidade e identidade...");
        System.out.println("     Tipo no papel     : " + boleto.getTipoInscricaoPapel()
                           + " [" + boleto.getDocumentoEmissorNoPapel() + "]");
        System.out.println("     Tipo conta real    : " + boleto.getTipoDestinatarioReal()
                           + " [" + boleto.getDocumentoDestinatarioReal() + "]");

        boolean tiposDivergentes   = !boleto.getTipoInscricaoPapel()
                                           .equalsIgnoreCase(boleto.getTipoDestinatarioReal());

        boolean documentosDivergentes = !boleto.getDocumentoEmissorNoPapel()
                                               .equals(boleto.getDocumentoDestinatarioReal());

        if (tiposDivergentes || documentosDivergentes) {
           
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