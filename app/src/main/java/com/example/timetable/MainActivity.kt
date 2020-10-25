package com.example.timetable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.widget.CalendarView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.LayoutInflaterCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timetable.addclass.AskGroupDialogFragment
import com.example.timetable.addclass.LoadDialogFragment
import com.example.timetable.addclass.LoadDoneDialogFragment
import com.example.timetable.addclass.LoadErrorDialogFragment
import com.example.timetable.database.Class
import com.example.timetable.databinding.ActivityMainBinding
import com.example.timetable.helpers.DateFormatter
import com.example.timetable.themeswitcher.CustomLayoutInflater
import com.example.timetable.themeswitcher.ThemeManager
import com.example.timetable.widget.TimetableWidgetProvider
import com.skydoves.balloon.ArrowConstraints
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import kotlin.math.max
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(),
    CalendarView.OnDateChangeListener,
    AskGroupDialogFragment.OnPositiveButtonClickedListener,
    LoadDoneDialogFragment.OnPositiveButtonLoadDialogListener,
    LoadErrorDialogFragment.OnPositiveButtonErrorDialogListener,
    LoadDialogFragment.OnNegativeButtonClickedListener {

    enum class DropState {
        OPEN,
        CLOSE
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ClassViewModel by viewModels()
    private val askGroupDialog = AskGroupDialogFragment().apply {
        isCancelable = false
    }
    private val loadDialogFragment = LoadDialogFragment().apply {
        isCancelable = false
    }
    private val loadDoneDialogFragment = LoadDoneDialogFragment().apply {
        isCancelable = false
    }
    private val loadErrorDialogFragment = LoadErrorDialogFragment().apply {
        isCancelable = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        LayoutInflaterCompat.setFactory2(
            LayoutInflater.from(this),
            CustomLayoutInflater(delegate)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupToolbar()

        binding.calendarView.setOnDateChangeListener(this)
        binding.calendarView.date = viewModel.selectedDate.timeInMillis

        binding.rootLayout.post {
            initBackdropState()
        }

        //set this property to prevent Apache POI errors
        setSystemProperty()

        binding.recyclerViewClasses.layoutManager = LinearLayoutManager(this)

        val classListObserver = Observer<MutableList<Class>> { list ->
            if(list.size == 0) {
                binding.textViewCardHeader.text = getString(R.string.message_no_classes)
                binding.imageEmptyClass.visibility = View.VISIBLE
                binding.recyclerViewClasses.visibility = View.GONE
            } else {
                binding.textViewCardHeader.text = resources.getQuantityString(R.plurals.number_of_classes, list.size, list.size)
                binding.imageEmptyClass.visibility = View.GONE
                binding.recyclerViewClasses.visibility = View.VISIBLE
            }
            binding.recyclerViewClasses.adapter = ClassAdapter(list, viewModel.selectedDate)
        }
        viewModel.getClassList().observe(this, classListObserver)

        setTheme(ThemeManager.theme)
    }

    override fun onStart() {
        super.onStart()

        val parseStateObserver = Observer<LoadDialogState> { loadDialogState ->
            Log.d(TAG, "isParsingDone in MainActivity value $loadDialogState")
            when (loadDialogState) {
                LoadDialogState.NOT_SHOWN -> {
                    removeLoadDialog()
                }
                LoadDialogState.STATE_LOAD -> {
                    val fragment = supportFragmentManager.findFragmentByTag("loadDialog")
                    if(fragment == null) {
                        loadDialogFragment.showNow(supportFragmentManager, "loadDialog")
                    }
                }
                LoadDialogState.STATE_DONE -> {
                    removeLoadDialog()
                    val doneFragment = supportFragmentManager.findFragmentByTag("loadDoneDialog")
                    if(doneFragment == null) {
                        loadDoneDialogFragment.showNow(supportFragmentManager, "loadDoneDialog")
                    }
                    //update schedule for current day
                    viewModel.getClassesForDay()
                    //update widgets
                    updateAppWidgets()
                }
                LoadDialogState.STATE_ERROR -> {
                    removeLoadDialog()
                    val errorFragment = supportFragmentManager.findFragmentByTag("loadErrorDialog")
                    if(errorFragment == null) {
                        loadErrorDialogFragment.setErrorMessage(viewModel.loadErrorMessage)
                        loadErrorDialogFragment.showNow(supportFragmentManager, "loadErrorDialog")
                    }
                }
            }
        }
        viewModel.loadDialogState.observe(this, parseStateObserver)
    }

    override fun onResume() {
        super.onResume()

        viewModel.getClassesForDay()
        Log.d(TAG, "onResume")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "uri ${data.data}")
            viewModel.fileUri = data.data

            askGroupDialog.show(supportFragmentManager, "askGroupDialog")
        }
    }

    override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
        viewModel.setDate(year, month, dayOfMonth)
        viewModel.getClassesForDay()
        changeDropState()
    }

    override fun onPositiveButtonClicked(groupName: String) {
        readFile(groupName)
    }

    override fun onPositiveButtonErrorDialogClicked(loadDialogState: LoadDialogState) {
        viewModel.loadDialogState.postValue(loadDialogState)
    }

    override fun onPositiveButtonLoadDialogClicked(loadDialogState: LoadDialogState) {
        viewModel.loadDialogState.postValue(loadDialogState)
    }

    override fun onNegativeButtonClicked(loadDialogState: LoadDialogState) {
        Log.d(TAG, "onNegativeButtonClicked")
        viewModel.onCancelClicked()
        viewModel.loadDialogState.postValue(loadDialogState)
    }

    private fun removeLoadDialog() {
        val loadFragment = supportFragmentManager.findFragmentByTag("loadDialog")
        if(loadFragment != null) {
            supportFragmentManager
                .beginTransaction()
                .remove(loadFragment)
                .commit()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.toolbar)
        binding.toolbar.setOnMenuItemClickListener {item ->
            when (item.itemId) {
                R.id.menu_add_from_file -> {
                    requestFile()
                    true
                }
                R.id.menu_switch_theme -> {
                    switchAppTheme()
                    true
                }
                else -> false
            }
        }
        val view = findViewById<View>(R.id.menu_add_from_file)
        //post cause menu cannot inflates in time
        view.post {
            val location = IntArray(2)
            view.getLocationInWindow(location)
            val x = location[0]
            val y = location[1]
            //subtract status bar height in px
            val dp = 24f
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
            //check whether the user opens app first time. If so, show tooltip
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            if(!sharedPref.contains(FIRST_USE_KEY)) {
                val balloon = createTooltip(x, y - px)
                balloon.setOnBalloonClickListener {
                    putSharedPreference(sharedPref)
                    Log.d(TAG, "OnBalloonClickListener")
                    balloon.dismiss()
                }
                balloon.setOnBalloonOverlayClickListener {
                    putSharedPreference(sharedPref)
                    Log.d(TAG, "OnBalloonOverlayClickListener")
                    balloon.dismiss()
                }
                balloon.showAlignBottom(view)
            }
        }
        //close icon here because app starts with opened calendar state
        binding.toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_close_24dp, null)
        binding.toolbar.setNavigationOnClickListener {
            changeDropState()
        }
    }

    private fun putSharedPreference(sharedPref: SharedPreferences) {
        with(sharedPref.edit()) {
            putBoolean(FIRST_USE_KEY, true)
            apply()
        }
    }

    private fun changeDropState() {
        viewModel.changeDropState()
        drawDropState()
    }

    private fun initBackdropState() {
        binding.backLayer.y = binding.toolbar.y + binding.toolbar.height
        Log.d(TAG, "init back layer y ${binding.backLayer.y}")
        binding.frontLayer.layoutParams.height = binding.rootLayout.height - binding.toolbar.height
        Log.d(TAG, "init front layer height ${binding.frontLayer.layoutParams.height}")
        drawDropState()
    }

    private fun drawDropState() {
        when (viewModel.dropState) {
            DropState.CLOSE -> {
                drawClosedState()
                binding.toolbar.navigationIcon =
                    resources.getDrawable(R.drawable.ic_calendar_24dp, null)
                binding.toolbar.title =
                    DateFormatter.convertCalendarToString(viewModel.selectedDate)
            }
            DropState.OPEN -> {
                drawOpenedState()
                binding.toolbar.navigationIcon =
                    resources.getDrawable(R.drawable.ic_close_24dp, null)
                binding.toolbar.title = resources.getString(R.string.app_name)
            }
        }
    }

    private fun drawClosedState() {
        val position = binding.backLayer.y
        Log.d(TAG, "old y position draw closed state ${binding.frontLayer.y}")
        Log.d(TAG, "drawClosedState $position")
        binding.frontLayer.animate().y(position).setDuration(250L).start()
    }

    private fun drawOpenedState() {
        val position = binding.backLayer.y + binding.backLayer.height
        Log.d(TAG, "old y position draw opened state ${binding.frontLayer.y}")
        Log.d(TAG, "drawOpenedState $position")
        binding.frontLayer.animate().y(position).setDuration(250L).start()
    }

    private fun switchAppTheme() {
        //avoid multiple animation
        if (binding.imageViewSwitchTheme.visibility == View.VISIBLE) {
            return
        }
        //getting switch theme button location
        val switchThemeMenuItem = binding.toolbar.findViewById<View>(R.id.menu_switch_theme)
        val location = IntArray(2)
        switchThemeMenuItem.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]

        //draw bitmap with old theme to prevent switch theme
        val w = binding.subrootLayout.measuredWidth
        val h = binding.subrootLayout.measuredHeight
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        binding.subrootLayout.draw(canvas)

        binding.imageViewSwitchTheme.setImageBitmap(bitmap)
        binding.imageViewSwitchTheme.visibility = View.VISIBLE

        val finalRadius = max(sqrt(((w - x) * (w - x) + (h - y) * (h - y)).toDouble()), sqrt((x * x + (h - y) * (h - y)).toDouble())).toFloat()

        //switch theme
        ThemeManager.theme = !ThemeManager.theme

        //update view to new theme
        setTheme(ThemeManager.theme)

        //save new theme
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences
            .edit()
            .putBoolean(getString(R.string.night_theme_flag), ThemeManager.theme)
            .apply()

        //start switch theme animation
        val anim = ViewAnimationUtils.createCircularReveal(binding.subrootLayout, x, y, 0.0f, finalRadius)
        anim.duration = 400
        anim.interpolator = AccelerateInterpolator()
        anim.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                binding.imageViewSwitchTheme.setImageDrawable(null)
                binding.imageViewSwitchTheme.visibility = View.GONE
            }
        })
        anim.start()
    }

    private fun setTheme(theme: Boolean) {
        if(theme) {
            binding.subrootLayout.setBackgroundColor( ContextCompat.getColor(this, R.color.colorPrimary) )
            binding.calendarView.setBackgroundColor( ContextCompat.getColor(this, R.color.colorPrimary) )
            binding.frontLayer.setCardBackgroundColor( ContextCompat.getColor(this, R.color.colorSurface) )
            binding.toolbar.setBackgroundColor( ContextCompat.getColor(this, R.color.colorPrimary) )
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        } else {
            binding.subrootLayout.setBackgroundColor( ContextCompat.getColor(this, R.color.colorSurfaceNight) )
            binding.calendarView.setBackgroundColor( ContextCompat.getColor(this, R.color.colorSurfaceNight) )
            binding.frontLayer.setCardBackgroundColor( ContextCompat.getColor(this, R.color.colorSurfaceColoredNight) )
            binding.toolbar.setBackgroundColor( ContextCompat.getColor(this, R.color.colorSurfaceNight) )
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorSurfaceNight)
        }
    }

    private fun setSystemProperty() {
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl"
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl"
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.stax.EventFactoryImpl"
        )
    }

    private fun requestFile() {
        Log.d(TAG, "onAddFromFileMenuItem clicked")
        val intentPickFile = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = ("*/*")
        }
        if(intentPickFile.resolveActivity(packageManager) != null) {
            startActivityForResult(intentPickFile, PICKFILE_REQUEST_CODE)
        }
    }

    private fun readFile(groupName: String) {
        //show load dialog
        viewModel.loadDialogState.value = LoadDialogState.STATE_LOAD
        viewModel.parseFile(groupName)
    }

    private fun updateAppWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, TimetableWidgetProvider::class.java))
        val intent = Intent(this, TimetableWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }

    private fun createTooltip(x: Int, y: Int): Balloon {
        return Balloon.Builder(this)
            .setLifecycleOwner(this)
            .setText("Добавьте файл с расписанием")
            .setPaddingTop(6)
            .setPaddingBottom(6)
            .setPaddingLeft(8)
            .setPaddingRight(8)
            .setMarginLeft(8)
            .setMarginRight(8)
            .setTextSize(16.0f)
            .setArrowSize(8)
            .setArrowConstraints(ArrowConstraints.ALIGN_ANCHOR)
            .setBackgroundColorResource(R.color.colorAccent)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setIsVisibleOverlay(true)
            .setOverlayColorResource(R.color.overlay)
            .setOverlayPosition(Point(x, y))
            .setDismissWhenOverlayClicked(false)
            .setDismissWhenTouchOutside(false)
            .build()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PICKFILE_REQUEST_CODE = 101
        private const val FIRST_USE_KEY = "firstUseKey"
    }
}
