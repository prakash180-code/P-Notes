package com.prakash.pnotes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.prakash.pnotes.MainActivity
import com.prakash.pnotes.R
import com.prakash.pnotes.di.AppModule
import com.prakash.pnotes.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NoteWidgetRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsFactory {

    private val appWidgetId: Int =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    private var notes: List<Note> = emptyList()

    override fun onCreate() {
        loadNotes()
    }

    override fun onDataSetChanged() {
        loadNotes()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in notes.indices) {
            return RemoteViews(context.packageName, R.layout.widget_item_note)
        }
        val note = notes[position]
        return RemoteViews(context.packageName, R.layout.widget_item_note).apply {
            setTextViewText(
                R.id.widget_item_title,
                note.title.ifBlank { context.getString(R.string.widget_untitled) }
            )
            setTextViewText(
                R.id.widget_item_content,
                if (note.content.isNotBlank()) {
                    note.content
                } else {
                    context.getString(R.string.widget_empty_content)
                }
            )

            val fillInIntent = Intent().apply {
                putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
            }
            setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        notes.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadNotes() {
        notes = try {
            val repository = AppModule.getRepository(context)
            runBlocking {
                repository.getActiveNotes().first()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
