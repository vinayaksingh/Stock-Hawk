package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.ui.MainActivity;

/**
 * Created by vin on 03/04/17.
 */

public class StockWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // set the adapter [by id] contained in remote view with data via Remote View Service.
            views.setRemoteAdapter(R.id.widget_list,
                    new Intent(context, StockWidgetRemoteViewService.class));

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(new Intent(context, DetailActivity.class))
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            /*
            * When using collections (eg. ListView, StackView etc.) in widgets, it is very costly to set PendingIntents on the individual items, and is hence not permitted.
            * Instead this method should be used to set a single PendingIntent template on the collection, and individual items can differentiate their on-click behavior using setOnClickFillInIntent(int, Intent).
            *
            * We are setting pending intent on click of list items.
            * */
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntentTemplate);
            // Set error string when list is empty.
            views.setEmptyView(R.id.widget_list, R.id.empty);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        if (QuoteSyncJob.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            // refresh list (identified by list id , for all widget instances)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }
    }
}
