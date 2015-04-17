package dk.aau.cs.psylog.analysis.sleepstationary;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
    public void Analyse()
    {
        Log.i("Analyse soevn", "analysen startet");
        AccelerationSleepAnalysis accelerationSleepAnalysis = new AccelerationSleepAnalysis(contentResolver);
        AmplitudeSleepAnalysis amplitudeSleepAnalysis = new AmplitudeSleepAnalysis(contentResolver);
        Map<String, Float> resAcc = accelerationSleepAnalysis.Analyse();
        Map<String, Float> resAmpl = amplitudeSleepAnalysis.Analyse();
        if(resAcc ==  null && resAmpl == null)
            return;
        else if(resAcc == null)
        {
            for(Map.Entry<String, Float> entry : resAmpl.entrySet())
            {
                reportState(entry.getValue(), entry.getKey());
            }
            return;
        }
        else if(resAmpl == null)
        {
            for(Map.Entry<String, Float> entry : resAcc.entrySet())
            {
                reportState(entry.getValue(), entry.getKey());
            }
            return;
        }

        try {
            //kombinering
            Map<String, Float> result = weightedAverage(resAcc, resAmpl);
            for(Map.Entry<String, Float> entry : result.entrySet())
            {
                reportState(entry.getValue(), entry.getKey());
            }

        }
        catch (ParseException e){Log.e("ERROR", e.getMessage());}

    }

    private Map<String, Float> weightedAverage(Map<String, Float> map1, Map<String,Float> map2) throws ParseException
    {
        LinkedHashMap<String, Float> resultMap = new LinkedHashMap<String, Float>();
        Iterator<Map.Entry<String, Float>> it1 = map1.entrySet().iterator();
        Iterator<Map.Entry<String, Float>> it2 = map2.entrySet().iterator();
        Map.Entry<String, Float> ele1 = it1.next();
        Map.Entry<String, Float> ele2 = it2.next();
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
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

    private void reportState(float probability, String time)
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_sleepcalc");
        ContentValues values = new ContentValues();
        values.put("prob", probability);
        values.put("time", time);
        contentResolver.insert(uri,values);
    }



    @Override
    public void doTask() {
        Analyse();
        Log.i("Analyse soevn", "analysen færdig");
    }

    @Override
    public void setParameters(Intent i) {

    }
}

