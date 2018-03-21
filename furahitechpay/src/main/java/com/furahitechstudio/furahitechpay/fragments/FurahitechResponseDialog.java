package com.furahitechstudio.furahitechpay.fragments;


import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.furahitechstudio.furahitechpay.FurahitechPay;
import com.furahitechstudio.furahitechpay.R;
import com.furahitechstudio.furahitechpay.listeners.DialogClickListener;
import com.furahitechstudio.furahitechpay.models.PaymentStatus;

import static com.furahitechstudio.furahitechpay.utils.Furahitech.PAYMENT_REDIRECTION_PARAM;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.STATUS_CANCELLED;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.STATUS_SUCCESS;
import static com.furahitechstudio.furahitechpay.utils.Furahitech.PaymentConstant.STATUS_TIMEOUT;

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
 * <h1>FurahitechResponseDialog</h1>
 * <p>
 *     FurahitechResponseDialog is responsible for showing payment status upon payment task completion
 * </p>
 *
 * @author Lukundo Kileha (kileha3)
 *         lkileha@furahitech.co.tz
 *
 */
public class FurahitechResponseDialog extends DialogFragment{
    private DialogClickListener clickListener;

    /**
     * Instantiate FurahitechResponseDialog as singleton
     * @param status Payment status
     * @return FurahitechResponseDialog object
     * @see PaymentStatus
     */
    public FurahitechResponseDialog getInstance(PaymentStatus status) {
       FurahitechResponseDialog ratingDialog=new FurahitechResponseDialog();
       Bundle bundle=new Bundle();
       bundle.putSerializable(PAYMENT_REDIRECTION_PARAM,status);
       ratingDialog.setArguments(bundle);
       return ratingDialog;
    }

    public void setClickListener(DialogClickListener clickListener){
        this.clickListener=clickListener;
    }


    @SuppressLint("StaticFieldLeak")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_response_dialog, container, false);
        TextView paymentMemo = view.findViewById(R.id.paymentMemo);
        TextView statusText = view.findViewById(R.id.paymentStatus);
        ImageView paymentStatusIcon = view.findViewById(R.id.paymentStatusIcon);
        Button closeDialog = view.findViewById(R.id.closeDialog);
        FrameLayout paymentStatusHolder = view.findViewById(R.id.paymentStatusHolder);

        //Setting up payment status views from status itself
        final PaymentStatus paymentStatus= (PaymentStatus) getArguments().getSerializable(PAYMENT_REDIRECTION_PARAM);

        if(paymentStatus!=null && clickListener !=null){
            boolean isPaidSuccessfully= paymentStatus.getPaymentStatus() == STATUS_SUCCESS;
            int paymentStatusColors= ContextCompat.getColor(getActivity(), isPaidSuccessfully ? R.color.colorSuccess
                    : paymentStatus.getPaymentStatus() == STATUS_TIMEOUT ? android.R.color.black:
                    R.color.colorError);
            paymentStatus.setPaymentExtraParam(FurahitechPay.getInstance().getPaymentRequest().getPaymentExtraParam());
            int paymentStatusIconsRes= isPaidSuccessfully ? R.drawable.ic_check_white_24dp: paymentStatus.getPaymentStatus() == STATUS_TIMEOUT
                    ? R.drawable.ic_timer_off_white_24dp:R.drawable.ic_close_white_24dp;
            String paymentStatusLabel= isPaidSuccessfully ? "Success": paymentStatus.getPaymentStatus() == STATUS_TIMEOUT ? "Timeout":"Failure";
            String message= paymentStatus.getPaymentStatus() == STATUS_CANCELLED ? getString(R.string.payment_cancelled)
                    : isPaidSuccessfully ? getString(R.string.payment_received)
                    : paymentStatus.getPaymentStatus() == STATUS_TIMEOUT ? getString(R.string.payment_timed_out)
                    :getString(R.string.no_payment_received);

            //Setting data to the views and holders
            paymentMemo.setText(message);
            statusText.setText(paymentStatusLabel);
            paymentStatusIcon.setImageResource(paymentStatusIconsRes);
            GradientDrawable bgShape = (GradientDrawable)paymentStatusHolder.getBackground();
            bgShape.setColor(paymentStatusColors);
            closeDialog.setBackgroundColor(paymentStatusColors);
            setCancelable(false);
            closeDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                    clickListener.onClose(paymentStatus);
                }
            });
        }else{
            dismiss();
        }

        return view;
    }
    
    /**
     * Setting up extra dialog views details on resiming it
     */
    @Override
    public void onResume() {
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = LinearLayout.LayoutParams.MATCH_PARENT;
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        super.onResume();
    }

}
