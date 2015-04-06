/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
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

package net.zionsoft.obadiah.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.TranslationInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class TranslationListAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TRANSLATION = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private static class TranslationInfoHolder {
        final TranslationInfo translationInfo;
        final SpannableStringBuilder title;
        final boolean downloaded;

        TranslationInfoHolder(TranslationInfo translationInfo, SpannableStringBuilder title, boolean downloaded) {
            this.translationInfo = translationInfo;
            this.title = title;
            this.downloaded = downloaded;
        }
    }

    @Inject
    Settings settings;

    private final LayoutInflater inflater;
    private final Resources resources;
    private final String currentTranslation;
    private final String downloadedTranslationsTitle;

    private final int textColor;
    private final float textSize;
    private final AbsoluteSizeSpan mediumSizeSpan;
    private final AbsoluteSizeSpan smallSizeSpan;

    private final List<String> sectionHeaders = new ArrayList<>();
    private final List<List<TranslationInfoHolder>> translations = new ArrayList<>();
    private int count = 0;

    public TranslationListAdapter(Context context, String currentTranslation) {
        App.get(context).getInjectionComponent().inject(this);

        inflater = LayoutInflater.from(context);
        resources = context.getResources();
        this.currentTranslation = currentTranslation;

        downloadedTranslationsTitle = resources.getString(R.string.text_downloaded_translations);

        textColor = settings.getTextColor();

        textSize = resources.getDimension(settings.getTextSize().textSize);
        mediumSizeSpan = new AbsoluteSizeSpan((int) textSize);
        smallSizeSpan = new AbsoluteSizeSpan(
                (int) resources.getDimension(settings.getTextSize().smallerTextSize));
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }
        for (List<TranslationInfoHolder> translations : this.translations) {
            position -= translations.size() + 1;
            if (position < 0) {
                return VIEW_TYPE_TRANSLATION;
            } else if (position == 0) {
                return VIEW_TYPE_HEADER;
            }
        }
        return -1;
    }

    @Override
    public Object getItem(int position) {
        int index = 0;
        if (position == 0) {
            return sectionHeaders.get(index);
        }
        for (List<TranslationInfoHolder> translations : this.translations) {
            --position;
            final int size = translations.size();
            if (position < size) {
                return translations.get(position);
            }

            position -= size;
            ++index;
            if (position == 0) {
                return sectionHeaders.get(index);
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                return getHeaderView(position, convertView, parent);
            case VIEW_TYPE_TRANSLATION:
                return getTranslationView(position, convertView, parent);
            default:
                return null;
        }
    }

    private View getHeaderView(int position, View convertView, ViewGroup parent) {
        final TextView textView = (TextView) (convertView == null
                ? inflater.inflate(R.layout.item_translation_section, parent, false) : convertView);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setText((String) getItem(position));
        return textView;
    }

    private View getTranslationView(int position, View convertView, ViewGroup parent) {
        final TextView textView = (TextView) (convertView == null
                ? inflater.inflate(R.layout.item_translation, parent, false) : convertView);

        final TranslationInfoHolder translation = (TranslationInfoHolder) getItem(position);
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                translation.translationInfo.shortName.equals(currentTranslation) ? R.drawable.ic_check : 0, 0);
        textView.setText(translation.title);
        textView.setTextColor(textColor);

        return textView;
    }

    public void setTranslations(@Nullable List<TranslationInfo> downloaded,
                                @Nullable List<TranslationInfo> available) {
        sectionHeaders.clear();
        translations.clear();
        count = 0;
        if (downloaded != null && downloaded.size() > 0) {
            final List<TranslationInfoHolder> translations
                    = new ArrayList<TranslationInfoHolder>(downloaded.size());
            for (TranslationInfo translationInfo : downloaded) {
                final SpannableStringBuilder text = new SpannableStringBuilder(translationInfo.name);
                text.setSpan(mediumSizeSpan, 0, translationInfo.name.length(), 0);

                translations.add(new TranslationInfoHolder(translationInfo, text, true));
            }
            sectionHeaders.add(downloadedTranslationsTitle);
            this.translations.add(translations);
            count = downloaded.size() + 1;
        }

        if (available != null) {
            for (TranslationInfo translationInfo : available) {
                final String language = new Locale(translationInfo.language.split("_")[0]).getDisplayLanguage();
                int index = 0;
                List<TranslationInfoHolder> translations = null;
                for (String sectionHeader : sectionHeaders) {
                    if (sectionHeader.equals(language)) {
                        translations = this.translations.get(index);
                        break;
                    }
                    ++index;
                }
                if (translations == null) {
                    translations = new ArrayList<TranslationInfoHolder>();
                    sectionHeaders.add(language);
                    this.translations.add(translations);
                    ++count;
                }

                final SpannableStringBuilder text = new SpannableStringBuilder(
                        resources.getString(R.string.text_available_translation_info,
                                translationInfo.name, translationInfo.size / 1024)
                );
                text.setSpan(mediumSizeSpan, 0, translationInfo.name.length(), 0);
                text.setSpan(smallSizeSpan, translationInfo.name.length(), text.length(), 0);

                translations.add(new TranslationInfoHolder(translationInfo, text, false));
                ++count;
            }
        }
    }

    @Nullable
    public Pair<TranslationInfo, Boolean> getTranslation(int position) {
        final Object item = getItem(position);
        if (!(item instanceof TranslationInfoHolder)) {
            return null;
        }
        final TranslationInfoHolder translation = (TranslationInfoHolder) item;
        return new Pair<TranslationInfo, Boolean>(translation.translationInfo, translation.downloaded);
    }
}
