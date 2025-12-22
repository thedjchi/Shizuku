package moe.shizuku.manager.stealth

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowInsets.Type
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable
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
                        fab.animate(this@StealthTutorialActivity, state.menuOpen)
                        fabMenu.animate(state.menuOpen)
                        if (state.menuOpen) {
                            fab.setImageResource(R.drawable.ic_close_24)
                            fab.setOnClickListener { viewModel.onCloseUnhideMenu() }
                        } else {
                            fab.setImageResource(R.drawable.ic_visibility_on_filled_24)
                            fab.setOnClickListener { viewModel.onOpenUnhideMenu() }
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

                if (isKeyboardVisible) fab.hide() else fab.show()
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

    private fun <T> ValueAnimator.animate(update: (T) -> Unit) {
        duration = 300
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            @Suppress("UNCHECKED_CAST")
            update(it.animatedValue as T)
        }
        start()
    }

    private fun FloatingActionButton.animate(
        context: Context,
        menuOpen: Boolean,
    ) {
        ValueAnimator
            .ofArgb(
                backgroundTintList?.defaultColor ?: Color.TRANSPARENT,
                context.resolveColor(if (menuOpen) R.attr.colorPrimary else R.attr.colorPrimaryContainer),
            ).animate<Int> { backgroundTintList = ColorStateList.valueOf(it) }

        ValueAnimator
            .ofArgb(
                imageTintList?.defaultColor ?: Color.TRANSPARENT,
                context.resolveColor(if (menuOpen) R.attr.colorOnPrimary else R.attr.colorOnPrimaryContainer),
            ).animate<Int> { imageTintList = ColorStateList.valueOf(it) }

        ValueAnimator
            .ofFloat(
                if (menuOpen) 0.25f else 0.5f,
                if (menuOpen) 0.5f else 0.25f,
            ).animate<Float> {
                shapeAppearanceModel =
                    shapeAppearanceModel
                        .toBuilder()
                        .setAllCornerSizes(RelativeCornerSize(it))
                        .build()
            }
    }

    private fun ViewGroup.animate(shouldShow: Boolean) {
        val transition =
            MaterialSharedAxis(MaterialSharedAxis.X, shouldShow).apply {
                duration = 300
            }
        TransitionManager.beginDelayedTransition(parent as ViewGroup, transition)
        isVisible = shouldShow
    }

    private fun Context.resolveColor(color: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(color, typedValue, true)
        val resolvedColor = typedValue.data

        return resolvedColor
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
