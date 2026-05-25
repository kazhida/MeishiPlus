package com.abplus.meishiplus.di

import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.data.repositories.firestore.FireStoreCardRepository
import com.abplus.meishiplus.data.repositories.firestore.FireStoreUserRepository
import com.abplus.meishiplus.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
    ): UserRepository =
        FireStoreUserRepository(firestore)

    @Provides
    @Singleton
    fun provideCardRepository(
        firestore: FirebaseFirestore,
    ): CardRepository =
        FireStoreCardRepository(firestore)

    @Provides
    @Singleton
    fun provideUserViewModel(
        userRepository: UserRepository,
        cardRepository: CardRepository,
    ): UserViewModel =
        UserViewModel(
            userRepository = userRepository,
            cardRepository = cardRepository,
        )
}
