package com.kanapa4.shop.order.adapter;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

public class CartClientErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception = defaultDecoder.decode(methodKey, response);
        if (response.status() >= 500 && exception instanceof FeignException feignException) {
            return new RetryableException(
                    response.status(),
                    feignException.getMessage(),
                    response.request().httpMethod(),
                    (Long) null,
                    response.request());
        }
        return exception;
    }
}
