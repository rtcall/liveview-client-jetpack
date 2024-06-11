package org.phoenixframework.liveview.data.mappers.modifiers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import org.phoenixframework.liveview.data.constants.ModifierArgs

fun Modifier.horizontalScrollFromStyle(arguments: List<ModifierDataAdapter.ArgumentData>): Modifier {
    val args = argsOrNamedArgs(arguments)
    val enabled = argOrNamedArg(args, ModifierArgs.argEnabled, 0)?.booleanValue ?: true
    val reverseScrolling =
        argOrNamedArg(args, ModifierArgs.argReverseScrolling, 1)?.booleanValue ?: false
    return composed {
        this.then(
            Modifier.horizontalScroll(
                state = rememberScrollState(),
                enabled = enabled,
                reverseScrolling = reverseScrolling,
            )
        )
    }
}

fun Modifier.verticalScrollFromStyle(arguments: List<ModifierDataAdapter.ArgumentData>): Modifier {
    val args = argsOrNamedArgs(arguments)
    val enabled = argOrNamedArg(args, ModifierArgs.argEnabled, 0)?.booleanValue ?: true
    val reverseScrolling =
        argOrNamedArg(args, ModifierArgs.argReverseScrolling, 1)?.booleanValue ?: false
    return composed {
        this.then(
            Modifier.verticalScroll(
                state = rememberScrollState(),
                enabled = enabled,
                reverseScrolling = reverseScrolling,
            )
        )
    }
}