package br.ufpb.dsc.mercado.config;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
import br.ufpb.dsc.mercado.repository.CupomRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
            Categoria eletronicos = new Categoria("Eletrônicos", "Dispositivos e aparelhos eletrônicos");
            eletronicos = categoriaRepository.save(eletronicos);

            Categoria livros = new Categoria("Livros", "Livros físicos e digitais");
            livros = categoriaRepository.save(livros);

            Categoria vestuario = new Categoria("Vestuário", "Roupas, calçados e acessórios");
            vestuario = categoriaRepository.save(vestuario);

            if (produtoRepository.count() == 0) {
                Produto p1 = new Produto("Smartphone Galaxy S24", "Smartphone Samsung Galaxy S24 Ultra 512GB", BigDecimal.valueOf(5499.00), eletronicos, 15);
                p1.addImagem(new ProdutoImagem(p1, "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500"));
                produtoRepository.save(p1);

                Produto p2 = new Produto("Notebook Dell Inspiron", "Notebook Dell Inspiron Intel Core i5 16GB SSD 512GB", BigDecimal.valueOf(3899.00), eletronicos, 8);
                p2.addImagem(new ProdutoImagem(p2, "https://images.unsplash.com/photo-1496181130204-7552cc145cdb?w=500"));
                produtoRepository.save(p2);

                Produto p3 = new Produto("Livro Design Patterns", "Livro Padrões de Projetos de Software do GoF", BigDecimal.valueOf(189.90), livros, 30);
                p3.addImagem(new ProdutoImagem(p3, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500"));
                produtoRepository.save(p3);

                Produto p4 = new Produto("Livro Clean Code", "Livro Código Limpo por Robert C. Martin", BigDecimal.valueOf(95.00), livros, 40);
                p4.addImagem(new ProdutoImagem(p4, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=500"));
                produtoRepository.save(p4);

                Produto p5 = new Produto("Tênis Esportivo Run", "Tênis esportivo para corrida de alto impacto", BigDecimal.valueOf(299.90), vestuario, 20);
                p5.addImagem(new ProdutoImagem(p5, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500"));
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