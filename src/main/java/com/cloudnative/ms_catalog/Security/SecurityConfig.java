package com.cloudnative.ms_catalog.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site Request Forgery) deshabilitado evita el adjuntado de sesion
                // via Cookie y que este pueda ser expuesto
                .csrf(csrf -> csrf.disable())
                // Configuramos el CORS con detalle especifico
                .cors(cors -> cors.configurationSource(request -> CorsConfigurationSource()))
                // Evitamos la generacion de un session en el navegador , solo queremos el
                // manejo de un token de validacion otorgado via IAM
                // Por esta razon generamos una plitica Stateless , la cual no maneja ni sesion
                // ni estados en la sesion
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Este apartado nos permite definir quien y que es necesario para acceder a
                // cada punto del sistema/backend
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()

                        // Public endpoints
                        .requestMatchers("/api/publico/**").permitAll()

                        // Catalog - lectura
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/catalog/products/**")
                        .hasAnyRole("Admin", "Operador", "Cliente")

                        // Catalog - crear productos
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/catalog/products")
                        .hasRole("Admin")

                        // Catalog - modificar productos
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/catalog/products/**")
                        .hasRole("Admin")

                        // Catalog - modificar stock
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/catalog/products/*/stock")
                        .hasAnyRole("Admin", "Operador")

                        // Catalog - eliminar
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/catalog/products/**")
                        .hasRole("Admin")

                        // Existing private endpoints
                        .requestMatchers("/api/privado/**").authenticated()

                        // Existing admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("Admin")

                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        String jwkSetUri = "https://login.microsoftonline.com/f9bce5c0-eb96-4341-aad7-411ae980b12a/discovery/v2.0/keys";
        org.springframework.security.oauth2.jwt.NimbusJwtDecoder jwtDecoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri).build();

        List<String> validIssuers = List.of(
                "https://login.microsoftonline.com/f9bce5c0-eb96-4341-aad7-411ae980b12a/v2.0",
                "https://sts.windows.net/f9bce5c0-eb96-4341-aad7-411ae980b12a/");

        org.springframework.security.oauth2.core.OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> validator = new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                new org.springframework.security.oauth2.jwt.JwtTimestampValidator(),
                token -> {
                    String issuer = token.getIssuer() != null ? token.getIssuer().toString() : "";
                    if (validIssuers.contains(issuer) || issuer.contains("f9bce5c0-eb96-4341-aad7-411ae980b12a")) {
                        return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                    }
                    return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                            new org.springframework.security.oauth2.core.OAuth2Error(
                                    "invalid_token",
                                    "The iss claim is not valid: " + issuer,
                                    null));
                });

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private CorsConfiguration CorsConfigurationSource() {
        // Configuraciones base para CORS(Cross-Origin Resource Sharing)
        // Nos permite definir que configuraciones tendremos sobre el CORS de nuestras
        // solicitudes
        // QUien la envia - Que metodos esperamos o tendremos
        // Que headers esperamos
        // Con esto estandarizamos y protegemos la informacion que debemos recibir y
        // cual responderemos
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(
                List.of("http://localhost:5500", "http://localhost:5173", "https://cloudnative2026.github.io"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        return config;
    }
}
