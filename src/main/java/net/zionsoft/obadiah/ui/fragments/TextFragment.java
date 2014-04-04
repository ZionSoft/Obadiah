package net.zionsoft.obadiah.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.ui.adapters.VersePagerAdapter;

public class TextFragment extends Fragment {
    public static interface Listener {
        public void onChapterSelected(int chapterIndex);
    }

    private Listener mListener;

    private VersePagerAdapter mVersePagerAdapter;
    private ViewPager mVerseViewPager;

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

        mVersePagerAdapter = new VersePagerAdapter(getActivity());

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
    public void onDetach() {
        mListener = null;

        super.onDetach();
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
