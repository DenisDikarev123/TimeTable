package com.example.timetable.addclass

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.timetable.LoadDialogState
import com.example.timetable.R
import com.example.timetable.themeswitcher.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoadDialogFragment: DialogFragment() {

    private var dialogStyleId: Int = R.style.ThemeOverlay_TimeTable_MaterialAlertDialog_Night
    private lateinit var listener: OnNegativeButtonClickedListener

    interface OnNegativeButtonClickedListener{
        fun onNegativeButtonClicked(loadDialogState: LoadDialogState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity?.layoutInflater?.inflate(R.layout.dialog_load_schedule, null)
        val loadMessageTextView = view?.findViewById<TextView>(R.id.text_load_message)!!

        if(ThemeManager.theme){
            loadMessageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface))
        } else {
            loadMessageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceNight))
        }
        dialogStyleId = if (ThemeManager.theme) 0 else R.style.ThemeOverlay_TimeTable_MaterialAlertDialog_Night

        Log.d(TAG, "onCreateDialog")
        return MaterialAlertDialogBuilder(requireContext(), dialogStyleId)
            .setTitle(getString(R.string.label_load))
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(resources.getString(R.string.decline)) { _, _ -> }
            .setPositiveButton(R.string.accept) { _, _ -> }
            .create()
    }

    override fun onResume() {
        super.onResume()

        showLoadState()
        Log.d(TAG, "onResume")
    }

    //onAttach save with process death and configuration changes.
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context !is OnNegativeButtonClickedListener) {
            throw RuntimeException("${context.javaClass} must implement OnNegativeButtonClickedListener interface")
        }

        listener = context
    }

    private fun showLoadState() {
        val alertDialog = dialog as AlertDialog
        Log.d(TAG, "show load animation")
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).visibility = View.VISIBLE
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.GONE
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            listener.onNegativeButtonClicked(LoadDialogState.NOT_SHOWN)
            alertDialog.dismiss()
        }
    }

    companion object {
        private const val TAG = "LoadDialogFragment"
    }
}