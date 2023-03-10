package id.holigo.services.holigotransactionservice.listeners;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import id.holigo.services.common.model.*;
import id.holigo.services.holigotransactionservice.domain.QueueUpdateUserReferralPoint;
import id.holigo.services.holigotransactionservice.events.PaymentStatusEvent;
import id.holigo.services.holigotransactionservice.repositories.QueueUpdateUserReferralPointRepository;
import id.holigo.services.holigotransactionservice.services.airlines.AirlinesService;
import id.holigo.services.holigotransactionservice.services.deposit.DepositService;
import id.holigo.services.holigotransactionservice.services.holiclub.HoliclubService;
import id.holigo.services.holigotransactionservice.services.point.PointService;
import id.holigo.services.holigotransactionservice.services.train.TrainService;
import id.holigo.services.holigotransactionservice.services.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.JmsException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.holigo.services.common.events.TransactionEvent;
import id.holigo.services.holigotransactionservice.config.JmsConfig;
import id.holigo.services.holigotransactionservice.domain.Transaction;
import id.holigo.services.holigotransactionservice.repositories.TransactionRepository;
import id.holigo.services.holigotransactionservice.services.OrderStatusTransactionService;
import id.holigo.services.holigotransactionservice.services.PaymentStatusTransactionService;
import id.holigo.services.holigotransactionservice.services.TransactionService;
import id.holigo.services.holigotransactionservice.services.payment.PaymentService;
import id.holigo.services.holigotransactionservice.web.mappers.TransactionMapper;

@Slf4j
@Component
public class TransactionListener {

    private DepositService depositService;

    private UserService userService;

    private AirlinesService airlinesService;

    private TrainService trainService;

    private QueueUpdateUserReferralPointRepository queueUpdateUserReferralPointRepository;

    @Autowired
    public void setQueueUpdateUserReferralPointRepository(QueueUpdateUserReferralPointRepository queueUpdateUserReferralPointRepository) {
        this.queueUpdateUserReferralPointRepository = queueUpdateUserReferralPointRepository;
    }

    @Autowired
    public void setTrainService(TrainService trainService) {
        this.trainService = trainService;
    }

    @Autowired
    public void setAirlinesService(AirlinesService airlinesService) {
        this.airlinesService = airlinesService;
    }

    @Autowired
    public void setDepositService(DepositService depositService) {
        this.depositService = depositService;
    }

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    private TransactionService transactionService;

    @Autowired
    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private TransactionRepository transactionRepository;

