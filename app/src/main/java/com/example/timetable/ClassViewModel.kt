package com.example.timetable

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.timetable.database.Class
import com.example.timetable.database.ClassDao
import com.example.timetable.database.ClassDatabase
import com.example.timetable.helpers.CalendarHelper
import com.example.timetable.helpers.ParseExcelFileHelper
import kotlinx.coroutines.*
import org.apache.xmlbeans.impl.piccolo.io.FileFormatException
import java.io.InputStream
import java.util.*
import kotlin.NoSuchElementException

class ClassViewModel(application: Application, private val state: SavedStateHandle): AndroidViewModel(application) {

    private var classDatabase = ClassDatabase.getInstance(application)
    private var classDao: ClassDao = classDatabase.classDao()
    private var groupCode: String = "ИВМО-02-20"

    var dropState: MainActivity.DropState
    var selectedDate: Calendar
    var loadErrorMessage: String? = null
    var fileUri: Uri? = null
        set(value) {
            field = value
            state.set(FILE_URI_KEY, field)
        }

    private val classListForDay: MutableLiveData<MutableList<Class>> = object:MutableLiveData<MutableList<Class>>() {}
    val loadDialogState: MutableLiveData<LoadDialogState> = object:MutableLiveData<LoadDialogState>() {}

    init {
        loadDialogState.postValue(LoadDialogState.NOT_SHOWN)
        selectedDate = if (state.contains(SELECTED_DATE_KEY)) {
            state.get(SELECTED_DATE_KEY)!!
        } else {
            Calendar.getInstance()
        }
        dropState = if (state.contains(DROP_STATE_KEY)) {
            state.get(DROP_STATE_KEY)!!
        } else {
            MainActivity.DropState.OPEN
        }
        if (state.contains(ERROR_MESSAGE_KEY)) {
            loadErrorMessage = state.get(ERROR_MESSAGE_KEY)!!
        }
        if (state.contains(FILE_URI_KEY)) {
            fileUri = state.get(FILE_URI_KEY)
        }
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        selectedDate.set(year, month, dayOfMonth)
        state.set(SELECTED_DATE_KEY, selectedDate)
    }

    fun getClassList(): MutableLiveData<MutableList<Class>> {
        return classListForDay
    }

    private suspend fun saveClassList(classList: MutableList<Class>) =
        viewModelScope.launch(Dispatchers.IO) {
            classDatabase.clearAllTables()
            classDao.insertAllClasses(classList)
        }

    fun setGroupCode(groupCode: String) {
        this.groupCode = groupCode
    }

    fun getGroupCode(): String {
        return groupCode
    }

    fun getClassesForDay() {
        val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekName = CalendarHelper.getDayOfWeekName(dayOfWeek)
        val weekNum = CalendarHelper.calculateCurrentWeek(selectedDate)

        //room main safety under the hood so operation with room can be called from Dispatchers.Main
        //viewModelScope uses Dispatchers.Main by default
        viewModelScope.launch {
            classListForDay.postValue(classDao.getClassesForDay(dayOfWeekName, weekNum))
        }
    }

    fun parseFile(groupName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val classList: MutableList<Class>?
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(fileUri!!)
                //parsing file
                classList = parsingFile(inputStream, groupName)
                Log.d(TAG, "parsing file")
                withContext(Dispatchers.IO) {
                    //save parsing results in database
                    saveClassList(classList)
                    Log.d(TAG, "saving results")
                }
                loadDialogState.postValue(LoadDialogState.STATE_DONE)
                Log.d(TAG, "switching parse flag to true")
            } catch (e: NoSuchElementException) {
                loadErrorMessage = e.message
                state.set(ERROR_MESSAGE_KEY, loadErrorMessage)
                Log.d(TAG, "NoSuchElementException")
                loadDialogState.postValue(LoadDialogState.STATE_ERROR)
                this.cancel()
            } catch (e2: FileFormatException) {
                loadErrorMessage = e2.message
                state.set(ERROR_MESSAGE_KEY, loadErrorMessage)
                Log.d(TAG, "FileFormatException")
                loadDialogState.postValue(LoadDialogState.STATE_ERROR)
                this.cancel()
            } catch (e3: CancellationException) {
                Log.d(TAG, "CancellationException")
            }
        }
    }

     private suspend fun parsingFile(inputStream: InputStream?, groupName: String): MutableList<Class> {
        return ParseExcelFileHelper.readFromExcel(inputStream, groupName)
    }

    fun changeDropState() {
        dropState = when (dropState) {
            MainActivity.DropState.CLOSE -> {
                MainActivity.DropState.OPEN
            }
            MainActivity.DropState.OPEN -> {
                MainActivity.DropState.CLOSE
            }
        }
        state.set(DROP_STATE_KEY, dropState)
    }

    fun onCancelClicked() {
        viewModelScope.cancel()
        Log.d(TAG, "onCancelClicked")
    }

    override fun onCleared() {
        super.onCleared()

        loadDialogState.value = LoadDialogState.NOT_SHOWN
        Log.d(TAG, "onCleared")
    }

    companion object {
        private const val TAG = "ClassViewModel"
        private const val SELECTED_DATE_KEY = "selectedDate"
        private const val DROP_STATE_KEY = "dropState"
        private const val ERROR_MESSAGE_KEY = "loadErrorMessage"
        private const val FILE_URI_KEY = "fileUriKey"
    }
}