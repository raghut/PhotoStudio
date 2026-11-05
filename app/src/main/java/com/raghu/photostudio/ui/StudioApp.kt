package com.raghu.photostudio.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.raghu.photostudio.model.PhotoSize
import com.raghu.photostudio.ui.screens.AdjustScreen
import com.raghu.photostudio.ui.screens.CameraScreen
import com.raghu.photostudio.ui.screens.EditPhotoScreen
import com.raghu.photostudio.ui.screens.HistoryScreen
import com.raghu.photostudio.ui.screens.InterestScreen
import com.raghu.photostudio.ui.screens.PhotoHomeScreen
import com.raghu.photostudio.ui.screens.SettingsScreen
import com.raghu.photostudio.ui.screens.SheetScreen
import com.raghu.photostudio.ui.screens.SizeScreen
import com.raghu.photostudio.ui.screens.ToolsHomeScreen

/** Holds the just-captured photo between the Camera and Adjust screens. */
object CaptureHolder {
    var photo: Bitmap? = null
}

private fun sizeArg(value: String?): PhotoSize =
    PhotoSize.entries.firstOrNull { it.name == value } ?: PhotoSize.PASSPORT

@Composable
fun StudioApp() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            ToolsHomeScreen(
                onPhotos = { nav.navigate("photo") },
                onInterest = { nav.navigate("interest") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("interest") {
            InterestScreen(onBack = { nav.popBackStack() })
        }
        composable("photo") {
            PhotoHomeScreen(
                onBack = { nav.popBackStack() },
                onTakePhoto = { nav.navigate("size") },
                onHistory = { nav.navigate("history") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("size") {
            SizeScreen(
                onBack = { nav.popBackStack() },
                onPick = { size -> nav.navigate("camera/${size.name}") },
            )
        }
        composable("camera/{size}") { entry ->
            val size = sizeArg(entry.arguments?.getString("size"))
            CameraScreen(
                size = size,
                onBack = { nav.popBackStack() },
                onCaptured = { nav.navigate("adjust/${size.name}") },
            )
        }
        composable("adjust/{size}") { entry ->
            val size = sizeArg(entry.arguments?.getString("size"))
            AdjustScreen(
                size = size,
                onRetake = { nav.popBackStack() },
                onMissingPhoto = { nav.popBackStack("photo", inclusive = false) },
                onSaved = { jobId ->
                    // Photo is saved; "back" from the print screen now returns to the photo start screen.
                    nav.navigate("sheet/$jobId") { popUpTo("photo") }
                },
            )
        }
        composable("sheet/{id}") { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            SheetScreen(
                jobId = id,
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack("photo", inclusive = false) },
                onEdit = { nav.navigate("edit/$id") },
            )
        }
        composable("edit/{id}") { entry ->
            EditPhotoScreen(
                jobId = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
                onMissingPhoto = { nav.popBackStack("photo", inclusive = false) },
                // The screen underneath reloads the photo from disk, so the change shows up there.
                onSaved = { nav.popBackStack() },
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate("sheet/$id") },
                onEdit = { id -> nav.navigate("edit/$id") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
