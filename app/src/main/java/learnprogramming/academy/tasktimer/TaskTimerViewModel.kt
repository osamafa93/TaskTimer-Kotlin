package learnprogramming.academy.tasktimer

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by timbuchalka for the Android Pie using Kotlin course
 * from www.learnprogramming.academy
 */

private const val TAG = "TaskTimerViewModel"

class TaskTimerViewModel (application: Application) : AndroidViewModel(application) {

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "contentObserver.onChange: called. uri is $uri")
            loadTasks()
        }
    }

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor: LiveData<Cursor>
        get() = databaseCursor

    init {
        Log.d(TAG, "TaskTimerViewModel: created")
        getApplication<Application>().contentResolver.registerContentObserver(TasksContract.CONTENT_URI,
                 true, contentObserver)
        loadTasks()
    }

    private fun loadTasks() {
        val projection = arrayOf(TasksContract.Columns.ID,
                TasksContract.Columns.TASK_NAME,
                TasksContract.Columns.TASK_DESCRIPTION,
                TasksContract.Columns.TASK_SORT_ORDER)
        // <order by> Tasks.SortOrder, Tasks.Name
        val sortOrder = "${TasksContract.Columns.TASK_SORT_ORDER}, ${TasksContract.Columns.TASK_NAME}"

        GlobalScope.launch {
        val cursor = getApplication<Application>().contentResolver.query(
                TasksContract.CONTENT_URI,
                projection, null, null,
                sortOrder)
        databaseCursor.postValue(cursor)
        }
    }

    fun saveTask(task: Task): Task {
        val values = ContentValues()

        if (task.name.isNotEmpty()) {
            // Don't save a task wth no name
            values.put(TasksContract.Columns.TASK_NAME, task.name)
            values.put(TasksContract.Columns.TASK_DESCRIPTION, task.description)
            values.put(TasksContract.Columns.TASK_SORT_ORDER, task.sortOrder)  // defaults to zero if empty

            if (task.id == 0L) {
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: adding new task")
                    val uri = getApplication<Application>().contentResolver?.insert(TasksContract.CONTENT_URI, values)
                    if (uri != null) {
                        task.id = TasksContract.getId(uri)
                        Log.d(TAG, "saveTask: new id is ${task.id}")
                    }
                }
            } else {
                // task has an id, so we're updating
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: updating task")
                    getApplication<Application>().contentResolver?.update(TasksContract.buildUriFromId(task.id), values, null, null)
                }
            }
        }
        return task
    }

    fun deleteTask(taskId: Long) {
        Log.d(TAG, "Deleting task")
        GlobalScope.launch {
            getApplication<Application>().contentResolver?.delete(TasksContract.buildUriFromId(taskId), null, null)
        }
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}