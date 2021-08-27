package com.example.mstransference.config;

import com.example.mstransference.handler.TransferenceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> rutas(TransferenceHandler handler){
        return route(GET("/transference"), handler::findAll)
                .andRoute(POST("/transference/create"), handler::createDepositWithCard);
    }
}
