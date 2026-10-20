package com.clinica.recordatorios.channel.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TwilioNotificadorSmsGateway implements NotificadorSmsGateway {

    @Override
    public void enviarSms(String numeroDestino, String texto) {
        // Simula la llamada HTTP al proveedor de SMS para no depender de credenciales ni de red.
        log.info("[SMS a {}] {}", numeroDestino, texto);
    }
}
