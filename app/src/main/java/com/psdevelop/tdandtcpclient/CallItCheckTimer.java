package com.psdevelop.tdandtcpclient;

import android.os.Message;

public class CallItCheckTimer extends Thread {
    private final RouteService ownerSrv;

    public CallItCheckTimer(RouteService own)   {
        this.ownerSrv = own;
        this.start();
    }

    public void checkWaitingSMS()   {
        Message msg = new Message();
        msg.obj = this.ownerSrv;
        msg.arg1 = RouteService.CHECK_CALLIT_ORDER;
        RouteService.handle.sendMessage(msg);
    }

    public void run() {
        while (true) {
            try {
                sleep(4000);
                checkWaitingSMS();
            } catch (Exception e) {
                //showMyMsg(
                //        "\nОшибка таймера!" + e.getMessage());
            }
        }
    }
}
