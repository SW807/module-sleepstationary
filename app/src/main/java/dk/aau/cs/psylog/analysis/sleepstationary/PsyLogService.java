package dk.aau.cs.psylog.analysis.sleepstationary;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Praetorian on 24-03-2015.
 */
public class PsyLogService extends Service{
    StationaryAnalysis analysis;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        analysis = new StationaryAnalysis(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startid)
    {
        super.onStartCommand(intent, flag, startid);
        analysis.Analyse();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        super.onDestroy();
    }

}
