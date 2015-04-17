package dk.aau.cs.psylog.analysis.sleepstationary;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import dk.aau.cs.psylog.module_lib.DBAccessContract;
import dk.aau.cs.psylog.module_lib.ScheduledService;

/**
 * Created by Praetorian on 24-03-2015.
 */
public class PsyLogService extends ScheduledService {

    public PsyLogService() {
        super("StationarySleep");
    }

    @Override
    public void setScheduledTask() {
        this.scheduledTask = new StationaryAnalysis(this);
    }
}
