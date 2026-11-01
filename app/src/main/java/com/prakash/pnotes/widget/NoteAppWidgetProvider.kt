package com.prakash.pnotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.prakash.pnotes.MainActivity
import com.prakash.pnotes.R

class NoteAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
    }

    companion object {
        /**
         * Rebuilds every placed widget and asks its list adapter to reload.
         * Called when notes change so the widget never shows stale data.
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, NoteAppWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            ids.forEach { appWidgetId ->
                updateAppWidget(context, manager, appWidgetId)
            }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val adapterIntent = Intent(context, NoteWidgetRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            // Fill-in intents (the tapped note id) are only merged into a mutable
            // PendingIntent, so the template must be mutable on Android 12+.
            var pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingFlags = pendingFlags or PendingIntent.FLAG_MUTABLE
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                pendingFlags
            )

            val views = RemoteViews(context.packageName, R.layout.note_widget).apply {
                setRemoteAdapter(R.id.widget_list, adapterIntent)
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                setPendingIntentTemplate(R.id.widget_list, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
