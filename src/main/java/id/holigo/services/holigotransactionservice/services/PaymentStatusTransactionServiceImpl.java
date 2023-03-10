package id.holigo.services.holigotransactionservice.services;

import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.holigo.services.common.model.PaymentStatusEnum;
import id.holigo.services.holigotransactionservice.domain.Transaction;
import id.holigo.services.holigotransactionservice.events.PaymentStatusEvent;
import id.holigo.services.holigotransactionservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PaymentStatusTransactionServiceImpl implements PaymentStatusTransactionService {

    public static final String PAYMENT_STATUS_HEADER = "transaction_id_payment";

    private final TransactionRepository transactionRepository;

    private final StateMachineFactory<PaymentStatusEnum, PaymentStatusEvent> stateMachineFactory;

    private final PaymentStatusTransactionInterceptor paymentStatusTransactionInterceptor;

    @Transactional
    @Override
    public StateMachine<PaymentStatusEnum, PaymentStatusEvent> transactionHasBeenPaid(UUID transactionId) {
        StateMachine<PaymentStatusEnum, PaymentStatusEvent> sm = build(transactionId);
        sendEvent(transactionId, sm, PaymentStatusEvent.PAYMENT_PAID);
        return sm;
    }

    @Transactional
    @Override
    public StateMachine<PaymentStatusEnum, PaymentStatusEvent> paymentHasExpired(UUID transactionId) {
        StateMachine<PaymentStatusEnum, PaymentStatusEvent> sm = build(transactionId);
        sendEvent(transactionId, sm, PaymentStatusEvent.PAYMENT_EXPIRED);
        return sm;
    }

    @Transactional
    @Override
    public StateMachine<PaymentStatusEnum, PaymentStatusEvent> paymentHasCanceled(UUID transactionId) {
        StateMachine<PaymentStatusEnum, PaymentStatusEvent> sm = build(transactionId);
        sendEvent(transactionId, sm, PaymentStatusEvent.PAYMENT_CANCEL);
        return sm;
    }

    private void sendEvent(UUID id, StateMachine<PaymentStatusEnum, PaymentStatusEvent> sm,
                           PaymentStatusEvent event) {
        Message<PaymentStatusEvent> message = MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_STATUS_HEADER, id).build();
        sm.sendEvent(message);
    }

    private StateMachine<PaymentStatusEnum, PaymentStatusEvent> build(UUID id) {
        Transaction transaction = transactionRepository.getById(id);

        StateMachine<PaymentStatusEnum, PaymentStatusEvent> sm = stateMachineFactory
                .getStateMachine(transaction.getId().toString());

        sm.stop();
        sm.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(paymentStatusTransactionInterceptor);
            sma.resetStateMachine(new DefaultStateMachineContext<>(
                    transaction.getPaymentStatus(), null, null, null));
        });
        sm.start();
        return sm;
    }

}
