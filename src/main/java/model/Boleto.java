package model;

public class Boleto {

    public enum Status {

        PENDENTE,

        PAGO,
        
        SUSPEITO,

        FRAUDE_MODALIDADE
    }

    private int id;

    private double valor;

    private String assinaturaDigital;

    private Status status;

    private String nomeEmissorNoPapel;

    private String documentoEmissorNoPapel;

    private String tipoInscricaoPapel;

    private String documentoDestinatarioReal;

    private String tipoDestinatarioReal; // "CPF" ou "CNPJ"

    public Boleto() {
        this.status = Status.PENDENTE;
    }

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
