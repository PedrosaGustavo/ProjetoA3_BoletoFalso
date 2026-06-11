package model;

/**
 * Representa um Boleto Bancário no sistema, contendo:
 *
 *  - Dados financeiros básicos (id, valor).
 *  - Assinatura digital gerada no momento da emissão legítima.
 *  - Status do ciclo de vida do boleto (rastreabilidade).
 *  - DADOS VISUAIS DO PAPEL: o que está impresso/visível para o pagador.
 *  - METADADOS REAIS DA CONTA: para onde o dinheiro REALMENTE vai.
 *
 * ============================================================
 * CONCEITO-CHAVE DE SEGURANÇA — Por que dois conjuntos de dados?
 * ============================================================
 * O "Golpe do Boleto Falso" funciona exatamente na lacuna entre
 * o que o pagador VÊ (dados visuais do papel) e para onde o
 * dinheiro REALMENTE vai (metadados reais da conta bancária).
 *
 * Um golpista pode:
 *  1. Interceptar um boleto legítimo de uma concessionária (CNPJ).
 *  2. Alterar a linha digitável/código de barras para apontar
 *     para sua conta pessoal (CPF).
 *  3. Manter o visual intacto — logo, nome da empresa, valor.
 *  4. O pagador vê "Energia Elétrica S.A." e paga sem desconfiar.
 *
 * As duas travas do BancoController atacam exatamente isso:
 *  - Trava 1 (Hash): detecta qualquer alteração no boleto.
 *  - Trava 2 (Modalidade): detecta divergência PJ → PF mesmo
 *    que o hash tenha sido re-gerado pelo fraudador.
 */
public class Boleto {

    // -------------------------------------------------------
    // Enum de Status — rastreia o ciclo de vida do boleto
    // -------------------------------------------------------

    /**
     * Define os possíveis estados de um boleto no sistema.
     */
    public enum Status {
        /** Estado inicial: boleto gerado, aguardando pagamento. */
        PENDENTE,

        /** Passou em todas as verificações de segurança e foi quitado. */
        PAGO,

        /**
         * TRAVA 1 ativada: o hash dos dados não bate com a assinatura
         * gravada no momento da emissão. O arquivo foi adulterado.
         */
        SUSPEITO,

        /**
         * TRAVA 2 ativada: dados visuais do papel apontam para uma
         * entidade (ex: CNPJ de concessionária), mas a conta bancária
         * real de destino pertence a outra modalidade ou documento
         * (ex: CPF de golpista). Desvio de fundos confirmado.
         */
        FRAUDE_MODALIDADE
    }

    // -------------------------------------------------------
    // Atributos — Dados Básicos
    // -------------------------------------------------------

    /** Identificador único do boleto no sistema. */
    private int id;

    /** Valor financeiro do boleto (em reais). */
    private double valor;

    /**
     * Assinatura digital (hash SHA-256) gerada no momento da emissão
     * legítima do boleto pelo SegurancaService.
     *
     * Funciona como um "selo lacrado": qualquer alteração posterior
     * nos dados (valor, conta destinatária etc.) fará com que o hash
     * recalculado na validação seja diferente deste, denunciando a fraude.
     */
    private String assinaturaDigital;

    /** Estado atual do boleto no fluxo de processamento. */
    private Status status;

    // -------------------------------------------------------
    // Atributos — Dados Visuais do Papel (o que o pagador VÊ)
    // -------------------------------------------------------

    /**
     * Nome do emissor conforme impresso no boleto.
     * Ex: "Energia Elétrica do Nordeste S.A."
     */
    private String nomeEmissorNoPapel;

    /**
     * Número do documento (CPF ou CNPJ) impresso no boleto.
     * Somente dígitos, sem formatação.
     * Ex: "12345678000195"
     */
    private String documentoEmissorNoPapel;

    /**
     * Tipo do documento impresso: "CPF" ou "CNPJ".
     * Uma concessionária legítima terá sempre "CNPJ".
     */
    private String tipoInscricaoPapel; // "CPF" ou "CNPJ"

    // -------------------------------------------------------
    // Atributos — Metadados Reais da Conta Bancária de Destino
    // -------------------------------------------------------

    /**
     * Número do documento do titular da conta bancária que
     * REALMENTE receberá o pagamento (somente dígitos).
     *
     * PONTO CRÍTICO: Em um boleto legítimo, este valor deve
     * coincidir com 'documentoEmissorNoPapel'. Se divergirem,
     * o dinheiro irá para um destinatário diferente do anunciado.
     */
    private String documentoDestinatarioReal;

