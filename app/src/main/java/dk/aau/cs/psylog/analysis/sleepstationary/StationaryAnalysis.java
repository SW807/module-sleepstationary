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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import dk.aau.cs.psylog.module_lib.DBAccessContract;
import dk.aau.cs.psylog.module_lib.IScheduledTask;

public class StationaryAnalysis implements IScheduledTask {

    ContentResolver contentResolver;

    public StationaryAnalysis(Context context) {
        contentResolver = context.getContentResolver();
    }

    private Pair<Integer, Integer> getLastPos() {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        Cursor cursor = contentResolver.query(uri, new String[]{"accPos", "amplPos"}, null, null, null);
        if (cursor.moveToFirst()) {
            return new Pair<Integer, Integer>(cursor.getInt(cursor.getColumnIndex("accPos")), cursor.getInt(cursor.getColumnIndex("amplPos")));
        } else {
            return new Pair<Integer, Integer>(0, 0);
        }
    }

    int lastPosAcc = 0;

    private List<Pair<String, Float>> loadAcc(int lastPos) {
        List<Pair<String, Float>> resultMap = new ArrayList<Pair<String, Float>>();
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "ACCELERATIONSLEEPANALYSIS_sleepcalc");
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "prob", "time"}, "_id >= " + lastPos, null, "_id");
        if (cursor.moveToFirst()) {
            do {
                float prob = cursor.getFloat(cursor.getColumnIndex("prob"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                lastPosAcc = cursor.getInt(cursor.getColumnIndex("_id"));
                resultMap.add(new Pair<String, Float>(time, prob));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return resultMap;
    }

    int lastPosAmpl = 0;

    private List<Pair<String, Float>> loadAmpl(int lastPos) {
        List<Pair<String, Float>> resultMap = new ArrayList<Pair<String, Float>>();
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SOUNDSLEEPANALYSIS_sleepcalc");
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "prob", "time"}, "_id >= " + lastPos, null, "_id");
        if (cursor.moveToFirst()) {
            do {
                float prob = cursor.getFloat(cursor.getColumnIndex("prob"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                lastPosAmpl = cursor.getInt(cursor.getColumnIndex("_id"));
                resultMap.add(new Pair<String, Float>(time, prob));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return resultMap;
    }

    public void Analyse() {
        Pair<Integer, Integer> accAmplLastPos = getLastPos();
        List<Pair<String, Float>> resAcc = loadAcc(accAmplLastPos.first);
        List<Pair<String, Float>> resAmpl = loadAmpl(accAmplLastPos.second);

        try {
            //kombinering
            List<Pair<String, Float>> result = weightedAverage(resAcc, resAmpl);
            for (Pair<String, Float> entry : result) {
                insertData(entry.second, entry.first);
            }

        } catch (ParseException e) {
            Log.e("ERROR", e.getMessage());
        }

        reportState();
    }

    private List<Pair<String, Float>> weightedAverage(List<Pair<String, Float>> map1, List<Pair<String, Float>> map2) throws ParseException {
        List<Pair<String, Float>> resultMap = new ArrayList<Pair<String, Float>>();
        Iterator<Pair<String, Float>> it1 = map1.iterator();
        Iterator<Pair<String, Float>> it2 = map2.iterator();
        Pair<String, Float> ele1 = it1.next();
        Pair<String, Float> ele2 = it2.next();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        while (it1.hasNext() && it2.hasNext()) {
            float combination = 0.3f * ele1.second + 0.7f * ele2.second;
            Date d1 = sdf.parse(ele1.first);
            Date d2 = sdf.parse(ele2.first);
            if (d1.getTime() < d2.getTime()) {
                resultMap.add(new Pair<String, Float>(ele1.first, combination));
                ele1 = it1.next();
            } else if (d2.getTime() < d1.getTime()) {
                resultMap.add(new Pair<String, Float>(ele2.first, combination));
                ele2 = it2.next();
            } else {
                resultMap.add(new Pair<String, Float>(ele1.first, combination));
                ele1 = it1.next();
                ele2 = it2.next();
            }
        }

        while (it1.hasNext()) {
            float combination = 0.3f * ele1.second + 0.7f * ele2.second;
            resultMap.add(new Pair<String, Float>(ele1.first, combination));
            ele1 = it1.next();
        }
        while (it2.hasNext()) {
            float combination = 0.3f * ele1.second + 0.7f * ele2.second;
            resultMap.add(new Pair<String, Float>(ele2.first, combination));
            ele2 = it2.next();
        }
        return resultMap;
    }

    private void insertData(float probability, String time) {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_sleepcalc");
        ContentValues values = new ContentValues();
        values.put("prob", probability);
        values.put("time", time);
        contentResolver.insert(uri, values);
    }

    private void reportState() {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        ContentValues values = new ContentValues();
        values.put("accPos", lastPosAcc);
        values.put("amplPos", lastPosAmpl);

        Cursor cursor = contentResolver.query(uri, new String[]{"accPos", "amplPos"}, null, null, null);
        if (cursor.getCount() > 0) {
            contentResolver.update(uri, values, "1=1", null);
        } else {
            contentResolver.insert(uri, values);
        }
        cursor.close();
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

