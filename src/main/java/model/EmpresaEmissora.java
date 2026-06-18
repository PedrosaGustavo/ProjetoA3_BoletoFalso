package model;

public class EmpresaEmissora {

    private int id;

    private String nomeEmpresa;

    private String cnpj;

    private String chavePublicaAutenticacao;

    public EmpresaEmissora() {
    }

    public EmpresaEmissora(int id, String nomeEmpresa, String cnpj, String chavePublicaAutenticacao) {
        this.id = id;
        this.nomeEmpresa = nomeEmpresa;
        this.cnpj = cnpj;
        this.chavePublicaAutenticacao = chavePublicaAutenticacao;
    }

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
