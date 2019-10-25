package com.team7.socialblind.util

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.*
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import timber.log.Timber




@BindingAdapter("layoutManager", "mangerDetails", "hasHeader")
fun setLayoutmanager(recyclerView: RecyclerView, layoutManager: LayoutManagersType, managerDetails: Int, hasHeader: Boolean = false) {
    Timber.v("layout manager : $layoutManager , $managerDetails")
    recyclerView.layoutManager = when (layoutManager) {
        LayoutManagersType.LINEARMANAGER -> {
            val manager = LinearLayoutManager(recyclerView.context)
            manager.orientation = managerDetails
            manager.reverseLayout = true
            manager.stackFromEnd = true
            manager
        }
        LayoutManagersType.GRIDMANAGER -> GridLayoutManager(recyclerView.context, managerDetails)
        LayoutManagersType.FLEXBOX_MANAGER -> FlexboxLayoutManager(recyclerView.context, FlexDirection.ROW).apply {
            flexWrap = FlexWrap.WRAP }
        LayoutManagersType.STAGGERDMANAGER -> {
            StaggeredGridLayoutManager(managerDetails, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        }
    }

}

@BindingAdapter("epoxyController")
fun setRecyclerData(recyclerView: EpoxyRecyclerView, controller: EpoxyController) {
    recyclerView.setController(controller)
}

@BindingAdapter("seperator")
fun setDecorator(recyclerView: RecyclerView, boolean: Boolean) {
    val decorator = DividerItemDecoration(recyclerView.context,
        (recyclerView.layoutManager as LinearLayoutManager).orientation)
    recyclerView.addItemDecoration(decorator)
}

@BindingAdapter("selected")
fun hideView(button: MaterialButton, selected: Boolean) {
    button.isSelected = selected
}



