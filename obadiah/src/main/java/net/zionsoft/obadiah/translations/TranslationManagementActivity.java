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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;
import net.zionsoft.obadiah.ui.widget.ProgressDialog;

import javax.inject.Inject;

import butterknife.Bind;

public class TranslationManagementActivity extends BaseAppCompatActivity
        implements TranslationManagementView, SwipeRefreshLayout.OnRefreshListener,
        RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener,
        View.OnCreateContextMenuListener, Toolbar.OnMenuItemClickListener {
    private static final String KEY_MESSAGE_TYPE = "net.zionsoft.obadiah.translations.TranslationManagementActivity.KEY_MESSAGE_TYPE";

    public static Intent newStartReorderToTopIntent(Context context, String messageType) {
        final Intent startIntent = newStartIntent(context).putExtra(KEY_MESSAGE_TYPE, messageType);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // there's some horrible issue with FLAG_ACTIVITY_REORDER_TO_FRONT for KitKat and above
            // ref. https://code.google.com/p/android/issues/detail?id=63570
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        return startIntent;
    }

    public static Intent newStartIntent(Context context) {
        return new Intent(context, TranslationManagementActivity.class);
    }

    private static final int CONTEXT_MENU_ITEM_DELETE = 0;

    @Inject
    TranslationManagementPresenter translationManagementPresenter;

    @Inject
    Settings settings;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.swipe_container)
    SwipeRefreshLayout swipeContainer;

    @Bind(R.id.translation_list)
    RecyclerView translationList;

    @Bind(R.id.ad_view)
    AdView adView;

    private MenuItem removeAdsMenuItem;

    private TranslationListAdapter translationListAdapter;

    private ProgressDialog removeTranslationProgressDialog;
    private ProgressDialog downloadTranslationProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TranslationManagementComponentFragment.FRAGMENT_TAG) == null) {
            fm.beginTransaction()
                    .add(TranslationManagementComponentFragment.newInstance(),
                            TranslationManagementComponentFragment.FRAGMENT_TAG)
                    .commit();
        }

        initializeUi();
        checkDeepLink();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_translation_management);

        toolbar.setLogo(R.drawable.ic_action_bar);
        toolbar.setTitle(R.string.activity_manage_translation);

        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.menu_translation_management);
        removeAdsMenuItem = toolbar.getMenu().findItem(R.id.action_remove_ads);

        swipeContainer.setColorSchemeResources(R.color.dark_cyan, R.color.dark_lime, R.color.blue, R.color.dark_blue);
        swipeContainer.setOnRefreshListener(this);

        // workaround for https://code.google.com/p/android/issues/detail?id=77712
        swipeContainer.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        swipeContainer.setRefreshing(true);

        translationList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        translationList.addOnChildAttachStateChangeListener(this);
        initializeAdapter();
    }

    private void checkDeepLink() {
        final Intent startIntent = getIntent();
        final String messageType = startIntent.getStringExtra(KEY_MESSAGE_TYPE);
        if (TextUtils.isEmpty(messageType)) {
            return;
        }
        Analytics.trackEvent(Analytics.CATEGORY_NOTIFICATION, Analytics.NOTIFICATION_ACTION_OPENED, messageType);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof TranslationManagementComponentFragment) {
            ((TranslationManagementComponentFragment) fragment).getComponent().inject(this);

            final View rootView = getWindow().getDecorView();
            rootView.setBackgroundColor(settings.getBackgroundColor());
            rootView.setKeepScreenOn(settings.keepScreenOn());

            initializeAdapter();
        }
    }

    private void initializeAdapter() {
        if (translationList == null || settings == null || translationListAdapter != null) {
            // if the activity is recreated due to screen orientation change, the component fragment
            // is called before the UI is initialized, i.e. onAttachFragment() is called inside
            // super.onCreate()
            // therefore, we try to do the initialization in both places
            return;
        }

        translationListAdapter = new TranslationListAdapter(this, settings,
                translationManagementPresenter.loadCurrentTranslation());
        translationList.setAdapter(translationListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        adView.resume();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        translationManagementPresenter.takeView(this);
        translationManagementPresenter.loadAdsStatus();
        loadTranslations(true);
    }

    private void loadTranslations(final boolean forceRefresh) {
        translationList.setVisibility(View.GONE);
        translationManagementPresenter.loadTranslations(forceRefresh);
    }

    @Override
    protected void onPause() {
        adView.pause();
        translationManagementPresenter.dropView();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        adView.destroy();

        if (isFinishing()) {
            translationManagementPresenter.cleanup();
        }

        translationManagementPresenter.cancelRemoveTranslation();
        translationManagementPresenter.cancelFetchTranslation();
        dismissRemoveProgressDialog();
        dismissDownloadProgressDialog();

        super.onDestroy();
    }

    private void dismissRemoveProgressDialog() {
        if (removeTranslationProgressDialog != null) {
            removeTranslationProgressDialog.dismiss();
            removeTranslationProgressDialog = null;
        }
    }

    private void dismissDownloadProgressDialog() {
        if (downloadTranslationProgressDialog != null) {
            downloadTranslationProgressDialog.dismiss();
            downloadTranslationProgressDialog = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!translationManagementPresenter.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onTranslationLoaded(Translations translations) {
        swipeContainer.setRefreshing(false);
        AnimationHelper.fadeIn(translationList);

        translationListAdapter.setTranslations(translations);
        translationListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTranslationLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.dialog_retry_network,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadTranslations(true);
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
                    }
                }
        );
    }

    @Override
    public void onTranslationRemoved(TranslationInfo translation) {
        dismissRemoveProgressDialog();
        Toast.makeText(this, R.string.toast_translation_deleted, Toast.LENGTH_SHORT).show();
        loadTranslations(false);
    }

    @Override
    public void onTranslationRemovalFailed(final TranslationInfo translation) {
        dismissRemoveProgressDialog();

        DialogHelper.showDialog(this, true, R.string.dialog_translation_remove_failure_message,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeTranslation(translation);
                    }
                }, null);
    }

    private void removeTranslation(TranslationInfo translation) {
        removeTranslationProgressDialog = ProgressDialog.showIndeterminateProgressDialog(
                this, R.string.progress_dialog_translation_deleting);

        translationManagementPresenter.removeTranslation(translation);
    }

    @Override
    public void onTranslationDownloadProgressed(TranslationInfo translation, int progress) {
        getOrCreateDownloadDialog().setProgress(progress);
    }

    @NonNull
    private ProgressDialog getOrCreateDownloadDialog() {
        if (downloadTranslationProgressDialog == null) {
            downloadTranslationProgressDialog = ProgressDialog.showProgressDialog(
                    this, R.string.progress_dialog_translation_downloading, 100);
        }
        return downloadTranslationProgressDialog;
    }

    @Override
    public void onTranslationDownloaded(TranslationInfo translation) {
        dismissDownloadProgressDialog();

        Toast.makeText(this, R.string.toast_translation_downloaded, Toast.LENGTH_SHORT).show();
        loadTranslations(false);
    }

    @Override
    public void onTranslationDownloadFailed(final TranslationInfo translation) {
        dismissDownloadProgressDialog();

        DialogHelper.showDialog(this, true, R.string.dialog_retry_network,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        downloadTranslation(translation);
                    }
                }, null);
    }

    private void downloadTranslation(TranslationInfo translationInfo) {
        getOrCreateDownloadDialog();
        translationManagementPresenter.fetchTranslation(translationInfo);
    }

    @Override
    public void onRefresh() {
        loadTranslations(true);
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        view.setOnClickListener(this);
        view.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setOnClickListener(null);
        view.setOnCreateContextMenuListener(null);
    }

    @Override
    public void onClick(View v) {
        final TranslationListAdapter.TranslationViewHolder translationViewHolder = getTranslationViewHolder(v);
        if (translationViewHolder == null) {
            return;
        }
        if (translationViewHolder.isDownloaded()) {
            translationManagementPresenter.saveCurrentTranslation(translationViewHolder.getTranslationInfo());

            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
        } else {
            downloadTranslation(translationViewHolder.getTranslationInfo());
        }
    }

    @Nullable
    private TranslationListAdapter.TranslationViewHolder getTranslationViewHolder(View v) {
        final int position = translationList.getChildAdapterPosition(v);
        if (position == RecyclerView.NO_POSITION) {
            return null;
        }
        final RecyclerView.ViewHolder viewHolder = translationList.findViewHolderForAdapterPosition(position);
        if (!(viewHolder instanceof TranslationListAdapter.TranslationViewHolder)) {
            return null;
        }
        return (TranslationListAdapter.TranslationViewHolder) viewHolder;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final TranslationListAdapter.TranslationViewHolder translationViewHolder = getTranslationViewHolder(v);
        if (translationViewHolder == null) {
            return;
        }
        if (translationViewHolder.isDownloaded()) {
            menu.setHeaderTitle(translationViewHolder.getTranslationInfo().name);
            menu.add(Menu.NONE, CONTEXT_MENU_ITEM_DELETE, Menu.NONE, R.string.action_delete_translation)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case CONTEXT_MENU_ITEM_DELETE:
                                    DialogHelper.showDialog(TranslationManagementActivity.this, true,
                                            R.string.dialog_translation_delete_confirm_message,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    removeTranslation(translationViewHolder.getTranslationInfo());
                                                }
                                            }, null);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
        }
    }

    @Override
    public void showAds() {
        if (removeAdsMenuItem != null) {
            removeAdsMenuItem.setVisible(true);
        }

        adView.setVisibility(View.VISIBLE);
        adView.loadAd(new AdRequest.Builder()
                .addKeyword("bible").addKeyword("jesus").addKeyword("christian")
                .build());
    }

    @Override
    public void hideAds() {
        if (removeAdsMenuItem != null) {
            removeAdsMenuItem.setVisible(false);
        }

        adView.setVisibility(View.GONE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_ads:
                Analytics.trackEvent(Analytics.CATEGORY_UI, Analytics.UI_ACTION_BUTTON_CLICK, "remove_ads");
                translationManagementPresenter.removeAds(this);
                return true;
            default:
                return false;
        }
    }
}
