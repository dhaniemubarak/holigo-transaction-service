package id.holigo.services.holigotransactionservice.config;

import java.util.EnumSet;
import java.util.UUID;

import id.holigo.services.common.model.DepositTransactionDto;
import id.holigo.services.common.model.PaymentStatusEnum;
import id.holigo.services.common.model.banktransfer.PostpaidBankTransferTransactionDto;
import id.holigo.services.common.model.gas.PostpaidGasTransactionDto;
import id.holigo.services.common.model.hotel.HotelTransactionDto;
import id.holigo.services.common.model.streaming.PrepaidStreamingTransactionDto;
import id.holigo.services.holigotransactionservice.services.airlines.AirlinesService;
import id.holigo.services.holigotransactionservice.services.deposit.DepositService;
import id.holigo.services.holigotransactionservice.services.hotel.HotelService;
import id.holigo.services.holigotransactionservice.services.postpaid.*;
import id.holigo.services.holigotransactionservice.services.prepaid.*;
import id.holigo.services.holigotransactionservice.services.train.TrainService;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import id.holigo.services.common.model.OrderStatusEnum;
import id.holigo.services.common.model.electricities.PrepaidElectricitiesTransactionDto;
import id.holigo.services.common.model.electricities.PostpaidElectricitiesTransactionDto;
import id.holigo.services.common.model.pulsa.PrepaidPulsaTransactionDto;
import id.holigo.services.common.model.games.PrepaidGameTransactionDto;
import id.holigo.services.common.model.ewallet.PrepaidWalletTransactionDto;
import id.holigo.services.common.model.netv.PostpaidTvInternetTransactionDto;
import id.holigo.services.common.model.telephone.PostpaidTelephoneTransactionDto;
import id.holigo.services.common.model.insurance.PostpaidInsuranceTransactionDto;
import id.holigo.services.common.model.multifinance.PostpaidMultifinanceTransactionDto;
import id.holigo.services.common.model.creditcard.PostpaidCreditcardTransactionDto;
import id.holigo.services.holigotransactionservice.domain.Transaction;
import id.holigo.services.holigotransactionservice.events.OrderStatusEvent;
import id.holigo.services.holigotransactionservice.repositories.TransactionRepository;
import id.holigo.services.holigotransactionservice.services.OrderStatusTransactionServiceImpl;
import id.holigo.services.common.model.pdam.PostpaidPdamTransactionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@EnableStateMachineFactory(name = "orderStatusTransactionSMF")
public class OrderTransactionSMConfig extends StateMachineConfigurerAdapter<OrderStatusEnum, OrderStatusEvent> {

    private final TransactionRepository transactionRepository;

    private final PrepaidElectricitiesTransactionService prepaidElectricitiesTransactionService;

    private final PostpaidPdamTransactionService postpaidPdamTransactionService;

    private final PrepaidPulsaTransactionService prepaidPulsaTransactionService;

    private final PostpaidElectricitiesTransactionService postpaidElectricitiesTransactionService;

    private final PrepaidGameTransactionService prepaidGameTransactionService;

    private final PrepaidWalletTransactionService prepaidWalletTransactionService;

    private final PostpaidTvInternetTransactionService postpaidTvInternetTransactionService;

    private final PostpaidTelephoneTranasctionService postpaidTelephoneTranasctionService;

    private final PostpaidInsuranceTransactionService postpaidInsuranceTransactionService;

    private final PostpaidMultifinanceTransactionService postpaidMultifinanceTransactionService;

    private final PostpaidCreditCardTransactionService postpaidCreditCardTransactionService;

    private final PostpaidGasTransactionService postpaidGasTransactionService;

    private final PrepaidStreamingTransactionService prepaidStreamingTransactionService;

    private final AirlinesService airlinesService;

    private final TrainService trainService;

    private final DepositService depositService;

