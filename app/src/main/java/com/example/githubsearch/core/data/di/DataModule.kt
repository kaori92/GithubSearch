package com.example.githubsearch.core.data.di

import com.example.githubsearch.core.data.DefaultGithubSearchRepository
import com.example.githubsearch.core.data.GithubSearchRepository
import com.example.githubsearch.core.data.LogcatSearchFailureLogger
import com.example.githubsearch.core.data.SearchFailureLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGithubSearchRepository(
        impl: DefaultGithubSearchRepository,
    ): GithubSearchRepository

    @Binds
    @Singleton
    abstract fun bindSearchFailureLogger(impl: LogcatSearchFailureLogger): SearchFailureLogger
}
