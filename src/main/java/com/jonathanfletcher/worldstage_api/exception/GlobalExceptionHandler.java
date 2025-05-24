package com.jonathanfletcher.worldstage_api.exception;

import com.jonathanfletcher.worldstage_api.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TRACE = "trace";

    @Value("${reflectoring.trace:false}")
    private boolean printStackTrace;

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("An exception occurred, which will cause a {} response", status, ex);
        } else if (status.is4xxClientError()) {
            log.warn("An exception occurred, which will cause a {} response", status, ex);
        } else {
            log.debug("An exception occurred, which will cause a {} response", status, ex);
        }
        return buildErrorResponse(ex, status, (ServletWebRequest)request);
    }


    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handleNoSuchElementFoundException(EntityNotFoundException exception,
                                                                    ServletWebRequest webRequest) {
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND, webRequest);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleNoSuchElementException(NoSuchElementException exception, ServletWebRequest webRequest) {

        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST, webRequest);
    }

    private ResponseEntity<Object> buildErrorResponse(Exception exception, HttpStatusCode httpStatus, ServletWebRequest webRequest) {
        String errorMessage = StringUtils.substringBefore(exception.getMessage(), ';');

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(httpStatus.value())
            .message(errorMessage)
            .error(httpStatus.toString())
            .timestamp(new Date())
            .path(String.format("%s %s", webRequest.getRequest().getMethod(), webRequest.getRequest().getRequestURI()))
            .build();

        if(printStackTrace && isTraceOn(webRequest)) {
            errorResponse.setStackTrace(ExceptionUtils.getStackTrace(exception));
        }
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    private boolean isTraceOn(WebRequest request) {
        String [] value = request.getParameterValues(TRACE);
        return Objects.nonNull(value)
               && value.length > 0
               && value[0].contentEquals("true");
    }
}
