package net.zionsoft.obadiah.injection;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgressManager;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.database.DatabaseHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public final class InjectionModule {
    private final App mApplication;

    public InjectionModule(App application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    public App provideApplication() {
        return mApplication;
    }

    @Provides
    @Singleton
    public Bible provideBible() {
        return new Bible(mApplication);
    }

    @Provides
    @Singleton
    public DatabaseHelper provideDatabaseHelper() {
        return new DatabaseHelper(mApplication);
    }

    @Provides
    @Singleton
    public ReadingProgressManager provideReadingProgressManager() {
        return new ReadingProgressManager(mApplication);
    }

    @Provides
    @Singleton
    public Settings provideSettings() {
        return new Settings(mApplication);
    }
}
