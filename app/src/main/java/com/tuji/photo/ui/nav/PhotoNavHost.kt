package com.tuji.photo.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuji.photo.data.model.PendingTemplate
import com.tuji.photo.data.model.TemplateCatalog
import com.tuji.photo.ui.editor.EditorScreen
import com.tuji.photo.ui.home.HomeScreen
import com.tuji.photo.ui.mine.MineScreen
import com.tuji.photo.ui.recent.RecentScreen
import com.tuji.photo.ui.settings.SettingsScreen
import com.tuji.photo.ui.templates.TemplatePreviewScreen
import com.tuji.photo.ui.templates.TemplatesScreen

object Routes {
    const val HOME = "home"
    const val TEMPLATES = "templates"
    const val TEMPLATE_PREVIEW = "templates/preview?templateId={templateId}"
    const val MINE = "mine"
    const val SETTINGS = "settings"
    const val RECENT = "recent"
    const val EDITOR = "editor?projectId={projectId}&imageUri={imageUri}"

    fun editor(projectId: Long, imageUri: String) =
        "editor?projectId=$projectId&imageUri=${android.net.Uri.encode(imageUri)}"

    fun templatePreview(templateId: String) = "templates/preview?templateId=$templateId"
}

@Composable
fun PhotoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenEditor = { projectId, imageUri ->
                    navController.navigate(Routes.editor(projectId, imageUri))
                },
                onOpenTemplates = { navController.navigate(Routes.TEMPLATES) },
                onOpenMine = { navController.navigate(Routes.MINE) },
                onOpenRecent = { navController.navigate(Routes.RECENT) },
            )
        }
        composable(Routes.TEMPLATES) {
            TemplatesScreen(
                onBack = { navController.popBackStack() },
                onOpenPreview = { id -> navController.navigate(Routes.templatePreview(id)) },
                onOpenMine = { navController.navigate(Routes.MINE) },
                onOpenRecent = { navController.navigate(Routes.RECENT) },
            )
        }
        composable(
            Routes.TEMPLATE_PREVIEW,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val template = TemplateCatalog.byId(entry.arguments?.getString("templateId").orEmpty())
            if (template != null) {
                TemplatePreviewScreen(
                    template = template,
                    onBack = { navController.popBackStack() },
                    onUse = {
                        // 使用模板：暂存模板，回到图库选图后自动套用
                        PendingTemplate.template = template
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }
        }
        composable(Routes.RECENT) {
            RecentScreen(
                onOpenEditor = { projectId, imageUri ->
                    navController.navigate(Routes.editor(projectId, imageUri))
                },
                onOpenTemplates = { navController.navigate(Routes.TEMPLATES) },
                onOpenMine = { navController.navigate(Routes.MINE) },
            )
        }
        composable(Routes.MINE) {            MineScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenTemplates = { navController.navigate(Routes.TEMPLATES) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("imageUri") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            EditorScreen(onClose = { navController.popBackStack() })
        }
    }
}
