package xyz.bluelemondev.hkh_sample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.service.autofill.FieldClassification;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private LocationManager locationManager;
    private static final int REQUEST_CODE_LOCATION = 2;
    double lat;
    double lon;
    String AEDAddress;

    String HospitalInfo01; //상급종합병원
    String HospitalInfo11; //종합병원
    String HospitalBase;

    String radius;
    String SearchingRadius;

    String APIKey = "szwzJ0CofY7gfIsU7KqrwCs79Lhnbis1VwbketfdGSG%2FuJAVdRoEeeEt3SQFLI8qIxFuIlwhV4Kp7PDl4aJToQ%3D%3D";

    List<Hospital> hospitalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void location(View v) {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location userLocation = getMyLocation();
        if (userLocation != null){
            lat = userLocation.getLatitude();
            lon = userLocation.getLongitude();
            TextView textView = (TextView)findViewById(R.id.locat);
            textView.setText("LATITUDE=" + lat + "\nLONGITUDE=" + lon);
        }
    }

    private Location getMyLocation(){
        Location currentLocation = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, this.REQUEST_CODE_LOCATION);
            getMyLocation();
        } else {
            String locationProvider = LocationManager.GPS_PROVIDER;
            currentLocation = locationManager.getLastKnownLocation(locationProvider);
            if (currentLocation != null) {
                lon = currentLocation.getLongitude();
                lat = currentLocation.getLatitude();
            }
        }
        return currentLocation;
    }

    public void getAEDAPI(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                AEDAddress = getAEDData();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = (TextView)findViewById(R.id.apireturn);
                        tv.setText(AEDAddress);
                    }
                });
            }
        }).start();
    }

    public void getHospAPI(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HospitalInfo11 = getHospData11();
                HospitalInfo01 = getHospData01();
                HospitalBase = HospitalInfo11 + HospitalInfo01;
                //String nearer = makeList();
                /*String regex = "병원 주소: (.+)\\n거리: (.+)m\\n전화번호: (.+)\\n병원명: (.+)";
                Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(HospitalBase);
                while (m.find()){
                    hospitalList.add(new Hospital(m.group(1), m.group(2), m.group(3), m.group(4)));
                }*/
                //for (int i=0; i<hospitalList.size(); i++){
                    //TODO 입력방법 변경?
                    //TODO https://codeday.me/ko/qa/20190308/21681.html
                //}
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = (TextView)findViewById(R.id.hospinfo);
                        tv.setText(hospitalList.toString());
                    }
                });
            }
        }).start();
    }

    String getAEDData(){
        StringBuffer buffer = new StringBuffer();

        String query = "http://apis.data.go.kr/B552657/AEDInfoInqireService/getAedLcinfoInqire?"
                + "WGS84_LON=" + lon
                + "&WGS84_LAT=" + lat
                + "&ServiceKey=" + APIKey
                + "&numOfRows=1";
        Log.i("url", query);
        try {
            URL url = new URL(query);
            InputStream inputStream = url.openStream();

            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPP = xmlFactory.newPullParser();
            xmlPP.setInput(new InputStreamReader(inputStream, "UTF-8"));

            String tag;

            xmlPP.next();
            int eventType = xmlPP.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("Near AED Address\n\n");
                        break;

                    case XmlPullParser.START_TAG:
                        tag = xmlPP.getName();

                        if (tag.equals("item"));
                        else if (tag.equals("buildAddress")) {
                            buffer.append("AED 주소: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        } else if (tag.equals("buildPlace")) {
                            buffer.append("설치 장소: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        } else if (tag.equals("distance")) {
                            buffer.append("거리: ");
                            xmlPP.next();
                            String dt = xmlPP.getText();
                            float dis = Float.parseFloat(dt);
                            float diss = dis*1000;
                            int dist = (int)diss;
                            String distan = String.valueOf(dist);
                            buffer.append(distan);
                            buffer.append("m\n");
                        }
                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xmlPP.getName();
                        if (tag.equals("item")) {
                            buffer.append("\n");
                        }
                        break;
                }
                eventType = xmlPP.next();
                }
            } catch (Exception e){
            e.printStackTrace();
        }
        buffer.append("");
        return buffer.toString();
    }

    String getHospData11(){
        StringBuffer buffer = new StringBuffer();
        EditText radiusET = (EditText)findViewById(R.id.hospRadius);
        radius = radiusET.getText().toString();
        if (radius.matches("")){
            SearchingRadius = "5000";
        } else {
            SearchingRadius = radius;
        }

        String query = "http://apis.data.go.kr/B551182/hospInfoService/getHospBasisList?serviceKey=" + APIKey
                + "&pageNo=1"
                + "&numOfRows=5"
                + "&zipCd=2010" //종합병원
                + "&clCd=11" //종합병원
                + "&dgsbjtCd=24" //응급의학과
                + "&xPos=" + lon
                + "&yPos=" + lat
                + "&radius=" + SearchingRadius;
        Log.i("url", query);
        try {
            URL url = new URL(query);
            InputStream inputStream = url.openStream();

            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPP = xmlFactory.newPullParser();
            xmlPP.setInput(new InputStreamReader(inputStream, "UTF-8"));

            String tag;

            xmlPP.next();
            int eventType = xmlPP.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("Near AED Address\n\n");
                        break;

                    case XmlPullParser.START_TAG:
                        tag = xmlPP.getName();

                        if (tag.equals("item"));
                        else if (tag.equals("addr")) {
                            buffer.append("병원 주소: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        } else if (tag.equals("distance")) {
                            buffer.append("거리: ");
                            xmlPP.next();
                            String dt = xmlPP.getText();
                            double dis = Double.parseDouble(dt);
                            int dist = (int)dis;
                            String distan = String.valueOf(dist);
                            buffer.append(distan);
                            buffer.append("m\n");
                        } else if (tag.equals("telno")) {
                            buffer.append("전화번호: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        } else if (tag.equals("yadmNm")) {
                            buffer.append("병원명: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        }
                        break;
                    //건강보험심사평가원 데이터 수정작업 진행중, 순차정렬 관련해서는 진행 안됨

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xmlPP.getName();
                        if (tag.equals("item")) {
                            buffer.append("\n");
                        }
                        break;
                }
                eventType = xmlPP.next();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        buffer.append("");
        return buffer.toString();
    }

    String getHospData01(){
        StringBuffer buffer = new StringBuffer();
        EditText radiusET = (EditText)findViewById(R.id.hospRadius);
        radius = radiusET.getText().toString();
        if (radius.matches("")){
            SearchingRadius = "5000";
        } else {
            SearchingRadius = radius;
        }

        String query = "http://apis.data.go.kr/B551182/hospInfoService/getHospBasisList?serviceKey=" + APIKey
                + "&pageNo=1"
                + "&numOfRows=5"
                + "&zipCd=2010" //종합병원
                + "&clCd=01" //상급종합병원
                + "&dgsbjtCd=24" //응급의학과
                + "&xPos=" + lon
                + "&yPos=" + lat
                + "&radius=" + SearchingRadius;
        Log.i("url", query);
        try {
            URL url = new URL(query);
            InputStream inputStream = url.openStream();

            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPP = xmlFactory.newPullParser();
            xmlPP.setInput(new InputStreamReader(inputStream, "UTF-8"));

            String tag;

            xmlPP.next();
            int eventType = xmlPP.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("Near AED Address\n\n");
                        break;

                    case XmlPullParser.START_TAG:
                        tag = xmlPP.getName();

                        if (tag.equals("item"));
                        else if (tag.equals("addr")) {
                            buffer.append("병원 주소: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        } else if (tag.equals("distance")) {
                            buffer.append("거리: ");
                            xmlPP.next();
                            String dt = xmlPP.getText();
                            double dis = Double.parseDouble(dt);
                            int dist = (int)dis;
                            String distan = String.valueOf(dist);
                            buffer.append(distan);
                            buffer.append("m\n");
                        } else if (tag.equals("telno")) {
                            buffer.append("전화번호: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        } else if (tag.equals("yadmNm")) {
                            buffer.append("병원명: ");
                            xmlPP.next();
                            buffer.append(xmlPP.getText());
                            buffer.append("\n");
                        }
                        break;
                    //건강보험심사평가원 데이터 수정작업 진행중, 순차정렬 관련해서는 진행 안됨

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xmlPP.getName();
                        if (tag.equals("item")) {
                            buffer.append("\n");
                        }
                        break;
                }
                eventType = xmlPP.next();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        buffer.append("");
        return buffer.toString();
    }

    /*class Hospital{
        public Hospital(String nam, String teln, String dis, String addr) {
            String name = nam;
            String tel = teln;
            String distance = dis;
            String address = addr;
        }
    }*/

    class Hospital {
        String name;
        String tel;
        int  distance;
        String addr;
    }

    public class makeList(String responce) {
        String regex = "병원 주소: (.+)\\n거리: (.+)m\\n전화번호: (.+)\\n병원명: (.+)";
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(HospitalBase);
        while (m.find()){
                hospitalList.add(new Hospital(m.group(4),m.group(3), Integer.parseInt(m.group(2)), m.group(1)));
            }
        }
    }

    /*String makeList(){
        String regex = "병원 주소: (.+)\\n거리: (.+)m\\n전화번호: (.+)\\n병원명: (.+)";
        for (FieldClassification.Match match : regex.matches(HospitalBase)){
            hospitalList.add(new Hospital())
        }
    }*/
}