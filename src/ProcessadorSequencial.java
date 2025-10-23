import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ProcessadorSequencial {

    public static void main(String[] args) {
        // 1. Verifica se os argumentos de linha de comando foram fornecidos corretamente
        if (args.length != 2) {
            System.out.println("Uso: java ProcessadorSequencial <imagem_de_entrada> <imagem_de_saida>");
            System.exit(1);
        }

        String caminhoEntrada = args[0];
        String caminhoSaida = args[1];

        try {
            // 2. Tenta carregar a imagem de entrada a partir do caminho fornecido
            File arquivoEntrada = new File(caminhoEntrada);
            BufferedImage imagemOriginal = ImageIO.read(arquivoEntrada);

            if (imagemOriginal == null) {
                System.out.println("Erro: Não foi possível ler a imagem. Verifique o caminho e o formato do arquivo.");
                System.exit(1);
            }
            
            System.out.println("Processando a imagem '" + caminhoEntrada + "' de forma sequencial...");

            // --- Medição de Tempo ---
            long inicio = System.nanoTime();

            // 3. Chama o método que realiza a conversão para escala de cinza
            BufferedImage imagemProcessada = converterParaEscalaDeCinza(imagemOriginal);

            long fim = System.nanoTime();
            // --- Fim da Medição de Tempo ---


            // 4. Salva a imagem resultante
            File arquivoSaida = new File(caminhoSaida);
            // Extrai a extensão do arquivo para salvar no formato correto (ex: "jpg", "png")
            String formato = caminhoSaida.substring(caminhoSaida.lastIndexOf('.') + 1);
            ImageIO.write(imagemProcessada, formato, arquivoSaida);
            
            // Calcula e exibe o tempo total de execução em milissegundos
            long tempoExecucaoMs = (fim - inicio) / 1_000_000;
            System.out.println("Imagem convertida com sucesso e salva em '" + caminhoSaida + "'");
            System.out.println("Tempo de execução (sequencial): " + tempoExecucaoMs + " ms");

        } catch (IOException e) {
            System.out.println("Ocorreu um erro de I/O (Entrada/Saída): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aplica o filtro de escala de cinza em uma imagem.
     *
     * @param imagemOriginal A imagem a ser processada.
     * @return A nova imagem em escala de cinza.
     */
    public static BufferedImage converterParaEscalaDeCinza(BufferedImage imagemOriginal) {
        int largura = imagemOriginal.getWidth();
        int altura = imagemOriginal.getHeight();

        // Cria uma nova imagem em branco com as mesmas dimensões para o resultado
        BufferedImage novaImagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);

        // Itera por cada pixel da imagem (de forma sequencial)
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                // Pega o valor inteiro do pixel, que contém as informações de R, G e B
                int pixel = imagemOriginal.getRGB(x, y);

                // Extrai os valores individuais de R, G e B usando operações de bitwise
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                // Aplica a fórmula de luminância para calcular o valor de cinza
                int valorCinza = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // Cria o novo valor de pixel. Em escala de cinza, R=G=B.
                int novoPixel = (valorCinza << 16) | (valorCinza << 8) | valorCinza;

                // Define o pixel na nova imagem
                novaImagem.setRGB(x, y, novoPixel);
            }
        }
        return novaImagem;
    }
}