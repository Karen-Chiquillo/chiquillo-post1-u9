package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class ReminderChannelFactoryImplTest {

    @Mock
    private NotificadorSmsGateway gatewayMock;

    private EmailReminderChannel emailReminderChannel;
    private SmsReminderChannel smsReminderChannel;
    private ReminderChannelFactoryImpl reminderChannelFactory;

    @BeforeEach
    void setUp() {
        emailReminderChannel = new EmailReminderChannel();
        smsReminderChannel = new SmsReminderChannel(gatewayMock);
        reminderChannelFactory = new ReminderChannelFactoryImpl(emailReminderChannel, smsReminderChannel);
    }

    @Test
    void obtenerCanal_email_retornaEmailReminderChannel() {
        ReminderChannel canal = reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL);

        assertSame(emailReminderChannel, canal);
    }

    @Test
    void obtenerCanal_sms_retornaSmsReminderChannel() {
        ReminderChannel canal = reminderChannelFactory.obtenerCanal(CanalNotificacion.SMS);

        assertSame(smsReminderChannel, canal);
    }
}
