package com.prakash.pnotes.widget

import android.content.Intent
import android.widget.RemoteViewsService

class NoteWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetRemoteViewsFactory(applicationContext, intent)
    }
}
