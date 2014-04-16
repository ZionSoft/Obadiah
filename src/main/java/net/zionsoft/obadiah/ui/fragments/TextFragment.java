package net.zionsoft.obadiah.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Analytics;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.ui.adapters.VersePagerAdapter;

import java.util.List;

public class TextFragment extends Fragment implements VersePagerAdapter.Listener {
    public static interface Listener {
        public void onChapterSelected(int chapterIndex);
    }

    private Listener mListener;

    private VersePagerAdapter mVersePagerAdapter;
    private ViewPager mVerseViewPager;

    private ActionMode mActionMode;
    @SuppressWarnings("deprecation")
    private ClipboardManager mClipboardManager;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
        mListener = (Listener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_text, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mVersePagerAdapter = new VersePagerAdapter(getActivity(), this);

        mVerseViewPager = (ViewPager) view.findViewById(R.id.verse_view_pager);
        mVerseViewPager.setAdapter(mVersePagerAdapter);
        mVerseViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
                // do nothing
            }

            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                // do nothing
            }

            public void onPageSelected(int position) {
                if (mActionMode != null)
                    mActionMode.finish();

                if (mListener != null)
                    mListener.onChapterSelected(position);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mVersePagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        if (mActionMode != null)
            mActionMode.finish();

        super.onStop();
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    @Override
    public void onVersesSelectionChanged(boolean hasSelected) {
        if (hasSelected) {
            if (mActionMode != null)
                return;
            mActionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    actionMode.getMenuInflater().inflate(R.menu.menu_text_selection_context, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_copy:
                            Analytics.trackUICopy();

                            final Activity activity = getActivity();
                            if (mClipboardManager == null) {
                                // noinspection deprecation
                                mClipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            }
                            mClipboardManager.setText(buildText(mVersePagerAdapter.getSelectedVerses(mVerseViewPager.getCurrentItem())));
                            Toast.makeText(activity, R.string.toast_verses_copied, Toast.LENGTH_SHORT).show();
                            actionMode.finish();
                            return true;
                        case R.id.action_share:
                            Analytics.trackUIShare();

                            startActivity(Intent.createChooser(new Intent().setAction(Intent.ACTION_SEND).setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT,
                                                    buildText(mVersePagerAdapter.getSelectedVerses(mVerseViewPager.getCurrentItem()))),
                                    getResources().getText(R.string.text_share_with)
                            ));
                            actionMode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    if (actionMode != mActionMode)
                        return;
                    mVersePagerAdapter.deselectVerses();
                    mActionMode = null;
                }
            });
        } else {
            if (mActionMode != null)
                mActionMode.finish();
        }
    }

    private static String buildText(List<Verse> verses) {
        if (verses == null || verses.size() == 0)
            return null;

        // format: <book name> <chapter index>:<verse index> <verse text>
        final StringBuilder text = new StringBuilder();
        for (Verse verse : verses) {
            text.append(String.format("%S %d:%d %s\n", verse.bookName, verse.chapterIndex + 1,
                    verse.verseIndex + 1, verse.verseText));
        }
        return text.toString();
    }

    public int getCurrentVerse() {
        return mVersePagerAdapter.getCurrentVerse(mVerseViewPager.getCurrentItem());
    }

    public void setSelected(int currentBook, int currentChapter, int currentVerse) {
        mVersePagerAdapter.setSelected(currentBook, currentChapter, currentVerse);
        mVersePagerAdapter.notifyDataSetChanged();

        mVerseViewPager.setCurrentItem(currentChapter, true);
    }

    public void setSelected(String translationShortName, int currentBook,
                            int currentChapter, int currentVerse) {
        mVersePagerAdapter.setTranslationShortName(translationShortName);

        setSelected(currentBook, currentChapter, currentVerse);
    }
}
