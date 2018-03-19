package com.furahitechstudio.furahitechpay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;

import com.furahitechstudio.furahitechpay.activities.PayCard;
import com.furahitechstudio.furahitechpay.activities.PayMobile;
import com.furahitechstudio.furahitechpay.listeners.CallBackListener;
import com.furahitechstudio.furahitechpay.listeners.StateChangedListener;
import com.furahitechstudio.furahitechpay.models.PaymentRequest;
import com.furahitechstudio.furahitechpay.utils.Furahitech;
import com.furahitechstudio.furahitechpay.utils.FurahitechException;
import com.furahitechstudio.furahitechpay.utils.FurahitechUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.furahitechstudio.furahitechpay.networks.FurahitechNetworkCall.checkCallbackStatus;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.CALLBACK_CHECK_INTERVAL;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.CUSTOM_PHONE_NUMBER_HINT;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.CUSTOM_PHONE_NUMBER_MASK;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PAYMENT_REDIRECTION_PARAM;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.REQUEST_CODE_PAYMENT_STATUS;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.GATEWAY_MPESA;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.GATEWAY_NONE;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.GATEWAY_TIGOPESA;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.MODE_MOBILE;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentEnvironment.LIVE;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentEnvironment.SANDBOX;
import static com.furahitechstudio.furahitechpay.utils.FurahitechUtils.isConnected;
import static com.furahitechstudio.furahitechpay.utils.FurahitechUtils.logEvent;

/*
 * Copyright (c) 2018 Lukundo Kileha
 *
 * Licensed under The MIT License,
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 */

/**
 * <h1>FurahitechPay</h1>
 * <p>
 *     FurahitechPay is singleton class which acts as a core for every operation within this API.
 * </p>
 *
 * @author Lukundo Kileha (kileha3)
 *         lkileha@furahitech.co.tz
 */
public class FurahitechPay{

    @SuppressLint("StaticFieldLeak")
    private static FurahitechPay furahitechPay;
    private Enum paymentMode;
    private String paymentEnvironment= LIVE;
    private PaymentRequest paymentRequest;
    private Activity activity;
    private Enum [] supportedGateway;
    private String customPhoneNumberHint=CUSTOM_PHONE_NUMBER_HINT;
    private String customPhoneNumberMask=CUSTOM_PHONE_NUMBER_MASK;
    private List<StateChangedListener> stateChangedListeners=new CopyOnWriteArrayList<>();

    private static Enum currentGateway;
    private static String transactionUUID;
    private static CallBackListener callBackListener;

    /**
     * Creating schedules for periodic callback status change
     */
    private static Handler mHandler;

    private static Runnable mRunnable;



    /**
     * Responsible for getting PaymentGatewaysAPI instance as singleton
     * @return FurahitechPay object
     */
    public static FurahitechPay getInstance(){
        if(furahitechPay ==null){
            furahitechPay =new FurahitechPay();
            logEvent(false,GATEWAY_NONE,"API instance acquired");
        }
        return furahitechPay;
    }

    /**
     * Responsible for setting up application activity and statusChangeListeners
     * @param activity: Application activity
     * @return FurahitechPay :object
     */
    public FurahitechPay with(Activity activity){
        this.activity =activity;
        return this;
    }


    /**
     * Scheduling periodic callback status check from the server
     * @param mContext Application context
     * @param gateway Currently selected Gateway
     * @param UUID Currently created UUID
     * @param listener Status check listener
     */
    public static void startCallBackCheckTask(Context mContext,Enum gateway, String UUID, CallBackListener listener){
        currentGateway=gateway;
        transactionUUID=UUID;
        callBackListener=listener;
        mHandler=new Handler();
        if(mRunnable!=null){
            cancelCallBackCheckTask();
        }
        mRunnable=new PaymentRunnable(mContext);
        mRunnable.run();
    }


    /**
     * Cancel periodic callback status check task
     */
    public static void cancelCallBackCheckTask(){
        if(mHandler!=null && mRunnable!=null){
            mHandler.removeCallbacksAndMessages(null);
        }
    }


    /**
     * Responsible for getting payment request object
     * @return PaymentRequest
     */
    public PaymentRequest getPaymentRequest(){
        return paymentRequest;
    }

