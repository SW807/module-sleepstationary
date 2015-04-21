package dk.aau.cs.psylog.analysis.sleepstationary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import dk.aau.cs.psylog.module_lib.DBAccessContract;
import dk.aau.cs.psylog.module_lib.IScheduledTask;

/**
 * Created by Praetorian on 24-03-2015.
 */
public class StationaryAnalysis implements IScheduledTask{

    ContentResolver contentResolver;

    public StationaryAnalysis(Context context)
    {
        contentResolver = context.getContentResolver();
    }

    private Pair<Integer,Integer> getLastPos(){
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        Cursor cursor = contentResolver.query(uri, new String[]{"accPos", "amplPos"}, null, null, null);
        if(cursor.moveToFirst())
        {
            return new Pair<Integer,Integer>(cursor.getInt(cursor.getColumnIndex("accPos")),cursor.getInt(cursor.getColumnIndex("accPos")));
        }
        else
        {
            return new Pair<Integer,Integer>(0,0);
        }
    }

    int lastPosAcc = 0;
    private Map<String,Float> loadAcc(int lastPos){
        Map<String,Float> resultMap = new HashMap<String, Float>();
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "ACCELERATIONSLEEPANALYSIS_sleepcalc");
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "prob", "time"},"_id >= " + lastPos, null,null);
        if(cursor.moveToFirst()){
            do{
                float prob = cursor.getFloat(cursor.getColumnIndex("prob"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                lastPosAcc = cursor.getInt(cursor.getColumnIndex("_id"));
                resultMap.put(time, prob);
            }while (cursor.moveToNext());
            cursor.close();
        }
        return resultMap;
    }

    int lastPosAmpl = 0;
    private Map<String,Float> loadAmpl(int lastPos){
        Map<String,Float> resultMap = new HashMap<String, Float>();
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SOUNDSLEEPANALYSIS_sleepcalc");
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "prob", "time"},"_id >= " + lastPos, null,null);
        if(cursor.moveToFirst()){
            do{
                float prob = cursor.getFloat(cursor.getColumnIndex("prob"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                lastPosAmpl = cursor.getInt(cursor.getColumnIndex("_id"));
                resultMap.put(time, prob);
            }while (cursor.moveToNext());
            cursor.close();
        }
        return resultMap;
    }

    public void Analyse()
    {
        Pair<Integer,Integer> accAmplLastPos = getLastPos();
        Map<String, Float> resAcc = loadAcc(accAmplLastPos.first);
        Map<String, Float> resAmpl = loadAmpl(accAmplLastPos.first);

        try {
            //kombinering
            Map<String, Float> result = weightedAverage(resAcc, resAmpl);
            for(Map.Entry<String, Float> entry : result.entrySet())
            {
                insertData(entry.getValue(), entry.getKey());
            }

        }
        catch (ParseException e){Log.e("ERROR", e.getMessage());}

        reportState();
    }

    private Map<String, Float> weightedAverage(Map<String, Float> map1, Map<String,Float> map2) throws ParseException
    {
        LinkedHashMap<String, Float> resultMap = new LinkedHashMap<String, Float>();
        Iterator<Map.Entry<String, Float>> it1 = map1.entrySet().iterator();
        Iterator<Map.Entry<String, Float>> it2 = map2.entrySet().iterator();
        Map.Entry<String, Float> ele1 = it1.next();
        Map.Entry<String, Float> ele2 = it2.next();
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        while(it1.hasNext() && it2.hasNext())
        {
            float combination = 0.3f*ele1.getValue() + 0.7f*ele2.getValue();
            Date d1 = sdf.parse(ele1.getKey());
            Date d2 = sdf.parse(ele2.getKey());
            if(d1.getTime() < d2.getTime())
            {
                resultMap.put(ele1.getKey(), combination);
                ele1 = it1.next();
            }
            else if(d2.getTime() < d1.getTime())
            {
                resultMap.put(ele2.getKey(), combination);
                ele2 = it2.next();
            }
            else
            {
                resultMap.put(ele1.getKey(), combination);
                ele1 = it1.next();
                ele2 = it2.next();
            }
        }

        while(it1.hasNext())
        {
            float combination = 0.3f*ele1.getValue() + 0.7f*ele2.getValue();
            resultMap.put(ele1.getKey(), combination);
            ele1 = it1.next();
        }
        while(it2.hasNext())
        {
            float combination = 0.3f*ele1.getValue() + 0.7f*ele2.getValue();
            resultMap.put(ele2.getKey(), combination);
            ele2 = it2.next();
        }
        return resultMap;
    }

    private void insertData(float probability, String time)
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_sleepcalc");
        ContentValues values = new ContentValues();
        values.put("prob", probability);
        values.put("time", time);
        contentResolver.insert(uri,values);
    }

    private void reportState(){
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        ContentValues values = new ContentValues();
        values.put("accPos", lastPosAcc);
        values.put("amplPos", lastPosAmpl);
        contentResolver.insert(uri, values);
    }


    @Override
    public void doTask() {
        Log.i("Analyse soevn", "analysen startet");
        Analyse();
        Log.i("Analyse soevn", "analysen færdig");
    }

    @Override
    public void setParameters(Intent i) {

    }
}

