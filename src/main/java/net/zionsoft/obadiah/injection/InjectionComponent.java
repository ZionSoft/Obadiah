package net.zionsoft.obadiah.injection;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.ui.activities.ReadingProgressActivity;
import net.zionsoft.obadiah.ui.activities.SearchActivity;
import net.zionsoft.obadiah.ui.activities.SettingsActivity;
import net.zionsoft.obadiah.ui.activities.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.adapters.ReadingProgressListAdapter;
import net.zionsoft.obadiah.ui.adapters.SearchResultListAdapter;
import net.zionsoft.obadiah.ui.adapters.TranslationListAdapter;
import net.zionsoft.obadiah.ui.adapters.VerseListAdapter;
import net.zionsoft.obadiah.ui.adapters.VersePagerAdapter;
import net.zionsoft.obadiah.ui.fragments.ChapterSelectionFragment;
import net.zionsoft.obadiah.ui.fragments.TranslationListFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = InjectionModule.class)
public interface InjectionComponent {
    public void inject(App app);

    public void inject(BookSelectionActivity bookSelectionActivity);

    public void inject(ReadingProgressActivity readingProgressActivity);

    public void inject(SearchActivity searchActivity);

    public void inject(SettingsActivity settingsActivity);

    public void inject(TranslationManagementActivity translationManagementActivity);

    public void inject(ChapterSelectionFragment chapterSelectionFragment);

    public void inject(TranslationListFragment translationListFragment);

    public void inject(ReadingProgressListAdapter readingProgressListAdapter);

    public void inject(SearchResultListAdapter searchResultListAdapter);

    public void inject(TranslationListAdapter translationListAdapter);

    public void inject(VerseListAdapter verseListAdapter);

    public void inject(VersePagerAdapter versePagerAdapter);
}
