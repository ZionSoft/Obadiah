/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.biblereading.toolbar;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Spinner;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.misc.settings.SettingsActivity;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.readingprogress.ReadingProgressActivity;
import net.zionsoft.obadiah.search.SearchActivity;
import net.zionsoft.obadiah.translations.TranslationManagementActivity;

import java.util.ArrayList;
import java.util.List;

public class BibleReadingToolbar extends Toolbar implements ToolbarView,
        Toolbar.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {
    private ToolbarPresenter toolbarPresenter;
    private List<String> bookNames;

    public BibleReadingToolbar(Context context) {
        super(context);
        initialize();
    }

    public BibleReadingToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BibleReadingToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setTitle(R.string.app_name);
        inflateMenu(R.menu.menu_bible_reading);
        setOnMenuItemClickListener(this);
    }

    @Override
    public void onTranslationsLoaded(List<String> translations) {
        String currentTranslation = toolbarPresenter.loadCurrentTranslation();
        final int translationsCount = translations.size();
        int selected;
        for (selected = 0; selected < translationsCount; ++selected) {
            if (translations.get(selected).equals(currentTranslation)) {
                break;
            }
        }
        if (selected == translationsCount) {
            // the requested translation is not available, use the first one in the list
            // this might happen if the user opens a URL for a translation that hasn't been installed yet
            selected = 0;
            currentTranslation = translations.get(0);
            toolbarPresenter.saveCurrentTranslation(currentTranslation);
        }

        // appends "More" to end of list that is to be shown in spinner
        final List<String> names = new ArrayList<>(translationsCount + 1);
        names.addAll(translations);
        names.add(getResources().getString(R.string.text_more_translations));

        final Spinner translationsSpinner = (Spinner) MenuItemCompat.getActionView(
                getMenu().findItem(R.id.action_translations));
        translationsSpinner.setAdapter(new TranslationSpinnerAdapter(getContext(), toolbarPresenter, names));
        translationsSpinner.setSelection(selected);
        translationsSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onBookNamesLoaded(List<String> bookNames) {
        this.bookNames = bookNames;
        refresh(toolbarPresenter.loadCurrentBook());
    }

    private void refresh(int book) {
        if (bookNames != null) {
            setTitle(bookNames.get(book));
        }
    }

    @Override
    public void onReadingProgressUpdated(Verse.Index index) {
        refresh(index.book);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search: {
                final Context context = getContext();
                context.startActivity(SearchActivity.newStartReorderToTopIntent(context));
                return true;
            }
            case R.id.action_reading_progress: {
                final Context context = getContext();
                context.startActivity(ReadingProgressActivity.newStartIntent(context));
                return true;
            }
            case R.id.action_settings: {
                final Context context = getContext();
                context.startActivity(SettingsActivity.newStartIntent(context));
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final Adapter adapter = parent.getAdapter();
        if (position == adapter.getCount() - 1) {
            // last item ("More") selected, opens the translation management activity
            final Context context = getContext();
            context.startActivity(TranslationManagementActivity.newStartIntent(context));
            return;
        }

        String currentTranslation = toolbarPresenter.loadCurrentTranslation();
        final String selected = (String) adapter.getItem(position);
        if (TextUtils.isEmpty(selected) || selected.equals(currentTranslation)) {
            return;
        }

        currentTranslation = selected;
        toolbarPresenter.saveCurrentTranslation(currentTranslation);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    public void setPresenter(ToolbarPresenter toolbarPresenter) {
        this.toolbarPresenter = toolbarPresenter;
    }

    public void onResume() {
        toolbarPresenter.takeView(this);
        toolbarPresenter.loadTranslations();
        toolbarPresenter.loadBookNamesForCurrentTranslation();
    }

    public void onPause() {
        toolbarPresenter.dropView();
    }
}
