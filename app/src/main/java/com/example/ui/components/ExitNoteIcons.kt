package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoorBack
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SensorWindow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExitNoteTheme

object ExitNoteIcons {
    val Home: ImageVector = Icons.Outlined.Home
    val History: ImageVector = Icons.Outlined.History
    val Checklist: ImageVector = Icons.Outlined.Checklist
    val Settings: ImageVector = Icons.Outlined.Settings
    val Add: ImageVector = Icons.Outlined.Add
    val Edit: ImageVector = Icons.Outlined.Edit
    val Delete: ImageVector = Icons.Outlined.DeleteOutline
    val Confirm: ImageVector = Icons.Outlined.CheckCircle
    val CheckMark: ImageVector = Icons.Outlined.Check
    val Pending: ImageVector = Icons.Outlined.RadioButtonUnchecked
    val DoorLock: ImageVector = Icons.Outlined.Lock
    val FrontDoor: ImageVector = Icons.Outlined.DoorFront
    val BackDoor: ImageVector = Icons.Outlined.DoorBack
    val Stove: ImageVector = Icons.Outlined.LocalFireDepartment
    val Oven: ImageVector = Icons.Outlined.Kitchen
    val Windows: ImageVector = Icons.Outlined.SensorWindow
    val Lights: ImageVector = Icons.Outlined.Lightbulb
    val Keys: ImageVector = Icons.Outlined.Key
    val Wallet: ImageVector = Icons.Outlined.AccountBalanceWallet
    val Phone: ImageVector = Icons.Outlined.PhoneAndroid
    val Receipt: ImageVector = Icons.AutoMirrored.Outlined.ReceiptLong
    val Calendar: ImageVector = Icons.Outlined.CalendarToday
    val Clock: ImageVector = Icons.Outlined.Schedule
    val MoveUp: ImageVector = Icons.Outlined.KeyboardArrowUp
    val MoveDown: ImageVector = Icons.Outlined.KeyboardArrowDown
    val Warning: ImageVector = Icons.Outlined.WarningAmber
    val Info: ImageVector = Icons.Outlined.Info
    val Shield: ImageVector = Icons.Outlined.Shield
    val Close: ImageVector = Icons.Outlined.Close
    val Restore: ImageVector = Icons.Outlined.Restore
    val DefaultItem: ImageVector = Icons.AutoMirrored.Outlined.FactCheck

    val availableIconKeys: List<Pair<String, String>> = listOf(
        "door_lock" to "Door Lock",
        "back_door" to "Back Door",
        "stove" to "Stove / Gas",
        "oven" to "Oven / Kitchen",
        "windows" to "Windows",
        "lights" to "Lights & Fans",
        "keys" to "Keys",
        "wallet" to "Wallet / ID",
        "phone" to "Phone",
        "check" to "General Check"
    )

    fun forKey(iconKey: String?): ImageVector {
        return when (iconKey) {
            "door_lock" -> DoorLock
            "front_door" -> FrontDoor
            "back_door" -> BackDoor
            "stove" -> Stove
            "oven" -> Oven
            "windows" -> Windows
            "lights" -> Lights
            "keys" -> Keys
            "wallet" -> Wallet
            "phone" -> Phone
            else -> DefaultItem
        }
    }
}

/**
 * Renders the approved ExitNote brand icon:
 * A clean outlined document silhouette with a folded corner and prominent confirmation checkmark.
 */
@Composable
fun ExitNoteBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    containerColor: Color = ExitNoteTheme.colors.primaryText,
    contentColor: Color = ExitNoteTheme.colors.primarySurface
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.58f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeWidth = w * 0.105f

            // Document outline with folded top-right corner
            val docPath = Path().apply {
                moveTo(w * 0.18f, h * 0.10f)
                lineTo(w * 0.62f, h * 0.10f)
                lineTo(w * 0.84f, h * 0.32f)
                lineTo(w * 0.84f, h * 0.88f)
                lineTo(w * 0.18f, h * 0.88f)
                close()
            }
            drawPath(
                path = docPath,
                color = contentColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Folded corner crease
            val foldPath = Path().apply {
                moveTo(w * 0.60f, h * 0.12f)
                lineTo(w * 0.60f, h * 0.34f)
                lineTo(w * 0.82f, h * 0.34f)
            }
            drawPath(
                path = foldPath,
                color = contentColor,
                style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Prominent confirmation checkmark inside document
            val checkPath = Path().apply {
                moveTo(w * 0.34f, h * 0.56f)
                lineTo(w * 0.47f, h * 0.69f)
                lineTo(w * 0.69f, h * 0.45f)
            }
            drawPath(
                path = checkPath,
                color = contentColor,
                style = Stroke(width = strokeWidth * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
