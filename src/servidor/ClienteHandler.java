package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteHandler implements Runnable {

    private final Socket socket;
    private final GerenciadorLeilao gerenciador;

    private PrintWriter saida;
    private String apelido = null;

    public ClienteHandler(Socket socket, GerenciadorLeilao gerenciador) {
        this.socket = socket;
        this.gerenciador = gerenciador;
    }

    @Override
    public void run() {
        try (
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.saida = saida;
            String mensagem;

            while ((mensagem = entrada.readLine()) != null) {
                String resposta = processarMensagem(mensagem.trim());
                if (resposta != null) {
                    saida.println(resposta);
                }
            }

        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + apelido);
        } finally {
            if (apelido != null) {
                gerenciador.removerCliente(this);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String processarMensagem(String mensagem) {
        String[] partes = mensagem.split(" ", 2);
        String comando = partes[0].toUpperCase();

        switch (comando) {
            case "LOGIN":
                return processarLogin(partes);

            case "LANCE":
                return processarLance(partes);

            case "STATUS":
                return gerenciador.processarStatus();

            case "SAIR":
                enviar("ADEUS " + apelido);
                return null;

            default:
                return "ERRO comando desconhecido";
        }
    }

    private String processarLogin(String[] partes) {
        if (partes.length < 2 || partes[1].isBlank()) {
            return "LOGIN_ERR apelido invalido";
        }
        if (apelido != null) {
            return "LOGIN_ERR ja autenticado";
        }
        String novoApelido = partes[1].trim();
        if (gerenciador.apelidoEmUso(novoApelido)) {
            return "LOGIN_ERR apelido em uso";
        }
        apelido = novoApelido;
        gerenciador.registrarCliente(this);
        return "LOGIN_OK " + apelido;
    }

    private String processarLance(String[] partes) {
        if (apelido == null) {
            return "LANCE_ERR nao autenticado";
        }
        if (partes.length < 2) {
            return "LANCE_ERR valor ausente";
        }
        try {
            double valor = Double.parseDouble(partes[1].trim());
            return gerenciador.processarLance(apelido, valor);
        } catch (NumberFormatException e) {
            return "LANCE_ERR valor invalido";
        }
    }

    public void enviar(String mensagem) {
        if (saida != null) {
            saida.println(mensagem);
        }
    }

    public String getApelido() {
        return apelido;
    }
}