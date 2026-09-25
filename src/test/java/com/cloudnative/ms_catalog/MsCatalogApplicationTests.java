package com.cloudnative.ms_catalog;

import com.cloudnative.ms_catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsCatalogApplicationTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    ProductRepository products;
    @MockitoBean
    JwtDecoder decoder;
    static final String URL = "/api/v1/catalog";
    static final String BODY = """
            {"name":"Coffee","description":"250 g","price":4990,"stock":20,"imageUrl":null,"active":true}
            """;

    @BeforeEach
    void setup() {
        products.deleteAll();
        for (String role : List.of("Admin", "Operador", "Cliente")) {
            when(decoder.decode(role)).thenReturn(Jwt.withTokenValue(role)
                    .header("alg", "RS256").subject("test-user")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                    .claim("roles", List.of(role)).build());
        }
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));
    }

    @Test
    void adminCanCompleteProductLifecycle() throws Exception {
        String location = mvc.perform(post(URL).header("Authorization", "Bearer Admin")
                .contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Coffee"))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(URL).header("Authorization", "Bearer Operador"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(location).header("Authorization", "Bearer Operador"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(20));
        mvc.perform(put(location).header("Authorization", "Bearer Admin")
                .contentType("application/json").content(BODY.replace("Coffee", "Tea")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Tea"));
        mvc.perform(patch(location + "/stock").header("Authorization", "Bearer Operador")
                .contentType("application/json").content("{\"stock\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
        mvc.perform(get(location).header("Authorization", "Bearer Admin"))
                .andExpect(jsonPath("$.stock").value(7));
        mvc.perform(delete(location).header("Authorization", "Bearer Admin"))
                .andExpect(status().isNoContent());
        mvc.perform(get(location).header("Authorization", "Bearer Admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissingInvalidAndUnauthorizedTokens() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header("Authorization", "Bearer Cliente")).andExpect(status().isOk());
        mvc.perform(post(URL).header("Authorization", "Bearer Cliente").contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(
                put(URL + "/1").header("Authorization", "Bearer Cliente").contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(delete(URL + "/1").header("Authorization", "Bearer Cliente")).andExpect(status().isForbidden());
        mvc.perform(post(URL).header("Authorization", "Bearer Operador")
                .contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(URL + "/1").header("Authorization", "Bearer Operador")
                .contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        mvc.perform(delete(URL + "/1").header("Authorization", "Bearer Operador"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(URL + "/1/stock").header("Authorization", "Bearer Cliente")
                .contentType("application/json").content("{\"stock\":7}")).andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidProductAndStockRequests() throws Exception {
        for (String body : List.of(BODY.replace("Coffee", " "), BODY.replace("4990", "-1"),
                BODY.replace("4990", "1.123"), BODY.replace("20", "-1"), "{}")) {
            mvc.perform(post(URL).header("Authorization", "Bearer Admin")
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        for (String body : List.of("{\"stock\":-1}", "{}", "{")) {
            mvc.perform(patch(URL + "/1/stock").header("Authorization", "Bearer Operador")
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingProductsReturn404() throws Exception {
        mvc.perform(put(URL + "/99999").header("Authorization", "Bearer Admin")
                .contentType("application/json").content(BODY)).andExpect(status().isNotFound());
        mvc.perform(delete(URL + "/99999").header("Authorization", "Bearer Admin"))
                .andExpect(status().isNotFound());
        mvc.perform(patch(URL + "/99999/stock").header("Authorization", "Bearer Operador")
                .contentType("application/json").content("{\"stock\":1}")).andExpect(status().isNotFound());
    }

    @Test
    void browserCanPreflightStockUpdateAndReadDocs() throws Exception {
        mvc.perform(options(URL + "/1/stock").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
