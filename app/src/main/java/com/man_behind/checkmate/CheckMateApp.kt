package com.man_behind.checkmate

import android.app.Application
import com.man_behind.checkmate.data.repository.QuestionSetRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CheckMateApp: Application() {

    @Inject
    lateinit var questionSetRepository: QuestionSetRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            questionSetRepository.loadDefaultQuestionSetIfNeeded()
        }
    }
}