package com.blackfoxis.telros.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        simulateAppPreparation()
    }

    private fun simulateAppPreparation() {
        viewModelScope.launch {
            delay(1500) // Симуляция подготовки приложения
            _isReady.value = true
        }
    }
}