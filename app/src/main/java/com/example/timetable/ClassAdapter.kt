package com.example.timetable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.timetable.database.Class
import com.example.timetable.databinding.ItemClassBinding
import com.example.timetable.helpers.CalendarHelper
import java.util.*

class ClassAdapter(private var classList: MutableList<Class>, var selectedDate: Calendar) :
    RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        val binding = ItemClassBinding.bind(view)

        return ClassViewHolder(view, binding)
    }

    override fun getItemCount(): Int {
        return classList.size
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val cl = classList[position]
        holder.bind(cl, selectedDate)
    }



    class ClassViewHolder(itemView: View, var binding: ItemClassBinding): RecyclerView.ViewHolder(itemView) {

        fun bind(cl: Class, selectedDate: Calendar) {
            binding.apply {
                textClassNumber.text = cl.number.toString()
                textClassName.text = cl.name
                textClassTeacherName.text = cl.teacherName
                textClassType.text = cl.type
                textClassStartTime.text = cl.startTime?.let { correctTime(it) }
                textClassEndTime.text = cl.endTime?.let { correctTime(it) }
                textClassAudience.text = cl.audience
                val classState = cl.startTime?.let { cl.endTime?.let { it1 -> CalendarHelper.checkClassState(it, it1, selectedDate) } }
                when (classState) {
                    ClassState.NOT_TODAY -> {
                        itemView.alpha = 1.0f
                        textClassCountdown.visibility = View.GONE
                    }
                    ClassState.BEFORE -> {
                        itemView.alpha = 0.38f
                        textClassCountdown.visibility = View.GONE
                    }
                    ClassState.NOW -> {
                        itemView.alpha = 1.0f
                        textClassCountdown.visibility = View.VISIBLE
                    }
                    ClassState.AFTER -> {
                        itemView.alpha = 1.0f
                        textClassCountdown.visibility = View.GONE
                    }
                }
            }
        }

        private fun correctTime(time: String): String {
            return time.replace('-', ':')
        }

    }
}