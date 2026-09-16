package com.exito.stockai.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String usuarioEmail,
        String usuarioNombre,
        String rol,
        Long productoId,
        String productoNombre,
        String operacion,
        Integer stockAnterior,
        Integer cantidad,
        Integer stockPosterior,
        LocalDate fecha,
        LocalTime hora,
        OffsetDateTime fechaHora,
        String observacion
) {
}