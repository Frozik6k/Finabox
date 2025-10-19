package ru.frozik6k.finabox

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.badoo.mobile.util.WeakHandler
import ru.frozik6k.lohouse.thing.Thing
import ru.frozik6k.lohouse.thing.ThingContent

class ThingFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "ThingFragment"
        private const val ARG_IS_EDIT = "is_edit"
        private const val ARG_POS_FOTO = "pos_foto"

        @JvmStatic
        fun newInstance(isEdit: Boolean): ThingFragment {
            val fragment = ThingFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_EDIT, isEdit)
            fragment.arguments = args
            return fragment
        }
    }

    private var mIsEdit: Boolean = false
    private var positionFoto: Int = 0 // Позиция фотографии

    private lateinit var mThing: Thing
    private lateinit var mView: View

    private lateinit var mPagerFoto: ViewPager
    private var mPagerAdapter: PagerAdapter? = null

    private lateinit var mContext: Context
    private var mListener: OnEditListener? = null

    // Для позднего обновления адаптера mPagerAdapter
    private var mHandler: WeakHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val thingContent = ThingContent.get(requireContext())

        positionFoto = savedInstanceState?.getInt(ARG_POS_FOTO, 0) ?: 0
        mIsEdit = arguments?.getBoolean(ARG_IS_EDIT, false) ?: false

        mThing = thingContent.thingEdit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "onCreateView")

        mView = if (!mIsEdit) {
            inflater.inflate(R.layout.fragment_thing_view, container, false).also { root ->
                val fab = root.findViewById<FloatingActionButton>(R.id.fab)
                fab.setOnClickListener {
                    mListener?.onEdit()
                }
            }
        } else {
            inflater.inflate(R.layout.fragment_thing_edit, container, false).also { root ->
                val fab = root.findViewById<FloatingActionButton>(R.id.fab)
                fab.setOnClickListener {
                    activity?.finish()
                }
            }
        }

        return mView
    }

    private fun updateUI() {
        mPagerFoto = mView.findViewById(R.id.pagerFotos)
        if (mPagerFoto.adapter == null) {
            val fm = requireActivity().supportFragmentManager
            if (mPagerAdapter == null) {
                mPagerAdapter = AdapterPagerFoto(fm, mThing.mFotos, mIsEdit)
            }
            mPagerFoto.adapter = mPagerAdapter
        } else {
            mPagerAdapter = mPagerFoto.adapter
        }

        mPagerFoto.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val tvCountFoto = mView.findViewById<TextView>(R.id.tvCountFoto)
                val viewPagerFotos = mView.findViewById<ViewPager>(R.id.pagerFotos)
                tvCountFoto.text = "${position + 1}/${viewPagerFotos.adapter.count}"
                positionFoto = position
            }

            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })

        val tvCountFoto = mView.findViewById<TextView>(R.id.tvCountFoto)
        tvCountFoto.text = "${mPagerFoto.currentItem + 1}/${mPagerFoto.adapter.count}"

        val nameItem = mView.findViewById<TextView>(R.id.nameItem)
        val description = mView.findViewById<TextView>(R.id.description)

        nameItem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                mThing.thingName = s.toString()
            }
            override fun afterTextChanged(s: Editable) {}
        })

        description.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (mIsEdit) mThing.description = s.toString()
            }
            override fun afterTextChanged(s: Editable) {}
        })

        nameItem.text = mThing.thingName

        if (!mIsEdit) {
            val tvPath = mView.findViewById<TextView>(R.id.path)
            tvPath?.text = ThingContent.get(mContext).path
        }

        if (::mContext.isInitialized) {
            val titleDescription = mContext.resources.getString(R.string.description)
            val desc = mThing.description
            if (!desc.isNullOrEmpty()) {
                if (!mIsEdit) description.text = "$titleDescription: $desc"
                else description.text = desc
            }
        }

        mPagerAdapter?.notifyDataSetChanged()

        if (mHandler == null) {
            mHandler = WeakHandler()
            mHandler?.postDelayed({
                mPagerAdapter?.notifyDataSetChanged()
                mPagerFoto.setCurrentItem(positionFoto)
                mHandler = null
            }, 100)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is OnEditListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnEditListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ARG_POS_FOTO, positionFoto)
        super.onSaveInstanceState(outState)
    }

    interface OnEditListener {
        fun onEdit()
    }

    private inner class AdapterPagerFoto(
        fm: FragmentManager,
        private val mListFoto: List<String>,
        private val mIsEdit: Boolean
    ) : FragmentStatePagerAdapter(fm) {

        companion object {
            const val TAG_FOTO_FRAGMENT = "tag_foto_fragment"
        }

        override fun getItem(position: Int): Fragment {
            return if (mIsEdit) {
                if ((count - 1) == position) {
                    FotosFragment.newInstance("", FotosFragment.FOTO_ADD, position, TAG_FOTO_FRAGMENT)
                } else {
                    FotosFragment.newInstance(mListFoto[position], FotosFragment.FOTO_FILE, position, TAG_FOTO_FRAGMENT)
                }
            } else {
                if (mListFoto.isEmpty()) {
                    FotosFragment.newInstance("", FotosFragment.FOTO_NO, position, TAG_FOTO_FRAGMENT)
                } else {
                    FotosFragment.newInstance(mListFoto[position], FotosFragment.FOTO_FILE, position, TAG_FOTO_FRAGMENT)
                }
            }
        }

        override fun getCount(): Int {
            return if (mIsEdit) {
                mListFoto.size + 1
            } else {
                if (mListFoto.isNotEmpty()) mListFoto.size else 1
            }
        }

        override fun getItemPosition(`object`: Any): Int = PagerAdapter.POSITION_NONE

        override fun notifyDataSetChanged() {
            super.notifyDataSetChanged()
        }
    }
}