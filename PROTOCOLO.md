# Protocolo LAP — Leilão Application Protocol

## Visão Geral

Protocolo de texto proprietário para comunicação entre cliente e servidor em um sistema de leilão online. Utiliza TCP como transporte, garantindo entrega e ordem das mensagens.

- **Transporte:** TCP
- **Porta padrão:** 12345
- **Codificação:** UTF-8
- **Formato:** texto simples, uma mensagem por linha (`\n`)

---

## Formato das Mensagens

```
COMANDO [argumento1] [argumento2] ...
```

Campos separados por espaço. Toda mensagem termina com quebra de linha (`\n`).

---

## Mensagens do Cliente → Servidor

| Mensagem | Argumentos | Descrição |
|---|---|---|
| `LOGIN <apelido>` | apelido: string sem espaços | Autentica o cliente com um apelido único |
| `LANCE <valor>` | valor: número decimal | Envia um lance para o item em leilão |
| `STATUS` | — | Solicita o estado atual do leilão |
| `SAIR` | — | Encerra a conexão com o servidor |

---

## Mensagens do Servidor → Cliente

| Mensagem | Argumentos | Descrição |
|---|---|---|
| `LOGIN_OK <apelido>` | apelido confirmado | Login realizado com sucesso |
| `LOGIN_ERR <motivo>` | motivo: string | Falha no login |
| `ITEM <nome> <valor> <tempo>` | nome: string, valor: decimal, tempo: inteiro (segundos) | Informa o item em leilão, lance atual e tempo restante |
| `LANCE_OK <valor> <apelido>` | valor: decimal, apelido: string | Lance aceito; notifica todos os clientes |
| `LANCE_ERR <motivo>` | motivo: string | Lance rejeitado |
| `ENCERRADO <vencedor> <valor>` | vencedor: apelido ou `nenhum_lance`, valor: decimal | Leilão do item encerrado |
| `AGUARDE <segundos>` | segundos: inteiro | Servidor aguardando; 0 = sem previsão, >0 = próximo item em N segundos |
| `ADEUS <apelido>` | apelido: string | Confirmação de desconexão |

---

## Motivos de Erro

| Mensagem | Motivo |
|---|---|
| `LOGIN_ERR apelido invalido` | Apelido vazio ou em branco |
| `LOGIN_ERR ja autenticado` | Cliente já realizou login |
| `LOGIN_ERR apelido em uso` | Apelido já utilizado por outro cliente |
| `LANCE_ERR nao autenticado` | Cliente não realizou login |
| `LANCE_ERR valor ausente` | Nenhum valor informado |
| `LANCE_ERR valor invalido` | Valor não é um número válido |
| `LANCE_ERR leilao encerrado` | Nenhum leilão ativo no momento |
| `LANCE_ERR valor abaixo do mínimo permitido <valor>` | Lance abaixo do valor mínimo do item |
| `LANCE_ERR valor abaixo do atual <valor>` | Lance menor ou igual ao maior lance atual |

---

## Fluxo de uma Sessão

```
Cliente                          Servidor
  |                                  |
  |---------- LOGIN carlos --------->|
  |<--------- LOGIN_OK carlos -------|
  |                                  |
  |       (servidor digita INICIAR)  |
  |<--- ITEM Notebook 1500.00 30 ----|  (broadcast para todos)
  |                                  |
  |---------- LANCE 1600.00 -------->|
  |<--- LANCE_OK 1600.00 carlos -----|  (broadcast para todos)
  |                                  |
  |   (tempo esgota)                 |
  |<--- ENCERRADO carlos 1600.00 ----|  (broadcast para todos)
  |<--- AGUARDE 3 -------------------|  (broadcast para todos)
  |<--- ITEM Fone 300.00 5 ----------|  (próximo item)
  |                                  |
  |---------- SAIR ----------------->|
  |<--------- ADEUS carlos ----------|
  |                                  |
```

---

## Controle do Servidor (terminal)

| Comando | Descrição |
|---|---|
| `INICIAR` | Inicia o próximo item da fila de leilão |

---

## Estados do Leilão

```
[AGUARDANDO] --INICIAR--> [EM ANDAMENTO] --tempo esgota--> [ENCERRADO]
                                                                  |
                                          há mais itens? --sim--> [EM ANDAMENTO]
                                                        --não--> [AGUARDANDO]
```
