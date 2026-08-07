import { useState, useRef, useEffect } from "react";
import { enviarMensagemAssistente } from "../../api/assistenteApi";
import "./AssistenteChat.css";

const AssistenteChat = () => {
  const [aberto, setAberto] = useState(false);
  const [mensagens, setMensagens] = useState([
    { autor: "ia", texto: "Oi! Posso te ajudar a ver o cardápio, montar um pedido ou acompanhar uma entrega. 🍰" },
  ]);
  const [input, setInput] = useState("");
  const [carregando, setCarregando] = useState(false);
  const mensagensRef = useRef(null);

  // Rola só o container de mensagens (scrollTop), nunca a página por trás —
  // scrollIntoView rola o ancestral scrollável mais próximo, que em alguns
  // layouts acaba sendo a página inteira em vez da caixinha do chat.
  useEffect(() => {
    const el = mensagensRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [mensagens, aberto]);

  const enviar = async () => {
    const texto = input.trim();
    if (!texto || carregando) return;

    setMensagens((m) => [...m, { autor: "usuario", texto }]);
    setInput("");
    setCarregando(true);

    try {
      const { resposta } = await enviarMensagemAssistente(texto);
      setMensagens((m) => [...m, { autor: "ia", texto: resposta }]);
    } catch (err) {
      setMensagens((m) => [...m, { autor: "ia", texto: err.mensagem ?? "Não consegui responder agora, tenta de novo." }]);
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="assistente-chat-container">
      {aberto && (
        <div className="assistente-chat-panel">
          <div className="assistente-chat-header">
            <span>Assistente Sweet Delights</span>
            <button onClick={() => setAberto(false)}>×</button>
          </div>
          <div className="assistente-chat-mensagens" ref={mensagensRef}>
            {mensagens.map((m, i) => (
              <div key={i} className={`assistente-chat-bolha ${m.autor}`}>{m.texto}</div>
            ))}
            {carregando && <div className="assistente-chat-bolha ia">digitando...</div>}
          </div>
          <div className="assistente-chat-input">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && enviar()}
              placeholder="Pergunte algo..."
            />
            <button onClick={enviar} disabled={carregando}>Enviar</button>
          </div>
        </div>
      )}
      <button className="assistente-chat-fab" onClick={() => setAberto((a) => !a)}>
        {aberto ? "×" : "💬"}
      </button>
    </div>
  );
};

export default AssistenteChat;
