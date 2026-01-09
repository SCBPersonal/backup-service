package com.scb.backup.utils;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class JPathUtils {

    public static Object get(String json, String expression) {
        log.debug("getting for expression -{} ",expression);
        try {
            var value= JsonPath.read(json, expression);
            log.debug("Getting for expression -{} with value {}",expression,value);
            return value;
        } catch (Exception e) {
            log.error("Error in Getting value for Expression -{}", expression,e);
            return null;
            // throw new RuntimeException("Error in Getting value for Expression-" +expression,e);
        }
    }


}