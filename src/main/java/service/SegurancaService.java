package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import model.Boleto;
import model.EmpresaEmissora;

public class SegurancaService {

    private static final String ALGORITMO_HASH = "SHA-256";

    public String gerarAssinaturaDigital(Boleto boleto, EmpresaEmissora empresa) {

        String dadosParaAssinar = boleto.getId()
                + "|" + boleto.getValor()
                + "|" + boleto.getDocumentoDestinatarioReal()
                + "|" + boleto.getTipoDestinatarioReal()
                + "|" + empresa.getChavePublicaAutenticacao(); // componente secreto

        return calcularHashSha256(dadosParaAssinar);
    }

    public boolean validarIntegridadeBoleto(Boleto boleto, EmpresaEmissora empresa) {

        String hashRecalculado = gerarAssinaturaDigital(boleto, empresa);

        String hashOriginal = boleto.getAssinaturaDigital();

        boolean integro = hashRecalculado.equals(hashOriginal);

        System.out.println("  [SegurancaService] Hash original   : " + hashOriginal);
        System.out.println("  [SegurancaService] Hash recalculado: " + hashRecalculado);
        System.out.println("  [SegurancaService] Integridade OK?  : " + integro);

        return integro;
    }

    private String calcularHashSha256(String entrada) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);

            byte[] hashBytes = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hashBytes) {

                hexBuilder.append(String.format("%02x", b));
            }


            return hexBuilder.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "ERRO CRÍTICO: Algoritmo " + ALGORITMO_HASH + " não encontrado na JVM. "
                    + "Verifique a instalação do Java.", e);
        }
    }
}
