package com.exito.stockai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompraRequest(
        @NotEmpty(message = "El carrito está vacío")
        List<@Valid CompraItemRequest> items
) {
}