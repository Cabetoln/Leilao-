package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    private static final int PORTA = 12345;

    public static void main(String[] args) {
        GerenciadorLeilao gerenciador = new GerenciadorLeilao();

        System.out.println("Servidor iniciado na porta " + PORTA);

        new Thread(() -> {
            try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("Digite INICIAR para começar o leilão.");
                String linha;
                while ((linha = teclado.readLine()) != null) {
                    if (linha.trim().equalsIgnoreCase("INICIAR")) {
                        gerenciador.iniciarProximoLeilao();
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro na leitura do terminal.");
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + socket.getInetAddress());

                ClienteHandler handler = new ClienteHandler(socket, gerenciador);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}