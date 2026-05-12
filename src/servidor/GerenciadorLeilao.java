package servidor;
 
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
 
public class GerenciadorLeilao {
 
    public static class Item {
        public final String nome;
        public final double valorMinimo;
        public final int tempoSegundos;
 
        public Item(String nome, double valorMinimo, int tempoSegundos) {
            this.nome = nome;
            this.valorMinimo = valorMinimo;
            this.tempoSegundos = tempoSegundos;
        }
    }
 
    private final Queue<Item> filaItens = new LinkedList<>();
    private final List<ClienteHandler> clientes = new CopyOnWriteArrayList<>();
 
    private Item itemAtual = null;
    private double maiorLance = 0;
    private String vencedor = null;
    private int tempoRestante = 0;
    private boolean leilaoAtivo = false;
 
    private Thread threadTimer = null;
 
    public GerenciadorLeilao() {
        filaItens.add(new Item("Enérgetico Monster", 15.00, 10));
        filaItens.add(new Item("Imagination LV", 2000.00, 10));
        filaItens.add(new Item("Teclado mecanico", 250.00, 10));
    }
 
    public synchronized void registrarCliente(ClienteHandler cliente) {
        clientes.add(cliente);
        if (leilaoAtivo) {
            cliente.enviar(String.format("ITEM %s %.2f %d",
                    itemAtual.nome, maiorLance > 0 ? maiorLance : itemAtual.valorMinimo, tempoRestante));
        } else {
            cliente.enviar("AGUARDE 0");
        }
    }
 
    public synchronized void removerCliente(ClienteHandler cliente) {
        clientes.remove(cliente);
    }

    public synchronized boolean apelidoEmUso(String apelido) {
        for (ClienteHandler c : clientes) {
            if (apelido.equals(c.getApelido())) {
                return true;
            }
        }
        return false;
    }
 
    public synchronized String processarLance(String apelido, double valor) {
        if (!leilaoAtivo) {
            return "LANCE_ERR leilao encerrado";
        }
        if (valor < itemAtual.valorMinimo) {
            return String.format("LANCE_ERR valor abaixo do mínimo permitido %.2f", itemAtual.valorMinimo);
        }
        if (valor <= maiorLance) {
            return String.format("LANCE_ERR valor abaixo do atual %.2f", maiorLance);
        }
        maiorLance = valor;
        vencedor = apelido;
        broadcast(String.format("LANCE_OK %.2f %s", maiorLance, vencedor));
        return null;
    }
 
    public synchronized String processarStatus() {
        if (!leilaoAtivo || itemAtual == null) {
            return "AGUARDE 0";
        }
        return String.format("ITEM %s %.2f %d", itemAtual.nome,
                maiorLance > 0 ? maiorLance : itemAtual.valorMinimo, tempoRestante);
    }
 
    public synchronized void iniciarProximoLeilao() {
        if (filaItens.isEmpty()) {
            broadcast("AGUARDE 0");
            return;
        }
 
        itemAtual = filaItens.peek();
        maiorLance = 0;
        vencedor = null;
        tempoRestante = itemAtual.tempoSegundos;
        leilaoAtivo = true;
 
        broadcast(String.format("ITEM %s %.2f %d",
                itemAtual.nome, itemAtual.valorMinimo, itemAtual.tempoSegundos));
 
        threadTimer = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000);
                    synchronized (this) {
                        tempoRestante--;
                        if (tempoRestante <= 0) {
                            encerrarLeilao();
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        threadTimer.setDaemon(true);
        threadTimer.start();
    }
 
    private void encerrarLeilao() {
        leilaoAtivo = false;
        if (vencedor != null) {
            filaItens.poll();
            broadcast(String.format("ENCERRADO %s %.2f", vencedor, maiorLance));
        } else {
            filaItens.add(filaItens.poll());
            broadcast("ENCERRADO nenhum_lance 0.00");
        }
 
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
 
        if (!filaItens.isEmpty()) {
            broadcast("AGUARDE O PRÒXIMO ITEM");
            iniciarProximoLeilao();
        } else {
            broadcast("SEM ITENS RESTANTES");
        }
    }
 
    private void broadcast(String mensagem) {
        for (ClienteHandler c : clientes) {
            c.enviar(mensagem);
        }
    }
}