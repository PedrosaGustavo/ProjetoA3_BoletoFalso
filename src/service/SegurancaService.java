package service;

import model.Boleto;
import model.EmpresaEmissora;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Serviço responsável por toda a criptografia e integridade de dados
 * do sistema antifrau de boletos.
 *
 * ============================================================
 * FUNDAMENTO CRIPTOGRÁFICO — SHA-256 (Secure Hash Algorithm 256)
 * ============================================================
 * SHA-256 é uma função de hash criptográfico pertencente à família
 * SHA-2, padronizada pelo NIST (National Institute of Standards
 * and Technology). Suas propriedades fundamentais para este projeto:
 *
 *  1. DETERMINISMO: o mesmo input sempre produz o mesmo output.
 *     → Permite recomputar e comparar o hash na validação.
 *
 *  2. EFEITO AVALANCHE: qualquer mínima alteração no input (um
 *     dígito, um centavo) gera um hash completamente diferente.
 *     → Impossível adulterar dados sem alterar o hash.
 *
 *  3. RESISTÊNCIA À PRÉ-IMAGEM (One-Way): é computacionalmente
 *     inviável reconstruir o input a partir do hash.
 *     → O golpista não consegue fabricar um hash válido sem a chave.
 *
 *  4. RESISTÊNCIA A COLISÕES: é computacionalmente inviável
 *     encontrar dois inputs diferentes com o mesmo hash.
 *     → Não há como forjar um boleto falso com hash idêntico.
 *
 * A classe java.security.MessageDigest é nativa do Java SE e
 * implementa SHA-256 sem necessidade de bibliotecas externas.
 */
public class SegurancaService {

    // -------------------------------------------------------
    // Constante do algoritmo
    // -------------------------------------------------------

    /**
     * Nome do algoritmo de hash conforme especificado na JCA
     * (Java Cryptography Architecture). Centralizado aqui para
     * facilitar auditoria e eventual migração futura (ex: SHA-3).
     */
    private static final String ALGORITMO_HASH = "SHA-256";

    // -------------------------------------------------------
    // Método 1: Geração da Assinatura Digital
    // -------------------------------------------------------

    /**
     * Gera a assinatura digital (hash SHA-256) de um boleto,
     * combinando seus dados cruciais com a chave da empresa emissora.
     *
     * DADOS INCLUÍDOS NO HASH (o que é "selado"):
     *  - ID do boleto
     *  - Valor (fundamental: qualquer centavo alterado quebra o hash)
     *  - Documento do destinatário real (conta de destino)
     *  - Tipo do destinatário real (CPF/CNPJ — modalidade da conta)
     *  - Chave pública da empresa (o "segredo" que vincula o hash à emissora)
     *
     * POR QUE NÃO INCLUIR O NOME DO EMISSOR NO PAPEL?
     * Dados visuais são o alvo do golpista — ele os mantém intactos.
     * O que importa selar criptograficamente são os dados financeiros
     * reais: valor e conta de destino.
     *
     * ANALOGIA: é como lacrar um envelope com cera — qualquer tentativa
     * de abrir e modificar o conteúdo quebra o lacre de forma visível.
     *
     * @param boleto  O boleto cujos dados serão incluídos no hash.
     * @param empresa A empresa emissora, cuja chave é o componente secreto.
     * @return        String hexadecimal de 64 caracteres representando
     *                o hash SHA-256 (256 bits = 32 bytes = 64 hex chars).
     * @throws RuntimeException se o algoritmo SHA-256 não estiver disponível
     *                          na JVM (situação extremamente improvável).
     */
    public String gerarAssinaturaDigital(Boleto boleto, EmpresaEmissora empresa) {
        /*
         * Concatenamos os campos críticos em uma única String de entrada.
         * O separador "|" evita colisões acidentais entre campos adjacentes.
         * Ex: valor="100" + doc="200" seria igual a valor="10" + doc="0200"
         * sem separador — com "|" isso não ocorre.
         */
        String dadosParaAssinar = boleto.getId()
                + "|" + boleto.getValor()
                + "|" + boleto.getDocumentoDestinatarioReal()
                + "|" + boleto.getTipoDestinatarioReal()
                + "|" + empresa.getChavePublicaAutenticacao(); // componente secreto

        return calcularHashSha256(dadosParaAssinar);
    }

    // -------------------------------------------------------
    // Método 2: Validação de Integridade do Boleto
    // -------------------------------------------------------

