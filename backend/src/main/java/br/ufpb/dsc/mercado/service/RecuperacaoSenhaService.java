package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.RecuperacaoSenha;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.repository.RecuperacaoSenhaRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecuperacaoSenhaService {

    private final RecuperacaoSenhaRepository recuperacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RecuperacaoSenhaService(RecuperacaoSenhaRepository recuperacaoRepository,
                                   UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder) {
        this.recuperacaoRepository = recuperacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String solicitarRecuperacao(String email) {
        // Resposta genérica mesmo se e-mail não existir — evita enumeração de usuários
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) return null;

        // Invalida tokens anteriores
        recuperacaoRepository.invalidarTokensDoUsuario(usuario.getId());

        // Gera novo token — expira em 30 minutos
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiraEm = Instant.now().plus(30, ChronoUnit.MINUTES);

        recuperacaoRepository.save(new RecuperacaoSenha(usuario, token, expiraEm));

        // Em produção, aqui enviaria o e-mail com o token
        // Por enquanto retorna o token para exibir em dev
        return token;
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        RecuperacaoSenha recuperacao = recuperacaoRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));

        if (!recuperacao.isValido()) {
            throw new IllegalArgumentException("Token inválido ou expirado");
        }

        Usuario usuario = recuperacao.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        recuperacao.setUsado(true);
        recuperacaoRepository.save(recuperacao);
    }
}