package org.icomd.app.display;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.icomd.app.display.DisplayDataGenerator;
import org.icomd.app.display.DisplayDataVO;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;


public class DisplayTimeMain extends AppCompatActivity {

    static Boolean DEV_MODE=false;

    static long REFRESH_INTERVEL = 5 * 60 * 1000;
    static float SCREEN_SIZE = 48;

    static{
        if(DEV_MODE){
            SCREEN_SIZE=36;
            REFRESH_INTERVEL=1 * 60 * 1000;
        }
    }


   // static float size = 33;

    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";
    String spacesize = "  ";
    Hashtable<String, TextView> viewMap = new Hashtable<>();
    Hashtable<String, Integer> prayerTimes = new Hashtable<>();
    public static final String inputFormat = "HH:mm a";
    SimpleDateFormat inputParser = new SimpleDateFormat(inputFormat, Locale.US);
    static int val = 1;
    DisplayDataGenerator ddGenerator = new DisplayDataGenerator();
    String sideSpace = spacesize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_fullscreen);


        //setTitle("                                                                  Islamic Center of Maryland Masjid Salah and Jummah Timing");
        ddGenerator.load();
        launchTimeThread();

        createMainTable(ddGenerator.getDisplayDataVO());
        createSideTable(ddGenerator.getDisplayDataVO());
    }
    private static Integer counter=0;
    private void launchTimeThread() {
        Thread thread = new Thread() {
            long timer = System.currentTimeMillis();
            Date lastSucess=null;
            @Override
            public void run() {
                Log.e("***Background Thread***", " launch");
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (timer + REFRESH_INTERVEL < System.currentTimeMillis()) {
                            ++counter;
                            Log.d("Time thread", " lastSucess="+lastSucess);
                            timer = System.currentTimeMillis();
                            ddGenerator.load();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshPrayTIme(ddGenerator.getDisplayDataVO());
                                    refreshSideTable(ddGenerator.getDisplayDataVO());
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView dateDisplay = (TextView) findViewById(R.id.dateDisplay);
                                dateDisplay.setText(ddGenerator.getNow());
                            }
                        });
                        lastSucess=new Date();
                    }
                } catch (Exception e) {
                    Log.e("Time thread", "Error ", e);
                }
            }
        };
        thread.start();
    }


    public void refreshPrayTIme(DisplayDataVO data) {
        try {

            /**************************
             * SALAH REFRESH
             ***************************/

            for (int i = 0; i < 5; i++) {
                DailyPrayer prayer=data.getDailyPrayerByNumber(i+1);
                String Salah = prayer.getLabel();
                String starts = prayer.getStarts();
                String iqama = prayer.getIqama();
                int starts_id = (i + 1) * 10;
                int iqama_id = starts_id + i + 1;
                findAndReplace(starts_id,starts);
                findAndReplace(iqama_id,iqama);
                Log.e("Salah Val", Salah);
            }
            Log.e("refreshPrayTIme", "DailyPrayer complete");
            /**************************
             * JUMMAH REFRESH
             ***************************/

            for (int i = 0; i < data.jummahList.size(); i++) {
                JummahPrayer item = data.getJummahList().get(i);
                String location = item.getLocation();
                String khatib = item.getKhatib();
                String time = item.getTime();
                if(khatib.length()>15){
                    khatib=khatib.substring(0,15);
                }
                if (location != null && location.toUpperCase().contains("ICM") ) {

                    int location_id = DisplayConstants.JUMMAH_START+i*10;
                    int khatib_id = DisplayConstants.JUMMAH_START+i*10+1;
                    int time_id = DisplayConstants.JUMMAH_START+i*10+2;
                    findAndReplace(location_id,location);
                    findAndReplace(khatib_id,khatib);
                    findAndReplace(time_id,time);
                    Log.e("print khatibs", "data location_id="+location_id+"-location"+location+" khatib_id="+khatib_id+"-khatib="+khatib+" time_id="+time_id+"-time="+time);
                }
            }
            Log.e("refreshPrayTIme", "JummahPrayer complete");
        } catch (Exception e) {
            Log.e("refreshPrayTIme", "failed ",e);
        }
    }

    public void findAndReplace(Integer id, String value){
       TextView view=findViewById(id);
       if(value==null){
           value="*";
       }
       if(DEV_MODE){
           view.setText(value+counter);
       }else{
           view.setText(value);
       }

    }

    public void refreshSideTable(DisplayDataVO data) {
        try {
            findAndReplace(DisplayConstants.SUNRISE,data.getSunrise());
            findAndReplace(DisplayConstants.HIJIR_YEAR,data.getHijriYear());
            findAndReplace(DisplayConstants.HIJIR_MONTH,data.getHijriMonth());
            findAndReplace(DisplayConstants.HIJIR_DAY,data.getHijriDay());
        } catch (Exception e) {
            Log.e("hijri date refresh ", "Error  " + e.toString());
        }
    }


    public void emailAlert(String message) {

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"nadeem@icomd.org"});
        i.putExtra(Intent.EXTRA_SUBJECT, "Error Occurred in display-time ");
        i.putExtra(Intent.EXTRA_TEXT, message);
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e("email ", "Error  " + ex.toString());
        }

    }


    private TextView createCell(String text, TableRow row) {
        return createCell(text, row, false);

    }

    private TextView createCell(String text, TableRow row, Boolean reverse) {
       return createCell(text,row,reverse,false);
    }

    private TextView createCell(String text, TableRow row, Boolean reverse,boolean center) {
        if(text==null){
            text="*";
        }
        //Log.d("create cell ","txt="+text+" row="+row+" reverse="+reverse);
        TextView view = new TextView(this);
        view.setText(text);
        if (reverse) {
            view.setTextColor(getResources().getColor(R.color.white));
            view.setAllCaps(true);
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
            view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            view.setBackgroundColor(getResources().getColor(R.color.darkgreen));
        } else {
            if(center){
                view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
            view.setTextColor(getResources().getColor(R.color.black));
            view.setBackgroundColor(getResources().getColor(R.color.white));

        }
        view.setTextSize(SCREEN_SIZE);
        row.addView(view);
        viewMap.put(text, view);

        return view;
    }


    public void createMainTable(DisplayDataVO data) {

        //TableLayout ll = (TableLayout) findViewById(R.id.table_main);
        Log.e("Start create Main","given:"+data);

        TableLayout table = (TableLayout) findViewById(R.id.table_main);

        TableRow row = new TableRow(this);
        createCell(spacesize, row, Boolean.FALSE);
        createCell("Prayer", row, Boolean.TRUE);
        createCell(spacesize, row, Boolean.FALSE);
        createCell("Starts", row, Boolean.TRUE);
        createCell(spacesize, row, Boolean.FALSE);
        createCell("Iqama", row, Boolean.TRUE);
        table.addView(row);

        try {

            Calendar now = Calendar.getInstance();
            int distance = 0;
            for (int pno = 1; pno <6; pno++) {
                try {
                    Log.e("Get Daily Prayer by No","working on no "+pno);
                    DailyPrayer item = data.getDailyPrayerByNumber(pno);
                    if(item==null){
                        Log.e("daily pray failed","received null "+pno);
                    }else {
                        Log.d("received data","received  "+pno+" item="+item);
                        row = new TableRow(this);
                        createCell(spacesize, row, Boolean.FALSE);
                        String Salah = item.getLabel();
                        String starts = item.getStarts();
                        String iqama = item.getIqama();

                        createCell(Salah, row,Boolean.FALSE,Boolean.TRUE);
                        createCell(spacesize, row, Boolean.FALSE);
                        int starts_id = (pno) * 10;
                        int iqama_id = starts_id + pno;
                        createCell(starts, row,Boolean.FALSE,Boolean.TRUE).setId(starts_id);
                        Log.e("IDS", starts + " id: " + starts_id);
                        createCell(spacesize, row, Boolean.FALSE);
                        createCell(iqama, row).setId(iqama_id);
                        Log.e("IDS", iqama + " id: " + iqama_id);
                        table.addView(row);
                        Log.d("icmSalah # " + pno + "=", Salah + "-" + starts + "-" + iqama);
                    }
                } catch (Exception e) {
                    Log.e("load daily failed ","loop no"+pno,e);

                }

            }


            table = (TableLayout) findViewById(R.id.table_main);
            row = new TableRow(this);
            createCell(spacesize, row, Boolean.FALSE);
            createCell("Site", row, Boolean.TRUE);
            createCell(spacesize, row, Boolean.FALSE);
            createCell("Khatib", row, Boolean.TRUE);
            createCell(spacesize, row, Boolean.FALSE);
            createCell("Starts", row, Boolean.TRUE);
            table.addView(row);
            Log.e("Jummah  Main","Jummah on "+data.jummahList.size());
            for (int i = 0; i < data.jummahList.size(); i++) {
                try {
                    JummahPrayer item = data.getJummahList().get(i);
                    Log.e("Jummah ","Jummah on "+i);
                    String location = item.getLocation();
                    String khatib = item.getKhatib();
                    if(khatib.length()>15){
                        khatib=khatib.substring(0,15);
                    }
                    String time = item.getTime();
                    if (location != null && location.toUpperCase().contains("ICM") ) {
                        row = new TableRow(this);

                        int location_id = DisplayConstants.JUMMAH_START+i*10;
                        int khatib_id = DisplayConstants.JUMMAH_START+i*10+1;
                        int time_id = DisplayConstants.JUMMAH_START+i*10+2;

                        createCell(spacesize, row, Boolean.FALSE);
                        createCell(location, row,Boolean.FALSE).setId(location_id);
                        createCell(spacesize, row, Boolean.FALSE);
                        createCell(khatib, row,Boolean.FALSE).setId(khatib_id);
                        createCell(spacesize, row, Boolean.FALSE);
                        createCell(time, row,Boolean.FALSE).setId(time_id);
                        Log.e("print khatibs", "data location_id="+location_id+"-location"+location+" khatib_id="+khatib_id+"-khatib="+khatib+" time_id="+time_id+"-time="+time);
                        table.addView(row);
                    }else{
                        Log.d("NOT ICM jummah # " + i + "=", location + "-" + khatib + "-" + time);
                    }

                } catch (Exception e) {
                    Log.e("Error in jummah loop", e.getMessage());
                }

            }
        } catch (Exception e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }


    }


    private void createSideTable(DisplayDataVO data) {
        String year = "*";
        String month = "*";
        String day = "*";
        String sunrise="*";
        Log.e("Start side table","started");
        try {
            year = data.getHijriYear();
            month = data.getHijriMonth();
            day=data.getHijriDay();
            sunrise=data.getSunrise();
        } catch (Exception e) {
            Log.e("createSideTable ", "Error parsing data " + e.toString());
        }

        TableLayout table = (TableLayout) findViewById(R.id.table_side);
        Log.e("Start side table","create table");

        TableRow row = new TableRow(this);
        table.addView(row);
        row = new TableRow(this);
        table.addView(row);

/**
 * start sunrise
 */

        row = new TableRow(this);
        table.addView(row);

        createCell(sideSpace, row, Boolean.FALSE);
        createCell("Sunrise", row, Boolean.TRUE);
        createCell(sideSpace, row, Boolean.FALSE);

        row = new TableRow(this);
        table.addView(row);
        createCell(sideSpace, row, Boolean.FALSE);
        createCell(sunrise, row, Boolean.FALSE,true).setId(DisplayConstants.SUNRISE);

        /**
         * end sunrise
         */


        row = new TableRow(this);
        table.addView(row);

        createCell(sideSpace, row, Boolean.FALSE);
        createCell("Hijri Year", row, Boolean.TRUE);
        createCell(sideSpace, row, Boolean.FALSE);

        row = new TableRow(this);
        table.addView(row);
        createCell(sideSpace, row, Boolean.FALSE);
        createCell(year, row, Boolean.FALSE,Boolean.TRUE).setId(DisplayConstants.HIJIR_YEAR);
        createCell(sideSpace, row, Boolean.FALSE);

        row = new TableRow(this);
        table.addView(row);
        createCell(sideSpace, row, Boolean.FALSE);

        createCell("Month", row, Boolean.TRUE);
        createCell(sideSpace, row, Boolean.FALSE);

        row = new TableRow(this);
        table.addView(row);
        createCell(sideSpace, row, Boolean.FALSE);

        createCell(month, row, Boolean.FALSE).setId(DisplayConstants.HIJIR_MONTH);
        createCell(sideSpace, row, Boolean.FALSE);

        row = new TableRow(this);
        table.addView(row);
        //createSideSpaceCell(row, 3);
        //row = new TableRow(this);
        //table.addView(row);
      //  createSideSpaceCell(row, 3);

/**
 * start day
 */

        row = new TableRow(this);
        table.addView(row);

        createCell(sideSpace, row, Boolean.FALSE);
        createCell("Day", row, Boolean.TRUE);
        createCell(sideSpace, row, Boolean.FALSE);

        row = new TableRow(this);
        table.addView(row);
        createCell(sideSpace, row, Boolean.FALSE);
        createCell(day, row, Boolean.FALSE,Boolean.TRUE).setId(DisplayConstants.HIJIR_DAY);

        /**
         * end day
         */

        row = new TableRow(this);
        table.addView(row);
        createSideSpaceCell(row, 3);

    }

    private void createSideSpaceCell(TableRow row, int l) {
        for (int i = 0; i < l; ++i) {
            createCell(sideSpace, row, Boolean.FALSE);
        }
    }
}