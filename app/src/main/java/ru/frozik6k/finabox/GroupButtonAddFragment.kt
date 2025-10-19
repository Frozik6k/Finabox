package ru.frozik6k.finabox

import android.content.Context
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.Nullable
import ru.frozik6k.lohouse.thing.Thing

class GroupButtonAddFragment : Fragment() {

    private var viewRoot: View? = null
    private var mListener: OnAddListener? = null

    companion object {
        fun newInstance(): GroupButtonAddFragment = GroupButtonAddFragment()
    }

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_group_button_add, container, false)
        viewRoot = root

        val anim = AnimationUtils.loadAnimation(requireContext().applicationContext, R.anim.scale_fab)

        val viewAddThing: View = root.findViewById(R.id.fabAddThing)
        viewAddThing.startAnimation(anim)
        viewAddThing.setOnClickListener { mListener?.onAdd(Thing.AS_THING) }

        val viewAddBox: View = root.findViewById(R.id.fabAddBox)
        viewAddBox.startAnimation(anim)
        viewAddBox.setOnClickListener { mListener?.onAdd(Thing.AS_BOX) }

        root.setOnClickListener { mListener?.onAdd(-1) }

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnAddListener) {
            context
        } else {
            throw ClassCastException("$context must implement OnAddListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnAddListener {
        fun onAdd(isBox: Int)
    }
}