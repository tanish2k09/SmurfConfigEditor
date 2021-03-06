package com.tanish2k09.sce.fragments.containerFragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.tanish2k09.sce.R

import android.content.Context.MODE_PRIVATE

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class CategoryFragment : Fragment() {

    private var categoryName = "Not categorized"
    private var dividerTop: View? = null
    private var dividerBottom: View? = null
    private var catText: TextView? = null

    fun setInstanceCategory(category: String) {
        categoryName = category
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_category, container, false)

        catText = v.findViewById(R.id.categoryTitle)
        catText!!.text = categoryName

        dividerTop = v.findViewById(R.id.catDivTop)
        dividerBottom = v.findViewById(R.id.catDivBottom)

        return v
    }

    override fun onResume() {
        super.onResume()
        val sp = catText!!.context.getSharedPreferences("settings", MODE_PRIVATE)
        val accent = Color.parseColor(sp.getString("accentCol", "#00bfa5"))
        catText!!.setTextColor(accent)
        dividerBottom!!.setBackgroundColor(accent)
        dividerTop!!.setBackgroundColor(accent)
    }

    fun pushConfigVarFragment(`var`: ConfigVar, index: Int) {
        childFragmentManager.beginTransaction().add(R.id.categoryList, `var`, "configCard$index").commit()
    }

    fun popFragment(index: Int): Boolean {
        childFragmentManager.beginTransaction()
                .remove(childFragmentManager.findFragmentByTag("configCard$index")!!)
                .commitNow()
        return childFragmentManager.fragments.size == 0
    }
}// Required empty public constructor
