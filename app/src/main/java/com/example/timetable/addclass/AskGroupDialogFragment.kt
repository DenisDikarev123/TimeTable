package com.example.timetable.addclass

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.timetable.ClassViewModel
import com.example.timetable.R
import com.example.timetable.themeswitcher.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AskGroupDialogFragment: DialogFragment() {

    private val viewModel: ClassViewModel by viewModels()

    private lateinit var groupNameInputLayout: TextInputLayout
    private lateinit var groupNameEditText: TextInputEditText
    private lateinit var listener: OnPositiveButtonClickedListener
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            //Group pattern looks like IVMO-04-20
            val trimmedString = s?.trim()
            //auto complete '-' symbols
            if (trimmedString != null) {
                if(trimmedString.length >= 5) {
                    if (trimmedString[4] != '-') {
                        s.insert(4, "-")
                    }
                }
                if(trimmedString.length >= 8) {
                    if (trimmedString[7] != '-') {
                        s.insert(7, "-")
                    }
                }
            }
            viewModel.setGroupCode(trimmedString.toString())
            val d = dialog as AlertDialog
            Log.d(TAG, "now we have string $s")
            if (trimmedString != null) {
                if(trimmedString.length == 10) {
                    //verify entered group name
                    val isVerified = verifyEnteredGroupName(viewModel.getGroupCode())
                    if(isVerified) {
                        d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        groupNameInputLayout.error = null
                    } else {
                        //show error on edit text
                        groupNameInputLayout.error = "Введите корректное название групы"
                    }
                }
            }
        }

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private var dialogStyleId: Int = R.style.ThemeOverlay_TimeTable_MaterialAlertDialog_Night

    interface OnPositiveButtonClickedListener{
        fun onPositiveButtonClicked(groupName: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity?.layoutInflater?.inflate(R.layout.dialog_ask_group, null)
        groupNameInputLayout = view?.findViewById(R.id.input_layout_group_name)!!
        groupNameEditText = view.findViewById(R.id.text_ask_group)

        //update colors for night theme
        if(!ThemeManager.theme) updateTextInputLayoutStyle(groupNameInputLayout, groupNameEditText)
        dialogStyleId = if (ThemeManager.theme) 0 else R.style.ThemeOverlay_TimeTable_MaterialAlertDialog_Night

        groupNameEditText.addTextChangedListener(textWatcher)

        val dialog = MaterialAlertDialogBuilder(requireContext(), dialogStyleId)
            .setTitle(getString(R.string.label_study_group))
            .setMessage(getString(R.string.message_enter_study_group))
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(resources.getString(R.string.decline)) { _, _ ->
                // Respond to negative button press
            }
            .setPositiveButton(resources.getString(R.string.accept)) { _, _ -> }
            .create()

        return dialog
    }

    override fun onResume() {
        super.onResume()

        val alertDialog = dialog as AlertDialog
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            //callback to activity
            listener.onPositiveButtonClicked(viewModel.getGroupCode())
            alertDialog.dismiss()
        }
    }

    //onAttach save with process death and configuration changes.
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context !is OnPositiveButtonClickedListener) {
            throw RuntimeException("${context.javaClass} must implement OnPositiveButtonClickedListener interface")
        }

        listener = context
    }

    override fun onDestroyView() {
        super.onDestroyView()

        groupNameEditText.removeTextChangedListener(textWatcher)
    }

    private fun verifyEnteredGroupName(groupName: String): Boolean {
        groupName.trim()
        return (groupName[4] == '-'
                && groupName[7] == '-'
                && groupName[5].isDigit()
                && groupName[6].isDigit()
                && groupName[8].isDigit()
                && groupName[9].isDigit())
    }

    private fun updateTextInputLayoutStyle(textInputLayout: TextInputLayout, textInputEditText: TextInputEditText) {
        textInputLayout.setBoxStrokeColorStateList(ContextCompat.getColorStateList(requireContext(), R.color.text_edit_stroke_state_list_night)!!)
        textInputLayout.boxStrokeErrorColor = ContextCompat.getColorStateList(requireContext(), R.color.edit_text_error_stroke_list_night)!!
        textInputLayout.setBoxBackgroundColorResource(R.color.colorSurfaceColoredNight)
        textInputLayout.setErrorIconTintList(ContextCompat.getColorStateList(requireContext(), R.color.text_edit_error_icon_list_night)!!)
        textInputLayout.setEndIconTintList(ContextCompat.getColorStateList(requireContext(), R.color.text_edit_end_icon_list_night)!!)
        textInputLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), R.color.text_edit_helper_text_list_night)!!)
        textInputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.text_edit_error_text_list_night)!!)
        textInputLayout.counterTextColor = ContextCompat.getColorStateList(requireContext(), R.color.text_edit_counter_text_list_night)!!
        //for extended hint state
        textInputLayout.defaultHintTextColor = ContextCompat.getColorStateList(requireContext(), R.color.text_edit_default_hint_list_night)!!
        //for collapsed hint state
        textInputLayout.hintTextColor = ContextCompat.getColorStateList(requireContext(), R.color.text_edit_stroke_state_list_night)!!
        textInputEditText.setTextColor( ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceNight) )
    }

    companion object {
        private const val TAG = "AskGroupDialogFragment"
    }
}