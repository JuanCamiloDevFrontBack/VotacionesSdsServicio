package com.example.votacionessds.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
