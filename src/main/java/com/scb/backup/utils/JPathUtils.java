package com.scb.backup.utils;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;

/**
 * JPathUtils - Utility class for JSON path operations.
 *
 * This class provides static methods for extracting values from JSON strings
 * using JSONPath expressions. It wraps the Jayway JsonPath library with
 * error handling and logging.
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * String json = "{\"user\":{\"name\":\"John\"}}";
 * String name = (String) JPathUtils.get(json, "$.user.name");
 * // Returns: "John"
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@Slf4j
public class JPathUtils {

    /**
     * Extracts a value from JSON using a JSONPath expression.
     *
     * This method uses the Jayway JsonPath library to evaluate the expression
     * against the provided JSON string. If the expression is invalid or the
     * path doesn't exist, it logs the error and returns null instead of throwing
     * an exception.
     *
     * @param json JSON string to query
     * @param expression JSONPath expression (e.g., "$.user.name", "$.items[0].id")
     * @return The value at the specified path, or null if not found or error occurs
     */
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