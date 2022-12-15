package com.example.bugger;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringBootApplication
@RestController
@RequestMapping
@ControllerAdvice(annotations = RestController.class)
public class BuggerApplication {
    private final Logger logger = LoggerFactory.getLogger(BuggerApplication.class);

    @Autowired
    private Tracer tracer;

	public static void main(String[] args) {
		SpringApplication.run(BuggerApplication.class, args);
	}

    @GetMapping("/bug/{n}")
    public Mono<Integer> bug(@PathVariable int n) {
        return BuggerApplication.observeMono(() -> {
            Span s = tracer.currentSpan();
            logger.warn("Possible bug in span {}-{} ", s.context().traceId(), s.context().spanId());
            return Mono.just(1 / n);
        });
    }

    public static <T> Mono<T> observeMono(Supplier<Mono<T>> sup) {
        return Mono.deferContextual( ctx -> {
            try (ContextSnapshot.Scope scope = ContextSnapshot
                    .setThreadLocalsFrom(ctx, ObservationThreadLocalAccessor.KEY)) {
                return sup.get();
            }
        });
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ProblemDetail> defaultHandler(RuntimeException rex) {
        logger.error(String.format("%s", rex.getMessage()), rex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("type", rex.getClass().getName());
        problemDetail.setDetail(String.format("'%s'", rex.getMessage()));

        return Mono.just(problemDetail);
    }
}
