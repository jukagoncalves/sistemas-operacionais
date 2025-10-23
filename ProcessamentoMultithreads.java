import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ProcessamentoMultithreads {

    private static class FiltroRunnable implements Runnable {
        private final BufferedImage imagem;
        private final int startRow;
        private final int endRow;

        public FiltroRunnable(BufferedImage img, int startRow, int endRow) { //inicializador do objeto
            this.imagem = img;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        public void run() {
            int width = imagem.getWidth();
            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = imagem.getRGB(x, y);
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = (rgb) & 0xFF;
                    int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                    int newRgb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
                    imagem.setRGB(x, y, newRgb);
                }
            }
        }
    }
    // --------------------------------------------------------------------------


    // --- FUNÇÕES AUXILIARES DE CÓPIA E NOMECLATURA ---
    
    private static BufferedImage copiarImagem(BufferedImage original) {
        BufferedImage copia = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        copia.setData(original.getData());
        return copia;
    }
    
    private static String getOutputFileName(String originalPath, int threads) {
        int dotIndex = originalPath.lastIndexOf('.'); //posição do ultimo ponto (indice)
        if (dotIndex > 0) {
            String baseName = originalPath.substring(0, dotIndex);
            String extension = originalPath.substring(dotIndex); 
            return baseName + "_gray_t" + threads + extension; 
        }
        return originalPath + "_gray_t" + threads + ".jpg";
    }

    // --- LÓGICAS DE EXECUÇÃO ---

    /**
     * Aplica o filtro sequencial em uma CÓPIA da imagem.
     * NÃO SALVA O ARQUIVO DE SAÍDA.
     * @return O tempo de execução em milissegundos (Ts).
     */
    public static long executarSequencial(BufferedImage imagemOriginal) {
        // Trabalha em uma cópia para não alterar a imagem original para o próximo teste
        BufferedImage imagemDeTrabalho = copiarImagem(imagemOriginal);
        int largura = imagemDeTrabalho.getWidth();
        int altura = imagemDeTrabalho.getHeight();

        long inicio = System.currentTimeMillis();

        // Lógica sequencial pura
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                int pixel = imagemDeTrabalho.getRGB(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                int valorCinza = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int novoPixel = (valorCinza << 16) | (valorCinza << 8) | valorCinza;
                imagemDeTrabalho.setRGB(x, y, novoPixel);
            }
        }

        long fim = System.currentTimeMillis();
        return fim - inicio; //retorna tempo de processamento sequencial
    }


    /**
     * Aplica o filtro multithread em uma CÓPIA da imagem.
     * SALVA O ARQUIVO DE SAÍDA após a medição.
     * @return O tempo de execução em milissegundos (Tn).
     */
    public static long executarMultithread(BufferedImage imagemOriginal, int numThreads, String filePath) throws InterruptedException, IOException {
        // Trabalha em uma cópia
        BufferedImage imagemDeTrabalho = copiarImagem(imagemOriginal);
        int altura = imagemDeTrabalho.getHeight();
        int fatia = altura / numThreads;
        List<Thread> workers = new ArrayList<>(); //repositorio de threads ativas
        
        long inicio = System.currentTimeMillis();

        // 1. CRIAÇÃO E EXECUÇÃO DAS THREADS
        for (int i = 0; i < numThreads; i++) {
            int startRow = i * fatia;
            int endRow = (i == numThreads - 1) ? altura : startRow + fatia; //operador ternário

            FiltroRunnable runnable = new FiltroRunnable(imagemDeTrabalho, startRow, endRow);
            Thread t = new Thread(runnable);
            workers.add(t); //adc na lista
            t.start();
        }

        // 2. AGUARDAR O TÉRMINO (JOIN)
        for (Thread t : workers) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        long tempoExecucao = fim - inicio;  //calcula sem a gravação
        
        // 3. GRAVAÇÃO DA IMAGEM (Apenas para multithread)
        String outputFileName = getOutputFileName(filePath, numThreads);
        ImageIO.write(imagemDeTrabalho, "jpg", new File(outputFileName));
        System.out.println("Arquivo de saída multithread salvo: " + outputFileName);

        return tempoExecucao; //retorna tempo de processamento paralelo(ms)
    }

    // --- MÉTODO PRINCIPAL ---
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ComparadorProcessamento <caminho_imagem> <num_threads>");
            return;
        }

        String filePath = args[0]; //primeiro argumento do terminal
        int numThreads;

        try {
            numThreads = Integer.parseInt(args[1]); //converte em int
            if (numThreads < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.err.println("O número de threads deve ser um inteiro positivo (>= 1).");
            return;
        }

        BufferedImage imagemOriginal;
        try {
            imagemOriginal = ImageIO.read(new File(filePath));
        } catch (IOException e) {
            System.err.println("Erro ao carregar a imagem: " + e.getMessage()); //se falhar por I/O
            return;
        }

        if (imagemOriginal == null) {
            System.err.println("Não foi possível carregar a imagem."); //se a imagem esta em formato nao suportado
            return;
        }
        
        System.out.printf("Iniciando comparação para a imagem '%s' e %d threads...%n", filePath, numThreads);

        try {
            // --- 1. EXECUÇÃO SEQUENCIAL (Apenas Medição) ---
            long tempoSequencial = executarSequencial(imagemOriginal);
            System.out.println("\nTempo de execução (SEQUENCIAL - Ts): " + tempoSequencial + " ms");
            
            // --- 2. EXECUÇÃO MULTITHREAD (Medição e Saída) ---
            long tempoMultithread = executarMultithread(imagemOriginal, numThreads, filePath);
            System.out.printf("Tempo de execução (MULTITHREAD - Tn com %d threads): %d ms%n", numThreads, tempoMultithread);

            // --- 3. COMPARAÇÃO E RESULTADOS ---
            if (tempoSequencial > 0 && tempoMultithread > 0) {
                double speedup = (double) tempoSequencial / tempoMultithread;
                System.out.println("\n--- RESULTADOS DA COMPARAÇÃO ---");
                System.out.printf("Speedup (Ts / Tn): %.2f x%n", speedup);
            }

        } catch (InterruptedException | IOException e) { //captura join interrompido e falha de gravação
            System.err.println("Ocorreu um erro durante a execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}