    /**
     * Valida a integridade de um boleto comparando a assinatura
     * gravada nele com o hash recalculado a partir dos dados recebidos.
     *
     * FLUXO DA VALIDAÇÃO:
     *  1. Recebe o boleto (possivelmente adulterado) e a empresa emissora.
     *  2. Recalcula o hash SHA-256 dos dados atuais do boleto + chave.
     *  3. Compara o hash recalculado com a assinatura gravada no boleto.
     *  4. Se forem idênticos → dados íntegros (não adulterados).
     *     Se forem diferentes → adulteração detectada!
     *
     * TRAVA DE SEGURANÇA 1 — Por que isso funciona?
     * Um golpista que altera o valor ou a conta de destino do boleto
     * não consegue re-gerar um hash válido sem conhecer a chavePublicaAutenticacao
     * da empresa emissora (que fica custodiada no sistema bancário).
     * Logo, qualquer alteração resulta em hash inválido → boleto SUSPEITO.
     *
     * @param boleto  O boleto recebido para pagamento (pode estar adulterado).
     * @param empresa A empresa emissora cadastrada no sistema bancário.
     * @return        {@code true} se o boleto está íntegro;
     *                {@code false} se foi adulterado.
     */
    public boolean validarIntegridadeBoleto(Boleto boleto, EmpresaEmissora empresa) {
        // Recalcula o hash com os dados atuais do boleto
        String hashRecalculado = gerarAssinaturaDigital(boleto, empresa);

        // Obtém o hash que foi gravado no boleto no momento da emissão legítima
        String hashOriginal = boleto.getAssinaturaDigital();

        /*
         * Comparação segura: usamos equals() direto pois ambos são strings
         * hexadecimais produzidas pela mesma função determinística.
         * Em produção real, recomenda-se MessageDigest.isEqual() para evitar
         * ataques de timing side-channel, mas para este projeto acadêmico
         * equals() é suficiente e mais legível.
         */
        boolean integro = hashRecalculado.equals(hashOriginal);

        // Log de diagnóstico — útil para auditoria
        System.out.println("  [SegurancaService] Hash original   : " + hashOriginal);
        System.out.println("  [SegurancaService] Hash recalculado: " + hashRecalculado);
        System.out.println("  [SegurancaService] Integridade OK?  : " + integro);

        return integro;
    }

    // -------------------------------------------------------
    // Método privado: núcleo criptográfico SHA-256
    // -------------------------------------------------------

    /**
     * Calcula o hash SHA-256 de uma string de entrada e retorna
     * a representação hexadecimal do resultado.
     *
     * DETALHES TÉCNICOS:
     *  - A string é convertida para bytes usando UTF-8 (charset universal).
     *  - MessageDigest.getInstance("SHA-256") retorna uma instância da
     *    implementação SHA-256 registrada na JCA (nativa da JVM).
     *  - O resultado bruto são 32 bytes (256 bits).
     *  - Convertemos para hex string de 64 chars para fácil armazenamento
     *    e comparação (String.format "%02x" garante zero-padding).
     *
     * @param entrada A string cujo hash será calculado.
     * @return        Hash SHA-256 em formato hexadecimal (64 caracteres).
     */
    private String calcularHashSha256(String entrada) {
        try {
            // 1. Obtém a implementação SHA-256 da JVM
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);

            // 2. Processa a string convertida para bytes UTF-8
            byte[] hashBytes = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));

            // 3. Converte o array de bytes para representação hexadecimal
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                /*
                 * "%02x" formata cada byte como dois dígitos hexadecimais
                 * em minúsculo, com zero-padding à esquerda se necessário.
                 * Ex: byte 5 → "05" (não "5"), byte 255 → "ff"
                 * Isso garante que o hash sempre tenha exatamente 64 chars.
                 */
                hexBuilder.append(String.format("%02x", b));
            }

            return hexBuilder.toString();

        } catch (NoSuchAlgorithmException e) {
            /*
             * SHA-256 faz parte do Java SE desde a versão 1.4.2 e é
             * obrigatório em toda JVM compatível (RFC 4051 / NIST FIPS 180-4).
             * Esta exceção na prática nunca ocorre, mas Java nos obriga a
             * tratá-la por ser checked exception.
             */
            throw new RuntimeException(
                    "ERRO CRÍTICO: Algoritmo " + ALGORITMO_HASH + " não encontrado na JVM. "
                    + "Verifique a instalação do Java.", e);
        }
    }
}
