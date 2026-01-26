package host.senk.dosenk.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import host.senk.dosenk.data.local.DoSenkDatabase
import host.senk.dosenk.data.local.dao.UserDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Crear la Base de Datos Física
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoSenkDatabase {
        return Room.databaseBuilder(
            context,
            DoSenkDatabase::class.java,
            "dosenk_local.db" // Nombre del archivo en el cel
        ).build()
    }

    // Para no llamar a la db entera siempre
    @Provides
    fun provideUserDao(database: DoSenkDatabase): UserDao {
        return database.userDao()
    }
}