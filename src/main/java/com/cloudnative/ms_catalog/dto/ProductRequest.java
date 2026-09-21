package com.cloudnative.ms_catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 4000) String description,
    @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal price,
    @NotNull @PositiveOrZero Integer stock,
    @Size(max = 2048) String imageUrl,
    @NotNull Boolean active
) {}
