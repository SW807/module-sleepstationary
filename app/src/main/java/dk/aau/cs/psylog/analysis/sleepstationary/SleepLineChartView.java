package dk.aau.cs.psylog.analysis.sleepstationary;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import dk.aau.cs.psylog.module_lib.DBAccessContract;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class SleepLineChartView extends LineChartView {

    ContentResolver contentResolver;
    public SleepLineChartView(Context context) {
        super(context);
        contentResolver = context.getContentResolver();
        this.setInteractive(true);
        this.setZoomType(ZoomType.HORIZONTAL);
        this.setLineChartData(setupChart());
    }

    public LineChartData setupChart() {
        Line line = new Line(getDBData()).setColor(Color.BLUE).setCubic(true);
        List<Line> lines = new ArrayList<>();
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setBaseValue(Float.NEGATIVE_INFINITY);
        data.setLines(lines);
        List<AxisValue> axisValues = new ArrayList<>();
        int i = 0;
        for(PointValue p : line.getValues()) {
            if(i % 5 == 0) {
                String label = String.valueOf(p.getLabelAsChars());
                axisValues.add(new AxisValue(i).setLabel(label));
            }

            i++;
        }
        Axis axisX = new Axis(axisValues).setHasLines(true);
        axisX.setTextSize(8);

        Axis axisY = new Axis().setHasLines(true);
        axisX.setName("Tid");
        axisY.setName("Sandsynlighed For Søvn");
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);

        return data;
    }

    public ArrayList<PointValue> getDBData()
    {
        try {
            ArrayList<PointValue> returnList = new ArrayList<>();
            Uri uri = Uri.parse(DBAccessContract.DBACCESS_CONTENTPROVIDER + "SLEEPSTATIONARY_sleepcalc");

            Cursor cursor = contentResolver.query(uri, new String[]{"prob", "time"}, null, null, null);
            int i = 0;

            if (cursor.moveToFirst()) {
                do {
                    if(i % 5 == 0) {
                        float probSleeping = cursor.getFloat(cursor.getColumnIndex("prob"));
                        String time = cursor.getString(cursor.getColumnIndex("time"));
                        returnList.add(new PointValue(i, probSleeping).setLabel(time));
                    }
                    i++;
                }
                while (cursor.moveToNext());

            }
            return returnList;
        }
        catch (Exception e)
        {
            return new ArrayList<>();
        }
    }
}
