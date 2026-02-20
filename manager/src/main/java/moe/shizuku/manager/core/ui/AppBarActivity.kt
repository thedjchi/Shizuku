package moe.shizuku.manager.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import moe.shizuku.manager.R
import rikka.core.ktx.unsafeLazy

abstract class AppBarActivity : AppActivity() {

    protected val rootView: ViewGroup by unsafeLazy {
        findViewById<ViewGroup>(R.id.root)
    }

    private val toolbarContainer: AppBarLayout by unsafeLazy {
        findViewById<AppBarLayout>(R.id.toolbar_container)
    }

    private val toolbar: Toolbar by unsafeLazy {
        findViewById<Toolbar>(R.id.toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(getLayoutId())

        setSupportActionBar(toolbar)
    }

    @LayoutRes
    open fun getLayoutId(): Int {
        return R.layout.appbar_activity
    }

    override fun setContentView(layoutResID: Int) {
        layoutInflater.inflate(layoutResID, rootView, true)
        rootView.bringChildToFront(toolbarContainer)
    }

    override fun setContentView(view: View?) {
        setContentView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        rootView.addView(view, 0, params)
    }

}

abstract class AppBarFragmentActivity : AppBarActivity() {

    abstract fun createFragment(): Fragment

    override fun getLayoutId(): Int = R.layout.appbar_fragment_activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, createFragment())
                .commit()
        }
    }
    
}
