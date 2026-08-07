import { apiFetch } from "./api";

export function enviarMensagemAssistente(mensagem) {
  return apiFetch("/assistente/chat", {
    method: "POST",
    body: JSON.stringify({ mensagem }),
  });
}