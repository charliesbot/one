package com.charliesbot.one.features.profile.di

import com.charliesbot.one.features.profile.YouViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module { viewModelOf(::YouViewModel) }
