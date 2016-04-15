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

package net.zionsoft.obadiah.translations;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.ui.utils.BaseSectionAdapter;

import java.util.ArrayList;
import java.util.Locale;

class TranslationListAdapter extends BaseSectionAdapter<TranslationListAdapter.TranslationInfoHolder> {
    public static class TranslationInfoHolder {
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

        TranslationInfo getTranslationInfo() {
            return translationInfo;
        }

        boolean isDownloaded() {
            return downloaded;
        }
    }

    private final Resources resources;
    private final AbsoluteSizeSpan mediumSizeSpan;
    private final AbsoluteSizeSpan smallSizeSpan;

    private String currentTranslation;

    TranslationListAdapter(Context context, Settings settings) {
        super(context, settings);
        this.resources = context.getResources();
        this.mediumSizeSpan = new AbsoluteSizeSpan((int) textSize);
        this.smallSizeSpan = new AbsoluteSizeSpan((int) smallerTextSize);
    }

    @Override
    protected RecyclerView.ViewHolder createItemViewHolder(ViewGroup parent) {
        final TextView translation = (TextView)
                inflater.inflate(R.layout.item_translation, parent, false);
        translation.setTextColor(textColor);
        return new TranslationViewHolder(translation);
    }

    @Override
    protected void bindItemViewHeader(RecyclerView.ViewHolder holder, TranslationInfoHolder item) {
        final TextView textView = ((TextView) holder.itemView);
        textView.setText(item.title);
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                item.translationInfo.shortName().equals(currentTranslation) ? R.drawable.ic_check : 0, 0);

        final TranslationViewHolder viewHolder = (TranslationViewHolder) holder;
        viewHolder.translationInfo = item.translationInfo;
        viewHolder.downloaded = item.downloaded;
    }

    void setTranslations(Translations translations, String currentTranslation) {
        this.currentTranslation = currentTranslation;
        final ArrayList<String> headers = new ArrayList<>();
        final ArrayList<ArrayList<TranslationInfoHolder>> translationList = new ArrayList<>();
        int count = 0;

        final int downloaded = translations.downloaded.size();
        if (downloaded > 0) {
            final ArrayList<TranslationInfoHolder> downloadedTranslations = new ArrayList<>(downloaded);
            for (int i = 0; i < downloaded; ++i) {
                final TranslationInfo translationInfo = translations.downloaded.get(i);
                final SpannableStringBuilder text = new SpannableStringBuilder(translationInfo.name());
                text.setSpan(mediumSizeSpan, 0, translationInfo.name().length(), 0);
                downloadedTranslations.add(new TranslationInfoHolder(translationInfo, text, true));
            }

            headers.add(resources.getString(R.string.text_downloaded_translations));
            translationList.add(downloadedTranslations);
            count = downloaded + 1;
        }

        final int available = translations.available.size();
        for (int i = 0; i < available; ++i) {
            final TranslationInfo translationInfo = translations.available.get(i);

            ArrayList<TranslationInfoHolder> availableTranslations = null;
            final String language = new Locale(translationInfo.language().split("_")[0]).getDisplayLanguage();
            final int sections = headers.size();
            for (int j = 0; j < sections; ++j) {
                if (headers.get(j).equals(language)) {
                    availableTranslations = translationList.get(j);
                    break;
                }
            }
            if (availableTranslations == null) {
                availableTranslations = new ArrayList<>();
                headers.add(language);
                translationList.add(availableTranslations);
                ++count;
            }

            final SpannableStringBuilder text = new SpannableStringBuilder(
                    resources.getString(R.string.text_available_translation_info,
                            translationInfo.name(), translationInfo.size() / 1024)
            );
            text.setSpan(mediumSizeSpan, 0, translationInfo.name().length(), 0);
            text.setSpan(smallSizeSpan, translationInfo.name().length(), text.length(), 0);

            availableTranslations.add(new TranslationInfoHolder(translationInfo, text, false));
            ++count;
        }

        setData(headers, translationList, count);
    }
}
