package com.scb.backup.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AppUtils {


    public static String getBusinessDate(String date) {
        return date.replace("-","");
    }
    public static Date toDate(String date)  {
        SimpleDateFormat formatter = new SimpleDateFormat(AppConstants.DATE_PATTERN);
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            log.error("Exception in parsing date",e);
            return null;
        }
    }
    public static Map<String, Object> createBatchParams(String batchId, String businessDate, String categoryCode) {
        Map<String, Object> batchParams = new HashMap<>();
        batchParams.put(AppConstants.BATCH_ID, batchId);
        batchParams.put(AppConstants.BUSINESS_DATE, AppUtils.toDate(businessDate));
        batchParams.put(AppConstants.CATEGORY_CODE, categoryCode);
        return batchParams;
    }
}
