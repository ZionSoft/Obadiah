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

package net.zionsoft.obadiah.translations;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.model.domain.Translations;
import net.zionsoft.obadiah.ui.widget.SectionHeader;

import java.util.ArrayList;
import java.util.Locale;

class TranslationListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TRANSLATION = 1;

    private static class TranslationInfoHolder {
        private final TranslationInfo translationInfo;
        private final SpannableStringBuilder title;
        private final boolean downloaded;

        private TranslationInfoHolder(TranslationInfo translationInfo, SpannableStringBuilder title, boolean downloaded) {
            this.translationInfo = translationInfo;
            this.title = title;
            this.downloaded = downloaded;
        }
    }

    static class TranslationViewHolder extends RecyclerView.ViewHolder {
        private TranslationInfo translationInfo;
        private boolean downloaded;

        private TranslationViewHolder(View itemView) {
            super(itemView);
        }

        public TranslationInfo getTranslationInfo() {
            return translationInfo;
        }

        public boolean isDownloaded() {
            return downloaded;
        }
    }

    private final LayoutInflater inflater;
    private final Resources resources;

    private final int textColor;
    private final float textSize;
    private final AbsoluteSizeSpan mediumSizeSpan;
    private final AbsoluteSizeSpan smallSizeSpan;

    private final String currentTranslation;
    private final ArrayList<String> sectionHeaders = new ArrayList<>();
    private final ArrayList<ArrayList<TranslationInfoHolder>> translationList = new ArrayList<>();
    private int count = 0;

    TranslationListAdapter(Context context, Settings settings, String currentTranslation) {
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.currentTranslation = currentTranslation;

        this.textColor = settings.getTextColor();
        this.textSize = resources.getDimension(settings.getTextSize().textSize);
        this.mediumSizeSpan = new AbsoluteSizeSpan((int) textSize);
        this.smallSizeSpan = new AbsoluteSizeSpan((int) resources.getDimension(settings.getTextSize().smallerTextSize));
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }

        final int size = translationList.size();
        for (int i = 0; i < size; ++i) {
            position -= translationList.get(i).size() + 1;
            if (position < 0) {
                return VIEW_TYPE_TRANSLATION;
            } else if (position == 0) {
                return VIEW_TYPE_HEADER;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                final SectionHeader header = (SectionHeader)
                        inflater.inflate(R.layout.item_translation_section, parent, false);
                header.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                return new RecyclerView.ViewHolder(header) {
                };
            case VIEW_TYPE_TRANSLATION:
                final TextView translation = (TextView)
                        inflater.inflate(R.layout.item_translation, parent, false);
                translation.setTextColor(textColor);
                return new TranslationViewHolder(translation);
            default:
                throw new IllegalStateException("Unknown view type - " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            // VIEW_TYPE_HEADER
            ((SectionHeader) holder.itemView).setHeaderText(sectionHeaders.get(0));
            return;
        }

        final int translationListSize = translationList.size();
        for (int i = 0; i < translationListSize; ++i) {
            final ArrayList<TranslationInfoHolder> translations = translationList.get(i);
            --position;
            final int size = translations.size();
            if (position < size) {
                // VIEW_TYPE_TRANSLATION
                final TextView textView = ((TextView) holder.itemView);
                final TranslationInfoHolder translation = translations.get(position);
                textView.setText(translations.get(position).title);
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                        translation.translationInfo.shortName.equals(currentTranslation) ? R.drawable.ic_check : 0, 0);

                final TranslationViewHolder viewHolder = (TranslationViewHolder) holder;
                viewHolder.translationInfo = translation.translationInfo;
                viewHolder.downloaded = translation.downloaded;

                return;
            }

            position -= size;
            if (position == 0) {
                // VIEW_TYPE_HEADER
                ((SectionHeader) holder.itemView).setHeaderText(sectionHeaders.get(i + 1));
                return;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    @Override
    public int getItemCount() {
        return count;
    }

    void setTranslations(Translations translations) {
        sectionHeaders.clear();
        translationList.clear();
        count = 0;

        final int downloaded = translations.downloaded.size();
        if (downloaded > 0) {
            final ArrayList<TranslationInfoHolder> downloadedTranslations = new ArrayList<>(downloaded);
            for (int i = 0; i < downloaded; ++i) {
                final TranslationInfo translationInfo = translations.downloaded.get(i);
                final SpannableStringBuilder text = new SpannableStringBuilder(translationInfo.name);
                text.setSpan(mediumSizeSpan, 0, translationInfo.name.length(), 0);
                downloadedTranslations.add(new TranslationInfoHolder(translationInfo, text, true));
            }

            sectionHeaders.add(resources.getString(R.string.text_downloaded_translations));
            translationList.add(downloadedTranslations);
            count = downloaded + 1;
        }

        final int available = translations.available.size();
        for (int i = 0; i < available; ++i) {
            final TranslationInfo translationInfo = translations.available.get(i);

            ArrayList<TranslationInfoHolder> availableTranslations = null;
            final String language = new Locale(translationInfo.language.split("_")[0]).getDisplayLanguage();
            final int sections = sectionHeaders.size();
            for (int j = 0; j < sections; ++j) {
                if (sectionHeaders.get(j).equals(language)) {
                    availableTranslations = translationList.get(j);
                    break;
                }
            }
            if (availableTranslations == null) {
                availableTranslations = new ArrayList<>();
                sectionHeaders.add(language);
                translationList.add(availableTranslations);
                ++count;
            }

            final SpannableStringBuilder text = new SpannableStringBuilder(
                    resources.getString(R.string.text_available_translation_info,
                            translationInfo.name, translationInfo.size / 1024)
            );
            text.setSpan(mediumSizeSpan, 0, translationInfo.name.length(), 0);
            text.setSpan(smallSizeSpan, translationInfo.name.length(), text.length(), 0);

            availableTranslations.add(new TranslationInfoHolder(translationInfo, text, false));
            ++count;
        }
    }
}
