package br.ufpb.dsc.mercado.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.domain.Cupom;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.ProdutoImagem;
import br.ufpb.dsc.mercado.domain.TipoCupom;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
import br.ufpb.dsc.mercado.repository.CupomRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final CupomRepository cupomRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ Lido do .env — fallback "admin123" só é usado se a variável não estiver definida (dev local sem .env)
    @Value("${app.seed.admin-password:admin123}")
    private String senhaDefault;

    public DatabaseSeeder(UsuarioRepository usuarioRepository,
                          CategoriaRepository categoriaRepository,
                          ProdutoRepository produtoRepository,
                          CupomRepository cupomRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.cupomRepository = cupomRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Semear Usuários se a tabela estiver vazia
        if (usuarioRepository.count() == 0) {
            // ✅ Senha vem da variável de ambiente APP_SEED_ADMIN_PASSWORD
            String senhaPadrao = passwordEncoder.encode(senhaDefault);

            Usuario sysadmin = new Usuario("SysAdmin Sweet Delights", "sysadmin@mercado.com", senhaPadrao, Papel.SYSADMIN);
            usuarioRepository.save(sysadmin);

            Usuario admin = new Usuario("Admin Sweet Delights", "admin@mercado.com", senhaPadrao, Papel.ADMIN);
            usuarioRepository.save(admin);

            Usuario cliente = new Usuario("Cliente Teste", "cliente@mercado.com", senhaPadrao, Papel.CLIENTE);
            usuarioRepository.save(cliente);

            System.out.println("=== BANCO DE DADOS SEMEADO COM USUÁRIOS PADRÃO ===");
        }

        // 2. Semear Categorias e Produtos
        if (categoriaRepository.count() == 0) {
            Categoria bolos = new Categoria("Bolos", "Bolos artesanais para todas as ocasiões");
            bolos = categoriaRepository.save(bolos);

            Categoria doces = new Categoria("Doces Finos", "Brigadeiros, trufas e docinhos");
            doces = categoriaRepository.save(doces);

            Categoria salgados = new Categoria("Salgados", "Salgados para festas e eventos");
            salgados = categoriaRepository.save(salgados);

            if (produtoRepository.count() == 0) {
                Produto p1 = new Produto("Bolo de Chocolate com Ninho", "Bolo de chocolate recheado com creme de leite Ninho", BigDecimal.valueOf(89.90), bolos, 15);
                p1.addImagem(new ProdutoImagem(p1, "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500"));
                produtoRepository.save(p1);

                Produto p2 = new Produto("Red Velvet", "Bolo red velvet com cobertura de cream cheese", BigDecimal.valueOf(99.90), bolos, 8);
                p2.addImagem(new ProdutoImagem(p2, "https://images.unsplash.com/photo-1586985289906-406988974504?w=500"));
                produtoRepository.save(p2);

                Produto p3 = new Produto("Brigadeiro Gourmet (cx 12un)", "Caixa com 12 brigadeiros gourmet sortidos", BigDecimal.valueOf(39.90), doces, 30);
                p3.addImagem(new ProdutoImagem(p3, "https://images.unsplash.com/photo-1548907040-4baa419a2405?w=500"));
                produtoRepository.save(p3);

                Produto p4 = new Produto("Trufas de Chocolate Belga", "Caixa com 6 trufas artesanais de chocolate belga", BigDecimal.valueOf(45.00), doces, 40);
                p4.addImagem(new ProdutoImagem(p4, "https://images.unsplash.com/photo-1548907040-4baa419a2405?w=500"));
                produtoRepository.save(p4);

                Produto p5 = new Produto("Coxinha de Frango (cx 20un)", "Caixa com 20 coxinhas de frango congeladas", BigDecimal.valueOf(59.90), salgados, 20);
                p5.addImagem(new ProdutoImagem(p5, "https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?w=500"));
                produtoRepository.save(p5);

                System.out.println("=== BANCO DE DADOS SEMEADO COM PRODUTOS E CATEGORIAS ===");
            }
        }

        // 3. Semear Cupons
        if (cupomRepository.count() == 0) {
            Cupom c1 = new Cupom("MERCADO10", BigDecimal.valueOf(10.00), TipoCupom.PORCENTAGEM, Instant.now().plus(30, ChronoUnit.DAYS));
            cupomRepository.save(c1);

            Cupom c2 = new Cupom("DESCONTO50", BigDecimal.valueOf(50.00), TipoCupom.VALOR_FIXO, Instant.now().plus(30, ChronoUnit.DAYS));
            cupomRepository.save(c2);

            System.out.println("=== BANCO DE DADOS SEMEADO COM CUPONS ===");
        }
    }
}