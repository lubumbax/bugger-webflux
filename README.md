
# Bugger Webflux

Testing inclusion of `traceId/spanId` in logs by Micrometer Tracing, for _reactive/webflux_ based applications.

### Servet/Web

Micrometer Tracing, in _servlet/web_ based applications, automatically includes `traceId/spanId` in logs created from 
our controllers' endpoints (provided that we have configured `logging.pattern.level` for that - see `application.yaml`).

In case that we want `traceId/spanId` also included in errors resulting from unhandled exceptions thrown while
servicing our endpoints, we can implement `@ExceptionHandler(RuntimeException.class)` in a `@ControllerAdvice` and
explicitly log exception from there.  
Micrometer Tracing seems to be including `traceId/spanId` for these logs too.

See [Bugger Webflux](https://www.github.com/lubumbax/bugger-web) for more details.

### Reactive/Webflux

For _reactive/webflux_ based applications, firstly we need to wrap our endpoint with `Mono.deferContextual(ctx -> {..ourEndpoingCode..})` 
(see [BuggerApplication.java](src/main/java/com/example/bugger/BuggerApplication.java)).  

Doing that results in logs created from _ourEndpointCode_ to include `traceId/spanId`.  
However, `traceId/spanId` won't be included in logs created from a `@ExceptionHandler` as described above for 
_servet/web_.

### Test

Hit the bug:

```shell
http :8080/bug/0
```

That results in the following log:
```
2022-12-15T17:52:39.001+01:00  WARN [bugger-webflux,9b0a1a5120cc6787a6534b912b9f099e,6f21d98ca04d742f] 8420 --- [ctor-http-nio-3] com.example.bugger.BuggerApplication     : Possible bug in span 9b0a1a5120cc6787a6534b912b9f099e-6f21d98ca04d742f 
2022-12-15T17:52:39.003+01:00 ERROR [bugger-webflux,,] 8420 --- [ctor-http-nio-3] com.example.bugger.BuggerApplication     : / by zero
```
