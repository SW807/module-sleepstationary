package dk.aau.cs.psylog.analysis.module_sleepstationary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Praetorian on 24-03-2015.
 */
public class StationaryAnalysis {

    Queue<AccelerationData> previousDataQueue= new LinkedList<>();

    List<AccelerationData> data;
    public StationaryAnalysis(List<AccelerationData> data)
    {
        if(data.size() > 5)
        {
            for(int i = 0; i < 5; i++)
                previousDataQueue.add(data.get(i));
        }
        this.data = makeMovingAverage(data.subList(5, data.size()-1));
    }

    private float probabilityFunc(float t)
    {
        float b = (float)(14400/Math.log(2));

        return (-1.0f / ((float)Math.exp(t/b))) + 1.0f;
    }
    public void Analyse()
    {
        if(previousDataQueue.isEmpty())
            return;

        float timeElapsed = 0.0f;
        float probabilitySleeping = 0.0f;
        Date oldTime = convertTimeString(data.get(0).time);
        for(AccelerationData acc : data)
        {
            Date newTime = convertTimeString(acc.time);
            if(isStationary(acc))
            {
                float deltaTime= (newTime.getTime() - oldTime.getTime())/1000.0f;
                timeElapsed += deltaTime;
                probabilitySleeping = probabilityFunc(timeElapsed);
            }
            else
            {
                timeElapsed = 0.0f;
                probabilitySleeping = 0.0f;
            }
            reportState(probabilitySleeping, acc.time);
            oldTime = newTime;
        }
    }
    private Date convertTimeString(String s){
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss");
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

    }
}
