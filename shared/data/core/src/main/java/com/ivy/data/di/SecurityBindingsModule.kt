package com.ivy.data.di

import com.ivy.data.security.AndroidKeystoreCardSecretsCipher
import com.ivy.data.security.CardSecretsCipher
import com.ivy.data.security.CardSecretsStore
import com.ivy.data.security.SharedPrefsCardSecretsStore
import com.ivy.data.skin.CardSkinImageStore
import com.ivy.data.skin.FilesDirCardSkinImageStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SecurityBindingsModule {
    @Binds
    fun bindCardSecretsCipher(impl: AndroidKeystoreCardSecretsCipher): CardSecretsCipher

    @Binds
    fun bindCardSecretsStore(impl: SharedPrefsCardSecretsStore): CardSecretsStore

    @Binds
    fun bindCardSkinImageStore(impl: FilesDirCardSkinImageStore): CardSkinImageStore
}
