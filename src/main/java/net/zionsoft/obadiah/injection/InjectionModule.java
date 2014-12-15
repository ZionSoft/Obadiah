package net.zionsoft.obadiah.injection;

import net.zionsoft.obadiah.App;

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
}