    private final HotelService hotelService;
    private final PostpaidBankTransferTransactionService postpaidBankTransferTransactionService;

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatusEnum, OrderStatusEvent> states) throws Exception {
        states.withStates().initial(OrderStatusEnum.PROCESS_BOOK)
                .states(EnumSet.allOf(OrderStatusEnum.class))
                .end(OrderStatusEnum.BOOK_FAILED)
                .end(OrderStatusEnum.ISSUED)
                .end(OrderStatusEnum.ISSUED_FAILED)
                .end(OrderStatusEnum.ORDER_CANCELED)
                .end(OrderStatusEnum.ORDER_EXPIRED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatusEnum, OrderStatusEvent> transitions)
            throws Exception {
        transitions.withExternal().source(OrderStatusEnum.PROCESS_BOOK).target(OrderStatusEnum.BOOKED)
                .event(OrderStatusEvent.BOOK_SUCCESS)
                .and()
                .withExternal().source(OrderStatusEnum.PROCESS_BOOK).target(OrderStatusEnum.BOOK_FAILED)
                .event(OrderStatusEvent.BOOK_FAIL)
                .and()
                .withExternal().source(OrderStatusEnum.BOOKED).target(OrderStatusEnum.PROCESS_ISSUED)
                .action(processIssued())
                .event(OrderStatusEvent.PROCESS_ISSUED)
                .and()
                .withExternal().source(OrderStatusEnum.PROCESS_ISSUED).target(OrderStatusEnum.ISSUED)
                .event(OrderStatusEvent.ISSUED_SUCCESS)
                .and()
                .withExternal().source(OrderStatusEnum.PROCESS_ISSUED).target(OrderStatusEnum.ISSUED_FAILED)
                .event(OrderStatusEvent.ISSUED_FAIL)
                .and()
                .withExternal().source(OrderStatusEnum.PROCESS_ISSUED).target(OrderStatusEnum.WAITING_ISSUED)
                .event(OrderStatusEvent.WAITING_ISSUED)
                .and()
                .withExternal().source(OrderStatusEnum.WAITING_ISSUED).target(OrderStatusEnum.ISSUED)
                .event(OrderStatusEvent.ISSUED_SUCCESS)
                .and()
                .withExternal().source(OrderStatusEnum.WAITING_ISSUED).target(OrderStatusEnum.ISSUED_FAILED)
                .event(OrderStatusEvent.ISSUED_FAIL)
                .and()
                .withExternal().source(OrderStatusEnum.PROCESS_ISSUED).target(OrderStatusEnum.RETRYING_ISSUED)
                .event(OrderStatusEvent.RETRYING_ISSUED)
                .and()
                .withExternal().source(OrderStatusEnum.WAITING_ISSUED).target(OrderStatusEnum.RETRYING_ISSUED)
                .event(OrderStatusEvent.RETRYING_ISSUED)
                .and()
                .withExternal().source(OrderStatusEnum.RETRYING_ISSUED).target(OrderStatusEnum.ISSUED)
                .event(OrderStatusEvent.ISSUED_SUCCESS)
                .and()
                .withExternal().source(OrderStatusEnum.RETRYING_ISSUED).target(OrderStatusEnum.ISSUED_FAILED)
                .event(OrderStatusEvent.ISSUED_FAIL)
                .and()
                .withExternal().source(OrderStatusEnum.BOOKED).target(OrderStatusEnum.ORDER_CANCELED)
                .event(OrderStatusEvent.ORDER_CANCEL).action(orderCanceled())
                .and()
                .withExternal().source(OrderStatusEnum.BOOKED).target(OrderStatusEnum.ORDER_EXPIRED)
                .event(OrderStatusEvent.ORDER_EXPIRE).action(orderCanceled());
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderStatusEnum, OrderStatusEvent> config)
            throws Exception {
        StateMachineListenerAdapter<OrderStatusEnum, OrderStatusEvent> adapter = new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<OrderStatusEnum, OrderStatusEvent> from,
                                     State<OrderStatusEnum, OrderStatusEvent> to) {
                log.info(String.format("stateChange(from: %s, to %s)", from.getId(), to.getId()));
            }
        };
        config.withConfiguration().listener(adapter);
    }

    @Bean
    public Action<OrderStatusEnum, OrderStatusEvent> processIssued() {
        return context -> {
            Transaction transaction = transactionRepository.getById(UUID.fromString(
                    context.getMessageHeader(OrderStatusTransactionServiceImpl.ORDER_STATUS_HEADER).toString()));
            switch (transaction.getTransactionType()) {
                case "PRA", "PLNPRE" -> {
                    PrepaidElectricitiesTransactionDto prepaidElectricitiesTransactionDto = PrepaidElectricitiesTransactionDto
                            .builder().id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    prepaidElectricitiesTransactionService.issuedTransaction(prepaidElectricitiesTransactionDto);
                }
                case "PAM" -> {
                    PostpaidPdamTransactionDto postpaidPdamTransactionDto = PostpaidPdamTransactionDto.builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidPdamTransactionService.issuedTransaction(postpaidPdamTransactionDto);
                }
                case "PUL", "PD", "PR" -> {
                    PrepaidPulsaTransactionDto prepaidPulsaTransactionDto = PrepaidPulsaTransactionDto.builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    prepaidPulsaTransactionService.issuedTransaction(prepaidPulsaTransactionDto);
                }
                case "PAS", "PLNPOST" -> {
                    PostpaidElectricitiesTransactionDto postpaidElectricitiesTransactionDto = PostpaidElectricitiesTransactionDto
                            .builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidElectricitiesTransactionService.issuedTransaction(postpaidElectricitiesTransactionDto);
                }
                case "GAME" -> {
                    PrepaidGameTransactionDto prepaidGameTransactionDto = PrepaidGameTransactionDto.builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    prepaidGameTransactionService.issuedTransaction(prepaidGameTransactionDto);
                }
                case "EWAL", "DWAL" -> {
                    PrepaidWalletTransactionDto prepaidWalletTransactionDto = PrepaidWalletTransactionDto.builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    prepaidWalletTransactionService.issuedTransaction(prepaidWalletTransactionDto);
                }
                case "NETV" -> {
                    PostpaidTvInternetTransactionDto postpaidTvInternetTransactionDto = PostpaidTvInternetTransactionDto
                            .builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidTvInternetTransactionService.issuedTransaction(postpaidTvInternetTransactionDto);
                }
                case "TLP" -> {
                    PostpaidTelephoneTransactionDto postpaidTelephoneTransactionDto = PostpaidTelephoneTransactionDto
                            .builder().id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidTelephoneTranasctionService.issuedTransaction(postpaidTelephoneTransactionDto);
                }
                case "INS" -> {
                    PostpaidInsuranceTransactionDto postpaidInsuranceTransactionDto = PostpaidInsuranceTransactionDto
                            .builder().id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidInsuranceTransactionService.issuedTransaction(postpaidInsuranceTransactionDto);
                }
                case "MFN" -> {
                    PostpaidMultifinanceTransactionDto postpaidMultifinanceTransactionDto = PostpaidMultifinanceTransactionDto
                            .builder().id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidMultifinanceTransactionService.issuedTransaction(postpaidMultifinanceTransactionDto);
                }
                case "CC" -> {
                    PostpaidCreditcardTransactionDto postpaidCreditcardTransactionDto = PostpaidCreditcardTransactionDto
                            .builder().id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    postpaidCreditCardTransactionService.issuedTransaction(postpaidCreditcardTransactionDto);
                }
                case "HTL" -> {
                    HotelTransactionDto hotelTransactionDto = HotelTransactionDto
                            .builder().id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build();
                    hotelService.issuedTransaction(hotelTransactionDto);
                }
                case "HTD" -> depositService.issuedDeposit(DepositTransactionDto.builder()
                        .id(Long.valueOf(transaction.getTransactionId()))
                        .paymentStatus(transaction.getPaymentStatus())
                        .orderStatus(transaction.getOrderStatus())
                        .paymentServiceId(transaction.getPaymentServiceId())
                        .fareAmount(transaction.getFareAmount())
                        .paymentId(transaction.getPaymentId())
                        .build());
                case "AIR" -> airlinesService.issuedTransaction(Long.parseLong(transaction.getTransactionId()));
                case "TRAIN" -> trainService.issuedTransaction(Long.parseLong(transaction.getTransactionId()));
                case "GAS" -> postpaidGasTransactionService.issuedTransaction(PostpaidGasTransactionDto.builder()
                        .id(Long.valueOf(transaction.getTransactionId()))
                        .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                        .transactionId(transaction.getId()).build());
                case "STREAMING" ->
                        prepaidStreamingTransactionService.issuedTransaction(PrepaidStreamingTransactionDto.builder()
                                .id(Long.valueOf(transaction.getTransactionId()))
                                .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                                .transactionId(transaction.getId()).build());
                case "TF" ->
                    postpaidBankTransferTransactionService.issuedTransaction(PostpaidBankTransferTransactionDto.builder()
                            .id(Long.valueOf(transaction.getTransactionId()))
                            .paymentStatus(transaction.getPaymentStatus()).orderStatus(transaction.getOrderStatus())
                            .transactionId(transaction.getId()).build());
            }
        };
    }

    @Bean
    public Action<OrderStatusEnum, OrderStatusEvent> orderCanceled() {
        return stateContext -> {
            Transaction transaction = transactionRepository.getById(UUID.fromString(
                    stateContext.getMessageHeader(OrderStatusTransactionServiceImpl.ORDER_STATUS_HEADER).toString()));
            switch (transaction.getTransactionType()) {
                case "AIR" -> airlinesService.cancelTransaction(Long.parseLong(transaction.getTransactionId()));
                case "TRAIN" -> trainService.cancelTransaction(Long.parseLong(transaction.getTransactionId()));
                case "HTD" -> depositService.cancelDeposit(DepositTransactionDto.builder()
                        .id(Long.parseLong(transaction.getTransactionId()))
                        .orderStatus(stateContext.getTarget().getId())
                        .paymentStatus(PaymentStatusEnum.PAYMENT_CANCELED)
                        .build());
            }
        };
    }
}
