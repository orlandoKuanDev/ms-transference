package com.example.mstransference.handler;

import com.example.mstransference.model.Transference;
import com.example.mstransference.service.ITransferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j(topic = "TRANSFERENCE_HANDLER")
public class TransferenceHandler {

    private final ITransferenceService transferenceService;

    @Autowired
    public TransferenceHandler(ITransferenceService transferenceService) {
        this.transferenceService = transferenceService;
    }

    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(transferenceService.findAll(), Transference.class);
    }

}
