package dk.aau.cs.psylog.analysis.sleepstationary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dk.aau.cs.psylog.module_lib.DBAccessContract;


/**
 * Created by Praetorian on 24-03-2015.
 */
public class AmplitudeSleepAnalysis {

    Queue<AmplitudeData> previousDataQueue= new LinkedList<>();
    ContentResolver contentResolver;

    public AmplitudeSleepAnalysis(ContentResolver contentResolver){
        this.contentResolver = contentResolver;
    }

    private List<AmplitudeData> loadData()
    {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SOUND_amplitudes");
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","amplitude", "time"},null,null,null);
        List<AmplitudeData> returnList= new ArrayList<>();
        if((getLastPosition() > 5 && cursor.moveToPosition(getLastPosition()- 5)) || cursor.moveToFirst())
        {
            do{
                float amplitude = cursor.getFloat(cursor.getColumnIndex("amplitude"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                lastPos = cursor.getInt(cursor.getColumnIndex("_id"));
                returnList.add(new AmplitudeData(amplitude, time));
            }while(cursor.moveToNext());
            cursor.close();
        }
        return returnList;

    }

    //Logistic function
    private float probabilityFunc(float t)
    {
    	float k = 2.0f;
    	float res = (float)(1.0/(1.0+ Math.exp(-k*(t - 3.0))));
    	if(res > 1.0f)
    		return 1.0f;
    	return res;
    }
    Date oldTime;
    public LinkedHashMap<String, Float> Analyse()
    {
        List<AmplitudeData> data = loadData();
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
        try {
            oldTime = convertTimeString(loadTimeString());
        }
        catch (Exception e){
            try {
                oldTime = convertTimeString(data.get(0).time);
            }catch (NullPointerException el)
            {
                return null;
            }
        }
        for(AmplitudeData amplitudeData : data)
        {
            Date newTime = convertTimeString(amplitudeData.time);
            if(isStationary(amplitudeData))
            {
                float timeElapsed= (float)((newTime.getTime() - oldTime.getTime())/(60.0*60.0*1000.0));
                probabilitySleeping = probabilityFunc(timeElapsed);
            }
            else
            {
                probabilitySleeping = 0.0f;
                oldTime = newTime;
            }
            returnMap.put(amplitudeData.time, probabilitySleeping);
            previousDataQueue.remove();
            previousDataQueue.add(amplitudeData);
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

    private boolean isStationary(AmplitudeData accelerationData)
    {
        boolean b = true;
        for(AmplitudeData toConsider : previousDataQueue)
        {
            if(outOfThreshHold(accelerationData, toConsider))
                b = false;
        }
        return b;
    }

    private boolean outOfThreshHold(AmplitudeData acc1, AmplitudeData acc2)
    {
        return Math.abs(acc1.amplitude - acc2.amplitude) > 3000.0f;
    }

    private List<AmplitudeData> makeMovingAverage(List<AmplitudeData> data)
    {
        if(data.size() == 0)
            return null;
        List<AmplitudeData> returnList = new ArrayList<>();
        AmplitudeData MAOld = data.get(0);
        float alpha = 0.1f;
        for(AmplitudeData element : data)
        {
        	AmplitudeData MAnew = element;
            MAnew.amplitude = alpha* MAnew.amplitude + (1-alpha)*MAOld.amplitude;
            returnList.add(MAnew);
            MAOld = MAnew;
        }
        return returnList;
    }

    private String loadTimeString() throws Exception {
        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        Cursor cursor = contentResolver.query(uri, new String[]{"timeAmpl"}, null, null, null);
        if(cursor.moveToFirst())
        {
            String res = cursor.getString(cursor.getColumnIndex("timeAmpl"));
            if(res == null || res == "")
            {
                throw new Exception("No Time located");
            }
            cursor.close();
            return  res;
        }
        else
        {
            throw new Exception("No Time located");
        }
    }
    private int lastPos = -1;
    private int getLastPosition()
    {

        Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_state");
        Cursor cursor = contentResolver.query(uri, new String[]{"positionAmpl"}, null, null, null);
        if(cursor.moveToFirst())
        {
            return cursor.getInt(cursor.getColumnIndex("positionAmpl"));
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
        values.put("positionAmpl", lastPos);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        values.put("timeAmpl", df.format(oldTime));
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "positionAcc", "positionAmpl", "timeAcc" , "timeAmpl"}, null, null, null);
        if(cursor.getCount() > 0)
        {
            contentResolver.update(uri, values, "1=1", null);
        }
        else
        {
            values.put("positionAcc", 1);
            contentResolver.insert(uri, values);
        }
    }
}

