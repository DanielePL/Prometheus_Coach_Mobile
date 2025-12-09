package com.prometheuscoach.mobile.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable
    data object Auth : NavRoute

    @Serializable
    data object Dashboard : NavRoute

    @Serializable
    data object Clients : NavRoute

    @Serializable
    data class ClientDetail(val clientId: String) : NavRoute

    @Serializable
    data object Calendar : NavRoute

    @Serializable
    data object Inbox : NavRoute

    @Serializable
    data object Routines : NavRoute

    @Serializable
    data class RoutineDetail(val routineId: String) : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object Trends : NavRoute
}
