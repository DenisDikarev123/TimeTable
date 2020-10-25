package com.example.timetable.addclass

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.timetable.LoadDialogState
import com.example.timetable.R
import com.example.timetable.themeswitcher.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoadErrorDialogFragment: DialogFragment() {

    private var dialogStyleId: Int = R.style.ThemeOverlay_TimeTable_MaterialAlertDialog_Night
    private lateinit var listener: OnPositiveButtonErrorDialogListener

    private var errorMessage: String? = null

    interface OnPositiveButtonErrorDialogListener{
        fun onPositiveButtonErrorDialogClicked(loadDialogState: LoadDialogState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if(savedInstanceState != null && savedInstanceState.containsKey(ERROR_MESSAGE_KEY)) {
            errorMessage = savedInstanceState.getString(ERROR_MESSAGE_KEY)
        }
        Log.d(TAG, "getting from savedState error message $errorMessage")
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
            .setTitle(getString(R.string.label_load_error))
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(resources.getString(R.string.decline)) { _, _ -> }
            .setPositiveButton(R.string.accept) { _, _ -> }
            .create()
    }

    //onAttach save with process death and configuration changes.
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context !is OnPositiveButtonErrorDialogListener) {
            throw RuntimeException("${context.javaClass} must implement OnPositiveButtonErrorDialogListener interface")
        }

        listener = context
    }

    override fun onResume() {
        super.onResume()

        showErrorState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(ERROR_MESSAGE_KEY, errorMessage)
        Log.d(TAG, "saving error message $errorMessage")
    }

    private fun showErrorState() {
        val alertDialog = dialog as AlertDialog
        val progressBar = alertDialog.findViewById<ProgressBar>(R.id.progress_bar_load_schedule)
        val doneImageView = alertDialog.findViewById<ImageView>(R.id.image_view_load_done)
        val loadTextView = alertDialog.findViewById<TextView>(R.id.text_load_message)
        Log.d(TAG, "showDoneAnimation")
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).visibility = View.GONE
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.VISIBLE
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            listener.onPositiveButtonErrorDialogClicked(LoadDialogState.NOT_SHOWN)
            alertDialog.dismiss()
        }

        progressBar?.visibility = View.INVISIBLE
        doneImageView?.visibility = View.VISIBLE
        loadTextView?.text = errorMessage
        if (ThemeManager.theme) {
            doneImageView?.setImageDrawable(resources.getDrawable(R.drawable.ic_error_outline, null))
        } else {
            doneImageView?.setImageDrawable(resources.getDrawable(R.drawable.ic_error_outline_night, null))
        }
    }

    fun setErrorMessage(errorMessage: String?) {
        this.errorMessage = errorMessage
        Log.d(TAG, "setting new value to error message $errorMessage")
    }

    companion object {
        private const val TAG = "LoadErrorDialogFragment"
        private const val ERROR_MESSAGE_KEY = "errorMessageKey"
    }
}