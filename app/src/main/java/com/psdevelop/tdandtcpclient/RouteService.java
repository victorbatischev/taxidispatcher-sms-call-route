package com.psdevelop.tdandtcpclient;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RouteService extends Service {

    public static final String PHONE = "phone";
    public static final String SMS_TEXT = "sms_text";
    public static final String INFO_ACTION = "com.psdevelop.tdandtcpclient.INFO_ACTION";
    public static final String TYPE = "type";
    public static final String MSG_TEXT = "msg_text";
    public static final int ID_ACTION_SHOW_OUTSMS_INFO = 0;
    public static final int ID_ACTION_SHOW_INCALL_INFO = 1;
    public static final int ID_ACTION_SHOW_OUTCALL_INFO = 9;

    public final static int PROCESS_SMS_DATA = 5;
    public final static int RECEIVER_IS_DRIVER = 2;
    public final static int RECEIVER_IS_CLIENT = 3;
    public final static int CHECK_WAITING_SMS = 4;
    public final static int INSERT_DETECT_NUM = 6;
    public final static int PROCESS_CALL_DATA = 7;
    public final static int CHECK_CALLIT_ORDER = 8;
    static String MSSQL_HOST = "192.168.0.50";
    static String MSSQL_DBNAME = "TD5R1";
    static String MSSQL_INSTNAME = "SQLEXPRESS";
    static int MSSQL_PORT = 1433;
    static String MSSQL_DB = "jdbc:jtds:sqlserver://"+MSSQL_HOST+":"+MSSQL_PORT+"/"+MSSQL_DBNAME+";instance="+MSSQL_INSTNAME;
    static String MSSQL_LOGIN = "sa";
    static String MSSQL_PASS= "sadba";
    public static Handler handle;
    public static String DRV_SMS_TEXT="?????????? ***___msg_text";
    public static String START_ORD_CLSMS_TEXT="?? ?????? ?????????????? ???????????? ***___msg_text";
    public static String ONPLACE_CLIENT_SMS_TEMPLATE="?????? ?????????????? ?????????? ***___msg_text";
    public static String WAIT_CLIENT_SEND_TEMPLATE="(????. ????????. ***___tval ??????.)";
    public static String REPORT_CLSMS_TEXT="?????? ?????????? ???????????????? ***___msg_text";
    public static String REPORT_BONUS_INFO_TEMPLATE="(???????????? ***___bonusval)";
    SMSCheckTimer smsCheckTimer=null;
    CallItCheckTimer callItCheckTimer=null;

    public static boolean ENABLE_SMS_NOTIFICATIONS=false;
    public static boolean ENABLE_DRIVER_ORDER_SMS=false;
    public static boolean ENABLE_MOVETO_CLIENT_SMS=false;
    public static boolean ENABLE_ONPLACE_CLIENT_SMS=false;
    public static boolean ENABLE_WAIT_CLIENT_SEND=false;
    public static boolean ENABLE_REPORT_CLIENT_SMS=false;
    public static boolean ENABLE_REPORT_BONUS_INFO=false;
    public static boolean ENABLE_INCALL_DETECTING=false;
    public static boolean ENABLE_SMS_MAILING=false;
    public static boolean ENABLE_AUTO_CALLING=false;
    public static boolean ENABLE_CALLING=false;
    public static boolean ENABLE_SMS_MAP=false;
    static int CALL_DEVICE_NUM = 0;
    public static String PHONE_CODE = "+7";
    public static String CURRENCY_SHORT = "??????.";
    public static boolean ALT_FIX_DETECTING=false;

    static SharedPreferences prefs=null;
    PowerManager.WakeLock wakeLock;
    List<String> smsDeviceMap;
    boolean isSmsDeviceMapInit = false;

    public RouteService() {
    }

    public void sendInfoBroadcast(int action_id, String message) {
        Intent intent = new Intent(INFO_ACTION);
        intent.putExtra(TYPE, action_id);
        intent.putExtra(MSG_TEXT, message);
        sendBroadcast(intent);
    }

    public static boolean checkString(String str) {
        try {
            Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static int strToIntDef(String str_int, int def) {
        int res = def;

        if (checkString(str_int)) {
            res = Integer.parseInt(str_int);
        }

        return res;
    }

    public void reloadPrefs()    {
        try {
        if(prefs!=null) {
            ENABLE_SMS_NOTIFICATIONS = prefs.getBoolean("ENABLE_SMS_NOTIFICATIONS", false);
            ENABLE_DRIVER_ORDER_SMS = prefs.getBoolean("ENABLE_DRIVER_ORDER_SMS", false);
            ENABLE_MOVETO_CLIENT_SMS = prefs.getBoolean("ENABLE_MOVETO_CLIENT_SMS", false);
            ENABLE_ONPLACE_CLIENT_SMS = prefs.getBoolean("ENABLE_ONPLACE_CLIENT_SMS", false);
            ENABLE_WAIT_CLIENT_SEND = prefs.getBoolean("ENABLE_WAIT_CLIENT_SEND", false);
            ENABLE_REPORT_CLIENT_SMS = prefs.getBoolean("ENABLE_REPORT_CLIENT_SMS", false);
            ENABLE_REPORT_BONUS_INFO = prefs.getBoolean("ENABLE_REPORT_BONUS_INFO", false);
            ENABLE_INCALL_DETECTING = prefs.getBoolean("ENABLE_INCALL_DETECTING", false);
            ALT_FIX_DETECTING = prefs.getBoolean("ALT_FIX_DETECTING", false);
            ENABLE_SMS_MAILING = prefs.getBoolean("ENABLE_SMS_MAILING", false);
            ENABLE_AUTO_CALLING = prefs.getBoolean("ENABLE_AUTO_CALLING", false);
            ENABLE_CALLING = prefs.getBoolean("ENABLE_CALLING", false);
            ENABLE_SMS_MAP = prefs.getBoolean("ENABLE_SMS_MAP", false);
            CALL_DEVICE_NUM = strToIntDef(prefs.getString("CALL_DEVICE_NUM", "0"), 0);
            PHONE_CODE = prefs.getString("PHONE_CODE", "+7");
            CURRENCY_SHORT =  prefs.getString("CURRENCY_SHORT", "??????.");
            DRV_SMS_TEXT = prefs.getString("DRIVER_ORDER_SMS_TEMPLATE", "?????????? ***___msg_text");
            START_ORD_CLSMS_TEXT = prefs.getString("MOVETO_CLIENT_SMS_TEMPLATE", "?? ?????? ?????????????? ???????????? ***___msg_text");
            ONPLACE_CLIENT_SMS_TEMPLATE = prefs.getString("ONPLACE_CLIENT_SMS_TEMPLATE", "?????? ?????????????? ?????????? ***___msg_text");
            WAIT_CLIENT_SEND_TEMPLATE = prefs.getString("WAIT_CLIENT_SEND_TEMPLATE", "(????. ????????. ***___tval ??????.)");
            REPORT_CLSMS_TEXT = prefs.getString("REPORT_CLIENT_SMS_TEMPLATE", "?????? ?????????? ???????????????? ***___msg_text");
            REPORT_BONUS_INFO_TEMPLATE = prefs.getString("REPORT_BONUS_INFO_TEMPLATE", "(???????????? ***___bonusval)");
            MSSQL_HOST = prefs.getString("DB_HOST_NAME", "192.168.0.1");
            MSSQL_DBNAME = prefs.getString("DATABASE_NAME", "TD5R1");
            MSSQL_INSTNAME = prefs.getString("DBSRV_INSTANCE_NAME", "SQLEXPRESS");
            MSSQL_PORT = strToIntDef(prefs.getString("DBSERVER_PORT", "1433"),1433);
            MSSQL_LOGIN = prefs.getString("DBSERVER_LOGIN", "sa");
            MSSQL_PASS = prefs.getString("DBSERVER_PASSWORD", "sadba");
            MSSQL_DB = "jdbc:jtds:sqlserver://" + MSSQL_HOST + ":" + MSSQL_PORT + "/" + MSSQL_DBNAME + ";instance=" + MSSQL_INSTNAME;
        }
        } catch (Exception e) {
            //Toast.makeText(getBaseContext(),
            //        "???????????? ???????????????????? ????????????????! ?????????? ??????????????????: "
            //                +e.getMessage()+".", Toast.LENGTH_LONG).show();
        }
		if (ENABLE_SMS_MAP) {
			loadSMSSendMap();
		}
    }

    @Override
    public void onCreate() {

        prefs = PreferenceManager.
                getDefaultSharedPreferences(this);
        reloadPrefs();

        handle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.arg1 == MainActivity.SHOW_MESSAGE_TOAST) {
                    showToast(msg.getData().
                            getString("msg_text"));
                } else if (msg.arg1 == PROCESS_SMS_DATA) {
                    showToast("Sending SMS Signal! phone="+msg.getData().
                            getString(PHONE));
                    startMessageServiceIntent(getBaseContext(),msg.getData().
                            getString(SMS_TEXT),msg.getData().
                            getString(PHONE));
                } else if (msg.arg1 == CHECK_WAITING_SMS) {
                    checkWaitingSMS();
                } else if (msg.arg1 == CHECK_CALLIT_ORDER) {
                    checkCallIt();
                } else if (msg.arg1 == INSERT_DETECT_NUM) {
                    insertDetectNumberIntoDB(msg.getData().
                            getString(PHONE));
                }
                else if (msg.arg1 == PROCESS_CALL_DATA) {
                    try	{
                        Intent dialIntent = new Intent(Intent.ACTION_CALL,
                                Uri.parse("tel:" + (msg.getData().
                                        getString(PHONE).length() > 10 ? "" : PHONE_CODE) + msg.getData().
                                        getString(PHONE)));
                        dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(dialIntent);
                    } catch(Exception cex)	{
                        showToast(
                                "???????????? ???????????? ????????????!"+cex.getMessage());
                    }
                }
            }
        };

        PowerManager powerManager = (PowerManager)
                getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock
                (PowerManager.FULL_WAKE_LOCK//PARTIAL_WAKE_LOCK
                        , "No sleep");
        wakeLock.acquire();

        this.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                //try {
                //	Thread.sleep(5000);
                //} catch (InterruptedException e) {
                // TODO Auto-generated catch block
                //	e.printStackTrace();
                //}
                //SMSSendService.smsWait=false;
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:

                        try	{
                            //Toast.makeText(getBaseContext(), "SMS ???????????????????? " +
                                            //SMSSendService.LAST_SENT_RESULT_PHONE + "(" +
                                            //SMSSendService.LAST_SENT_RESULT_TEXT + ")",
                                    //Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "???????????? ?????????????????? ?????????????? ???????????????????? ????????????????! ?????????? ??????????????????: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        try	{
                            //insertLog("?????????? ????????. "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "??????????",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "???????????? ?????????????????? ?????????????? ???????????? ????????????????! ?????????? ??????????????????: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        try	{
                            //insertLog("?????? ???????????? "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "?????? ???????????? SMS",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "???????????? ?????????????????? ?????????????? ?????? ???????????? SMS! ?????????? ??????????????????: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        try	{
                            //insertLog("Null PDU "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "Null PDU",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "???????????? ?????????????????? ?????????????? Null PDU! ?????????? ??????????????????: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        try	{
                            //insertLog("?????? ?????????? "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "?????? ??????????",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "???????????? ?????????????????? ?????????????? ?????? ??????????! ?????????? ??????????????????: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        }, new IntentFilter(SMSSender.INTENT_MESSAGE_SENT));

        //---when the SMS has been delivered---
        this.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "????????????????????",
                                Toast.LENGTH_LONG).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        try	{
                            //insertLog("???? ???????????????????? "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "???? ????????????????????",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "???????????? ?????????????????? ?????????????? ???? ????????????????????! ?????????? ??????????????????: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        }, new IntentFilter(SMSSender.INTENT_MESSAGE_DELIVERED));

        smsCheckTimer = new SMSCheckTimer(this);
        callItCheckTimer = new CallItCheckTimer(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //wakeLock.release();
        return super.onUnbind(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        showToast("?????????????????????????? ??????????????????!");
        reloadPrefs();
        showNotification("??????. ????????", "???????????????? ???????????????? ???????????? ??????????!");
        sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "???????????????? ???????????????? ???????????? ??????????!");
        if(ENABLE_INCALL_DETECTING)
            sendInfoBroadcast( ID_ACTION_SHOW_INCALL_INFO, "???????????????? ???????????????? ??????????????...");
        else
            sendInfoBroadcast( ID_ACTION_SHOW_INCALL_INFO, "?????????????????????? ???????????????? ?????????????? ??????????????????!");
        if(ENABLE_CALLING)
            sendInfoBroadcast( ID_ACTION_SHOW_OUTCALL_INFO, "???????????????? ???????????? ????????????...");
        else
            sendInfoBroadcast( ID_ACTION_SHOW_OUTCALL_INFO, "?????????? ?????????????? ????????????????!");
        //startMessageServiceIntent(getBaseContext(),"???? ???? ????, ????????????","+79183120588");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(messageSent);
        //unregisterReceiver(messageDelivered );
        super.onDestroy();
    }

    public void showToast(String message)   {
        Toast.makeText(getBaseContext(),
                message, Toast.LENGTH_LONG).show();
    }

    public void showNotification(String title, String msg_txt)    {
        // prepare intent which is triggered if the
        // notification is selected
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(msg_txt);
        int NOTIFICATION_ID = 12345;

        Intent targetIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(NOTIFICATION_ID, builder.build());
        /*Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification n  = new Notification.Builder(this)
                .setContentTitle("New mail from " + "test@gmail.com")
                .setContentText("Subject")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher, "Call", pIntent)
                .addAction(R.drawable.ic_launcher, "More", pIntent)
                .addAction(R.drawable.ic_launcher, "And more", pIntent).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, n);*/
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startMessageServiceIntent(Context context, String message, String receiver) {
        Intent i = new Intent(context, SMSSender.class);
        sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS Sending: PHONE="+receiver+",TEXT="+message);
        i.putExtra(SMSSender.EXTRA_MESSAGE, message);
        i.putExtra(SMSSender.EXTRA_RECEIVERS, new String[] { receiver });
        startService(i);
    }

    public void performDial(String phone){
        if(phone!=null){
            try {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
            } catch (Exception e) {
                //e.printStackTrace();
                Toast toastErrorStartCallActivity = Toast.makeText(this,
                        "???????????? ?????????????????????????? ???????????????????? ????????????! ?????????? ??????????????????: "
                                +e.getMessage()+".", Toast.LENGTH_LONG);
                toastErrorStartCallActivity.show();
            }
        }
    }

    public void checkCallIt()   {
        if (ENABLE_CALLING) {
            new AsyncTask() {
                public void showMessageRequest(String msg_text) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                    Bundle bnd = new Bundle();
                    bnd.putString("msg_text", msg_text);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                public void callPhoneNumRequest(String phone) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = PROCESS_CALL_DATA;
                    Bundle bnd = new Bundle();
                    bnd.putString(PHONE, phone);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                @Override
                protected Object doInBackground(Object... params) {
                    // TODO Auto-generated method stub
                    try {
                        Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
                        //Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                        Connection con = null;

                        try {
                            con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                            //encrypt=true; trustServerCertificate=false;
                            if (con != null) {

                                Statement statement = con.createStatement();
                                String queryString = "select * from CallItOrders";
                                ResultSet rs = statement.executeQuery(queryString);

                                while (rs.next()) {
                                    int orderId = rs.getInt("BOLD_ID");
                                    String client_phone = rs.getString("Telefon_klienta");
                                    String order_adres = rs.getString("Adres_vyzova_vvodim");
                                    int dev_num = rs.getInt("dev_num");

                                    if (client_phone.length() == 10&&(
                                            ((CALL_DEVICE_NUM>0)&&(dev_num==CALL_DEVICE_NUM))
                                            ||(CALL_DEVICE_NUM<=0)
                                            )
                                            ) {
                                            //showMessageRequest("RESET DRIVER_SMS_SEND_STATE=2 phone=" +
                                            //        phone + " text=" + sms_text);
                                            if (statement.execute("update Zakaz set call_it=0 where BOLD_ID=" + orderId)) {

                                            }
                                        callPhoneNumRequest(client_phone);
                                    }

                                    //}

                                }

                            } else
                                showMessageRequest("???????????? ????????????????????!");
                        } catch (Exception e) {
                            showMessageRequest("???????????? ???????????? ?? ????! ?????????? ??????????????????: "
                                    + e.getMessage());
                        } finally {
                            try {
                                if (con != null) con.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        showMessageRequest(
                                "???????????? ???????????????????? ???????????? ????????????????! ?????????? ??????????????????: "
                                        + e.getMessage() + ".");
                    }
                    return null;
                }

                //protected void onPostExecute(Object obj) {
                // TODO: check this.exception
                // TODO: do something with the feed
                //}

            }.execute();
        }   else    {
            //sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS ???????????????? ??????????????????!");
        }
    }

    public void checkWaitingSMS()   {
        if (ENABLE_SMS_MAP && !isSmsDeviceMapInit) {
            loadSMSSendMap();
        }

        if (ENABLE_SMS_NOTIFICATIONS&&(ENABLE_DRIVER_ORDER_SMS||
                ENABLE_MOVETO_CLIENT_SMS||ENABLE_REPORT_CLIENT_SMS
        ||ENABLE_ONPLACE_CLIENT_SMS)) {
            new AsyncTask() {
                public void showMessageRequest(String msg_text) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                    Bundle bnd = new Bundle();
                    bnd.putString("msg_text", msg_text);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                public void sendSMSRequest(String sms_text, String phone) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = PROCESS_SMS_DATA;
                    Bundle bnd = new Bundle();
                    bnd.putString(SMS_TEXT, sms_text);
                    bnd.putString(PHONE, phone);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                @Override
                protected Object doInBackground(Object... params) {
                    // TODO Auto-generated method stub
                    try {
                        Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
                        //Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                        Connection con = null;

                        try {
                            con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                            //encrypt=true; trustServerCertificate=false;
                            if (con != null) {

                                Statement statement = con.createStatement();

                                String whereStatement = "";
                                if (ENABLE_SMS_MAP) {

                                	if (!isSmsDeviceMapInit) {
                                		return false;
									}

                                    whereStatement = " WHERE ";
                                    if (ENABLE_DRIVER_ORDER_SMS && DRV_SMS_TEXT.length() > 5) {
                                        whereStatement += " DRIVER_SMS_SEND_STATE = 1 ";
                                    }
                                    if (smsDeviceMap.size() == 0) {
                                        return false;
                                    }

                                    for (int dindex = 0; dindex < smsDeviceMap.size(); dindex++) {
                                        whereStatement += whereStatement.length() > 10
                                                ? " OR "
                                                : (dindex == 0 ? " " : " OR ");
                                        if (smsDeviceMap.get(dindex).length() > 1) {
                                            whereStatement += " CHARINDEX( '" + smsDeviceMap.get(dindex) +
                                                    "', Telefon_klienta) = 1 ";
                                        } else {
                                            whereStatement += " 1=0 ";
                                        }
                                    }
                                }

                                String queryString = "select * from SMSSendOrders" + whereStatement;

                                ResultSet rs = statement.executeQuery(queryString);

                                while (rs.next()) {
                                    String phone = rs.getString("SMS_SEND_DRNUM");
                                    int RECEIVER_TYPE = -1;
                                    String sms_text = DRV_SMS_TEXT;
                                    int orderId = rs.getInt("BOLD_ID");
                                    int waitTime = rs.getInt("WAITING");
                                    double order_summ = rs.getDouble("Uslovn_stoim");

                                    double bonus_add = 0;
                                    try {
                                        rs.findColumn("bonus_add");
                                        bonus_add = rs.getDouble("bonus_add");
                                    } catch (Exception e) { }

                                    double bonus_use = 0;
                                    try {
                                        rs.findColumn("bonus_use");
                                        bonus_use = rs.getDouble("bonus_use");
                                    } catch (Exception e) { }

                                    double bonus_all = 0;
                                    try {
                                        rs.findColumn("bonus_all");
                                        bonus_all = rs.getDouble("bonus_all");
                                    } catch (Exception e) { }

                                    String bonusInfo = (bonus_add > 0 ? "+" + bonus_add : "") +
                                            (bonus_use > 0 ? " -" + bonus_use : "") +
                                            (bonus_all > 0 ? " ??????????:" + bonus_all : "");

                                    String client_phone = rs.getString("Telefon_klienta");
                                    String order_adres = rs.getString("Adres_vyzova_vvodim");
                                    String CLIENT_ORDER_INFO = rs.getString("CLIENT_ORDER_INFO");

                                        if ((rs.getInt("DRIVER_SMS_SEND_STATE") == 1)&&ENABLE_DRIVER_ORDER_SMS&&
                                                (DRV_SMS_TEXT.length()>5)) {
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_DRIVER;
                                                showMessageRequest("RESET DRIVER_SMS_SEND_STATE=2 phone=" +
                                                        phone + " text=" + sms_text);
                                                if (statement.execute("update Zakaz set DRIVER_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {

                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        (phone.length() > 10 ? "" : PHONE_CODE)+client_phone + ":" + order_adres), (phone.length() >= 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("?????????? ???????????????? ?????? ???????????????? ?????? ???????????? 5-????");
                                        } else if ((rs.getInt("CLIENT_SMS_SEND_STATE") == 1)&&ENABLE_MOVETO_CLIENT_SMS&&
                                                (START_ORD_CLSMS_TEXT.length()>5)) {
                                            phone = rs.getString("Telefon_klienta");
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_CLIENT;
                                                sms_text = START_ORD_CLSMS_TEXT;
                                                if (statement.execute("update Zakaz set CLIENT_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {
                                                    showMessageRequest("RESET CLIENT_SMS_SEND_STATE=2");
                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        CLIENT_ORDER_INFO) + (ENABLE_WAIT_CLIENT_SEND ?
                                                        (waitTime > 0 ? WAIT_CLIENT_SEND_TEMPLATE.length() > 5 ? (
                                                                WAIT_CLIENT_SEND_TEMPLATE.replace("***___tval", waitTime + "")
                                                        ) : "" : "") : ""), (phone.length() > 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("?????????? ???????????????? ?????? ???????????????? ?????? ???????????? 5-????");
                                        } else if ((rs.getInt("CLIENT_SMS_SEND_STATE") == 3)&&ENABLE_REPORT_CLIENT_SMS&&
                                                (REPORT_CLSMS_TEXT.length()>5)) {
                                            phone = rs.getString("Telefon_klienta");
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_CLIENT;
                                                sms_text = REPORT_CLSMS_TEXT;
                                                if (statement.execute("update Zakaz set CLIENT_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {
                                                    showMessageRequest("RESET CLIENT_SMS_SEND_STATE=2");
                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        ((int) order_summ + " " + CURRENCY_SHORT)) + (ENABLE_REPORT_BONUS_INFO ?
                                                        (bonusInfo.length() > 0 ? REPORT_BONUS_INFO_TEMPLATE.length() > 5 ? (
                                                                REPORT_BONUS_INFO_TEMPLATE.replace("***___bonusval", bonusInfo)
                                                        ) : "" : "") : ""), (phone.length() > 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("?????????? ???????????????? ?????? ???????????????? ?????? ???????????? 5-????");
                                        } else if ((rs.getInt("CLIENT_SMS_SEND_STATE") == 4)&&ENABLE_ONPLACE_CLIENT_SMS&&
                                                (ONPLACE_CLIENT_SMS_TEMPLATE.length()>5)) {
                                            phone = rs.getString("Telefon_klienta");
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_CLIENT;
                                                sms_text = ONPLACE_CLIENT_SMS_TEMPLATE;
                                                if (statement.execute("update Zakaz set CLIENT_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {
                                                    showMessageRequest("RESET CLIENT_SMS_SEND_STATE=4");
                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        CLIENT_ORDER_INFO), (phone.length() > 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("?????????? ???????????????? ?????? ???????????????? ?????? ???????????? 5-????");
                                        }

                                }

                            } else
                                showMessageRequest("???????????? ????????????????????!");
                        } catch (Exception e) {
                            showMessageRequest("???????????? ???????????? ?? ????! ?????????? ??????????????????: "
                                    + e.getMessage());
                        } finally {
                            try {
                                if (con != null) con.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        showMessageRequest(
                                "???????????? ???????????????????? ???????????? ????????????????! ?????????? ??????????????????: "
                                        + e.getMessage() + ".");
                    }
                    return null;
                }

                //protected void onPostExecute(Object obj) {
                // TODO: check this.exception
                // TODO: do something with the feed
                //}

            }.execute();
        }   else    {
            sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS ???????????????? ??????????????????!");
        }
    }

    public void insertDetectNumberIntoDB(String phone_number)   {
        final String detect_num=phone_number;
        //showToast("["+phone_number+"]");
        new AsyncTask() {
            public void showMessageRequest(String msg_text)	{
                //this.showMyMsg("sock show timer");
                Message msg = new Message();
                //msg.obj = this.mainActiv;
                msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                Bundle bnd = new Bundle();
                bnd.putString("msg_text", msg_text);
                msg.setData(bnd);
                handle.sendMessage(msg);
            }

            @Override
            protected Object doInBackground(Object... params) {
                // TODO Auto-generated method stub
                try {
                    Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
                    //Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                    Connection con = null;

                    try {
                        con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                        //encrypt=true; trustServerCertificate=false;
                        if (con != null) {

                            Statement statement = con.createStatement();
                            //String queryString = "select * from SMSSendOrders";
                            if ((detect_num.length()==12) || (detect_num.length()==11) || true) {

                                if(ALT_FIX_DETECTING && false)   {
                                    if (detect_num.length() == 12) {
                                        if (statement.execute("EXEC InsertOrderWithParamsAlt '','" + detect_num.substring(2) +
                                                "', -1,0,0," + "0,-1010," + "0,0," + "'',-1,-1")) {
                                        }
                                    } else if (detect_num.length() == 11) {
                                        if (statement.execute("EXEC InsertOrderWithParamsAlt '','" + detect_num.substring(1) +
                                                "', -1,0,0," + "0,-1010," + "0,0," + "'',-1,-1")) {
                                        }
                                    }
                                }
                                else {
                                    if (detect_num.length() >= 12) {
                                        showMessageRequest("Request to add detected number 12 len" + detect_num);
                                        if (statement.execute("EXEC InsertOrderWithParams '','" +
                                                detect_num.replace(PHONE_CODE,"") +
                                                "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                        }
                                    } else if (detect_num.length() == 11) {
                                        showMessageRequest("Request to add detected number 11 len" + detect_num);
                                        if (statement.execute("EXEC InsertOrderWithParams '','" + detect_num.substring(1) +
                                                "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                        }
                                    }
                                    else    {
                                        showMessageRequest("Request to add detected number other len" + detect_num);
                                        if (statement.execute("EXEC InsertOrderWithParams '','" + detect_num +
                                                "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                        }
                                    }
                                }

                            }

                        }   else
                            showMessageRequest("???????????? ????????????????????!");
                    } catch (Exception e) {
                        showMessageRequest("???????????? ???????????? ?? ????! ?????????? ??????????????????: "
                                +e.getMessage());
                    } finally   {
                        try {
                            if (con != null) con.close();
                        } catch (SQLException e) {
                            throw new RuntimeException(e.getMessage());
                        }
                    }

                }   catch (Exception e) {
                    showMessageRequest(
                            "???????????? ???????????????????? ???????????? ????????????????! ?????????? ??????????????????: "
                                    +e.getMessage()+".");
                }
                return null;
            }

            //protected void onPostExecute(Object obj) {
            // TODO: check this.exception
            // TODO: do something with the feed
            //}

        }.execute();
    }

    public void loadSMSSendMap()   {
        smsDeviceMap = new ArrayList<String>();
        isSmsDeviceMapInit = false;
        if (ENABLE_SMS_MAP && CALL_DEVICE_NUM > 0) {
            new AsyncTask() {
                public void showMessageRequest(String msg_text) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                    Bundle bnd = new Bundle();
                    bnd.putString("msg_text", msg_text);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                public void sendSMSRequest(String sms_text, String phone) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = PROCESS_SMS_DATA;
                    Bundle bnd = new Bundle();
                    bnd.putString(SMS_TEXT, sms_text);
                    bnd.putString(PHONE, phone);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                @Override
                protected Object doInBackground(Object... params) {
                    // TODO Auto-generated method stub
                    try {
                        Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
                        //Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                        Connection con = null;

                        try {
                            con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                            //encrypt=true; trustServerCertificate=false;
                            if (con != null) {

                                Statement statement = con.createStatement();
                                String queryString = "select * from DEVICE_CODES where device_num=" +
                                        CALL_DEVICE_NUM;
                                ResultSet rs = statement.executeQuery(queryString);

                                while (rs.next()) {
                                    smsDeviceMap.add(rs.getString("code"));
                                }

                                isSmsDeviceMapInit = true;
                            } else
                                showMessageRequest("???????????? ????????????????????!");
                        } catch (Exception e) {
                            showMessageRequest("???????????? ???????????? ?? ????! ?????????? ??????????????????: "
                                    + e.getMessage());
                        } finally {
                            try {
                                if (con != null) {
                                    con.close();
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        showMessageRequest(
                                "???????????? ???????????????????? ???????????? ????????????????! ?????????? ??????????????????: "
                                        + e.getMessage() + ".");
                    }
                    return null;
                }


            }.execute();
        }   else    {
            //sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS ???????????????? ??????????????????!");
        }
    }

    static String phoneNumber = "";

    public static class CallReceiver extends BroadcastReceiver {

        public void showMessageRequest(String msg_text)	{
            Message msg = new Message();
            //msg.obj = this.mainActiv;
            msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
            Bundle bnd = new Bundle();
            bnd.putString("msg_text", msg_text);
            msg.setData(bnd);
            handle.sendMessage(msg);
        }

        public void insertDetectNumRequest(String detect_num)	{
            Message msg = new Message();
            //msg.obj = this.mainActiv;
            msg.arg1 = INSERT_DETECT_NUM;
            Bundle bnd = new Bundle();
            bnd.putString(PHONE, detect_num);
            msg.setData(bnd);
            handle.sendMessage(msg);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(ENABLE_INCALL_DETECTING) {
                if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                    //???????????????? ?????????????????? ??????????
                    phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
                } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                    String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        //?????????????? ????????????, ???????????????? ???????????????? ??????????
                        phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        showMessageRequest("???????????????? ?????????? ?? ????????????: " + phoneNumber);
                        insertDetectNumRequest(phoneNumber);
                    } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        //?????????????? ?????????????????? ?? ???????????? ???????????? (?????????? ???????????? / ????????????????)
                    } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        //?????????????? ?????????????????? ?? ???????????? ????????????. ?????? ?????????????? ?????????????????? ???? ?????????????????? ??????????????????, ?????????? ???? ?????? ?????????? ?????????? ?? ???????? ????????????
                    }
                }
            }   else    {
                //showMessageRequest("?????????????????????? ?????????????? ??????????????????!");
            }
        }
    }

}
