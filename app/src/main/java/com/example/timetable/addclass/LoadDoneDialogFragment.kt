package com.example.timetable.addclass

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
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

class LoadDoneDialogFragment: DialogFragment() {

    private var dialogStyleId: Int = R.style.ThemeOverlay_TimeTable_MaterialAlertDialog_Night
    private lateinit var listener: OnPositiveButtonLoadDialogListener

    interface OnPositiveButtonLoadDialogListener{
        fun onPositiveButtonLoadDialogClicked(loadDialogState: LoadDialogState)
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
            .setTitle(getString(R.string.label_load_done))
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(resources.getString(R.string.decline)) { _, _ -> }
            .setPositiveButton(R.string.accept) { _, _ -> }
            .create()
    }

    override fun onResume() {
        super.onResume()

        showDoneState()
    }

    //onAttach save with process death and configuration changes.
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context !is OnPositiveButtonLoadDialogListener) {
            throw RuntimeException("${context.javaClass} must implement OnPositiveButtonLoadDialogListener interface")
        }

        listener = context
    }

    private fun showDoneState() {
        val alertDialog = dialog as AlertDialog
        val progressBar = alertDialog.findViewById<ProgressBar>(R.id.progress_bar_load_schedule)
        val doneImageView = alertDialog.findViewById<ImageView>(R.id.image_view_load_done)
        val loadTextView = alertDialog.findViewById<TextView>(R.id.text_load_message)
        Log.d(TAG, "showDoneAnimation")
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).visibility = View.GONE
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.VISIBLE
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            listener.onPositiveButtonLoadDialogClicked(LoadDialogState.NOT_SHOWN)
            alertDialog.dismiss()
        }

        progressBar?.visibility = View.INVISIBLE
        doneImageView?.visibility = View.VISIBLE
        loadTextView?.text = getString(R.string.message_load_done)
        doneImageView?.setImageDrawable(resources.getDrawable(R.drawable.avd_done, null))
        val avd = doneImageView?.drawable as AnimatedVectorDrawable
        avd.start()
    }

    companion object {
        private const val TAG = "LoadDoneDialogFragment"
    }
}
