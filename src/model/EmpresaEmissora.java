package model;

/**
 * Representa uma Empresa Emissora de boletos cadastrada no sistema bancário.
 *
 * No contexto de segurança, esta entidade é a "âncora de confiança":
 * ela carrega a chavePublicaAutenticacao usada para assinar digitalmente
 * os boletos que emite. Qualquer boleto que alegue ser dessa empresa
 * precisa ter sua assinatura validada contra esta chave.
 *
 * Analogia: é o equivalente ao certificado digital de uma empresa —
 * sem ele, não há como provar autenticidade.
 */
public class EmpresaEmissora {

    // -------------------------------------------------------
    // Atributos
    // -------------------------------------------------------

    /** Identificador único interno da empresa no sistema bancário. */
    private int id;

    /** Nome comercial/razão social da empresa. */
    private String nomeEmpresa;

    /**
     * CNPJ da empresa emissora (somente dígitos, sem formatação).
     * Usado tanto para identificação cadastral quanto para validação
     * de correspondência com os dados impressos no boleto.
     */
    private String cnpj;

    /**
     * Chave pública de autenticação da empresa.
     * No cenário real, seria uma chave RSA/EC pública.
     * Neste projeto A3, funciona como o "segredo compartilhado"
     * usado pelo SegurancaService para gerar e validar o hash SHA-256
     * que garante a integridade dos boletos emitidos por esta empresa.
     *
     * TRAVA DE SEGURANÇA: Esta chave NUNCA deve ser transmitida
     * junto ao boleto. Ela fica custodiada pelo sistema bancário.
     */
    private String chavePublicaAutenticacao;

    // -------------------------------------------------------
    // Construtores
    // -------------------------------------------------------

    /**
     * Construtor padrão (necessário para frameworks de serialização,
     * caso venha a ser utilizado no futuro).
     */
    public EmpresaEmissora() {
    }

    /**
     * Construtor completo para instanciação direta com todos os atributos.
     *
     * @param id                      Identificador único da empresa.
     * @param nomeEmpresa             Razão social ou nome fantasia.
     * @param cnpj                    CNPJ da empresa (somente dígitos).
     * @param chavePublicaAutenticacao Chave usada para validar a assinatura digital.
     */
    public EmpresaEmissora(int id, String nomeEmpresa, String cnpj, String chavePublicaAutenticacao) {
        this.id = id;
        this.nomeEmpresa = nomeEmpresa;
        this.cnpj = cnpj;
        this.chavePublicaAutenticacao = chavePublicaAutenticacao;
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

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getChavePublicaAutenticacao() {
        return chavePublicaAutenticacao;
    }

    public void setChavePublicaAutenticacao(String chavePublicaAutenticacao) {
        this.chavePublicaAutenticacao = chavePublicaAutenticacao;
    }

    // -------------------------------------------------------
    // toString — útil para logs e debug
    // -------------------------------------------------------

    @Override
    public String toString() {
        return "EmpresaEmissora{"
                + "id=" + id
                + ", nomeEmpresa='" + nomeEmpresa + '\''
                + ", cnpj='" + cnpj + '\''
                + '}';
        // A chave NÃO é exibida no toString por razões de segurança.
    }
}
