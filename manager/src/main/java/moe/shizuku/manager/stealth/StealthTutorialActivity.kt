package moe.shizuku.manager.stealth

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowInsets.Type
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.transition.MaterialSharedAxis
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.StealthTutorialActivityBinding

class StealthTutorialActivity : AppBarActivity() {
    private val viewModel: StealthTutorialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = StealthTutorialActivityBinding.inflate(layoutInflater)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            setContentView(root)

            viewModel.uiState.observe(this@StealthTutorialActivity) { state ->
                packageNameEditText.isEnabled = state is UiState.Hide

                when (state) {
                    UiState.Hide -> {
                        fab.setImageResource(R.drawable.ic_visibility_off_filled_24)
                        fab.setOnClickListener { viewModel.onHide() }

                        packageNameLayout.helperText = getString(R.string.stealth_package_name_helper_text)
                    }

                    is UiState.Unhide -> {
                        fab.applyColors(this@StealthTutorialActivity, state.menuOpen)
                        if (state.menuOpen) {
                            fab.shapeAppearanceModel = circleShape
                            fab.setImageResource(R.drawable.ic_close_24)
                            fab.setOnClickListener { viewModel.onCloseUnhideMenu() }
                            fabMenu.animate(shouldShow = true)
                        } else {
                            fab.shapeAppearanceModel = defaultShape
                            fab.setImageResource(R.drawable.ic_visibility_on_filled_24)
                            fab.setOnClickListener { viewModel.onOpenUnhideMenu() }
                            fabMenu.animate(shouldShow = false)
                        }

                        packageNameEditText.setText(ORIGINAL_PACKAGE_NAME)
                    }
                }
            }

            packageNameEditText.apply {
                addTextChangedListener { text ->
                    val input = text?.toString().orEmpty()

                    packageNameLayout.error =
                        input.validatePackageName()?.let { error -> getString(error) }
                }
            }

            root.setOnApplyWindowInsetsListener { _, insets ->
                val imeHeight = insets.getInsets(Type.ime()).bottom
                val isKeyboardVisible = imeHeight > 0

                if (isKeyboardVisible) fab.hide()
                else fab.show()

                insets
            }

            scrollView.setOnTouchListener { _, _ ->
                packageNameEditText.isFocused
            }

            unhideTemp.setOnClickListener {
                viewModel.onUnhide(temporary = true)
            }
            unhidePerm.setOnClickListener {
                viewModel.onUnhide(temporary = false)
            }
        }
    }

    private fun FloatingActionButton.applyColors(
        context: Context,
        menuOpen: Boolean
    ) {
        val bgColor = if (menuOpen) R.attr.colorPrimary else R.attr.colorPrimaryContainer
        val iconColor = if (menuOpen) R.attr.colorOnPrimary else R.attr.colorOnPrimaryContainer

        backgroundTintList = context.resolveColor(bgColor)
        imageTintList = context.resolveColor(iconColor)
    }

    private fun ViewGroup.animate(shouldShow: Boolean) {
        val transition = MaterialSharedAxis(MaterialSharedAxis.X, shouldShow).apply {
            duration = 300
        }
        TransitionManager.beginDelayedTransition(this, transition)

        val indices = if (shouldShow) 0 until childCount else (childCount - 1 downTo 0)

        for ((step, i) in indices.withIndex()) {
            val child = getChildAt(i)
            child.postDelayed({
                child.isVisible = shouldShow
            }, step * 40L)
        }

        this.isVisible = shouldShow
    }

    private fun Context.resolveColor(color: Int): ColorStateList {
        val typedValue = TypedValue()
        theme.resolveAttribute(color, typedValue, true)
        val resolvedColor = typedValue.data

        return ColorStateList.valueOf(resolvedColor)
    }

    companion object {
        private val defaultShape by lazy {
            ShapeAppearanceModel.builder()
                .setAllCornerSizes(RelativeCornerSize(0.2f))
                .build()
        }
        private val circleShape by lazy {
            ShapeAppearanceModel.builder()
                .setAllCornerSizes(RelativeCornerSize(0.5f))
                .build()
        }
    }

}

private const val codeSnippet = """
    import android.content.Context
    import rikka.shizuku.ShizukuProvider

    private fun Context.shizukuPermission() =
        runCatching {
            packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
        }.getOrNull()

    fun Context.isShizukuInstalled() =
        shizukuPermission() != null

    fun Context.getShizukuPackageName() =
        shizukuPermission()?.packageName
"""
