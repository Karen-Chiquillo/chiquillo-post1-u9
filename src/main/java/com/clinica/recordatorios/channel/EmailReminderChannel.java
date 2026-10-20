package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.domain.Cita;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailReminderChannel implements ReminderChannel {

    @Override
    public void enviar(Cita cita, String mensaje) {
        // Simula el cliente SMTP: las pruebas verifican la selección del canal, no el transporte.
        log.info("[EMAIL a {}] {}", cita.getPacienteContacto(), mensaje);
    }
}