    @Autowired
    public void setTransactionRepository(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private TransactionMapper transactionMapper;

    @Autowired
    public void setTransactionMapper(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    private OrderStatusTransactionService orderStatusTransactionService;

    @Autowired
    public void setOrderStatusTransactionService(OrderStatusTransactionService orderStatusTransactionService) {
        this.orderStatusTransactionService = orderStatusTransactionService;
    }

    private PaymentStatusTransactionService paymentStatusTransactionService;

    @Autowired
    public void setPaymentStatusTransactionService(PaymentStatusTransactionService paymentStatusTransactionService) {
        this.paymentStatusTransactionService = paymentStatusTransactionService;
    }

    private PaymentService paymentService;

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private HoliclubService holiclubService;

    @Autowired
    public void setHoliclubService(HoliclubService holiclubService) {
        this.holiclubService = holiclubService;
    }

    private PointService pointService;

    @Autowired
    public void setPointService(PointService pointService) {
        this.pointService = pointService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Transactional
    @JmsListener(destination = JmsConfig.CREATE_NEW_TRANSACTION)
    public void listenForCreateNewTransaction(@Payload TransactionDto transactionDto,
                                              Message message) throws JmsException, JMSException {

        TransactionDto transaction = transactionService.createNewTransaction(transactionDto);

        jmsTemplate.convertAndSend(message.getJMSReplyTo(), transaction);
    }

    @JmsListener(destination = JmsConfig.GET_TRANSACTION_BY_ID)
    public void listenForGetTransaction(@Payload TransactionDto transactionDto,
                                        Message message) throws JmsException, JMSException {
        TransactionDto transaction = transactionService.getTransactionById(transactionDto.getId());
        if (transaction != null) {
            transactionDto = transaction;
        }
        jmsTemplate.convertAndSend(message.getJMSReplyTo(), transactionDto);
    }

    @Transactional
    @JmsListener(destination = JmsConfig.SET_ORDER_STATUS_BY_TRANSACTION_ID_TYPE)
    public void listenForSetOrderStatusTransaction(TransactionEvent transactionEvent) {
        TransactionDto transactionDto = transactionEvent.getTransactionDto();

        Optional<Transaction> fetchTransaction = transactionRepository.findById(transactionDto.getId());
        if (fetchTransaction.isPresent()) {
            Transaction transaction = fetchTransaction.get();
            TransactionDto transactionDtoForPayment = transactionMapper
                    .transactionToTransactionDto(transaction);
            transactionDtoForPayment.setOrderStatus(transactionDto.getOrderStatus());
            switch (transactionDto.getOrderStatus()) {
                case ISSUED:
                    orderStatusTransactionService.issuedSuccess(transaction.getId());
                    paymentService.transactionIssued(transactionDtoForPayment);
                    if (!transaction.getTransactionType().equals("HTD")) {
                        IncrementUserClubDto incrementUserClubDto = IncrementUserClubDto.builder()
                                .invoiceNumber(transaction.getInvoiceNumber()).userId(transaction.getUserId())
                                .fareAmount(transaction.getFareAmount()).build();
                        holiclubService.incrementUserClub(incrementUserClubDto);
                    }
                    if (transaction.getHpAmount().compareTo(BigDecimal.ZERO) > 0 && !transaction.getIsPointSent()) {
                        PointDto pointDto = PointDto.builder().creditAmount(transaction.getHpAmount().intValue())
                                .transactionId(transaction.getId())
                                .paymentId(transaction.getPaymentId())
                                .informationIndex("pointStatement.cashBackHoliPoint")
                                .transactionType(transaction.getTransactionType())
                                .invoiceNumber(transaction.getInvoiceNumber())
                                .userId(transaction.getUserId())
                                .build();
                        try {
                            PointDto resultPointDto = pointService.credit(pointDto);
                            if (resultPointDto.getIsValid()) {
                                transaction.setIsPointSent(resultPointDto.getIsValid());
                                transactionRepository.save(transaction);
                            }
                        } catch (JMSException | JsonProcessingException e) {
                            log.error("Error : {}", e.getMessage());
                        }
                    }
                    if (transaction.getPrAmount().compareTo(BigDecimal.ZERO) > 0 && transaction.getUserParentId() != null) {
                        String[] invoiceSplit = transaction.getInvoiceNumber().split("/");
                        String preInvoice = invoiceSplit[0] + "**********";
                        String postInvoiceNumber = (invoiceSplit[2].length() > 2) ? invoiceSplit[2].replace(invoiceSplit[2].substring(0, 2), "*") : invoiceSplit[2];
                        PointDto pointDto = PointDto.builder().creditAmount(transaction.getPrAmount().intValue())
                                .transactionId(transaction.getId())
                                .paymentId(transaction.getPaymentId())
                                .informationIndex("pointStatement.pointReferralReward")
                                .transactionType(transaction.getTransactionType())
                                .userId(transaction.getUserParentId())
                                .invoiceNumber(preInvoice + postInvoiceNumber)
                                .build();
                        try {
                            PointDto resultPointDto = pointService.credit(pointDto);
                            if (resultPointDto.getIsValid()) {
                                transaction.setIsPrSent(resultPointDto.getIsValid());
                                transactionRepository.save(transaction);
                                try {
                                    ResponseEntity<HttpStatus> response = userService.updatePointReferral(transaction.getUserParentId(), transaction.getPrAmount().intValue());
                                } catch (FeignException e) {
                                    queueUpdateUserReferralPointRepository.save(QueueUpdateUserReferralPoint.builder()
                                            .userId(transaction.getUserParentId()).hasSent(false).point(transaction.getPrAmount().intValue()).build());
                                }
                            }
                        } catch (JMSException | JsonProcessingException e) {
                            log.error("Error : {}", e.getMessage());
                        }
                    }

                    break;
                case ISSUED_FAILED:
                    orderStatusTransactionService.issuedFail(transaction.getId());
                    paymentService.transactionIssued(transactionDtoForPayment);
                    break;
                case RETRYING_ISSUED:
                    orderStatusTransactionService.retryingIssued(transaction.getId());
                    paymentService.transactionIssued(transactionDtoForPayment);
                    break;
                case WAITING_ISSUED:
                    orderStatusTransactionService.waitingIssued(transaction.getId());
                    paymentService.transactionIssued(transactionDtoForPayment);
                    break;
                case PROCESS_BOOK:
                case BOOKED:
                case BOOK_FAILED:
                case PROCESS_ISSUED:
                case ORDER_EXPIRED:
                case ORDER_CANCELED:
                    break;

            }
        }
    }

    @Transactional
    @JmsListener(destination = JmsConfig.ISSUED_TRANSACTION_BY_ID)
    public void listenForIssuedFromPayment(TransactionEvent transactionEvent) {
        TransactionDto transactionDto = transactionEvent.getTransactionDto();
        Optional<Transaction> fetchTransaction = transactionRepository.findByIdAndPaymentStatus(transactionDto.getId(),
                transactionDto.getPaymentStatus());
        if (fetchTransaction.isPresent()) {
            Transaction transaction = fetchTransaction.get();
            StateMachine<PaymentStatusEnum, PaymentStatusEvent> paidTransaction = paymentStatusTransactionService.transactionHasBeenPaid(transaction.getId());
            if (paidTransaction.getState().getId().equals(PaymentStatusEnum.PAID)) {
                orderStatusTransactionService.processIssued(transaction.getId());
            }
        }
    }

    @Transactional
    @JmsListener(destination = JmsConfig.SET_PAYMENT_IN_TRANSACTION_BY_ID)
    public void listenForSetPayment(TransactionEvent transactionEvent) {
        TransactionDto transactionDto = transactionEvent.getTransactionDto();
        Optional<Transaction> fetchTransaction = transactionRepository.findById(transactionDto.getId());
        if (fetchTransaction.isPresent()) {
            Transaction transaction = fetchTransaction.get();
            transaction.setPaymentId(transactionDto.getPaymentId());
            transaction.setPaymentStatus(transactionDto.getPaymentStatus());
            transaction.setPointAmount(transactionDto.getPointAmount());
            transaction.setPaymentServiceId(transactionDto.getPaymentServiceId());
            transaction.setVoucherCode(transactionDto.getVoucherCode());
            transaction.setDiscountAmount(transactionDto.getDiscountAmount());
            transactionRepository.save(transaction);
            switch (transaction.getTransactionType()) {
                case "HTD" -> {
                    if (transaction.getPaymentStatus().equals(PaymentStatusEnum.WAITING_PAYMENT)) {
                        depositService.issuedDeposit(DepositTransactionDto.builder()
                                .id(Long.valueOf(transaction.getTransactionId()))
                                .paymentStatus(transaction.getPaymentStatus())
                                .orderStatus(transaction.getOrderStatus())
                                .paymentServiceId(transaction.getPaymentServiceId())
                                .fareAmount(transaction.getFareAmount())
                                .paymentId(transaction.getPaymentId())
                                .build());
                    }
                }
                case "AIR" ->
                        airlinesService.updatePayment(Long.valueOf(transaction.getTransactionId()), transaction.getPaymentStatus());
                case "TRAIN" ->
                        trainService.updatePayment(Long.valueOf(transaction.getTransactionId()), transaction.getPaymentStatus());
            }

        }
    }
}
