package com.psdevelop.tdandtcpclient;

import android.os.Message;


public class SMSCheckTimer extends Thread {
    private final RouteService ownerSrv;

    public SMSCheckTimer(RouteService own)   {
        this.ownerSrv = own;
        this.start();
    }

    public void checkWaitingSMS()   {
        Message msg = new Message();
        msg.obj = this.ownerSrv;
        msg.arg1 = RouteService.CHECK_WAITING_SMS;
        RouteService.handle.sendMessage(msg);
    }

    public void run() {
        while (true) {
            try {
                sleep(10000);
                checkWaitingSMS();
            } catch (Exception e) {
                //showMyMsg(
                //        "\nОшибка таймера!" + e.getMessage());
            }
        }
    }
}
