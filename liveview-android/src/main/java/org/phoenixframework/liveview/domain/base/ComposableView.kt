package org.phoenixframework.liveview.domain.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import org.phoenixframework.liveview.data.constants.Attrs.attrAlign
import org.phoenixframework.liveview.data.constants.Attrs.attrAspectRatio
import org.phoenixframework.liveview.data.constants.Attrs.attrBackground
import org.phoenixframework.liveview.data.constants.Attrs.attrClass
import org.phoenixframework.liveview.data.constants.Attrs.attrClip
import org.phoenixframework.liveview.data.constants.Attrs.attrExposedDropdownSize
import org.phoenixframework.liveview.data.constants.Attrs.attrHeight
import org.phoenixframework.liveview.data.constants.Attrs.attrHorizontalPadding
import org.phoenixframework.liveview.data.constants.Attrs.attrMatchParentSize
import org.phoenixframework.liveview.data.constants.Attrs.attrMenuAnchor
import org.phoenixframework.liveview.data.constants.Attrs.attrPadding
import org.phoenixframework.liveview.data.constants.Attrs.attrPhxClick
import org.phoenixframework.liveview.data.constants.Attrs.attrPhxValue
import org.phoenixframework.liveview.data.constants.Attrs.attrPhxValueNamed
import org.phoenixframework.liveview.data.constants.Attrs.attrSize
import org.phoenixframework.liveview.data.constants.Attrs.attrTestTag
import org.phoenixframework.liveview.data.constants.Attrs.attrVerticalPadding
import org.phoenixframework.liveview.data.constants.Attrs.attrWeight
import org.phoenixframework.liveview.data.constants.Attrs.attrWidth
import org.phoenixframework.liveview.data.constants.ScrollingValues
import org.phoenixframework.liveview.data.constants.SizeValues
import org.phoenixframework.liveview.data.core.CoreAttribute
import org.phoenixframework.liveview.data.dto.ExposedDropdownMenuBoxScopeWrapper
import org.phoenixframework.liveview.data.dto.alignmentFromString
import org.phoenixframework.liveview.data.dto.horizontalAlignmentFromString
import org.phoenixframework.liveview.data.dto.onClickFromString
import org.phoenixframework.liveview.data.dto.verticalAlignmentFromString
import org.phoenixframework.liveview.data.mappers.modifiers.ModifiersParser.fromStyleName
import org.phoenixframework.liveview.domain.base.ComposableBuilder.Companion.KEY_PHX_VALUE
import org.phoenixframework.liveview.domain.extensions.isNotEmptyAndIsDigitsOnly
import org.phoenixframework.liveview.domain.extensions.toColor
import org.phoenixframework.liveview.domain.factory.ComposableTreeNode
import org.phoenixframework.liveview.ui.theme.shapeFromString

/**
 *  A `ComposableView` is the parent class of all components. Subclasses must implement the
 *  `Composable` function in order to call the real composable. The `modifier` param and all
 *  necessary params for the actual component implementation should be provided by a subclass of
 *  `ComposableBuilder`. In order to make a component available, a subclass of
 *  `ComposableViewFactory` must be implemented, the it must be registered on `ComposableRegistry`
 *  object informing the respective tag for the composable.
 */
abstract class ComposableView<CP : ComposableProperties>(protected open val props: CP) {

    @Composable
    abstract fun Compose(
        composableNode: ComposableTreeNode?, paddingValues: PaddingValues?, pushEvent: PushEvent
    )

    // This function is used to merge/join "on changed" values with the component value(s)
    // (phx-value and phx-value-*). For example, when a checkbox changes its checked state, the new
    // checked value (true/false) and the checkbox values (phx-value/phx-value-*) are sent to server
    protected fun mergeValueWithPhxValue(key: String, value: Any): Any {
        val currentPhxValue = props.commonProps.phxValue
        return if (currentPhxValue == null) {
            if (key == KEY_PHX_VALUE) {
                value
            } else {
                mapOf(key to value)
            }
        } else if (props.commonProps.value.size == 1 && props.commonProps.value.containsKey(
                KEY_PHX_VALUE
            )
        ) {
            if (key == KEY_PHX_VALUE) {
                value
            } else {
                val newMap = props.commonProps.value.toMutableMap()
                newMap[key] = value
                newMap
            }
        } else {
            val newMap = props.commonProps.value.toMutableMap()
            newMap[key] = value
            newMap
        }
    }
}

