package com.example.mstransference.handler;

import com.example.mstransference.model.*;
import com.example.mstransference.model.dto.CreateDepositWithCardDTO;
import com.example.mstransference.model.dto.TransferenceCreateDTO;
import com.example.mstransference.model.dto.TransferenceRequestDTO;
import com.example.mstransference.service.BillService;
import com.example.mstransference.service.DebitService;
import com.example.mstransference.service.ITransferenceService;
import com.example.mstransference.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j(topic = "TRANSFERENCE_HANDLER")
public class TransferenceHandler {

    private final ITransferenceService transferenceService;
    private final DebitService debitService;
    private final TransactionService transactionService;
    private final BillService billService;

    @Autowired
    public TransferenceHandler(ITransferenceService transferenceService, DebitService debitService, TransactionService transactionService, BillService billService) {
        this.transferenceService = transferenceService;
        this.debitService = debitService;
        this.transactionService = transactionService;
        this.billService = billService;
    }

    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(transferenceService.findAll(), Transference.class);
    }

    private Mono<TransferenceCreateDTO> createTransactionUpdateDebitWithCard(Mono<Tuple2<CreateDepositWithCardDTO, Debit>> tuple) {
        return tuple
                .zipWhen(data -> {
                    data.getT1().setCardNumber(data.getT2().getCardNumber());
                    return debitService.findByCardNumber(data.getT1().getCardNumber());
                })
                .zipWhen(result -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("DEPOSIT");
                    transaction.setTransactionAmount(result.getT1().getT1().getAmount());
                    transaction.setDescription(result.getT1().getT1().getDescription());
                    List<Acquisition> acquisitions = result.getT2().getAssociations();
                    Acquisition acquisition = acquisitions.stream()
                            .filter(acq-> Objects.equals(acq.getBill().getAccountNumber(), result.getT1().getT1().getAccountNumber()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("The retire amount exceeds the available balance in yours accounts"));
                    Bill bill = acquisition.getBill();
                    bill.setBalance(bill.getBalance() + result.getT1().getT1().getAmount());
                    transaction.setBill(bill);
                    return transactionService.createTransaction(transaction);
                })
                .zipWhen(result -> {
                    List<Acquisition> acquisitions = result.getT1().getT2().getAssociations().stream()
                            .peek(rx -> {
                                if (Objects.equals(rx.getBill().getAccountNumber(), result.getT2().getBill().getAccountNumber())){
                                    rx.setBill(result.getT2().getBill());
                                }
                            }).collect(Collectors.toList());
                    //validate is principal
                    Acquisition currentAcq = acquisitions.stream()
                            .filter(acquisition -> Objects.equals(acquisition.getBill().getAccountNumber(), result.getT2().getBill().getAccountNumber()))
                            .findFirst().orElseThrow(() -> new RuntimeException("The account does not exist in this card"));
                    Boolean isPrincipal = result.getT1().getT2().getPrincipal().getIban().equals(currentAcq.getIban());
                    if (Boolean.TRUE.equals(isPrincipal)){
                        result.getT1().getT2().getPrincipal().setBill(result.getT2().getBill());
                    }
                    Debit debit = new Debit();
                    debit.setAssociations(acquisitions);
                    debit.setPrincipal(result.getT1().getT2().getPrincipal());
                    debit.setCardNumber(result.getT1().getT2().getCardNumber());
                    return debitService.updateDebit(debit);
                })
                .flatMap(response -> {
                    TransferenceCreateDTO transference = new TransferenceCreateDTO();
                    transference.setAmount(response.getT1().getT1().getT1().getT1().getAmount());
                    transference.setDescription(response.getT1().getT1().getT1().getT1().getDescription());
                    transference.setBill(response.getT1().getT2().getBill());
                    return Mono.just(transference);
                });
    }

    private Mono<TransferenceCreateDTO> createTransactionCardLess(Mono<CreateDepositWithCardDTO> tuple){
        return tuple
                .zipWhen(depositRequest ->  {
                    return billService.findByAccountNumber(depositRequest.getAccountNumber());
                })
                .zipWhen(result -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("DEPOSIT");
                    transaction.setTransactionAmount(result.getT1().getAmount());
                    transaction.setDescription(result.getT1().getDescription());
                    Bill bill = result.getT2();
                    bill.setBalance(bill.getBalance() + result.getT1().getAmount());
                    transaction.setBill(bill);
                    return transactionService.createTransaction(transaction);
                })
                .flatMap(response -> {
                    TransferenceCreateDTO transference = new TransferenceCreateDTO();
                    transference.setAmount(response.getT1().getT1().getAmount());
                    transference.setDescription(response.getT1().getT1().getDescription());
                    transference.setBill(response.getT2().getBill());
                    return Mono.just(transference);
                });
    }

    public Mono<ServerResponse> createDepositWithCard(ServerRequest request){

        Mono<TransferenceRequestDTO> transferenceRequestDTO = request.bodyToMono(TransferenceRequestDTO.class);

        Mono<CreateDepositWithCardDTO> origenTransference = transferenceRequestDTO
                .flatMap(transference -> {
                    CreateDepositWithCardDTO or = new CreateDepositWithCardDTO();
                    or.setAmount(transference.getAmount());
                    or.setAccountNumber(transference.getOrigen());
                    or.setDescription(transference.getDescription());
                    return Mono.just(or);
                });

        Mono<CreateDepositWithCardDTO> destineTransference = transferenceRequestDTO
                .flatMap(transference -> {
                    CreateDepositWithCardDTO or = new CreateDepositWithCardDTO();
                    or.setAmount(transference.getAmount());
                    or.setAccountNumber(transference.getDestine());
                    or.setDescription(transference.getDescription());
                    return Mono.just(or);
                });

        List<Mono<CreateDepositWithCardDTO>> listTransferees = new ArrayList<>();
        listTransferees.add(origenTransference);
        listTransferees.add(destineTransference);

        return Flux.fromIterable(listTransferees)
                .flatMapSequential(t -> t
                        .zipWhen(depositRequest -> {
                            return debitService.findByAccountNumber(depositRequest.getAccountNumber())
                                    .switchIfEmpty(Mono.defer(() -> {
                                        return Mono.just(new Debit());
                                    }));
                        })
                        .flatMap(data -> {
                            if(data.getT2().getCardNumber() == null){
                                return Mono.just(data.getT1()).as(this::createTransactionCardLess);
                            }
                            return Mono.just(Tuples.of(data.getT1(), data.getT2()))
                                    .as(this::createTransactionUpdateDebitWithCard);
                        }))
                .collectList()
                .flatMap(t -> {
                    log.info("LIST_TRANSFERENCES, {}", t);
                    Bill billOrigen = t.stream().map(x -> x.getBill()).collect(Collectors.toList()).get(0);
                    Double billAmount = t.stream().map(x -> x.getAmount()).collect(Collectors.toList()).get(0);
                    String billDesc = t.stream().map(x -> x.getDescription()).collect(Collectors.toList()).get(0);
                    Bill billDestine = t.stream().map(x -> x.getBill()).collect(Collectors.toList()).get(1);
                    Transference transference = new Transference();
                    transference.setAmount(billAmount);
                    transference.setDescription(billDesc);
                    transference.setDestine(billDestine);
                    transference.setOrigen(billOrigen);
                    return transferenceService.create(transference);
                })
                .flatMap(depositCreate ->
                        ServerResponse.ok()
                                .contentType(APPLICATION_JSON)
                                .bodyValue(depositCreate))
                .log()
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));


       /* return createDepositDTO
                .zipWhen(depositRequest -> {
                    return debitService.findByAccountNumber(depositRequest.getAccountNumber())
                            .switchIfEmpty(Mono.defer(() -> {
                                return Mono.just(new Debit());
                            }));
                })
                .flatMap(data -> {
                    if(data.getT2().getCardNumber() == null){
                        return Mono.just(data.getT1()).as(this::createTransactionCardLess);
                    }
                    return Mono.just(Tuples.of(data.getT1(), data.getT2()))
                            .as(this::createTransactionUpdateDebitWithCard);
                })
                .flatMap(depositCreate ->
                        ServerResponse.ok()
                                .contentType(APPLICATION_JSON)
                                .bodyValue(depositCreate))
                .log()
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));*/
    }
}
