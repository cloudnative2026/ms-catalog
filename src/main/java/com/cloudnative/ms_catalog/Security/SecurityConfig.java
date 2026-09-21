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
                //CSRF(Cross-Site Request Forgery) deshabilitado evita el adjuntado de sesion via Cookie y que este pueda ser expuesto
                .csrf(csrf -> csrf.disable())
                //Configuramos el CORS con detalle especifico
                .cors(cors -> cors.configurationSource(request -> CorsConfigurationSource()))
                //Evitamos la generacion de un session en el navegador , solo queremos el manejo de un token de validacion otorgado via IAM
                //Por esta razon generamos una plitica Stateless , la cual no maneja ni sesion ni estados en la sesion
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //Este apartado nos permite definir quien y que es necesario para acceder a cada punto del sistema/backend
                .authorizeHttpRequests(auth -> auth
                        //Consultas ANY desde apigateway tienen acceso de consulta de metodos y endpoints
                        .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        //Toda consulta a api publica esta permitida sin necesidad de login
                        .requestMatchers("/api/publico/**").permitAll()
                        //Toda consulta a api privada debe estar correctamente logeado via MSAL
                        .requestMatchers("/api/privado/**").authenticated()
                        //Toda consulta a api linea admin debe tener rol administrados en el IAM
                        .requestMatchers("/api/admin/**").hasRole("Admin")
                        //Para todas las demas deben estar logeados
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
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
        //Configuraciones base para CORS(Cross-Origin Resource Sharing)
        //Nos permite definir que configuraciones tendremos sobre el CORS de nuestras solicitudes
        //QUien la envia - Que metodos esperamos o tendremos
        //Que headers esperamos
        //Con esto estandarizamos y protegemos la informacion que debemos recibir y cual responderemos
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5500", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        return config;
    }
}
