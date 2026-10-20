package com.clinica.recordatorios.service;

import java.time.LocalDateTime;

public class CitaFueraDeVentanaUrgenteException extends RuntimeException {
    public CitaFueraDeVentanaUrgenteException(Long citaId, LocalDateTime fechaHoraCita) {
        super("La cita " + citaId + " del " + fechaHoraCita
            + " no está dentro de las próximas 2 horas: no aplica recordatorio urgente");
    }
}
