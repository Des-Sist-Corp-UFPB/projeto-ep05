package br.ufpb.dsc.mercado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadeia de filtros 1: API REST (/api/**)
     * Autenticação via JWT, sem estado (stateless).
     */
    @Bean
    @Order(1)
    @SuppressWarnings("SPRING_CSRF_PROTECTION_DISABLED")
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cadastro, Login e catálogo de produtos são públicos
                        .requestMatchers("/api/auth/login", "/api/auth/cadastro").permitAll()
                        .requestMatchers("/api/produtos/**").permitAll()
                        // Quaisquer outros endpoints da API exigem login (papel de CLIENTE ou outros)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cadeia de filtros 2: Páginas MVC / Thymeleaf (admins e sysadmins)
     * Autenticação baseada em sessão (cookie).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos e login público
                        .requestMatchers("/webjars/**", "/css/**", "/js/**","/assets/**", "/actuator/health", "/admin/login", "/admin/cadastro", "/","/ping").permitAll()
                        // SysAdmin
                        .requestMatchers("/sysadmin/**").hasRole("SYSADMIN")
                        // Admin
                        .requestMatchers("/admin/**", "/produtos/**").hasAnyRole("ADMIN", "SYSADMIN")
                        // Qualquer outra página
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        // Redireciona dinamicamente pós-login baseado na role do usuário
                        .successHandler((request, response, authentication) -> {
                            var authorities = authentication.getAuthorities();
                            boolean isSysAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSADMIN"));
                            String contextPath = request.getContextPath();
                            if (isSysAdmin) {
                                response.sendRedirect(contextPath + "/sysadmin/dashboard");
                            } else {
                                response.sendRedirect(contextPath + "/admin/dashboard");
                            }
                        })
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect(request.getContextPath() + "/admin/login");
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        // Desabilita apenas para requisições de produtos HTMX para simplificação educacional
                        // Em produção, deve-se passar o header X-CSRF-Token no HTMX
                        .ignoringRequestMatchers("/produtos/**", "/admin/**", "/sysadmin/**", "/admin/cadastro")
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir o frontend React local (Vite padrão é 5173 ou 3000)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost",           // nginx porta 80 (sem porta explícita)
                "http://localhost:80",        // nginx porta 80 (explícita)
                "http://localhost:5173",      // Vite dev server padrão
                "http://localhost:3000",      // alternativo dev
                "https://eq05.dsc.rodrigor.com" // produção
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
