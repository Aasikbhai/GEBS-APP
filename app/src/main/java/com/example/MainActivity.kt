package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.CampusViewModel
import com.example.ui.UserSession
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.StudentPortalScreen
import com.example.ui.screens.TeacherPortalScreen
import com.example.ui.theme.CampusPulseTheme

const val ROUTE_LOGIN = "login"
const val ROUTE_STUDENT_PORTAL = "student_portal"
const val ROUTE_TEACHER_PORTAL = "teacher_portal"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusPulseTheme {
                CampusPulseApp()
            }
        }
    }
}

@Composable
fun CampusPulseApp(
    campusViewModel: CampusViewModel = viewModel()
) {
    val navController = rememberNavController()
    val session by campusViewModel.session.collectAsStateWithLifecycle()
    val loginError by campusViewModel.loginError.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        when (session) {
            is UserSession.LoggedOut -> {
                if (navController.currentDestination?.route != ROUTE_LOGIN) {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is UserSession.Student -> {
                if (navController.currentDestination?.route != ROUTE_STUDENT_PORTAL) {
                    navController.navigate(ROUTE_STUDENT_PORTAL) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is UserSession.Teacher -> {
                if (navController.currentDestination?.route != ROUTE_TEACHER_PORTAL) {
                    navController.navigate(ROUTE_TEACHER_PORTAL) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LOGIN
    ) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                loginError = loginError,
                onLoginSubmit = { user, pass ->
                    campusViewModel.login(user, pass)
                },
                onClearError = { campusViewModel.clearLoginError() },
                peerAddress = campusViewModel.peerAddress.collectAsStateWithLifecycle().value,
                onPeerAddressChange = { campusViewModel.setPeerAddress(it) },
                loginBusy = campusViewModel.loginBusy.collectAsStateWithLifecycle().value
            )
        }
        composable(ROUTE_STUDENT_PORTAL) {
            val studentSession = session as? UserSession.Student ?: UserSession.Student()
            StudentPortalScreen(
                session = studentSession,
                viewModel = campusViewModel,
                onLogout = { campusViewModel.logout() }
            )
        }
        composable(ROUTE_TEACHER_PORTAL) {
            val teacherSession = session as? UserSession.Teacher ?: UserSession.Teacher()
            TeacherPortalScreen(
                session = teacherSession,
                viewModel = campusViewModel,
                onLogout = { campusViewModel.logout() }
            )
        }
    }
}
