package dk.aau.cs.psylog.analysis.sleepstationary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dk.aau.cs.psylog.module_lib.DBAccessContract;

/**
 * Created by Praetorian on 24-03-2015.
 */
public class StationaryAnalysis {

    Queue<AccelerationData> previousDataQueue= new LinkedList<>();
    ContentResolver contentResolver;

    public StationaryAnalysis(Context context)
    {
        contentResolver = context.getContentResolver();

    }

    private List<AccelerationData> loadData()
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "accelerometer_accelerations");
        Cursor cursor = contentResolver.query(uri, new String[]{"accX", "accY", "accZ", "time"},null,null,null);
        List<AccelerationData> returnList= new ArrayList<>();
        if(cursor.moveToFirst())
        {
            do{
                float accX = cursor.getFloat(cursor.getColumnIndex("accX"));
                float accY = cursor.getFloat(cursor.getColumnIndex("accY"));
                float accZ = cursor.getFloat(cursor.getColumnIndex("accZ"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                returnList.add(new AccelerationData(accX, accY, accZ, time));
            }while(cursor.moveToNext());
        }
        return returnList;

    }
    private float probabilityFunc(float t)
    {
        float b = (float)(4/Math.log10(2));
        return (-1.0f / ((float)Math.exp(t/b))) + 1.0f;
    }
    public void Analyse()
    {
        Log.e("LARSALS", "analyse kaldt");
        List<AccelerationData> data = loadData();

        if(data.size() > 5)
        {
            data = makeMovingAverage(data.subList(5, data.size()-1));
            for(int i = 0; i < 5; i++)
                previousDataQueue.add(data.get(i));
        }
        else
        {
            return;
        }
        float probabilitySleeping = 0.0f;
        Date oldTime = convertTimeString(data.get(0).time);
        for(AccelerationData acc : data)
        {
            Date newTime = convertTimeString(acc.time);
            if(isStationary(acc))
            {
                float timeElapsed= (newTime.getTime() - oldTime.getTime())/(60.0f*60.0f*1000.0f);

                probabilitySleeping = probabilityFunc(timeElapsed);
            }
            else
            {
                probabilitySleeping = 0.0f;
                oldTime = newTime;
            }
            reportState(probabilitySleeping, acc.time);
        }
        Log.e("LARSALS", "analyseslut");
    }
    private Date convertTimeString(String s){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date convertedTime = new Date();
        try {
            convertedTime = dateFormat.parse(s);
        }catch (ParseException e){
            e.printStackTrace();
        }
        return convertedTime;
    }

    private boolean isStationary(AccelerationData accelerationData)
    {
        boolean b = true;
        for(AccelerationData toConsider : previousDataQueue)
        {
            if(outOfThreshHold(accelerationData, toConsider))
                b = false;
        }
        return b;
    }

    private boolean outOfThreshHold(AccelerationData acc1, AccelerationData acc2)
    {
        return Math.abs(acc1.accX - acc2.accX) > 0.5f ||
               Math.abs(acc1.accY - acc2.accY) > 0.5f ||
               Math.abs(acc1.accZ - acc2.accZ) > 0.5f;
    }

    private List<AccelerationData> makeMovingAverage(List<AccelerationData> data)
    {
        if(data.size() == 0)
            return null;
        List<AccelerationData> returnList = new ArrayList<>();
        AccelerationData MAOld = data.get(0);
        float alpha = 0.1f;
        for(AccelerationData element : data)
        {
            AccelerationData MAnew = element;
            MAnew.accX = alpha* MAnew.accX + (1-alpha)*MAOld.accX;
            MAnew.accY = alpha* MAnew.accY + (1-alpha)*MAOld.accY;
            MAnew.accZ = alpha* MAnew.accZ + (1-alpha)*MAOld.accZ;
            returnList.add(MAnew);
            MAOld = MAnew;
        }
        return returnList;
    }

    private void reportState(float probability, String time)
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_sleepcalc");
        ContentValues values = new ContentValues();
        values.put("prob", probability);
        values.put("time", time);
        contentResolver.insert(uri, values);
    }
}
