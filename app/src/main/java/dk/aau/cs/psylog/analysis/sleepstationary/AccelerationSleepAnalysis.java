package dk.aau.cs.psylog.analysis.sleepstationary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import dk.aau.cs.psylog.module_lib.DBAccessContract;


/**
 * Created by Praetorian on 24-03-2015.
 */
public class AccelerationSleepAnalysis {

    Queue<AccelerationData> previousDataQueue= new LinkedList<>();
    ContentResolver contentResolver;

    public AccelerationSleepAnalysis(ContentResolver contentResolver)
    {
        this.contentResolver = contentResolver;
    }


    private List<AccelerationData> loadData()
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "accelerometer_accelerations");
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","accX", "accY", "accZ", "time"},null,null,null);
        List<AccelerationData> returnList= new ArrayList<>();
        if((getLastPosition() > 5 && cursor.moveToPosition(getLastPosition()- 5)) || cursor.moveToFirst())
        {
            do{
                float accX = cursor.getFloat(cursor.getColumnIndex("accX"));
                float accY = cursor.getFloat(cursor.getColumnIndex("accY"));
                float accZ = cursor.getFloat(cursor.getColumnIndex("accZ"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                lastPos = cursor.getInt(cursor.getColumnIndex("_id"));
                returnList.add(new AccelerationData(accX, accY, accZ, time));
            }while(cursor.moveToNext());
            cursor.close();
        }
        return returnList;

    }
    private float probabilityFunc(float t)
    {
    	float k = 1.0f;
    	float res = (float)(1.0/(1.0+ Math.exp(-k*(t - 4.0))));
    	if(res > 1.0f)
    		return 1.0f;
    	return res;
    }
    public LinkedHashMap<String, Float> Analyse()
    {
        List<AccelerationData> data = loadData();
    	LinkedHashMap<String,Float> returnMap = new LinkedHashMap<String,Float>();
        if(data.size() > 5)
        {
            for(int i = 0; i < 5; i++)
                previousDataQueue.add(data.get(i));
            data = makeMovingAverage(data.subList(5, data.size()-1));
        }
        else
        {
            return null;
        }
        float probabilitySleeping = 0.0f;
        Date oldTime = convertTimeString(data.get(0).time);
        for(AccelerationData acc : data)
        {
            Date newTime = convertTimeString(acc.time);
            if(isStationary(acc))
            {
                float timeElapsed= (float)((newTime.getTime() - oldTime.getTime())/(60.0*60.0*1000.0));
                probabilitySleeping = probabilityFunc(timeElapsed);
            }
            else
            {
                probabilitySleeping = 0.0f;
                oldTime = newTime;
            }
            returnMap.put(acc.time, probabilitySleeping);
            //reportState(probabilitySleeping, acc.time);
            previousDataQueue.remove();
            previousDataQueue.add(acc);
        }
        updatePosition();
        return returnMap;
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

    private int lastPos = -1;
    private int getLastPosition()
    {

        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        Cursor cursor = contentResolver.query(uri, new String[]{"positionAcc"}, null, null, null);
        if(cursor.moveToFirst())
        {
            return cursor.getInt(cursor.getColumnIndex("positionAcc"));
        }
        else
        {
            return 0;
        }
    }

    private void updatePosition()
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        ContentValues values = new ContentValues();
        values.put("positionAcc", lastPos);
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "positionAcc", "positionAmpl"}, null, null, null);
        if(cursor.getCount() > 0)
        {
            contentResolver.update(uri, values, "1=1", null);
        }
        else
        {
            values.put("positionAmpl", 0);
            contentResolver.insert(uri, values);
        }
    }
}

