package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {

    private static final String HOST = "localhost";  
    private static final int PORTA = 22222;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(HOST, PORTA);
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(
                new InputStreamReader(System.in))
        ) {
            System.out.println("Conectado ao servidor.\n");
            System.out.println("Comandos disponíveis:");
            System.out.println("  LOGIN <apelido>   — entrar no leilão com um nome");
            System.out.println("  LANCE <valor>     — dar um lance (ex: LANCE 1600.00)");
            System.out.println("  STATUS            — ver o estado atual do leilão");
            System.out.println("  SAIR              — desconectar\n");

            Thread threadLeitura = new Thread(() -> {
                try {
                    String mensagem;
                    while ((mensagem = entrada.readLine()) != null) {
                        System.out.println("[Servidor] " + mensagem);
                    }
                } catch (IOException e) {
                    System.out.println("Conexao encerrada.");
                }
            });
            threadLeitura.setDaemon(true);
            threadLeitura.start();

            String linha;
            while ((linha = teclado.readLine()) != null) {
                String comando = linha.trim();
                if (comando.isEmpty()) continue;

                saida.println(comando);

                if (comando.equalsIgnoreCase("SAIR")) {
                    System.out.println("Saindo...");
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Nao foi possivel conectar ao servidor: " + e.getMessage());
        }
    }
}