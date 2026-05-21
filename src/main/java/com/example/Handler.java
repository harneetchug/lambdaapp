package com.example;


import org.apache.log4j.Logger;

import java.util.Map;

public class Handler {

    private Logger log;


    public void handleRequest(Map<String, Object> event) {
        log.info("S3 Event received:");
        log.info(event);
    }
}
