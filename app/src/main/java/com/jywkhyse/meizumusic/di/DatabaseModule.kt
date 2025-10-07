// 文件: /app/src/main/java/com/jywkhyse/meizumusic/di/DatabaseModule.kt
package com.jywkhyse.meizumusic.di

import android.content.Context
import androidx.room.Room
import com.jywkhyse.meizumusic.database.AppDatabase
import com.jywkhyse.meizumusic.database.SongUserDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "meizu_music.db"
        ).fallbackToDestructiveMigration() // <--- 添加这个，允许在升级时销毁旧数据
            .build()
    }

    @Provides
    @Singleton
    fun provideSongUserDataDao(appDatabase: AppDatabase): SongUserDataDao {
        return appDatabase.songUserDataDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(appDatabase: AppDatabase) = appDatabase.playlistDao() // <--- 新增
}