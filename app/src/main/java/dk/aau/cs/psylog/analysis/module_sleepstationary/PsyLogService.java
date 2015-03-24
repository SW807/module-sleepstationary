package dk.aau.cs.psylog.analysis.module_sleepstationary;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import dk.aau.cs.psylog.module_lib.ISensor;
import dk.aau.cs.psylog.module_lib.SensorService;

/**
 * Created by Praetorian on 24-03-2015.
 */
public class PsyLogService extends Service{
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
