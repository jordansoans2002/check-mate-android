package com.man_behind.checkmate.di.modules

import com.man_behind.checkmate.data.repository.ChecklistRepository
import com.man_behind.checkmate.data.repository.ChecklistRepositoryImpl
import com.man_behind.checkmate.data.repository.QuestionSetRepository
import com.man_behind.checkmate.data.repository.QuestionSetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuestionSetRepository(
        questionSetRepositoryImpl: QuestionSetRepositoryImpl
    ): QuestionSetRepository

    @Binds
    @Singleton
    abstract fun bindChecklistRepository(
        checklistRepositoryImpl: ChecklistRepositoryImpl
    ): ChecklistRepository
}