    /**
     * Tipo do documento do destinatário real: "CPF" ou "CNPJ".
     *
     * PONTO CRÍTICO: Em um boleto legítimo de empresa (CNPJ),
     * este campo também deve ser "CNPJ". Se for "CPF", significa
     * que o dinheiro vai para uma pessoa física — possível golpe.
     */
    private String tipoDestinatarioReal; // "CPF" ou "CNPJ"

    // -------------------------------------------------------
    // Construtores
    // -------------------------------------------------------

    /**
     * Construtor padrão.
     */
    public Boleto() {
        this.status = Status.PENDENTE;
    }

    /**
     * Construtor completo para criação de um boleto com todos os dados.
     * O status é inicializado como PENDENTE — aguardando processamento.
     *
     * @param id                       Identificador do boleto.
     * @param valor                    Valor financeiro.
     * @param assinaturaDigital        Hash SHA-256 gerado na emissão.
     * @param nomeEmissorNoPapel       Nome visível no documento impresso.
     * @param documentoEmissorNoPapel  Documento (CPF/CNPJ) visível no papel.
     * @param tipoInscricaoPapel       "CPF" ou "CNPJ" — tipo do doc. no papel.
     * @param documentoDestinatarioReal Documento do titular real da conta.
     * @param tipoDestinatarioReal     "CPF" ou "CNPJ" — tipo da conta real.
     */
    public Boleto(int id,
                  double valor,
                  String assinaturaDigital,
                  String nomeEmissorNoPapel,
                  String documentoEmissorNoPapel,
                  String tipoInscricaoPapel,
                  String documentoDestinatarioReal,
                  String tipoDestinatarioReal) {
        this.id = id;
        this.valor = valor;
        this.assinaturaDigital = assinaturaDigital;
        this.nomeEmissorNoPapel = nomeEmissorNoPapel;
        this.documentoEmissorNoPapel = documentoEmissorNoPapel;
        this.tipoInscricaoPapel = tipoInscricaoPapel;
        this.documentoDestinatarioReal = documentoDestinatarioReal;
        this.tipoDestinatarioReal = tipoDestinatarioReal;
        this.status = Status.PENDENTE;
    }

    // -------------------------------------------------------
    // Getters e Setters (Encapsulamento)
    // -------------------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getAssinaturaDigital() {
        return assinaturaDigital;
    }

    public void setAssinaturaDigital(String assinaturaDigital) {
        this.assinaturaDigital = assinaturaDigital;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNomeEmissorNoPapel() {
        return nomeEmissorNoPapel;
    }

    public void setNomeEmissorNoPapel(String nomeEmissorNoPapel) {
        this.nomeEmissorNoPapel = nomeEmissorNoPapel;
    }

    public String getDocumentoEmissorNoPapel() {
        return documentoEmissorNoPapel;
    }

    public void setDocumentoEmissorNoPapel(String documentoEmissorNoPapel) {
        this.documentoEmissorNoPapel = documentoEmissorNoPapel;
    }

    public String getTipoInscricaoPapel() {
        return tipoInscricaoPapel;
    }

    public void setTipoInscricaoPapel(String tipoInscricaoPapel) {
        this.tipoInscricaoPapel = tipoInscricaoPapel;
    }

    public String getDocumentoDestinatarioReal() {
        return documentoDestinatarioReal;
    }

    public void setDocumentoDestinatarioReal(String documentoDestinatarioReal) {
        this.documentoDestinatarioReal = documentoDestinatarioReal;
    }

    public String getTipoDestinatarioReal() {
        return tipoDestinatarioReal;
    }

    public void setTipoDestinatarioReal(String tipoDestinatarioReal) {
        this.tipoDestinatarioReal = tipoDestinatarioReal;
    }

    // -------------------------------------------------------
    // toString — exibe um resumo do boleto para logs
    // -------------------------------------------------------

    @Override
    public String toString() {
        return "Boleto{"
                + "id=" + id
                + ", valor=R$" + String.format("%.2f", valor)
                + ", status=" + status
                + ", emissorNoPapel='" + nomeEmissorNoPapel + '\''
                + ", tipoInscricaoPapel='" + tipoInscricaoPapel + '\''
                + ", tipoDestinatarioReal='" + tipoDestinatarioReal + '\''
                + '}';
    }
}