interface ComposableProperties {
    val commonProps: CommonComposableProperties
}

@Stable
data class CommonComposableProperties(
    val hasVerticalScrolling: Boolean,
    val hasHorizontalScrolling: Boolean,
    val modifier: Modifier,
    val value: ImmutableMap<String, Any>
) {
    val phxValue: Any?
        get() = if (value.isEmpty())
            null
        else if (value.size == 1 && value.containsKey(KEY_PHX_VALUE)) {
            value[KEY_PHX_VALUE]
        } else {
            value
        }
}

/**
 *  A `ComposableBuilder` is responsible to handle attributes declared in the LiveView tags. This
 *  class must parse and convert the `CoreAttribute` value in a corresponding property or `Modifier`
 *  to be used in the `ComposableView` implementation. All the required information required by a
 *  `ComposableView` must be provided by the respective `ComposableBuilder`.
 */
abstract class ComposableBuilder {
    var commonProps = CommonComposableProperties(
        hasVerticalScrolling = false,
        hasHorizontalScrolling = false,
        modifier = Modifier,
        value = persistentMapOf()
    )
        private set

    /**
     * Declare the preferred size (width and height) of a Composable. You can specify the exactly
     * [size] dp square, or:
     * - use `fill` to occupy the max available area;
     * - or use `wrap` to occupy just the minimum space requested by the children.
     * ```
     * <ComposableView size="fill" />
     * // or
     * <ComposableView size="16" />
     * ```
     * @param size the size of a `ComposableView` instance.
     */
    private fun size(size: String) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = when {
                size.isNotEmptyAndIsDigitsOnly() -> modifier.then(Modifier.size(size = size.toInt().dp))
                size == SizeValues.fill -> modifier.then(Modifier.fillMaxSize())
                size == SizeValues.wrap -> modifier.then(Modifier.wrapContentSize())
                else -> modifier
            }
        )
    }

    /**
     * Apply [padding] dp of additional space along each edge of the content, left, top, right and
     * bottom.
     * ```
     * <ComposableView padding="8" />
     * ```
     * @param padding int value for padding to be applied to the four edges.
     */
    private fun padding(padding: String) = apply {
        if (padding.isNotEmptyAndIsDigitsOnly()) {
            val modifier = this.commonProps.modifier
            this.commonProps =
                this.commonProps.copy(modifier = modifier.then(Modifier.padding(padding.toInt().dp)))
        }
    }

    /**
     * Apply [padding] dp space along the top and bottom edges of the content.
     * ```
     * <ComposableView verticalPadding="16" />
     * ```
     * @param padding int value for padding to be applied on top and bottom edges.
     */
    internal fun paddingVertical(padding: String) = apply {
        if (padding.isNotEmptyAndIsDigitsOnly()) {
            val modifier = this.commonProps.modifier
            this.commonProps = this.commonProps.copy(
                modifier = modifier.then(Modifier.padding(vertical = padding.toInt().dp))
            )
        }
    }

    /**
     * Apply [padding] dp space along the left and right edges of the content.
     * ```
     * <ComposableView horizontalPadding="16" />
     * ```
     * @param padding int value for padding to be applied on left and right edges.
     */
    internal fun paddingHorizontal(padding: String) = apply {
        if (padding.isNotEmptyAndIsDigitsOnly()) {
            val modifier = this.commonProps.modifier
            this.commonProps = this.commonProps.copy(
                modifier = modifier.then(Modifier.padding(horizontal = padding.toInt().dp))
            )
        }
    }

    /**
     * Declare the preferred height of the content to be exactly [height] dp, or:
     * - use `fill` to occupy the max available height;
     * - or use `wrap` to occupy just the minimum height requested by the children.
     * - use `intrinsicMin` to use the minimum intrinsic size.
     * - use `intrinsicMax` to use the maximum intrinsic size.
     * ```
     * <ComposableView height="fill" />
     * <ComposableView height="16" />
     * <ComposableView height="intrinsicMin" />
     * <ComposableView height="75%" />
     * ```
     * @param height int value for preferred component height.
     */
    private fun height(height: String) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = when {
                height.isNotEmptyAndIsDigitsOnly() -> modifier.then(Modifier.height(height.toInt().dp))
                height == SizeValues.fill -> modifier.then(Modifier.fillMaxHeight())
                height == SizeValues.wrap -> modifier.then(Modifier.wrapContentHeight())
                height == SizeValues.intrinsicMin -> modifier.then(Modifier.height(IntrinsicSize.Min))
                height == SizeValues.intrinsicMax -> modifier.then(Modifier.height(IntrinsicSize.Max))
                height.endsWith('%') -> {
                    handleFraction(height)?.let {
                        modifier.then(Modifier.fillMaxHeight(it))
                    } ?: modifier
                }

                else -> modifier
            })
    }

    private fun handleFraction(value: String): Float? {
        if (value.length > 1) {
            // Value without the '%' sign
            val percentageStr = value.substring(0, value.lastIndex)
            if (percentageStr.isNotEmptyAndIsDigitsOnly()) {
                return percentageStr.toFloat() / 100f
            }
        }
        return null
    }

    /**
     * Declare the preferred width of the content to be exactly [width] dp, or:
     * - use `fill` to occupy the max available width;
     * - use `wrap` to occupy just the minimum width requested by the children.
     * - use `intrinsicMin` to use the minimum intrinsic size.
     * - use `intrinsicMax` to use the maximum intrinsic size.
     * ```
     * <ComposableView width="fill" />
     * <ComposableView width="16" />
     * <ComposableView width="intrinsicMin" />
     * <ComposableView width="75%" />
     * ```
     * @param width int value for preferred component width.
     */
    private fun width(width: String) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = when {
                width.isNotEmptyAndIsDigitsOnly() -> modifier.then(Modifier.width(width.toInt().dp))
                width == SizeValues.fill -> modifier.then(Modifier.fillMaxWidth())
                width == SizeValues.wrap -> modifier.then(Modifier.wrapContentWidth())
                width == SizeValues.intrinsicMin -> modifier.then(Modifier.width(IntrinsicSize.Min))
                width == SizeValues.intrinsicMax -> modifier.then(Modifier.width(IntrinsicSize.Max))
                width.endsWith('%') -> {
                    handleFraction(width)?.let {
                        modifier.then(Modifier.fillMaxWidth(it))
                    } ?: modifier
                }

                else -> modifier
            }
        )
    }

    /**
     *  Clip the content to shape.
     * ```
     * <ComposableView clip="circle" />
     * ```
     * @param shape the content will be clipped to this. Supported values are: `circle`,
     * `rectangle`, or an integer representing the curve size applied for all four corners.
     */
    private fun clip(shape: String) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = modifier.then(
                Modifier.clip(shapeFromString(shape))
            )
        )
    }

    /**
     * Sets the event name to be triggered on the server when the composable is clicked.
     *
     * ```
     * <Composable phx-click="yourServerEventHandler" />
     * ```
     * @param event event name defined on the server to handle the composable's click.
     * @param pushEvent function responsible to dispatch the server call.
     */
    private fun clickable(event: String, pushEvent: PushEvent?) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = modifier.then(
                Modifier.clickable {
                    onClickFromString(pushEvent, event, this.commonProps.phxValue).invoke()
                }
            )
        )
    }

    /**
     * Sets the phx-value binding.
     *
     * ```
     * <Composable phx-value="someValue" />
     * ```
     * @param value event name defined on the server to handle the composable's click.
     */
    internal fun value(attributeName: String, value: Any) = apply {
        if (attributeName == attrPhxValue) {
            val newMap = this.commonProps.value.toMutableMap()
            newMap[KEY_PHX_VALUE] = value
            this.commonProps = this.commonProps.copy(value = newMap.toImmutableMap())
        } else if (attributeName.startsWith(attrPhxValueNamed)) {
            val phxValueKey = attributeName.substring(attrPhxValueNamed.length)
            val newMap = this.commonProps.value.toMutableMap()
            newMap[phxValueKey] = value
            this.commonProps = this.commonProps.copy(value = newMap.toImmutableMap())
        }
    }

    /**
     * Modify element to allow to scroll when size of the content is bigger than max size available
     * for it.
     *
     * ```
     * <Composable scroll="vertical" />
     * <Composable scroll="both" />
     * ```
     * @param scrolling scroll direction. Supported values are: `vertical`, `horizontal`, and `both`.
     */
    fun scrolling(scrolling: String) = apply {
        this.commonProps = this.commonProps.copy(
            hasHorizontalScrolling =
            scrolling == ScrollingValues.horizontal || scrolling == ScrollingValues.both,
            hasVerticalScrolling =
            scrolling == ScrollingValues.vertical || scrolling == ScrollingValues.both,
        )
    }

    /**
     * Draws shape with a solid color behind the content.
     *
     * ```
     * <Composable background="#FF0000FF" />
     * ```
     * @param background the background color in AARRGGBB format.
     */
    private fun background(background: String) = apply {
        if (background.isNotEmpty()) {
            val modifier = this.commonProps.modifier
            this.commonProps = this.commonProps.copy(
                modifier = modifier.then(Modifier.background(background.toColor()))
            )
        }
    }

    /**
     * Attempts to size the content to match a specified aspect ratio by trying to match one of the
     * incoming constraints.
     *
     * ```
     * <Composable aspectRatio={"#{4/3}"} />
     * ```
     * @param aspectRatio a floating number representing the aspect ratio.
     */
    private fun aspectRatio(aspectRatio: String) = apply {
        if (aspectRatio.isNotEmpty()) {
            val modifier = this.commonProps.modifier
            this.commonProps = this.commonProps.copy(
                modifier = modifier.then(Modifier.aspectRatio(aspectRatio.toFloat()))
            )
        }
    }

    /**
     * Tag used during the UI tests. It must be unique in the UI tree.
     * ```
     * <Composable testTag="myTag" />
     * ```
     * @param testTag tag used during the UI tests.
     */
    private fun testTag(testTag: String) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = modifier.then(Modifier.testTag(testTag))
        )
    }

    private fun modifier(string: String, scope: Any?, pushEvent: PushEvent?) = apply {
        val modifier = this.commonProps.modifier
        this.commonProps = this.commonProps.copy(
            modifier = modifier.then(Modifier.fromStyleName(string, scope, pushEvent))
        )
    }

    /**
     * Handle the properties that are common for most of composables.
     * @param attribute a `CoreAttribute` to be handled.
     * @param pushEvent function responsible to dispatch the server call.
     * @param scope some attributes are composable specific, the scope determine what parent
     * composable (e.g.: `Column`, `Row`, `Box`).
     */
    @OptIn(ExperimentalMaterial3Api::class)
    fun handleCommonAttributes(
        attribute: CoreAttribute,
        pushEvent: PushEvent?,
        scope: Any?,
    ): ComposableBuilder {
        when (attribute.name) {
            attrAspectRatio -> aspectRatio(attribute.value)
            attrBackground -> background(attribute.value)
            attrClass -> modifier(attribute.value, scope, pushEvent)
            attrClip -> clip(attribute.value)
            attrHeight -> height(attribute.value)
            attrHorizontalPadding -> paddingHorizontal(attribute.value)
            attrPadding -> padding(attribute.value)
            attrPhxClick -> clickable(attribute.value, pushEvent)
            attrPhxValue -> value(attrPhxValue, attribute.value)
            attrSize -> size(attribute.value)
            attrTestTag -> testTag(attribute.value)
            attrVerticalPadding -> paddingVertical(attribute.value)
            attrWidth -> width(attribute.value)
            else ->
                if (attribute.name.startsWith(attrPhxValueNamed)) {
                    value(attribute.name, attribute.value)
                }
        }
        when (scope) {
            is BoxScope -> {
                when (attribute.name) {
                    attrAlign -> scope.run {
                        val modifier = this@ComposableBuilder.commonProps.modifier
                        this@ComposableBuilder.commonProps =
                            this@ComposableBuilder.commonProps.copy(
                                modifier = modifier.then(
                                    Modifier.align(
                                        alignmentFromString(
                                            attribute.value,
                                            Alignment.TopStart
                                        )
                                    )
                                )
                            )
                    }

                    attrMatchParentSize -> scope.run {
                        if (attribute.value.toBoolean()) {
                            val modifier = this@ComposableBuilder.commonProps.modifier
                            this@ComposableBuilder.commonProps =
                                this@ComposableBuilder.commonProps.copy(
                                    modifier = modifier.then(Modifier.matchParentSize())
                                )
                        }
                    }
                }
            }

            is ColumnScope -> {
                when (attribute.name) {
                    attrWeight -> scope.run {
                        attribute.value.toFloatOrNull()?.let {
                            val modifier = this@ComposableBuilder.commonProps.modifier
                            this@ComposableBuilder.commonProps =
                                this@ComposableBuilder.commonProps.copy(
                                    modifier = modifier.then(Modifier.weight(it))
                                )
                        }
                    }

                    attrAlign -> scope.run {
                        val modifier = this@ComposableBuilder.commonProps.modifier
                        this@ComposableBuilder.commonProps =
                            this@ComposableBuilder.commonProps.copy(
                                modifier = modifier.then(
                                    Modifier.align(horizontalAlignmentFromString(attribute.value))
                                )
                            )
                    }
                }
            }

            is RowScope -> {
                when (attribute.name) {
                    attrWeight -> scope.run {
                        attribute.value.toFloatOrNull()?.let {
                            val modifier = this@ComposableBuilder.commonProps.modifier
                            this@ComposableBuilder.commonProps =
                                this@ComposableBuilder.commonProps.copy(
                                    modifier = modifier.then(Modifier.weight(it))
                                )
                        }
                    }

                    attrAlign -> scope.run {
                        val modifier = this@ComposableBuilder.commonProps.modifier
                        this@ComposableBuilder.commonProps =
                            this@ComposableBuilder.commonProps.copy(
                                modifier = modifier.then(
                                    Modifier.align(verticalAlignmentFromString(attribute.value))
                                )
                            )
                    }
                }
            }

            is ExposedDropdownMenuBoxScopeWrapper -> {
                when (attribute.name) {
                    attrMenuAnchor -> scope.scope.run {
                        val modifier = this@ComposableBuilder.commonProps.modifier
                        this@ComposableBuilder.commonProps =
                            this@ComposableBuilder.commonProps.copy(
                                modifier = modifier.then(Modifier.menuAnchor())
                            )
                    }

                    attrExposedDropdownSize -> scope.scope.run {
                        val modifier = this@ComposableBuilder.commonProps.modifier
                        this@ComposableBuilder.commonProps =
                            this@ComposableBuilder.commonProps.copy(
                                modifier =
                                modifier.then(Modifier.exposedDropdownSize(attribute.value.toBoolean()))
                            )
                    }
                }
            }
        }
        return this
    }

    companion object {
        internal const val EVENT_TYPE_CLICK = "click"
        internal const val EVENT_TYPE_CHANGE = "change"
        internal const val EVENT_TYPE_KEY_UP = "keyup"
        internal const val EVENT_TYPE_BLUR = "blur"
        internal const val EVENT_TYPE_SUBMIT = "submit"

        internal const val KEY_PHX_VALUE = "value"
    }
}

/**
 * A `ComposableViewFactory` is responsible to create a `ComposableView` using a list of attributes.
 */
abstract class ComposableViewFactory<CV : ComposableView<*>> {

    /**
     * Create a new instance of a `ComposableView`. Subclasses of this class must override this
     * method and handle specific attributes. Common attributes are handled by the
     * `handleCommonAttributes` declared in the `ComposableBuilder` class and should be called.
     * @param attributes a list of `CoreAttribute` to be handled.
     * @param pushEvent function responsible to dispatch the server call.
     * @param scope some attributes are composable specific, the scope determine what parent
     * composable (e.g.: `Column`, `Row`, `Box`).
     */
    abstract fun buildComposableView(
        attributes: ImmutableList<CoreAttribute>,
        pushEvent: PushEvent?,
        scope: Any?
    ): CV

    /**
     * Subclasses of ComposableViewFactory can register subtags specific for a particular component.
     * See ComposableRegistry and ComposableNodeFactory for more details.
     */
    open fun subTags(): Map<String, ComposableViewFactory<*>> = emptyMap()
}