    /**
     * Responsible for setting up payment mode (PAYMENT_MOBILE / PAYMENT_CARD)
     * @param paymentMode: Payment mode as indicated in PaymentMode
     * @return FurahitechPay: object
     */
    public FurahitechPay setPaymentMode(Enum paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    /**
     * Responsible for setting up payment environment
     * @param paymentEnvironment: Payment environment (LIVE/SANDBOX)
     * @return FurahitechPay: object
     */
    public FurahitechPay setPaymentEnvironment(String paymentEnvironment) {
        this.paymentEnvironment = paymentEnvironment;
        return this;
    }

    /**
     * Responsible for setting up payment request
     * @param request: Payment request object with all details
     *                      necessary for transaction initialization
     * @return FurahitechPay: Object
     */
    public FurahitechPay setPaymentRequest(PaymentRequest request) {
        this.paymentRequest = request;
        return this;
    }

    /**
     * Responsible for setting up list of supported Mobile Network Operators
     * @param supportedGateway List of supported Mobile Network Operators
     * @return FurahitechPay object
     */
    public FurahitechPay setSupportedGateway(Enum ... supportedGateway){
        this.supportedGateway =supportedGateway;
        return this;
    }

    public Enum[] getSupportedGateway() {
        return supportedGateway;
    }

    public String getPaymentEnvironment() {
        return paymentEnvironment;
    }

    public String getCustomPhoneNumberHint() {
        return customPhoneNumberHint;
    }

    /**
     * Responsible for setting up custom hint for customer phone number
     * @param customPhoneNumberHint custom hint
     * @return FurahitechPay object
     */
    public FurahitechPay setCustomPhoneNumberHint(String customPhoneNumberHint) {
        this.customPhoneNumberHint = customPhoneNumberHint;
        return this;
    }

    public String getCustomPhoneNumberMask() {
        return customPhoneNumberMask;
    }

    /**
     * Responsible for setting up custom mask for customer phone number
     * @param customPhoneNumberMask custom mask
     * @return FurahitechPay object
     */
    public FurahitechPay setCustomPhoneNumberMask(String customPhoneNumberMask) {
        this.customPhoneNumberMask = customPhoneNumberMask;
        return this;
    }

    public void registerStateListener(StateChangedListener listener){
        if(stateChangedListeners!=null){
            stateChangedListeners.add(listener);
        }
    }


    public void notifyStateChanged(Enum state){
        if(stateChangedListeners!=null){
            for(StateChangedListener listener:stateChangedListeners){
                listener.onTaskStateChanged(state);
            }
        }
    }

    private Enum getPaymentMode(){
        return paymentMode;
    }

    @Deprecated
    public void build(){
        request();
    }
    /**
     * Building logic to decide what logic to be executed
     */
    public void request(){

        boolean isValidAmount=getPaymentRequest()!=null && getPaymentRequest().getTransactionAmount() > 0;

        boolean isValidRequestObject=getPaymentRequest()!=null;

        boolean isValidPaymentMode=getPaymentMode()!=null;

        boolean isValidActivity=activity!=null;


        boolean isValidRequestEndPoint=getPaymentRequest()!=null && getPaymentRequest().getPaymentRequestEndPoint()!=null
                && !getPaymentRequest().getPaymentRequestEndPoint().isEmpty();

        boolean isMobileTransaction=paymentMode == MODE_MOBILE;

        boolean isValidCard=getPaymentRequest()!=null && getPaymentRequest().getCardMerchantKey()!=null
                && !getPaymentRequest().getCardMerchantKey().isEmpty() && getPaymentRequest().getCardMerchantSecret()!=null
                && !getPaymentRequest().getCardMerchantSecret().isEmpty();

        boolean isValidGateway=supportedGateway !=null && getPaymentRequest()!=null && supportedGateway.length > 0
                && ((Arrays.asList(supportedGateway).indexOf(GATEWAY_MPESA)!=-1
                && !getPaymentRequest().getPaymentLogsEndPoint().isEmpty())
                || Arrays.asList(supportedGateway).indexOf(GATEWAY_TIGOPESA)!=-1);

        boolean isMpesaSelected=supportedGateway!=null && Arrays.asList(supportedGateway).indexOf(GATEWAY_MPESA)!=-1;

        boolean isTigoPesaSelected=supportedGateway!=null && Arrays.asList(supportedGateway).indexOf(GATEWAY_TIGOPESA)!=-1;

        boolean isValidMpesa=getPaymentRequest()!=null && getPaymentRequest().getWazoHubClientID()!=null && !getPaymentRequest().getWazoHubClientID().isEmpty()
                && getPaymentRequest().getWazoHubClientSecret()!=null && !getPaymentRequest().getWazoHubClientSecret().isEmpty()
                && getPaymentRequest().getPaymentLogsEndPoint()!=null && !getPaymentRequest().getPaymentLogsEndPoint().isEmpty();


        boolean isValidTigoPesa=getPaymentRequest()!=null && getPaymentRequest().getTigoMerchantKey()!=null && !getPaymentRequest().getTigoMerchantKey().isEmpty()
                && getPaymentRequest().getTigoMerchantName()!=null && !getPaymentRequest().getTigoMerchantName().isEmpty()
                && getPaymentRequest().getTigoMerchantNumber()!=null && !getPaymentRequest().getTigoMerchantNumber().isEmpty()
                && getPaymentRequest().getTigoMerchantPin()!=null && !getPaymentRequest().getTigoMerchantPin().isEmpty()
                && getPaymentRequest().getTigoMerchantSecret()!=null && !getPaymentRequest().getTigoMerchantSecret().isEmpty();

        boolean isValidEnvironment=paymentEnvironment!=null && ((paymentEnvironment.equals(SANDBOX) && paymentEnvironment.toLowerCase().contains(SANDBOX.toLowerCase()))
                || (paymentEnvironment.equals(LIVE) && paymentEnvironment.toLowerCase().contains(LIVE.toLowerCase())));

        boolean isValidClientsDetails=getPaymentRequest()!=null && getPaymentRequest().getCustomerEmailAddress()!=null
                && !getPaymentRequest().getCustomerEmailAddress().isEmpty() && getPaymentRequest().getCustomerFirstName()!=null
                &&!getPaymentRequest().getCustomerFirstName().isEmpty() && getPaymentRequest().getCustomerLastName()!=null
                && !getPaymentRequest().getCustomerLastName().isEmpty();

        if(!isValidActivity){
            throw new FurahitechException("Missing application activity and ",new Throwable("Provide application activity"));
        }

        if(!isValidPaymentMode){
            throw new FurahitechException("Missing payment mode",new Throwable("Provide payment mode"));
        }

        if(!isValidRequestObject){
            throw new FurahitechException("Missing payment request param",new Throwable("Provide payment request param"));
        }

        if (!isValidRequestEndPoint && isMpesaSelected){
            throw new FurahitechException("Missing payment request HTTP base URL",new Throwable("Provide base URl for payment request HTTP calls"));
        }
        if(!isValidAmount){
            throw new FurahitechException("Total amount must be greater than zero",new Throwable("Make sure payable amount is greater than zero"));
        }

        if(!isValidClientsDetails){
            throw new FurahitechException("Missing customer details",new Throwable("Provide customer details required for transaction"));
        }

        if(!isValidEnvironment){
            throw new FurahitechException("URL provided is not sandbox URL",new Throwable("Provide correct sandbox URL"));
        }

        if(!isValidGateway && isMobileTransaction && isValidMpesa){
            throw new FurahitechException("Missing payment logging endpoint",new Throwable("Provide log en-point"));
        }

        if(!isValidMpesa && isMobileTransaction && isMpesaSelected){
            throw new FurahitechException("Missing wazohub client details",new Throwable("Provide WazoHub client credentials or payment log end-point"));
        }


        if(!isValidTigoPesa && isMobileTransaction && isTigoPesaSelected){
            throw new FurahitechException("Missing tigopesa merchant details",new Throwable("Provide all tigo pesa merchant details"));
        }

        if(!isValidCard && !isMobileTransaction){
            throw new FurahitechException("Card merchant details can't be null",new Throwable("Make sure you provide card merchant details"));
        }

        if(isValidMpesa && !isValidTigoPesa){
            setCustomPhoneNumberHint("759000000");
        }

        if(isValidTigoPesa && !isValidMpesa){
            setCustomPhoneNumberHint("712000000");
        }

        Intent resultIntent;
        if(isMobileTransaction && ((isValidTigoPesa && isTigoPesaSelected) || (isValidMpesa && isMpesaSelected))){
            resultIntent=new Intent(activity, PayMobile.class);
            logEvent(false,GATEWAY_NONE,"Mobile payment selected");
        }else{
            resultIntent=new Intent(activity, PayCard.class);
            logEvent(false,GATEWAY_NONE,"Card payment selected");
        }


        resultIntent.putExtra(PAYMENT_REDIRECTION_PARAM,getPaymentRequest());
        activity.startActivityForResult(resultIntent, REQUEST_CODE_PAYMENT_STATUS);
    }

    /**
     * Class to handle payment status checking
     */
    private static class PaymentRunnable implements Runnable {
        private Context mContext;
        PaymentRunnable(Context mContext){
            this.mContext=mContext;
        }
        @Override
        public void run() {
            Furahitech.currentRetryCount++;
            checkCallbackStatus(mContext,currentGateway,transactionUUID,callBackListener);
            mHandler.postDelayed(mRunnable, TimeUnit.SECONDS.toMillis(CALLBACK_CHECK_INTERVAL));
        }
    }

}
