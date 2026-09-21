package com.cloudnative.ms_catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// An absolute stock count, not a quantity to subtract.
public record StockUpdateRequest(@NotNull @PositiveOrZero Integer stock) {}
