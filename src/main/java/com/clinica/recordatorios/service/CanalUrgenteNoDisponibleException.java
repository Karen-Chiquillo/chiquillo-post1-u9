package com.clinica.recordatorios.service;

import com.clinica.recordatorios.domain.CanalNotificacion;

public class CanalUrgenteNoDisponibleException extends RuntimeException {
    public CanalUrgenteNoDisponibleException(Long citaId, CanalNotificacion canalPreferido) {
        super("La cita " + citaId + " tiene como canal preferido " + canalPreferido
            + ": el recordatorio urgente solo se envía por SMS");
    }
}
