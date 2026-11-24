/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility.setupwizard.items

import android.annotation.IntDef
import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants.CLOCK_TICK
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageView
import com.android.settings.R
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.slider.TickVisibilityMode
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import com.google.android.setupdesign.items.Item
import kotlin.math.abs

/** A custom slider item used in Setup Wizard accessibility screens. */
class SliderItem : Item {

    private lateinit var dimensions: SliderDimensions

    // Data class to host track-related dimensions
    private data class SliderDimensions(
        val trackHeight: Int,
        val trackInsideCornerSize: Int,
        val trackStopIndicatorSize: Int,
        val thumbWidth: Int,
        val thumbHeight: Int,
        val thumbElevation: Int,
        val thumbStrokeWidth: Int,
        val thumbTrackGapSize: Int,
        val tickRadius: Int,
    )

    private val buttonAccessibilityDelegate =
        object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Button::class.java.name
            }
        }

    /**
     * Listener reacting to the user pressing DPAD left/right keys if {@code adjustable} attribute
     * is set to true; it transfers the key presses to the {@link Slider} to be handled accordingly.
     */
    private val sliderKeyListener =
        View.OnKeyListener { v, keyCode, event ->
            // Only handle initial key presses
            if (event.action != KeyEvent.ACTION_DOWN) return@OnKeyListener false

            when (keyCode) {
                // Ignore DPAD navigation if the slider is disabled/non-adjustable
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    // Right or left keys are pressed when in non-adjustable mode; Skip the keys.
                    if (!adjustable) return@OnKeyListener false
                }

                // We don't want to propagate the click keys down to the Slider since it will
                // create the ripple effect for the thumb.
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    return@OnKeyListener false
                }
            }

            // Delegate valid key events to the slider instance
            v.findViewById<Slider>(R.id.slider)?.onKeyDown(keyCode, event) ?: false
        }

    private val sliderTouchListener =
        object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isTrackingTouch = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isTrackingTouch = false
                syncValueInternal(slider)
            }
        }

    private val sliderChangeListener =
        Slider.OnChangeListener { slider, value, fromUser ->
            if (fromUser && (updatesContinuously || !isTrackingTouch)) {
                syncValueInternal(slider)
            }
        }

    var sliderValue: Int = 0
        set(value) {
            val validated = value.coerceIn(min, max)
            if (field != validated) {
                field = validated
                notifyChanged()
            }
        }

    private var _sliderIncrement: Int = 0
    var sliderIncrement: Int
        get() = _sliderIncrement
        set(value) {
            val validated = (max - min).coerceAtMost(abs(value))
            if (_sliderIncrement != validated) {
                _sliderIncrement = validated
                notifyChanged()
            }
        }

    var min: Int = 0
        set(value) {
            if (field != value) {
                field = value
                // Re-validate sliderValue if min moves above it
                sliderValue = sliderValue
                notifyChanged()
            }
        }

    var max: Int = 100
        set(value) {
            val validated = value.coerceAtLeast(min)
            if (field != validated) {
                field = validated
                // Re-validate sliderValue if max moves below it
                sliderValue = sliderValue
                notifyChanged()
            }
        }

    var iconStartId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var iconStartContentDescriptionId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var iconEndId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var iconEndContentDescriptionId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var tickVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var showSliderValue: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var sliderContentDescription: CharSequence? = null
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var sliderStateDescription: CharSequence? = null
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    @HapticFeedbackMode var hapticFeedbackMode: Int = HAPTIC_FEEDBACK_MODE_NONE
    var isTrackingTouch: Boolean = false

    var updatesContinuously: Boolean = false

    var adjustable: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.obtainStyledAttributes(attrs, R.styleable.SliderItem).use { a ->
            // The ordering of these two statements are important. If we want to set max first, we
            // need to perform the same steps by changing min/max to max/min as following:
            // mMax = a.getInt(...) and setMin(...).
            min = a.getInt(R.styleable.SliderItem_min, min)
            max = a.getInt(R.styleable.SliderItem_max, max)
            adjustable = a.getBoolean(R.styleable.SliderItem_adjustable, adjustable)
            updatesContinuously =
                a.getBoolean(R.styleable.SliderItem_updatesContinuously, updatesContinuously)
            sliderIncrement = a.getInt(R.styleable.SliderItem_seekBarIncrement, sliderIncrement)
            showSliderValue = a.getBoolean(R.styleable.SliderItem_showSliderValue, showSliderValue)
            iconStartId = a.getResourceId(R.styleable.SliderItem_iconStart, iconStartId)
            iconStartContentDescriptionId =
                a.getResourceId(
                    R.styleable.SliderItem_iconStartContentDescription,
                    iconStartContentDescriptionId,
                )
            iconEndId = a.getResourceId(R.styleable.SliderItem_iconEnd, iconEndId)
            iconEndContentDescriptionId =
                a.getResourceId(
                    R.styleable.SliderItem_iconEndContentDescription,
                    iconEndContentDescriptionId,
                )
        }
    }

    override fun getDefaultLayoutResource(): Int = R.layout.setup_items_slider

    override fun onBindView(view: View) {
        super.onBindView(view)
        if (!::dimensions.isInitialized) {
            initDimensions(view.context)
        }

        view.setOnKeyListener(sliderKeyListener)
        view.isClickable = false

        view.findViewById<Slider>(R.id.slider)?.apply {
            labelBehavior =
                if (showSliderValue) {
                    LabelFormatter.LABEL_FLOATING
                } else {
                    LabelFormatter.LABEL_GONE
                }

            if (sliderIncrement != 0) {
                stepSize = sliderIncrement.toFloat()
                tickVisibilityMode = TickVisibilityMode.TICK_VISIBILITY_AUTO_LIMIT
            } else {
                // If no increment provided, sync model with default slider step
                _sliderIncrement = stepSize.toInt()
            }

            val title = getTitle()
            if (!sliderContentDescription.isNullOrEmpty()) {
                view.setContentDescription(sliderContentDescription)
                setContentDescription(sliderContentDescription)
            } else if (!title.isNullOrEmpty()) {
                setContentDescription(title)
            } else {
                setContentDescription(null)
            }
            if (!sliderStateDescription.isNullOrEmpty()) {
                setStateDescription(sliderStateDescription)
            } else {
                setStateDescription(null)
            }

            val hasRange = max > min
            valueFrom = min.toFloat()
            valueTo = if (hasRange) max.toFloat() else (min + 1).toFloat()
            value = sliderValue.toFloat().coerceIn(valueFrom, valueTo)
            if (!hasRange) isEnabled = false
            clearOnSliderTouchListeners()
            addOnSliderTouchListener(sliderTouchListener)
            clearOnChangeListeners()
            addOnChangeListener(sliderChangeListener)
            isClickable = false

            if (PartnerConfigHelper.isGlifExpressiveEnabled(context)) {
                applyExpressiveStyles(dimensions)
            }
        }

        updateIconStartIfNeeded(view.findViewById(R.id.icon_start))
        updateIconEndIfNeeded(view.findViewById(R.id.icon_end))
    }

    private fun initDimensions(context: Context) {
        val res = context.resources
        dimensions =
            SliderDimensions(
                trackHeight = res.getDimensionPixelSize(R.dimen.sud_slider_track_height),
                trackInsideCornerSize =
                    res.getDimensionPixelSize(R.dimen.sud_slider_track_inside_corner_size),
                trackStopIndicatorSize =
                    res.getDimensionPixelSize(R.dimen.sud_slider_track_stop_indicator_size),
                thumbWidth = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_width),
                thumbHeight = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_height),
                thumbElevation = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_elevation),
                thumbStrokeWidth = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_stroke_width),
                thumbTrackGapSize =
                    res.getDimensionPixelSize(R.dimen.sud_slider_thumb_track_gap_size),
                tickRadius = res.getDimensionPixelSize(R.dimen.sud_slider_tick_radius),
            )
    }

    private fun Slider.applyExpressiveStyles(dims: SliderDimensions) {
        trackHeight = dims.trackHeight
        trackInsideCornerSize = dims.trackInsideCornerSize
        trackStopIndicatorSize = dims.trackStopIndicatorSize
        thumbWidth = dims.thumbWidth
        thumbHeight = dims.thumbHeight
        thumbElevation = dims.thumbElevation.toFloat()
        thumbStrokeWidth = dims.thumbStrokeWidth.toFloat()
        thumbTrackGapSize = dims.thumbTrackGapSize
        tickActiveRadius = dims.tickRadius
        tickInactiveRadius = dims.tickRadius
    }

    private fun updateIconStartIfNeeded(iconView: ImageView?) {
        val iconFrame = iconView?.parent as? ViewGroup ?: return
        if (iconStartId == 0 || sliderIncrement == 0) {
            iconFrame.visibility = View.GONE
            return
        }

        iconView.setImageResource(iconStartId)
        if (iconStartContentDescriptionId != 0) {
            iconFrame.contentDescription =
                iconFrame.context.getString(iconStartContentDescriptionId)
        }

        iconFrame.setOnClickListener { if (sliderValue > min) sliderValue -= sliderIncrement }
        iconFrame.visibility = View.VISIBLE
        setIconViewAndFrameEnabled(iconView, iconFrame, sliderValue > min)
    }

    private fun updateIconEndIfNeeded(iconView: ImageView?) {
        val iconFrame = iconView?.parent as? ViewGroup ?: return
        if (iconEndId == 0 || sliderIncrement == 0) {
            iconFrame.visibility = View.GONE
            return
        }

        iconView.setImageResource(iconEndId)
        if (iconEndContentDescriptionId != 0) {
            iconFrame.contentDescription = iconFrame.context.getString(iconEndContentDescriptionId)
        }

        iconFrame.setOnClickListener { if (sliderValue < max) sliderValue += sliderIncrement }
        iconFrame.visibility = View.VISIBLE
        setIconViewAndFrameEnabled(iconView, iconFrame, sliderValue < max)
    }

    private fun setIconViewAndFrameEnabled(iconView: View, iconFrame: ViewGroup, enabled: Boolean) {
        iconView.isEnabled = enabled
        iconFrame.isEnabled = enabled
        iconFrame.accessibilityDelegate = buttonAccessibilityDelegate
    }

    private fun syncValueInternal(slider: Slider) {
        val newValue = slider.value.toInt()
        if (newValue != sliderValue) {
            sliderValue = newValue
            when (hapticFeedbackMode) {
                HAPTIC_FEEDBACK_MODE_ON_TICKS -> slider.performHapticFeedback(CLOCK_TICK)
                HAPTIC_FEEDBACK_MODE_ON_ENDS -> {
                    if (sliderValue == max || sliderValue == min) {
                        slider.performHapticFeedback(CLOCK_TICK)
                    }
                }
            }
        }
    }

    @IntDef(HAPTIC_FEEDBACK_MODE_NONE, HAPTIC_FEEDBACK_MODE_ON_TICKS, HAPTIC_FEEDBACK_MODE_ON_ENDS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class HapticFeedbackMode

    companion object {
        const val HAPTIC_FEEDBACK_MODE_NONE = 0
        const val HAPTIC_FEEDBACK_MODE_ON_TICKS = 1
        const val HAPTIC_FEEDBACK_MODE_ON_ENDS = 2
    }
}
