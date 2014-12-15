package net.zionsoft.obadiah.injection;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = InjectionModule.class)
public interface InjectionComponent {
}
