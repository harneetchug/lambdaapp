package com.example;

import java.util.Map;
import org.apache.log4j.Logger;

public class Handler {

    private Logger log;


    public void handleRequest(Map<String, Object> event) {
        log.info("S3 Event received:");
        log.info(event);
    }
}
